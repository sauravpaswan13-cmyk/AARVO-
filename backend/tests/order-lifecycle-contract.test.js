import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
const read = (relativePath) => fs.readFileSync(path.join(ROOT, relativePath), 'utf8');

const server = read('backend/src/server.js');
const launcher = read('backend/src/launcher.js');


test('order lifecycle contract keeps seller status transitions and buyer tracking access guarded', () => {
  assert.match(server, /const transitionMap=\{PAID:\['PACKED','CANCELLED'\],PACKED:\['SHIPPED','CANCELLED'\],SHIPPED:\['OUT_FOR_DELIVERY','DELIVERED'\],OUT_FOR_DELIVERY:\['DELIVERED'\],DELIVERED:\[\]\};/);
  assert.match(server, /app\.post\('\/v1\/orders\/:id\/tracking'/);
  assert.match(server, /SELLER_OR_ADMIN_REQUIRED/);
  assert.match(server, /INVALID_ORDER_TRANSITION/);
  assert.match(server, /app\.get\('\/v1\/orders\/:id\/tracking'/);
  assert.match(server, /delivery_events/);
});

test('refund and OTP runtime hardening remains wired into the production launcher', () => {
  assert.match(launcher, /SELECT id,total_paise,gateway_payment_id,payment_status,status FROM orders WHERE id=\$1 FOR UPDATE/);
  assert.match(launcher, /FOR UPDATE/);
  assert.match(launcher, /refund_status/);
  assert.match(launcher, /seller_amount_paise/);
  assert.match(launcher, /OTP_DELIVERY_FAILED/);
  assert.match(launcher, /setTimeout\(\(\) => controller\.abort\(\), 10000\)/);
});
