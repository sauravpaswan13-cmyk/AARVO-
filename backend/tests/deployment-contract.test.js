import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const compose = fs.readFileSync(new URL('../docker-compose.production.yml', import.meta.url), 'utf8');
const migrate = fs.readFileSync(new URL('../src/migrate.js', import.meta.url), 'utf8');
const dockerfile = fs.readFileSync(new URL('../Dockerfile', import.meta.url), 'utf8');
const envExample = fs.readFileSync(new URL('../.env.production.example', import.meta.url), 'utf8');

test('production compose requires external secrets and persists PostgreSQL data', () => {
  assert.match(compose, /POSTGRES_PASSWORD:\s*\$\{POSTGRES_PASSWORD:\?Set POSTGRES_PASSWORD/);
  assert.match(compose, /JWT_SECRET:\s*\$\{JWT_SECRET:\?Set JWT_SECRET/);
  assert.match(compose, /RAZORPAY_KEY_SECRET:\s*\$\{RAZORPAY_KEY_SECRET:\?Set RAZORPAY_KEY_SECRET/);
  assert.match(compose, /aarvo-postgres:\/var\/lib\/postgresql\/data/);
  assert.match(compose, /condition: service_healthy/);
});

test('production API has a container health check for the HTTP health endpoint', () => {
  assert.match(compose, /healthcheck:/);
  assert.match(compose, /127\.0\.0\.1:8080\/health/);
  assert.match(compose, /start_period:\s*10s/);
});

test('production container applies schema and numbered migrations before starting API', () => {
  assert.match(dockerfile, /COPY schema\.sql \.\/schema\.sql/);
  assert.match(dockerfile, /COPY migrations \.\/migrations/);
  assert.match(compose, /node src\/migrate\.js && node src\/server\.js/);
  assert.match(migrate, /schema\.sql/);
  assert.match(migrate, /\.filter\(/);
  assert.match(migrate, /\.filter\(.*\\d\+_\.\*\\\.sql/);
  assert.match(migrate, /\.sort\(\)/);
  assert.match(migrate, /BEGIN/);
  assert.match(migrate, /COMMIT/);
  assert.match(migrate, /ROLLBACK/);
});

test('production environment template keeps credentials out of source control', () => {
  assert.match(envExample, /POSTGRES_PASSWORD=/);
  assert.match(envExample, /JWT_SECRET=/);
  assert.match(envExample, /RAZORPAY_KEY_ID=rzp_live_REPLACE_ME/);
  assert.match(envExample, /RAZORPAY_KEY_SECRET=/);
  assert.match(envExample, /RAZORPAY_WEBHOOK_SECRET=/);
});
