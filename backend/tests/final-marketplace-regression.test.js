import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
const androidMain = fs.readFileSync(path.join(ROOT, 'app/src/main/java/com/aarvo/MainActivity.kt'), 'utf8');
const apiClient = fs.readFileSync(path.join(ROOT, 'app/src/main/java/com/aarvo/network/AarvoApiClient.kt'), 'utf8');
const productModel = fs.readFileSync(path.join(ROOT, 'app/src/main/java/com/aarvo/data/Product.kt'), 'utf8');
const workflow = fs.readFileSync(path.join(ROOT, '.github/workflows/android.yml'), 'utf8');

test('final Android checkout regression contracts remain server-authoritative', () => {
  assert.match(androidMain, /createOrder\(items, address\)/);
  assert.match(androidMain, /amountPaise/);
  assert.match(androidMain, /gatewayOrderId/);
  assert.match(androidMain, /verifyPayment\(/);
  assert.match(androidMain, /PaymentBridge\.lastSignature/);
  assert.match(androidMain, /BUYER_PAYMENT_CANCELLED/);
});

test('final API client regression contracts remain HTTPS-only and idempotent', () => {
  assert.ok(apiClient.includes('baseUrl.startsWith("https://")'));
  assert.match(apiClient, /Idempotency-Key/);
  assert.match(apiClient, /UUID\.randomUUID\(\)/);
  assert.ok(apiClient.includes('/v1/orders/$orderId/cancel'));
  assert.ok(apiClient.includes('/v1/orders/$orderId/reviews'));
  assert.ok(apiClient.includes('/v1/orders/$orderId/disputes'));
  assert.ok(apiClient.includes('/v1/payments/verify'));
});

test('final money regression keeps exact integer paise representation', () => {
  assert.match(productModel, /val pricePaise: Long/);
  assert.match(productModel, /pricePaise \/ 100/);
  assert.match(productModel, /pricePaise % 100/);
  assert.match(productModel, /require\(pricePaise >= 0/);
  assert.match(androidMain, /val pricePaise = o\.getLong\("price_paise"\)/);
  assert.match(androidMain, /pricePaise\)\)/);
});

test('final CI regression still builds both installable/debug and release artifacts', () => {
  assert.match(workflow, /Build debug APK/);
  assert.match(workflow, /Build release AAB \(unsigned\)/);
  assert.match(workflow, /Upload debug APK/);
  assert.match(workflow, /Upload release AAB/);
  assert.match(workflow, /backend-check/);
});
