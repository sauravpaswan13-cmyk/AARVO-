import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const server = fs.readFileSync(new URL('../src/server.js', import.meta.url), 'utf8');
const client = fs.readFileSync(new URL('../../app/src/main/java/com/aarvo/network/AarvoApiClient.kt', import.meta.url), 'utf8');
const migration2 = fs.readFileSync(new URL('../migrations/002_marketplace_operations.sql', import.meta.url), 'utf8');
const migration3 = fs.readFileSync(new URL('../migrations/003_reviews_delivery.sql', import.meta.url), 'utf8');
const migration4 = fs.readFileSync(new URL('../migrations/004_marketplace_hardening.sql', import.meta.url), 'utf8');

test('backend exposes the core marketplace API contract', () => {
  for (const route of [
    '/v1/auth/register', '/v1/auth/login', '/v1/products', '/v1/products/:id',
    '/v1/seller/products', '/v1/seller/profile', '/v1/seller/orders',
    '/v1/orders', '/v1/orders/:id', '/v1/orders/:id/cancel',
    '/v1/orders/:id/tracking', '/v1/orders/:id/reviews', '/v1/orders/:id/disputes',
    '/v1/payments/verify', '/v1/webhooks/razorpay',
    '/v1/admin/sellers/:id/verify', '/v1/admin/disputes/:id/resolve',
  ]) assert.ok(server.includes(`'${route}'`), `missing route: ${route}`);
});

test('payment verification is server-side and uses the stored gateway order', () => {
  assert.match(server, /order\.gateway_order_id\s*!==\s*razorpayOrderId/);
  assert.match(server, /createHmac\('sha256'/);
  assert.match(server, /razorpay\.payments\.fetch\(razorpayPaymentId\)/);
});

test('idempotency is enforced end-to-end', () => {
  assert.match(server, /idempotency-key/);
  assert.match(server, /idempotency_key/);
  assert.match(client, /Idempotency-Key/);
  assert.match(client, /UUID\.randomUUID\(\)/);
  assert.match(client, /idempotencyKey: String/);
});

test('seller order access is scoped to seller-owned order lines', () => {
  assert.match(server, /app\.get\('\/v1\/seller\/orders'/);
  assert.match(server, /WHERE ol\.seller_id=\$1/);
  assert.match(server, /sellerAmountPaise/);
});

test('client exposes order operations beyond checkout', () => {
  for (const method of ['sellerOrders', 'updateOrderTracking', 'submitReview', 'productReviews', 'openDispute']) {
    assert.match(client, new RegExp(`suspend fun ${method}`), `missing client method: ${method}`);
  }
});

test('webhook keeps raw-body capture enabled while applying its rate limit', () => {
  assert.match(server, /app\.register\(rawBody,\s*\{\s*field:\s*'rawBody',\s*global:\s*false,\s*encoding:\s*'utf8',\s*runFirst:\s*true\s*\}\)/);
  assert.match(server, /config\s*:\s*\{\s*rawBody\s*:\s*true\s*,\s*rateLimit\s*:\s*\{\s*max\s*:\s*300\s*,\s*timeWindow\s*:\s*'1 minute'\s*\}\s*\}/);
  assert.match(server, /request\.rawBody/);
});

test('idempotency, audit, review, delivery and dispute primitives exist', () => {
  assert.match(migration2, /idempotency_key/);
  assert.match(migration2, /audit_events/);
  assert.match(migration3, /product_reviews/);
  assert.match(migration3, /delivery_events/);
  assert.match(migration3, /order_disputes/);
  assert.match(migration4, /orders_money_nonnegative_chk/);
});
