import fs from 'node:fs/promises';
import path from 'node:path';
import pg from 'pg';

const { Pool } = pg;
const databaseUrl = process.env.DATABASE_URL;
if (!databaseUrl) throw new Error('DATABASE_URL is required for migrations');

const pool = new Pool({
  connectionString: databaseUrl,
  ssl: process.env.DATABASE_SSL === 'true' ? { rejectUnauthorized: false } : undefined,
});

const root = path.resolve(new URL('..', import.meta.url).pathname, '..');
const migrationDir = path.join(root, 'migrations');
const schema = await fs.readFile(path.join(root, 'schema.sql'), 'utf8');
const migrations = (await fs.readdir(migrationDir))
  .filter((name) => /^\d+_.*\.sql$/.test(name))
  .sort();

const client = await pool.connect();
try {
  await client.query('BEGIN');
  await client.query(schema);
  for (const file of migrations) {
    const sql = await fs.readFile(path.join(migrationDir, file), 'utf8');
    await client.query(sql);
    console.log(`Applied ${file}`);
  }
  await client.query('COMMIT');
  console.log(`Database ready: schema + ${migrations.length} migrations`);
} catch (error) {
  await client.query('ROLLBACK');
  throw error;
} finally {
  client.release();
  await pool.end();
}
