import assert from 'node:assert/strict';
import test from 'node:test';
import { registerSellerOnboarding } from '../src/seller-onboarding.js';

function setup({ kycStatus = 'PENDING', sellerExists = true } = {}) {
  const routes = new Map();
  const calls = [];
  const audits = [];
  const app = {
    get: (path, options, handler) => routes.set(`GET ${path}`, { options, handler }),
    put: (path, options, handler) => routes.set(`PUT ${path}`, { options, handler }),
    post: (path, options, handler) => routes.set(`POST ${path}`, { options, handler }),
  };
  const pool = {
    async query(sql, params = []) {
      calls.push({ sql, params });
      if (sql.includes('SELECT seller_id,onboarding_status,kyc_review_status')) {
        return sellerExists
          ? { rowCount: 1, rows: [{ seller_id: 'seller-1', onboarding_status: 'SUBMITTED', kyc_review_status: kycStatus }] }
          : { rowCount: 0, rows: [] };
      }
      if (sql.includes('UPDATE seller_profiles SET kyc_review_status')) {
        return { rowCount: 1, rows: [{ seller_id: params[3], kyc_review_status: params[0], kyc_review_notes: params[1], kyc_reviewed_at: new Date().toISOString() }] };
      }
      if (sql.includes('UPDATE seller_profiles SET onboarding_status')) {
        return { rowCount: 1, rows: [{ seller_id: params[4], onboarding_status: params[0], verified: params[1] }] };
      }
      return { rowCount: 0, rows: [] };
    },
  };
  const requireRole = (role) => ({ requiredRole: role });
  const audit = async (...args) => audits.push(args);
  registerSellerOnboarding({ app, pool, requireRole, audit });
  const reply = () => ({
    statusCode: 200,
    payload: null,
    code(code) { this.statusCode = code; return this; },
    send(payload) { this.payload = payload; return payload; },
  });
  return { routes, calls, audits, reply };
}

test('seller KYC routes remain admin-only', () => {
  const { routes } = setup();
  assert.equal(routes.get('POST /v1/admin/sellers/:sellerId/kyc-review').options.preHandler.requiredRole, 'ADMIN');
  assert.equal(routes.get('POST /v1/admin/sellers/:sellerId/onboarding/review').options.preHandler.requiredRole, 'ADMIN');
});

test('KYC review rejects invalid states and requires rejection notes', async () => {
  const { routes, calls, reply } = setup();
  const handler = routes.get('POST /v1/admin/sellers/:sellerId/kyc-review').handler;
  let res = reply();
  await handler({ body: { status: 'APPROVED' }, params: { sellerId: 'seller-1' }, user: { sub: 'admin-1' } }, res);
  assert.equal(res.statusCode, 400);
  assert.equal(res.payload.error, 'INVALID_KYC_REVIEW_STATUS');
  assert.equal(calls.length, 0);

  res = reply();
  await handler({ body: { status: 'REJECTED', notes: '   ' }, params: { sellerId: 'seller-1' }, user: { sub: 'admin-1' } }, res);
  assert.equal(res.statusCode, 400);
  assert.equal(res.payload.error, 'KYC_REJECTION_NOTES_REQUIRED');
  assert.equal(calls.length, 0);
});

test('KYC review records reviewer, status and audit event', async () => {
  const { routes, calls, audits, reply } = setup();
  const handler = routes.get('POST /v1/admin/sellers/:sellerId/kyc-review').handler;
  const res = reply();
  const result = await handler({
    body: { status: 'VERIFIED', notes: 'Original documents checked' },
    params: { sellerId: 'seller-1' },
    user: { sub: 'admin-1' },
  }, res);
  assert.equal(result.kyc.kyc_review_status, 'VERIFIED');
  assert.equal(calls[0].params[0], 'VERIFIED');
  assert.equal(calls[0].params[2], 'admin-1');
  assert.equal(calls[0].params[3], 'seller-1');
  assert.equal(audits.length, 1);
});

test('seller approval is blocked unless KYC is VERIFIED', async () => {
  const { routes, calls, audits, reply } = setup({ kycStatus: 'IN_REVIEW' });
  const handler = routes.get('POST /v1/admin/sellers/:sellerId/onboarding/review').handler;
  const res = reply();
  await handler({
    body: { decision: 'APPROVE' },
    params: { sellerId: 'seller-1' },
    user: { sub: 'admin-1' },
  }, res);
  assert.equal(res.statusCode, 409);
  assert.equal(res.payload.error, 'SELLER_KYC_NOT_VERIFIED');
  assert.equal(res.payload.kycStatus, 'IN_REVIEW');
  assert.equal(calls.length, 1);
  assert.equal(audits.length, 0);
});

test('seller approval proceeds after KYC is VERIFIED', async () => {
  const { routes, calls, audits, reply } = setup({ kycStatus: 'VERIFIED' });
  const handler = routes.get('POST /v1/admin/sellers/:sellerId/onboarding/review').handler;
  const res = reply();
  const result = await handler({
    body: { decision: 'APPROVE' },
    params: { sellerId: 'seller-1' },
    user: { sub: 'admin-1' },
  }, res);
  assert.equal(result.onboarding.onboarding_status, 'APPROVED');
  assert.equal(calls.length, 2);
  assert.equal(audits.length, 1);
});
