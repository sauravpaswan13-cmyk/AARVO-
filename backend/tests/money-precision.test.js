import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const server = fs.readFileSync(new URL('../src/server.js', import.meta.url), 'utf8');
const schema = fs.readFileSync(new URL('../schema.sql', import.meta.url), 'utf8');
const productModel = fs.readFileSync(new URL('../../app/src/main/java/com/aarvo/data/Product.kt', import.meta.url), 'utf8');

test('database money fields stay integer paise with non-negative constraints', () => {
  assert.match(schema, /price_paise\s+BIGINT/);
  assert.match(schema, /total_paise\s+BIGINT/);
  assert.match(schema, /delivery_fee_paise\s+BIGINT/);
  assert.match(schema, /platform_fee_paise\s+BIGINT/);
  assert.match(schema, /CHECK \(price_paise >= 0\)/);
});

test('order pricing is calculated from integer paise on the server', () => {
  assert.match(server, /price_paise/);
  assert.match(server, /DELIVERY_FEE_PAISE/);
  assert.match(server, /PLATFORM_FEE_BPS/);
  assert.match(server, /Math\.round\(/);
  assert.doesNotMatch(server, /parseFloat\([^\n]*price_paise/);
});

test('Android product model preserves exact paise and formats without floating point', () => {
  assert.match(productModel, /val pricePaise: Long/);
  assert.match(productModel, /pricePaise \/ 100/);
  assert.match(productModel, /pricePaise % 100/);
  assert.doesNotMatch(productModel, /pricePaise \/ 100\.0/);
});
