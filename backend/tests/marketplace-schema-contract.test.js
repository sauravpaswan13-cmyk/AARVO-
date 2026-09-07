import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
const read = (relativePath) => fs.readFileSync(path.join(ROOT, relativePath), 'utf8');

const schema = read('backend/schema.sql');
const migration = read('backend/migrations/001_marketplace_completion_integrity.sql');
const server = read('backend/src/server.js');

test('marketplace schema matches the auth/order/review/audit server contracts', () => {
  assert.match(schema, /CREATE TABLE IF NOT EXISTS phone_verification_challenges/);
  assert.match(schema, /CREATE TABLE IF NOT EXISTS orders/);
  assert.match(schema, /CREATE TABLE IF NOT EXISTS order_lines/);
  assert.match(schema, /CREATE TABLE IF NOT EXISTS seller_ledger/);

  assert.match(migration, /ADD COLUMN IF NOT EXISTS otp_hash TEXT/);
  assert.match(migration, /ADD COLUMN IF NOT EXISTS verified_at TIMESTAMPTZ/);
  assert.match(migration, /ALTER COLUMN code_hash DROP NOT NULL/);
  assert.match(migration, /ALTER COLUMN id SET DEFAULT md5\(random\(\)::text \|\| clock_timestamp\(\)::text\)::uuid/);
  assert.match(migration, /CREATE TABLE IF NOT EXISTS product_images/);
  assert.match(migration, /CREATE TABLE IF NOT EXISTS product_reviews/);
  assert.match(migration, /CREATE TABLE IF NOT EXISTS audit_events/);
  assert.match(migration, /CREATE TABLE IF NOT EXISTS buyer_addresses/);

  assert.match(server, /phone_verification_challenges/);
  assert.match(server, /otp_hash/);
  assert.match(server, /verified_at/);
  assert.match(server, /product_reviews/);
  assert.match(server, /audit_events/);
  assert.match(server, /seller_ledger/);
  assert.match(server, /idempotency-key/);
});
