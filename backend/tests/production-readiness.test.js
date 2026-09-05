import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const server = fs.readFileSync(new URL('../src/server.js', import.meta.url), 'utf8');
const schema = fs.readFileSync(new URL('../schema.sql', import.meta.url), 'utf8');
const migration3 = fs.readFileSync(new URL('../migrations/003_reviews_delivery.sql', import.meta.url), 'utf8');
const migration4 = fs.readFileSync(new URL('../migrations/004_marketplace_hardening.sql', import.meta.url), 'utf8');
const migration5 = fs.readFileSync(new URL('../migrations/005_operational_indexes.sql', import.meta.url), 'utf8');

test('production payment and webhook boundaries are explicit', () => {
  assert.match(server, /payments\/verify/);
  assert.match(server, /webhooks\/razorpay/);
  assert.match(server, /RAZORPAY_WEBHOOK_SECRET/);
  assert.match(server, /x-razorpay-signature/);
  assert.match(server, /x-razorpay-event-id/);
  assert.match(server, /ON CONFLICT \(event_id\) DO NOTHING/);
  assert.match(server, /payment_status='REFUNDED'/);
});

test('buyer order access is scoped to the authenticated buyer', () => {
  assert.match(server, /app\.get\('\/v1\/orders'/);
  assert.match(server, /WHERE o\.buyer_id=\$1/);
  assert.match(server, /orders\/\:id/);
  assert.match(server, /o\.buyer_id=\$2/);
});

test('seller order and inventory access is scoped to seller ownership', () => {
  assert.match(server, /WHERE ol\.seller_id=\$1/);
  assert.match(server, /WHERE id=\$2 AND seller_id=\$3/);
  assert.match(server, /EXISTS \(SELECT 1 FROM order_lines x WHERE x\.order_id=o\.id AND x\.seller_id=\$2\)/);
});

test('delivery tracking has a constrained state machine and event history', () => {
  assert.match(server, /const transitionMap=/);
  assert.match(server, /PACKED/);
  assert.match(server, /SHIPPED/);
  assert.match(server, /OUT_FOR_DELIVERY/);
  assert.match(server, /DELIVERED/);
  assert.match(server, /delivery_events/);
  assert.match(server, /ORDER_CANNOT_BE_CANCELLED/);
});

test('reviews require delivered orders and an order line for the reviewed product', () => {
  assert.match(server, /status='DELIVERED'/);
  assert.match(server, /PRODUCT_NOT_IN_ORDER/);
  assert.match(server, /product_reviews/);
  assert.match(migration3, /UNIQUE\s*\(product_id,buyer_id,order_id\)/i);
});

test('database hardening protects money and payment status fields', () => {
  assert.match(schema, /price_paise/);
  assert.match(schema, /total_paise/);
  assert.match(migration4, /orders_money_nonnegative_chk/);
  assert.match(migration4, /orders_payment_status_chk/);
  assert.match(migration5, /orders_created_at_idx/);
});

test('payment amount remains server-authoritative in paise', () => {
  assert.match(server, /Number\(p\.price_paise\)\*qty/);
  assert.match(server, /Number\(payment\.amount\)!==Number\(order\.total_paise\)/);
  assert.match(server, /razorpay\.orders\.create\(\{amount:total,currency:'INR'/);
});
