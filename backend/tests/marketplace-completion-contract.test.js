import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
const read = (relativePath) => fs.readFileSync(path.join(ROOT, relativePath), 'utf8');

const migration = read('backend/migrations/009_marketplace_media_addresses.sql');
const schema = read('backend/schema.sql');
const server = read('backend/src/server.js');
const completion = read('backend/src/marketplace-completion.js');
const launcher = read('backend/src/launcher.js');
const androidApi = read('app/src/main/java/com/aarvo/network/AarvoApiClient.kt');

for (const [name, source] of [['migration', migration], ['schema', schema]]) {
  test(`${name} contains product media and buyer address contracts`, () => {
    assert.match(source, /CREATE TABLE IF NOT EXISTS product_images/);
    assert.match(source, /image_url TEXT NOT NULL/);
    assert.match(source, /CREATE TABLE IF NOT EXISTS buyer_addresses/);
    assert.match(source, /is_default BOOLEAN NOT NULL DEFAULT false/);
    assert.match(source, /buyer_addresses_default_uidx/);
  });
}

test('marketplace completion runtime exposes product media and address APIs', () => {
  for (const contract of [
    /app\.get\('\/v1\/products\/:id\/images'/,
    /app\.post\('\/v1\/seller\/products\/:id\/images'/,
    /app\.delete\('\/v1\/seller\/products\/:id\/images\/:imageId'/,
    /app\.get\('\/v1\/addresses'/,
    /app\.post\('\/v1\/addresses'/,
    /app\.put\('\/v1\/addresses\/:id'/,
    /app\.delete\('\/v1\/addresses\/:id'/,
    /app\.post\('\/v1\/addresses\/:id\/default'/,
    /requireRole\('BUYER'\)/,
    /requireRole\('SELLER'\)/
  ]) assert.match(completion, contract);
  assert.match(launcher, /registerMarketplaceCompletion/);
});

test('product image lifecycle preserves a primary image after deleting the current primary', () => {
  assert.match(completion, /if \(result\.rows\[0\]\.is_primary\)/);
  assert.match(completion, /ORDER BY sort_order ASC,id ASC LIMIT 1/);
  assert.match(completion, /UPDATE product_images SET is_primary=true WHERE id=\$1/);
  assert.match(completion, /UPDATE products SET image_url=\$1,updated_at=now\(\)/);
  assert.match(completion, /image_url=NULL,updated_at=now\(\)/);
});

test('address lifecycle guarantees one usable default address', () => {
  assert.match(completion, /const isDefault = makeDefault \|\| Number\(existing\.rows\[0\]\.count\) === 0/);
  assert.match(completion, /const isDefault = requestedDefault \|\| current\.rows\[0\]\.is_default/);
  assert.match(completion, /UPDATE buyer_addresses SET is_default=false/);
  assert.match(completion, /UPDATE buyer_addresses SET is_default=true/);
  assert.match(completion, /if \(result\.rows\[0\]\.is_default\) await client\.query\('UPDATE buyer_addresses SET is_default=true/);
});

test('existing marketplace server keeps the protected purchase and settlement contracts', () => {
  for (const contract of [
    /app\.post\('\/v1\/orders'/,
    /app\.post\('\/v1\/payments\/verify'/,
    /app\.post\('\/v1\/orders\/:id\/cancel'/,
    /app\.post\('\/v1\/orders\/:id\/tracking'/,
    /app\.post\('\/v1\/orders\/:id\/return'/,
    /app\.post\('\/v1\/admin\/orders\/:id\/refund'/,
    /app\.get\('\/v1\/seller\/settlements'/,
    /idempotency-key/,
    /seller_ledger/,
    /payment_events/
  ]) assert.match(server, contract);
});

test('order state machine retains the complete delivery progression', () => {
  assert.match(server, /PAID:\['PACKED','CANCELLED'\]/);
  assert.match(server, /PACKED:\['SHIPPED','CANCELLED'\]/);
  assert.match(server, /SHIPPED:\['OUT_FOR_DELIVERY','DELIVERED'\]/);
  assert.match(server, /OUT_FOR_DELIVERY:\['DELIVERED'\]/);
});

test('Android API exposes the marketplace completion calls without removing guest browsing', () => {
  for (const contract of [
    /suspend fun productImages\(productId: Int\)/,
    /suspend fun productImageAdd\(/,
    /suspend fun productImageDelete\(/,
    /suspend fun addresses\(\)/,
    /suspend fun addAddress\(/,
    /suspend fun updateAddress\(/,
    /suspend fun deleteAddress\(/,
    /suspend fun setDefaultAddress\(/,
    /suspend fun createOrder\(/,
    /suspend fun verifyPayment\(/,
    /suspend fun sellerOrders\(\)/
  ]) assert.match(androidApi, contract);
});

test('guest browsing and authenticated purchase separation remains present in Android', () => {
  const mainActivity = read('app/src/main/java/com/aarvo/MainActivity.kt');
  assert.match(mainActivity, /guestMode/);
  assert.match(mainActivity, /Login Required/);
  assert.match(mainActivity, /api\.createOrder/);
});
