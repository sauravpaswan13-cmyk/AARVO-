import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const server = fs.readFileSync(new URL('../src/server.js', import.meta.url), 'utf8');
const schema = fs.readFileSync(new URL('../schema.sql', import.meta.url), 'utf8');
const migration2 = fs.readFileSync(new URL('../migrations/002_marketplace_operations.sql', import.meta.url), 'utf8');
const migration3 = fs.readFileSync(new URL('../migrations/003_reviews_delivery.sql', import.meta.url), 'utf8');
const migration4 = fs.readFileSync(new URL('../migrations/004_marketplace_hardening.sql', import.meta.url), 'utf8');
const migration5 = fs.readFileSync(new URL('../migrations/005_operational_indexes.sql', import.meta.url), 'utf8');

test('authentication and authorization boundaries are explicit', () => {
  assert.match(server, /jsonwebtoken/);
  assert.match(server, /JWT_SECRET/);
  assert.match(server, /scryptSync/);
  assert.match(server, /timingSafeEqual/);
  assert.match(server, /role.*SELLER|SELLER.*role/);
  assert.match(server, /role.*ADMIN|ADMIN.*role/);
});

test('security middleware and request throttling are enabled', () => {
  assert.match(server, /helmet/);
  assert.match(server, /rateLimit/);
  assert.match(server, /max:\s*120/);
  assert.match(server, /timeWindow:\s*'1 minute'/);
  assert.match(server, /0\.0\.0\.0/);
});

test('orders use transactional stock reservation and server-side totals', () => {
  assert.match(server, /BEGIN/);
  assert.match(server, /FOR UPDATE/);
  assert.match(server, /stock_quantity/);
  assert.match(server, /price_paise/);
  assert.match(server, /DELIVERY_FEE_PAISE/);
  assert.match(server, /PLATFORM_FEE_BPS/);
  assert.match(server, /COMMIT/);
  assert.match(server, /ROLLBACK/);
});

test('order creation is idempotent and protected against duplicate payment attempts', () => {
  assert.match(server, /Idempotency-Key|idempotency/i);
  assert.match(server, /idempotency_key/i);
  assert.match(server, /gateway_order_id/i);
  assert.match(server, /payment_status/i);
});

test('payment verification rejects mismatched or non-captured payments', () => {
  assert.match(server, /gateway_order_id\s*!==\s*razorpayOrderId/);
  assert.match(server, /razorpay\.payments\.fetch\(razorpayPaymentId\)/);
  assert.match(server, /captured/);
  assert.match(server, /createHmac\('sha256'/);
});

test('webhook processing is authenticated with the configured secret', () => {
  assert.match(server, /RAZORPAY_WEBHOOK_SECRET/);
  assert.match(server, /request\.rawBody/);
  assert.match(server, /timingSafeEqual/);
  assert.match(server, /payment_events/);
});

test('order lifecycle has cancellation, delivery tracking and dispute records', () => {
  assert.match(server, /\/v1\/orders\/:id\/cancel/);
  assert.match(server, /\/v1\/orders\/:id\/tracking/);
  assert.match(server, /\/v1\/orders\/:id\/disputes/);
  assert.match(migration2, /payment_expires_at/);
  assert.match(migration2, /audit_events/);
  assert.match(migration3, /delivery_events/);
  assert.match(migration3, /order_disputes/);
});

test('tracking and dispute actions remain buyer or seller scoped', () => {
  assert.match(server, /requireBuyer/);
  assert.match(server, /requireSeller/);
  assert.match(server, /UPDATE orders/);
  assert.match(server, /order_disputes/);
  assert.match(server, /delivery_events/);
});

test('review eligibility is persisted with a one-review constraint', () => {
  assert.match(migration3, /product_reviews/);
  assert.match(migration3, /UNIQUE\s*\(order_id, product_id, buyer_id\)/i);
  assert.match(server, /\/v1\/orders\/:id\/reviews/);
  assert.match(server, /delivered/i);
});

test('financial database constraints prevent invalid money states', () => {
  assert.match(schema, /price_paise/);
  assert.match(schema, /seller_ledger/);
  assert.match(migration4, /orders_money_nonnegative_chk/);
  assert.match(migration4, /orders_payment_status_chk/);
  assert.match(migration5, /orders_created_at_idx/);
});

test('production configuration does not silently fall back to demo payments', () => {
  assert.match(server, /RAZORPAY_KEY_ID/);
  assert.match(server, /RAZORPAY_KEY_SECRET/);
  assert.match(server, /razorpay\s*=.*new Razorpay/s);
  assert.doesNotMatch(server, /demo payment accepted|fake payment|mock payment/i);
});

test('seller order access remains scoped to seller-owned lines', () => {
  assert.match(server, /\/v1\/seller\/orders/);
  assert.match(server, /WHERE ol\.seller_id=\$1/);
  assert.match(server, /sellerAmountPaise/);
});
