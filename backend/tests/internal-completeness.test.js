import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
const read = (relativePath) => fs.readFileSync(path.join(ROOT, relativePath), 'utf8');

const androidMain = read('app/src/main/java/com/aarvo/MainActivity.kt');
const adminDashboard = read('app/src/main/java/com/aarvo/AdminDashboardActivity.kt');
const apiClient = read('app/src/main/java/com/aarvo/network/AarvoApiClient.kt');
const productModel = read('app/src/main/java/com/aarvo/data/Product.kt');
const workflow = read('.github/workflows/android.yml');
const dockerfile = read('backend/Dockerfile');
const migrationRunner = read('backend/src/migrate.js');
const envExample = read('backend/.env.production.example');
const guestBrowse = read('app/src/main/java/com/aarvo/GuestBrowseActivity.kt');
const phoneAuth = read('app/src/main/java/com/aarvo/PhoneAuthActivity.kt');
const addressBook = read('app/src/main/java/com/aarvo/AddressBookActivity.kt');
const adminLogin = read('app/src/main/java/com/aarvo/AdminLoginActivity.kt');
const manifest = read('app/src/main/AndroidManifest.xml');
const sellerOnboarding = read('app/src/main/java/com/aarvo/SellerOnboardingActivity.kt');


test('internal marketplace completion gate preserves the existing entry, auth and management flows', () => {
  assert.match(androidMain, /Create your AARVO account/);
  assert.match(androidMain, /Continue as Guest/);
  assert.match(guestBrowse, /Shopping Trolley|Shopping trolley|trolley/i);
  assert.match(phoneAuth, /MSG91|OTP|Verify/i);
  assert.match(addressBook, /AddressBookScreen/);
  assert.match(adminLogin, /AdminDashboardActivity/);
  assert.match(manifest, /android:name="\.AdminLoginActivity"/);
  assert.match(manifest, /android:name="\.AdminDashboardActivity"/);

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
  assert.match(adminDashboard, /adminRefundOrder\(/);

  assert.match(apiClient, /URI\(baseUrl\)/);
  assert.match(apiClient, /uri\.scheme\.equals\("https", ignoreCase = true\)/);
  assert.match(apiClient, /Idempotency-Key/);
  assert.match(apiClient, /submitReview/);
  assert.match(apiClient, /openDispute/);
  assert.match(apiClient, /sellerOrders/);

  assert.match(productModel, /val pricePaise: Long/);
  assert.match(productModel, /pricePaise % 100/);
  assert.match(productModel, /require\(pricePaise >= 0/);

  assert.match(workflow, /Build debug APK/);
  assert.match(workflow, /Build release AAB \(signed\)/);
  assert.match(workflow, /Attest debug APK provenance/);
  assert.match(workflow, /Attest release AAB provenance/);
  assert.match(workflow, /backend-check/);

  assert.match(dockerfile, /USER node/);
  assert.match(migrationRunner, /migrations/);
  assert.match(envExample, /RAZORPAY_KEY_ID/);
  assert.match(envExample, /RAZORPAY_KEY_SECRET/);
  assert.match(envExample, /POSTGRES_PASSWORD/);
});

test('seller onboarding is reachable from Android and wired to the protected backend contract', () => {
  assert.match(manifest, /android:name="\.SellerOnboardingActivity"/);
  assert.match(androidMain, /Seller Business Onboarding/);
  assert.match(apiClient, /sellerOnboarding\(\)/);
  assert.match(apiClient, /saveSellerOnboarding\(/);
  assert.match(sellerOnboarding, /Seller Business Onboarding/);
  assert.match(sellerOnboarding, /Submit for Admin Review/);
  assert.match(sellerOnboarding, /api\.sellerOnboarding\(\)/);
  assert.match(sellerOnboarding, /api\.saveSellerOnboarding\(/);
});

test('final internal marketplace scope is wired end-to-end before release build', () => {
  const rider = read('backend/src/rider-delivery.js');
  const seller = read('backend/src/seller-onboarding.js');
  const gap = read('backend/src/marketplace-gap-completion.js');
  const cart = read('backend/src/cart-completion.js');
  const launcher = read('backend/src/launcher.js');

  assert.match(apiClient, /serverCart\(\)/);
  assert.match(apiClient, /setServerCartItem\(/);
  assert.match(apiClient, /updateServerCartItem\(/);
  assert.match(apiClient, /removeServerCartItem\(/);
  assert.match(apiClient, /cancelOrder\(/);
  assert.match(apiClient, /returnOrder\(/);
  assert.match(apiClient, /invoice\(/);
  assert.match(apiClient, /notifications\(/);
  assert.match(apiClient, /createSupportTicket\(/);
  assert.match(apiClient, /adminRefundOrder\(/);

  assert.match(androidMain, /cancelOrder\(/);
  assert.match(androidMain, /returnOrder\(/);
  assert.match(androidMain, /invoice\(/);
  assert.match(androidMain, /createSupportTicket\(/);

  assert.match(gap, /return-request/);
  assert.match(gap, /recently-viewed/);
  assert.match(gap, /support\/tickets/);
  assert.match(gap, /notifications/);
  assert.match(cart, /v1\/checkout/);

  assert.match(seller, /seller\/onboarding/);
  assert.match(seller, /admin\/sellers\/onboarding/);
  assert.match(rider, /rider\/deliveries/);
  assert.match(rider, /admin\/deliveries\/assign/);

  assert.match(launcher, /marketplace-gap-completion/);
  assert.match(launcher, /cart-completion/);
  assert.match(launcher, /seller-onboarding/);
  assert.match(launcher, /rider-delivery/);
});
