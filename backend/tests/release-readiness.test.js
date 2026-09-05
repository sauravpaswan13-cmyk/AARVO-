import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';

const root = path.resolve(process.cwd(), '..');
const read = (relative) => fs.readFileSync(path.join(root, relative), 'utf8');

test('release readiness: backend production contracts are present', () => {
  const server = read('backend/src/server.js');
  const compose = read('backend/docker-compose.production.yml');
  const env = read('backend/.env.production.example');

  for (const required of [
    "app.get('/health'",
    "app.post('/v1/auth/register'",
    "app.post('/v1/auth/login'",
    "app.post('/v1/orders'",
    "app.post('/v1/payments/verify'",
    "app.post('/v1/orders/:id/cancel'",
    "app.post('/v1/orders/:id/tracking'",
    "app.post('/v1/orders/:id/reviews'",
    "app.post('/v1/orders/:id/disputes'",
    "app.post('/v1/webhooks/razorpay'",
    'BEGIN',
    'FOR UPDATE',
    'COMMIT',
    'ROLLBACK',
    'timingSafeEqual',
    'RAZORPAY_WEBHOOK_SECRET'
  ]) assert.ok(server.includes(required), `missing backend contract: ${required}`);

  assert.match(compose, /node src\/migrate\.js && node src\/server\.js/);
  assert.match(compose, /127\.0\.0\.1:8080\/health/);
  assert.match(env, /JWT_SECRET=/);
  assert.match(env, /RAZORPAY_KEY_ID=/);
  assert.match(env, /RAZORPAY_KEY_SECRET=/);
  assert.match(env, /RAZORPAY_WEBHOOK_SECRET=/);
  assert.doesNotMatch(env, /demo|fake|test[_-]?payment/i);
});

test('release readiness: Android uses exact paise money model and HTTPS API guard', () => {
  const product = read('app/src/main/java/com/aarvo/data/Product.kt');
  const api = read('app/src/main/java/com/aarvo/network/AarvoApiClient.kt');
  const gradle = read('app/build.gradle.kts');

  assert.match(product, /val pricePaise: Long/);
  assert.match(product, /val displayPrice: String/);
  assert.match(product, /pricePaise \/ 100/);
  assert.match(product, /pricePaise % 100/);
  assert.match(api, /BuildConfig\.AARVO_API_BASE_URL/);
  assert.match(api, /https:\\/\\//);
  assert.match(gradle, /AARVO_API_BASE_URL/);
});

test('release readiness: no temporary internal hardening workflows remain', () => {
  const workflowDir = path.join(root, '.github', 'workflows');
  const files = fs.readdirSync(workflowDir);
  assert.ok(!files.some((name) => /hardening/i.test(name)), 'temporary hardening workflow remains');
  assert.ok(files.some((name) => name === 'android.yml'), 'canonical Android CI workflow missing');
});
