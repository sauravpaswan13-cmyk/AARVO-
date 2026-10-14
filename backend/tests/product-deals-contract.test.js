import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = (name) => fs.readFileSync(path.join(ROOT, name), 'utf8');
const deals = read('src/product-deals.js');
const launcher = read('src/launcher.js');
const server = read('src/server.js');
const migration = read('migrations/025_product_deals.sql');

test('product deals require admin approval and use bounded discounts', () => {
  assert.match(deals, /discount_percent INTEGER NOT NULL CHECK \(discount_percent BETWEEN 1 AND 80\)/);
  assert.match(deals, /status TEXT NOT NULL DEFAULT 'PENDING'/);
  assert.match(deals, /app\.post\('\/v1\/seller\/deals'/);
  assert.match(deals, /app\.post\('\/v1\/admin\/deals'/);
  assert.match(deals, /app\.post\('\/v1\/admin\/deals\/:id\/review'/);
  assert.match(deals, /ANOTHER_ACTIVE_DEAL_EXISTS/);
  assert.match(deals, /reviewed_by/);
});

test('only approved, currently active deals are public and applied at checkout', () => {
  assert.match(deals, /app\.get\('\/v1\/deals'/);
  assert.match(deals, /status='APPROVED' AND d\.starts_at<=now\(\) AND d\.ends_at>now\(\)/);
  assert.match(server, /original_price_paise/);
  assert.match(server, /aarvo_product_deals/);
  assert.match(launcher, /const discountedProductQuery/);
  assert.match(launcher, /await registerProductDeals\(\{ app, pool, requireRole, audit \}\)/);
});

test('product deal migration uses the existing text user IDs', () => {
  assert.match(migration, /seller_id TEXT NOT NULL REFERENCES users\(id\)/);
  assert.match(migration, /reviewed_by TEXT REFERENCES users\(id\)/);
});
