import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const migrationDir = path.join(here, '..', 'migrations');
const files = fs.readdirSync(migrationDir).filter((name) => /^\d+_.+\.sql$/.test(name)).sort();

test('migration sequence has unique numeric prefixes', () => {
  const prefixes = files.map((name) => name.match(/^\d+/)[0]);
  assert.equal(new Set(prefixes).size, prefixes.length, `duplicate migration prefix: ${prefixes.join(', ')}`);
});

test('required marketplace migrations are present in order', () => {
  assert.deepEqual(files.slice(0, 4), [
    '001_production_integrity.sql',
    '002_marketplace_operations.sql',
    '003_reviews_delivery.sql',
    '004_marketplace_hardening.sql',
  ]);
});

test('hardening migration protects order and product invariants', () => {
  const sql = fs.readFileSync(path.join(migrationDir, '004_marketplace_hardening.sql'), 'utf8');
  for (const required of ['orders_payment_status_chk', 'orders_status_chk', 'orders_refund_status_chk', 'orders_money_nonnegative_chk', 'products_rating_chk']) {
    assert.match(sql, new RegExp(required));
  }
});
