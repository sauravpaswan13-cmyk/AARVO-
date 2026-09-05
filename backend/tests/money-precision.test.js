import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const server = fs.readFileSync(new URL('../src/server.js', import.meta.url), 'utf8');
const schema = fs.readFileSync(new URL('../schema.sql', import.meta.url), 'utf8');
const productModel = fs.readFileSync(new URL('../../app/src/main/java/com/aarvo/data/Product.kt', import.meta.url), 'utf8');

test('database money fields stay integer paise with non-negative constraints', () => {
  for (const field of ['price_paise', 'total_paise', 'delivery_fee_paise', 'platform_fee_paise']) {
    assert.match(schema, new RegExp(`${field}\\s+INTEGER`));
  }
  assert.match(schema, /CHECK \(price_paise > 0\)/);
  assert.match(schema, /total_paise INTEGER NOT NULL/);
});

test('order pricing is calculated from integer paise on the server', () => {
  assert.match(server, /price_paise/);
  assert.match(server, /DELIVERY_FEE_PAISE/);
  assert.match(server, /PLATFORM_FEE_BPS/);
  assert.match(server, /Math\.floor\(subtotal\*PLATFORM_FEE_BPS\/10000\)/);
  assert.match(server, /amount:total/);
  assert.doesNotMatch(server, /parseFloat\([^\n]*price_paise/);
});

test('Android product model preserves exact paise and formats without floating point', () => {
  assert.match(productModel, /val pricePaise: Long/);
  assert.match(productModel, /pricePaise \/ 100/);
  assert.match(productModel, /pricePaise % 100/);
  assert.doesNotMatch(productModel, /pricePaise \/ 100\.0/);
});
