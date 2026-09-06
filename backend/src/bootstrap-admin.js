import crypto from 'node:crypto';
import pg from 'pg';

const { Pool } = pg;

const databaseUrl = process.env.DATABASE_URL;
const rawAdminEmail = process.env.ADMIN_EMAIL ?? '';
// Render env values can occasionally contain accidental surrounding quotes.
const adminEmail = rawAdminEmail.trim().replace(/^(\["'])(.*)\1$/, '$2').trim().toLowerCase();
const adminPassword = process.env.ADMIN_PASSWORD;
const displayName = process.env.ADMIN_DISPLAY_NAME?.trim() || 'AARVO Admin';

console.log('[admin-bootstrap] starting');
console.log('[admin-bootstrap] env diagnostics:', JSON.stringify({
  ADMIN_EMAIL_present: Object.prototype.hasOwnProperty.call(process.env, 'ADMIN_EMAIL'),
  ADMIN_EMAIL_length: rawAdminEmail.length,
  ADMIN_EMAIL_trimmed_length: rawAdminEmail.trim().length,
  ADMIN_PASSWORD_present: Boolean(adminPassword),
  ADMIN_PASSWORD_length: adminPassword?.length ?? 0,
  DATABASE_URL_present: Boolean(databaseUrl),
  ADMIN_DISPLAY_NAME_present: Object.prototype.hasOwnProperty.call(process.env, 'ADMIN_DISPLAY_NAME'),
  NODE_ENV: process.env.NODE_ENV ?? null,
}));

if (!databaseUrl) throw new Error('DATABASE_URL is required');
if (!adminEmail || !/^\S+@\S+\.\S+$/.test(adminEmail)) {
  if (!rawAdminEmail) {
    console.error('[admin-bootstrap] ADMIN_EMAIL is missing or empty inside the container.');
  } else {
    console.error('[admin-bootstrap] ADMIN_EMAIL format validation failed.');
  }
  throw new Error('ADMIN_EMAIL must be a valid email address');
}
if (!adminPassword || adminPassword.length < 12) {
  if (!adminPassword) {
    console.error('[admin-bootstrap] ADMIN_PASSWORD is missing or empty inside the container.');
  } else {
    console.error('[admin-bootstrap] ADMIN_PASSWORD is present but shorter than 12 characters.');
  }
  throw new Error('ADMIN_PASSWORD must be at least 12 characters');
}

const pool = new Pool({
  connectionString: databaseUrl,
  ssl: process.env.DATABASE_SSL === 'true' ? { rejectUnauthorized: false } : undefined,
  connectionTimeoutMillis: 10000,
  query_timeout: 15000,
  idleTimeoutMillis: 10000,
});

const hashPassword = async (password) => {
  const salt = crypto.randomBytes(16).toString('hex');
  const derivedKey = await new Promise((resolve, reject) => {
    crypto.scrypt(password, salt, 64, (error, key) => {
      if (error) reject(error);
      else resolve(key);
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

    if (existingAdmin.rowCount > 0) {
      await client.query('COMMIT');
      console.log(`Admin already exists (${existingAdmin.rows[0].email}); no changes made.`);
      return;
    }

    const existingUser = await client.query(
      'SELECT id, role FROM users WHERE email = $1 LIMIT 1',
      [adminEmail],
    );

    if (existingUser.rowCount > 0) {
      throw new Error(`ADMIN_EMAIL is already registered with role ${existingUser.rows[0].role}`);
    }

    const passwordHash = await hashPassword(adminPassword);
    const id = crypto.randomUUID();

    await client.query(
      `INSERT INTO users (id, email, display_name, password_hash, role)
       VALUES ($1, $2, $3, $4, 'ADMIN')`,
      [id, adminEmail, displayName, passwordHash],
    );

    await client.query(
      `INSERT INTO audit_events (actor_id, actor_role, entity_type, entity_id, action, metadata)
       VALUES ($1, 'ADMIN', 'USER', $1, 'ADMIN_BOOTSTRAP', jsonb_build_object('email', $2::text))`,
      [id, adminEmail],
    );

    await client.query('COMMIT');
    console.log(`Admin account created: ${adminEmail}`);
  } catch (error) {
    try { await client.query('ROLLBACK'); } catch {}
    console.error('[admin-bootstrap] FAILED:', error?.stack || error);
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
