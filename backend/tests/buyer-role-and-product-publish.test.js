import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
const server = fs.readFileSync(path.join(ROOT, 'backend/src/server.js'), 'utf8');

test('seller accounts can use buyer-owned address and shopping endpoints without weakening seller/admin checks', () => {
  assert.match(server, /const roleAllowed = actualRole === role \|\| \(role === 'BUYER' && actualRole === 'SELLER'\)/);
  assert.match(server, /if \(!roleAllowed\) return reply\.code\(403\)/);
  assert.match(server, /request\.user\.role !== role/);
});

test('product listing requires seller verification but not payout-account activation', () => {
  const createStart = server.indexOf("app.post('/v1/seller/products'");
  const publishStart = server.indexOf("app.post('/v1/seller/products/:id/publish'");
  assert.ok(createStart >= 0 && publishStart > createStart);
  const createRoute = server.slice(createStart, server.indexOf("app.get('/v1/seller/products'", createStart));
  const publishRoute = server.slice(publishStart, server.indexOf("app.get('/v1/seller/settlements'", publishStart));
  assert.match(createRoute, /const canPublish = Boolean\(seller\.rows\[0\]\.verified\)/);
  assert.match(publishRoute, /const allowed = Boolean\(seller\.rows\[0\]\.verified\)/);
  assert.doesNotMatch(createRoute, /payout_account_ready/);
  assert.doesNotMatch(publishRoute, /payout_account_ready/);
});

test('payout readiness remains a separate control for order fulfilment and settlement', () => {
  assert.match(server, /payout_account_ready/);
  assert.match(server, /AARVO_MANUAL_SELLER_SETTLEMENT_ENABLED/);
});
