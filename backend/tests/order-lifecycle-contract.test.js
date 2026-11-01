import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
const read = (relativePath) => fs.readFileSync(path.join(ROOT, relativePath), 'utf8');

const server = read('backend/src/server.js');
const launcher = read('backend/src/launcher.js');
const otpDelivery = read('backend/src/otp-delivery.js');


test('order lifecycle contract keeps seller status transitions and buyer tracking access guarded', () => {
  assert.match(server, /const transitionMap=\{PAID:\['PACKED','CANCELLED'\],CONFIRMED:\['PACKED','CANCELLED'\],PACKED:\['SHIPPED','CANCELLED'\],SHIPPED:\['OUT_FOR_DELIVERY','DELIVERED'\],OUT_FOR_DELIVERY:\['DELIVERED'\],DELIVERED:\[\]\};/);
  assert.match(server, /app\.post\('\/v1\/orders\/:id\/tracking'/);
  assert.match(server, /SELLER_OR_ADMIN_REQUIRED/);
  assert.match(server, /INVALID_ORDER_TRANSITION/);
  assert.match(server, /app\.get\('\/v1\/orders\/:id\/tracking'/);
  assert.match(server, /delivery_events/);
});

test('refund and OTP runtime hardening remains wired into the production launcher', () => {
  assert.match(launcher, /SELECT id,total_paise,gateway_payment_id,payment_status,status,refund_status FROM orders WHERE id=\$1 FOR UPDATE/);
  assert.match(launcher, /FOR UPDATE/);
  assert.match(launcher, /refund_status/);
  assert.match(launcher, /seller_amount_paise/);
  assert.match(launcher, /OTP_DELIVERY_FAILED/);
  assert.match(otpDelivery, /setTimeout\(\(\) => controller\.abort\(\), 10000\)/);
});


test('manual seller settlement is opt-in and never bypasses seller verification', () => {
  assert.match(server, /AARVO_MANUAL_SELLER_SETTLEMENT_ENABLED/);
  assert.match(server, /seller\.rows\[0\]\.verified && \(seller\.rows\[0\]\.payout_account_ready \|\| manualSettlementEnabled\)/);
  assert.match(server, /seller\.rows\[0\]\.verified && \(seller\.rows\[0\]\.payout_account_ready \|\| manualSettlementEnabled\), isPublished/);
});


test('COD/manual settlement migration is additive and keeps Razorpay as the legacy default', () => {
  const migration = read('backend/migrations/029_cod_manual_settlement.sql');
  const schema = read('backend/schema.sql');
  assert.match(migration, /ADD COLUMN IF NOT EXISTS payment_method TEXT NOT NULL DEFAULT 'RAZORPAY'/);
  assert.match(migration, /payment_method IN \('RAZORPAY', 'COD'\)/);
  assert.match(migration, /cod_collected_at TIMESTAMPTZ/);
  assert.match(migration, /cod_collected_by TEXT REFERENCES users\(id\)/);
  assert.match(migration, /MANUAL_PAYOUT/);
  assert.match(migration, /settlement_reference TEXT/);
  assert.match(schema, /payment_method TEXT NOT NULL DEFAULT 'RAZORPAY'/);
  assert.match(schema, /MANUAL_PAYOUT/);
});


test('COD checkout is disabled by default and never calls Razorpay for COD orders', () => {
  assert.match(server, /AARVO_COD_ENABLED/);
  assert.match(server, /app\.get\('\/v1\/checkout\/options'/);
  assert.match(server, /paymentMethod === 'COD' && !codEnabled/);
  assert.match(server, /paymentMethod === 'COD' \? null/);
  assert.match(server, /'COD_PENDING'/);
  assert.match(server, /'CONFIRMED'/);
  assert.match(server, /IDEMPOTENCY_PAYMENT_METHOD_MISMATCH/);
  assert.match(server, /COD_CANCELLED/);
});

test('COD collection requires delivered status and creates seller sale ledger once', () => {
  assert.match(server, /COD_COLLECTION_REQUIRES_DELIVERED_ORDER/);
  assert.match(server, /COD_ALREADY_COLLECTED/);
  assert.match(server, /COD_CASH_COLLECTED/);
  assert.match(server, /cod_collected_by/);
  assert.match(server, /AND sl\.type='SALE'/);
});

test('manual seller payouts are admin-only, feature-gated, reference-audited and balance-limited', () => {
  assert.match(server, /app\.post\('\/v1\/admin\/sellers\/:id\/manual-payout'/);
  assert.match(server, /MANUAL_SETTLEMENT_DISABLED/);
  assert.match(server, /SETTLEMENT_REFERENCE_ALREADY_USED/);
  assert.match(server, /PAYOUT_EXCEEDS_AVAILABLE_BALANCE/);
  assert.match(server, /MANUAL_PAYOUT_RECORDED/);
  assert.match(server, /seller_ledger_manual_settlement_reference_uidx/);
});
