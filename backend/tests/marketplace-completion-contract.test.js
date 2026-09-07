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
    /GET \'\/v1\/products\/:id\/images\'/,
    /POST \'\/v1\/seller\/products\/:id\/images\'/,
    /DELETE \'\/v1\/seller\/products\/:id\/images\/:imageId\'/,
    /GET \'\/v1\/addresses\'/,
    /POST \'\/v1\/addresses\'/,
    /PUT \'\/v1\/addresses\/:id\'/,
    /DELETE \'\/v1\/addresses\/:id\'/,
    /POST \'\/v1\/addresses\/:id\/default\'/,
    /requireRole\('BUYER'\)/,
    /requireRole\('SELLER'\)/
  ]) assert.match(completion, contract);
  assert.match(launcher, /registerMarketplaceCompletion/);
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

test('guest browsing and authenticated purchase separation remains present in Android', () => {
  const mainActivity = read('app/src/main/java/com/aarvo/MainActivity.kt');
  assert.match(mainActivity, /guestMode/);
  assert.match(mainActivity, /Login Required/);
  assert.match(mainActivity, /api\.createOrder/);
});
