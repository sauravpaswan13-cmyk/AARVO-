import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import pg from 'pg';

const { Pool } = pg;
const databaseUrl = process.env.DATABASE_URL;
if (!databaseUrl) throw new Error('DATABASE_URL is required for migrations');

const pool = new Pool({
  connectionString: databaseUrl,
  ssl: process.env.DATABASE_SSL === 'true' ? { rejectUnauthorized: false } : undefined,
});

// migrate.js lives in /app/src in the production container.
// Resolve paths from this file rather than relying on the process working directory.
const srcDir = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(srcDir, '..');
const schemaPath = path.join(root, 'schema.sql');
const migrationDir = path.join(root, 'migrations');

const schema = await fs.readFile(schemaPath, 'utf8');
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
