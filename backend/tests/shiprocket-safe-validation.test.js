import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
const source = fs.readFileSync(path.join(ROOT, 'backend/src/third-party-delivery.js'), 'utf8');

test('Shiprocket diagnostic is Admin-only and read-only', () => {
  assert.match(source, /app\.post\('\/v1\/admin\/delivery\/shiprocket\/validate',\{preHandler:requireRole\('ADMIN'\)\}/);
  assert.match(source, /loginShiprocket\(true\)/);
  assert.match(source, /shiprocketPath\.listPickups/);
  assert.match(source, /pickupListAccessible:true/);
  const diagnostic = source.slice(source.indexOf("app.post('/v1/admin/delivery/shiprocket/validate'"), source.indexOf("app.get('/v1/delivery/provider-status'"));
  assert.doesNotMatch(diagnostic, /shiprocketPath\.(create|awb|pickup|addPickup)/);
  assert.match(diagnostic, /shipmentCreated:false/);
  assert.match(diagnostic, /awbAssigned:false/);
  assert.match(diagnostic, /pickupBooked:false/);
});

test('Shiprocket package measurements must be explicitly configured', () => {
  assert.match(source, /SHIPROCKET_PACKAGE_DIMENSIONS_REQUIRED/);
  assert.match(source, /Number\(process\.env\.SHIPROCKET_PACKAGE_WEIGHT_KG\|\|0\)/);
});

test('Shiprocket live booking remains feature-flagged off by default', () => {
  assert.match(source, /THIRD_PARTY_DELIVERY_ENABLED\|\|'false'/);
  assert.match(source, /if\(!enabled\(\)\)/);
});

test('Shiprocket shipment processing is seller-scoped and requires that seller to pack', () => {
  assert.match(source, /packedSellerIds/);
  assert.match(source, /actor_id::text AS seller_id/);
  assert.match(source, /WHERE order_id=\$1 AND seller_id=\$2/);
  assert.match(source, /INSERT INTO third_party_delivery_shipments\(order_id,seller_id,provider/);
});

test('Shiprocket queue retries failures and recovers stale worker leases', () => {
  assert.match(source, /enqueueSellerShipment/);
  assert.match(source, /processDeliveryQueue/);
  assert.match(source, /status IN \('PENDING','RETRY'\)/);
  assert.match(source, /attempts>=8/);
  assert.match(source, /WORKER_LEASE_EXPIRED/);
  assert.match(source, /app.get\('\/v1\/admin\/delivery\/jobs'/);
  assert.match(source, /app.post\('\/v1\/admin\/delivery\/jobs\/:id\/retry'/);
});
