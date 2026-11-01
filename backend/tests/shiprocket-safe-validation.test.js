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

test('Shiprocket live booking remains feature-flagged off by default', () => {
  assert.match(source, /THIRD_PARTY_DELIVERY_ENABLED\|\|'false'/);
  assert.match(source, /if\(!enabled\(\)\)/);
});
