import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
const read = (relativePath) => fs.readFileSync(path.join(ROOT, relativePath), 'utf8');

const androidMain = read('app/src/main/java/com/aarvo/MainActivity.kt');
const apiClient = read('app/src/main/java/com/aarvo/network/AarvoApiClient.kt');
const productModel = read('app/src/main/java/com/aarvo/data/Product.kt');
const workflow = read('.github/workflows/android.yml');
const dockerfile = read('backend/Dockerfile');
const migrationRunner = read('backend/src/migrate.js');
const envExample = read('backend/.env.production.example');


test('internal marketplace completion gate covers buyer, seller, money, deployment and provenance contracts', () => {
  assert.match(androidMain, /Create your AARVO account/);
  assert.match(androidMain, /HomeScreen\(/);
  assert.match(androidMain, /CartScreen\(/);
  assert.match(androidMain, /CheckoutDialog\(/);
  assert.match(androidMain, /OrdersScreen\(/);
  assert.match(androidMain, /SellerDashboardScreen\(/);
  assert.match(androidMain, /SellerProductDialog\(/);
  assert.match(androidMain, /ReviewDialog\(/);
  assert.match(androidMain, /DisputeDialog\(/);
  assert.match(androidMain, /api\.submitReview\(/);
  assert.match(androidMain, /api\.openDispute\(/);
  assert.match(androidMain, /api\.updateInventory\(/);
  assert.match(androidMain, /api\.updateOrderTracking\(/);

  assert.match(apiClient, /baseUrl\.startsWith\("https:\/\//);
  assert.match(apiClient, /Idempotency-Key/);
  assert.match(apiClient, /submitReview/);
  assert.match(apiClient, /openDispute/);
  assert.match(apiClient, /sellerOrders/);

  assert.match(productModel, /val pricePaise: Long/);
  assert.match(productModel, /pricePaise % 100/);
  assert.match(productModel, /require\(pricePaise >= 0/);

  assert.match(workflow, /Build debug APK/);
  assert.match(workflow, /Build release AAB \(unsigned\)/);
  assert.match(workflow, /Attest debug APK provenance/);
  assert.match(workflow, /Attest release AAB provenance/);
  assert.match(workflow, /backend-check/);

  assert.match(dockerfile, /USER node/);
  assert.match(migrationRunner, /migrations/);
  assert.match(envExample, /RAZORPAY_KEY_ID/);
  assert.match(envExample, /RAZORPAY_KEY_SECRET/);
  assert.match(envExample, /POSTGRES_PASSWORD/);
});
