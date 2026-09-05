import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const server = fs.readFileSync(new URL('../src/server.js', import.meta.url), 'utf8');
const migration2 = fs.readFileSync(new URL('../migrations/002_marketplace_operations.sql', import.meta.url), 'utf8');
const migration3 = fs.readFileSync(new URL('../migrations/003_reviews_delivery.sql', import.meta.url), 'utf8');

test('backend exposes the core marketplace API contract', () => {
  for (const route of [
    '/v1/auth/register',
    '/v1/auth/login',
    '/v1/products',
    '/v1/products/:id',
    '/v1/seller/products',
    '/v1/seller/profile',
    '/v1/orders',
    '/v1/orders/:id',
    '/v1/orders/:id/cancel',
    '/v1/orders/:id/tracking',
    '/v1/orders/:id/reviews',
    '/v1/orders/:id/disputes',
    '/v1/payments/verify',
    '/v1/webhooks/razorpay',
    '/v1/admin/sellers/:id/verify',
    '/v1/admin/disputes/:id/resolve',
  ]) assert.ok(server.includes(`'${route}'`), `missing route: ${route}`);
});

test('payment verification is server-side and uses the stored gateway order', () => {
  assert.match(server, /order\.gateway_order_id\s*!==\s*razorpayOrderId/);
  assert.match(server, /createHmac\('sha256'/);
  assert.match(server, /razorpay\.payments\.fetch\(razorpayPaymentId\)/);
});

test('idempotency, audit, review and delivery database primitives exist', () => {
  assert.match(migration2, /idempotency_key/);
  assert.match(migration2, /audit_events/);
  assert.match(migration3, /product_reviews/);
  assert.match(migration3, /delivery_events/);
});
