import crypto from 'node:crypto';
import pg from 'pg';

const { Pool } = pg;

const databaseUrl = process.env.DATABASE_URL;
const adminEmail = process.env.ADMIN_EMAIL?.trim().toLowerCase();
const adminPassword = process.env.ADMIN_PASSWORD;
const displayName = process.env.ADMIN_DISPLAY_NAME?.trim() || 'AARVO Admin';

if (!databaseUrl) throw new Error('DATABASE_URL is required');
if (!adminEmail || !/^\S+@\S+\.\S+$/.test(adminEmail)) {
  throw new Error('ADMIN_EMAIL must be a valid email address');
}
if (!adminPassword || adminPassword.length < 12) {
  throw new Error('ADMIN_PASSWORD must be at least 12 characters');
}

const pool = new Pool({
  connectionString: databaseUrl,
  ssl: process.env.DATABASE_SSL === 'true' ? { rejectUnauthorized: false } : undefined,
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

const client = await pool.connect();
try {
  await client.query('BEGIN');

  const existingAdmin = await client.query(
    "SELECT id, email FROM users WHERE role = 'ADMIN' LIMIT 1",
  );

  if (existingAdmin.rowCount > 0) {
    await client.query('COMMIT');
    console.log(`Admin already exists (${existingAdmin.rows[0].email}); no changes made.`);
    process.exitCode = 0;
  } else {
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
       VALUES ($1, 'ADMIN', 'USER', $1, 'ADMIN_BOOTSTRAP', jsonb_build_object('email', $2))`,
      [id, adminEmail],
    );

    await client.query('COMMIT');
    console.log(`Admin account created: ${adminEmail}`);
    process.exitCode = 0;
  }
} catch (error) {
  await client.query('ROLLBACK');
  throw error;
} finally {
  client.release();
  await pool.end();
}
