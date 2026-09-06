import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (relative) => fs.readFileSync(new URL(relative, import.meta.url), 'utf8');

test('release readiness: backend production contracts are present', () => {
  const server = read('../src/server.js');
  const compose = read('../docker-compose.production.yml');
  const env = read('../.env.production.example');

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
    'BEGIN', 'FOR UPDATE', 'COMMIT', 'ROLLBACK', 'timingSafeEqual', 'RAZORPAY_WEBHOOK_SECRET'
  ]) assert.ok(server.includes(required), `missing backend contract: ${required}`);

  assert.match(compose, /node src\/migrate\.js && node src\/server\.js/);
  assert.match(compose, /127\.0\.0\.1:8080\/health/);
  assert.match(env, /JWT_SECRET=/);
  assert.match(env, /RAZORPAY_KEY_ID=/);
  assert.match(env, /RAZORPAY_KEY_SECRET=/);
  assert.match(env, /RAZORPAY_WEBHOOK_SECRET=/);
  assert.match(env, /rzp_live_REPLACE_ME/);
});

test('release readiness: Android uses exact paise money model and HTTPS API guard', () => {
  const product = read('../../app/src/main/java/com/aarvo/data/Product.kt');
  const api = read('../../app/src/main/java/com/aarvo/network/AarvoApiClient.kt');
  const gradle = read('../../app/build.gradle.kts');

  assert.match(product, /val pricePaise: Long/);
  assert.match(product, /val displayPrice: String/);
  assert.match(product, /pricePaise \/ 100/);
  assert.match(product, /pricePaise % 100/);
  assert.match(api, /BuildConfig\.AARVO_API_BASE_URL/);
  assert.match(api, /URI\(baseUrl\)/);
  assert.match(api, /uri\.scheme\.equals\(\"https\", ignoreCase = true\\)/);
  assert.match(gradle, /AARVO_API_BASE_URL/);
});

test('release readiness: no temporary internal hardening workflows remain', () => {
  const workflowDir = new URL('../../.github/workflows/', import.meta.url);
  const files = fs.readdirSync(workflowDir);
  assert.ok(!files.some((name) => /hardening/i.test(name)), 'temporary hardening workflow remains');
  assert.ok(files.includes('android.yml'), 'canonical Android CI workflow missing');
});
