import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
const androidMain = fs.readFileSync(path.join(ROOT, 'app/src/main/java/com/aarvo/MainActivity.kt'), 'utf8');
const apiClient = fs.readFileSync(path.join(ROOT, 'app/src/main/java/com/aarvo/network/AarvoApiClient.kt'), 'utf8');
const productModel = fs.readFileSync(path.join(ROOT, 'app/src/main/java/com/aarvo/data/Product.kt'), 'utf8');
const cartViewModel = fs.readFileSync(path.join(ROOT, 'app/src/main/java/com/aarvo/cart/CartViewModel.kt'), 'utf8');
const wishlistStore = fs.readFileSync(path.join(ROOT, 'app/src/main/java/com/aarvo/wishlist/WishlistStore.kt'), 'utf8');
const marketplaceModels = fs.readFileSync(path.join(ROOT, 'app/src/main/java/com/aarvo/data/MarketplaceModels.kt'), 'utf8');
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
  assert.match(apiClient, /buildUrl\(path\)/);
  assert.match(apiClient, /AARVO live API is not configured in this build/);
  assert.match(apiClient, /Idempotency-Key/);
  assert.match(apiClient, /UUID\.randomUUID\(\)/);
  assert.ok(apiClient.includes('/v1/orders/$orderId/cancel'));
  assert.ok(apiClient.includes('/v1/orders/$orderId/reviews'));
  assert.ok(apiClient.includes('/v1/orders/$orderId/disputes'));
  assert.ok(apiClient.includes('/v1/payments/verify'));
});

test('buyer review and dispute UI is wired to authenticated API actions', () => {
  assert.match(androidMain, /if \(status == "DELIVERED"\).*Review/s);
  assert.match(androidMain, /ReviewDialog\(api, id/);
  assert.match(androidMain, /api\.submitReview\(orderId, productId, rating, text\)/);
  assert.match(androidMain, /DisputeDialog\(api, id/);
  assert.match(androidMain, /api\.openDispute\(orderId, reason, details\)/);
  assert.match(androidMain, /Unable to submit review/);
  assert.match(androidMain, /Unable to open dispute/);
});

test('wishlist regression keeps saved product IDs persistent, positive and serialized', () => {
  assert.match(wishlistStore, /SharedPreferences/);
  assert.match(wishlistStore, /KEY_PRODUCT_IDS/);
  assert.match(wishlistStore, /filter \{ it > 0 \}/);
  assert.match(wishlistStore, /require\(productId > 0/);
  assert.match(wishlistStore, /@Synchronized\s+fun toggle/);
  assert.match(wishlistStore, /@Synchronized\s+fun clear/);
  assert.match(wishlistStore, /prefs\.edit\(\)/);
  assert.match(androidMain, /WishlistStore\(prefs\)/);
  assert.match(androidMain, /wishlistStore\.toggle\(product\.id\)/);
  assert.match(androidMain, /WishlistScreen\(/);
});

test('cart regression never exceeds server-provided stock and sends exact quantities', () => {
  assert.match(cartViewModel, /product\.stockQuantity/);
  assert.match(cartViewModel, /requestedQuantity\.coerceIn\(0, product\.stockQuantity\)/);
  assert.match(cartViewModel, /quantity\(product\.id\)/);
  assert.match(androidMain, /"quantity", cartViewModel\.quantity\(product\.id\)/);
  assert.match(androidMain, /enabled = quantity < product\.stockQuantity/);
});

test('seller marketplace UI remains connected to product, inventory and fulfillment APIs', () => {
  assert.match(androidMain, /SellerDashboardScreen\(/);
  assert.match(androidMain, /api\.sellerProducts\(\)/);
  assert.match(androidMain, /api\.sellerOrders\(\)/);
  assert.match(androidMain, /api\.sellerProfile\(\)/);
  assert.match(androidMain, /api\.createSellerProduct\(/);
  assert.match(androidMain, /api\.updateInventory\(/);
  assert.match(androidMain, /api\.updateOrderTracking\(/);
});

test('marketplace domain model preserves explicit order/payment lifecycle states', () => {
  assert.match(marketplaceModels, /PENDING_PAYMENT/);
  assert.match(marketplaceModels, /CAPTURED/);
  assert.match(marketplaceModels, /DELIVERED/);
  assert.match(marketplaceModels, /REFUNDED/);
  assert.match(marketplaceModels, /DISPUTED/);
  assert.match(marketplaceModels, /Server-authoritative marketplace contract/);
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
  assert.match(workflow, /AARVO_API_BASE_URL/);
  assert.match(workflow, /-PaarvoApiBaseUrl=/);
  assert.match(workflow, /https:\/\/aarvo-api\.onrender\.com/);
  assert.match(workflow, /Upload debug APK/);
  assert.match(workflow, /Upload release AAB/);
  assert.match(workflow, /backend-check/);
  assert.match(workflow, /Attest debug APK provenance/);
  assert.match(workflow, /Attest release AAB provenance/);
});
