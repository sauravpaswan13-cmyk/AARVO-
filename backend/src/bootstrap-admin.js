import crypto from 'node:crypto';
import pg from 'pg';

const { Pool } = pg;

const databaseUrl = process.env.DATABASE_URL;
const rawAdminEmail = process.env.ADMIN_EMAIL ?? '';
const adminEmail = rawAdminEmail
  .trim()
  .replace(/^(['"])(.*)\1$/, '$2')
  .trim()
  .toLowerCase();

const adminPassword = process.env.ADMIN_PASSWORD;
const displayName = process.env.ADMIN_DISPLAY_NAME?.trim() || 'AARVO Admin';

// IMPORTANT:
// Set ADMIN_FORCE_RESET=true only when you intentionally want to reset
// the password of the existing ADMIN account.
const forceReset = process.env.ADMIN_FORCE_RESET === 'true';

console.log('[admin-bootstrap] starting');
console.log('[admin-bootstrap] env diagnostics:', JSON.stringify({
  ADMIN_EMAIL_present: Object.prototype.hasOwnProperty.call(process.env, 'ADMIN_EMAIL'),
  ADMIN_EMAIL_length: rawAdminEmail.length,
  ADMIN_EMAIL_trimmed_length: rawAdminEmail.trim().length,
  ADMIN_PASSWORD_present: Boolean(adminPassword),
  ADMIN_PASSWORD_length: adminPassword?.length ?? 0,
  ADMIN_FORCE_RESET: forceReset,
  DATABASE_URL_present: Boolean(databaseUrl),
  ADMIN_DISPLAY_NAME_present:
    Object.prototype.hasOwnProperty.call(process.env, 'ADMIN_DISPLAY_NAME'),
  NODE_ENV: process.env.NODE_ENV ?? null,
}));

if (!databaseUrl) {
  throw new Error('DATABASE_URL is required');
}

if (!adminEmail || !/^\S+@\S+\.\S+$/.test(adminEmail)) {
  if (!rawAdminEmail) {
    console.error(
      '[admin-bootstrap] ADMIN_EMAIL is missing or empty inside the container.',
    );
  } else {
    console.error(
      '[admin-bootstrap] ADMIN_EMAIL format validation failed.',
    );
  }

  throw new Error('ADMIN_EMAIL must be a valid email address');
}

if (!adminPassword || adminPassword.length < 12) {
  if (!adminPassword) {
    console.error(
      '[admin-bootstrap] ADMIN_PASSWORD is missing or empty inside the container.',
    );
  } else {
    console.error(
      '[admin-bootstrap] ADMIN_PASSWORD is present but shorter than 12 characters.',
    );
  }

  throw new Error('ADMIN_PASSWORD must be at least 12 characters');
}

const pool = new Pool({
  connectionString: databaseUrl,
  ssl:
    process.env.DATABASE_SSL === 'true'
      ? { rejectUnauthorized: false }
      : undefined,
  connectionTimeoutMillis: 10000,
  query_timeout: 15000,
  idleTimeoutMillis: 10000,
});

const hashPassword = async (password) => {
  const salt = crypto.randomBytes(16).toString('hex');

  const derivedKey = await new Promise((resolve, reject) => {
    crypto.scrypt(password, salt, 64, (error, key) => {
      if (error) {
        reject(error);
      } else {
        resolve(key);
      }
    });
  });

  return `scrypt:${salt}:${derivedKey.toString('hex')}`;
};

const run = async () => {
  const client = await pool.connect();

  try {
    console.log('[admin-bootstrap] database connection established');

    await client.query('BEGIN');

    const existingAdmin = await client.query(
      "SELECT id, email FROM users WHERE role = 'ADMIN' LIMIT 1",
    );

    // Existing ADMIN found.
    if (existingAdmin.rowCount > 0) {
      const existingAdminId = existingAdmin.rows[0].id;
      const existingAdminEmail = existingAdmin.rows[0].email;

      // Only reset the password when explicitly requested.
      if (forceReset) {
        console.log(
          `[admin-bootstrap] resetting existing admin password for ${adminEmail}`,
        );

        const passwordHash = await hashPassword(adminPassword);

        await client.query(
          `UPDATE users
           SET email = $1,
               display_name = $2,
               password_hash = $3
           WHERE id = $4`,
          [
            adminEmail,
            displayName,
            passwordHash,
            existingAdminId,
          ],
        );

        await client.query(
          `INSERT INTO audit_events
             (actor_id, actor_role, entity_type, entity_id, action, metadata)
           VALUES
             ($1, 'ADMIN', 'USER', $1, 'ADMIN_PASSWORD_RESET',
              jsonb_build_object(
                'email', $2::text,
                'previous_email', $3::text
              ))`,
          [
            existingAdminId,
            adminEmail,
            existingAdminEmail,
          ],
        );

        await client.query('COMMIT');

        console.log(
          `[admin-bootstrap] admin password reset successfully: ${adminEmail}`,
        );

        return;
      }

      // Normal startup: do not modify an existing admin.
      await client.query('COMMIT');

      console.log(
        `Admin already exists (${existingAdminEmail}); no changes made.`,
      );

      return;
    }

    // No ADMIN exists, so create the first one.
    const existingUser = await client.query(
      'SELECT id, role FROM users WHERE email = $1 LIMIT 1',
      [adminEmail],
    );

    if (existingUser.rowCount > 0) {
      throw new Error(
        `ADMIN_EMAIL is already registered with role ${existingUser.rows[0].role}`,
      );
    }

    const passwordHash = await hashPassword(adminPassword);
    const id = crypto.randomUUID();

    await client.query(
      `INSERT INTO users
         (id, email, display_name, password_hash, role)
       VALUES
         ($1, $2, $3, $4, 'ADMIN')`,
      [
        id,
        adminEmail,
        displayName,
        passwordHash,
      ],
    );

    await client.query(
      `INSERT INTO audit_events
         (actor_id, actor_role, entity_type, entity_id, action, metadata)
       VALUES
         ($1, 'ADMIN', 'USER', $1, 'ADMIN_BOOTSTRAP',
          jsonb_build_object('email', $2::text))`,
      [
        id,
        adminEmail,
      ],
    );

    await client.query('COMMIT');

    console.log(`Admin account created: ${adminEmail}`);
  } catch (error) {
    try {
      await client.query('ROLLBACK');
    } catch {}

    console.error(
      '[admin-bootstrap] FAILED:',
      error?.stack || error,
    );

    throw error;
  } finally {
    client.release();
  }
};

try {
  await run();
  console.log('[admin-bootstrap] completed successfully');
} finally {
  await pool.end();
}
