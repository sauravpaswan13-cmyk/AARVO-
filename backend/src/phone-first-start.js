import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const serverPath = path.join(here, 'server.js');
let source = await fs.readFile(serverPath, 'utf8');

if (!source.includes("import { sendPhoneOtp } from './otp-delivery.js';")) {
  source = source.replace(
    "import { createHmac, randomBytes, randomUUID, scryptSync, timingSafeEqual } from 'node:crypto';",
    "import { createHmac, randomBytes, randomUUID, scryptSync, timingSafeEqual } from 'node:crypto';\nimport { sendPhoneOtp } from './otp-delivery.js';"
  );
}

const registrationImports = [
  "import { registerMsg91WidgetAuth } from './msg91-widget-auth.js';",
  "import { registerMarketplaceCompletion } from './marketplace-completion.js';",
  "import { registerCartCompletion } from './cart-completion.js';",
  "import { registerSettlementCompletion } from './settlement-completion.js';",
  "import { registerSellerOnboarding } from './seller-onboarding.js';"
];
for (const statement of registrationImports) {
  if (!source.includes(statement)) {
    source = source.replace(
      "import { createHmac, randomBytes, randomUUID, scryptSync, timingSafeEqual } from 'node:crypto';",
      "import { createHmac, randomBytes, randomUUID, scryptSync, timingSafeEqual } from 'node:crypto';\n" + statement
    );
  }
}

const start = source.indexOf("app.post('/v1/auth/resend-phone-otp'");
const end = start >= 0 ? source.indexOf("\napp.post('/v1/ai/assistant'", start) : -1;
if (start >= 0 && end > start) {
  const route = `app.post('/v1/auth/resend-phone-otp', { config: { rateLimit: { max: 5, timeWindow: '1 minute' } } }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  if (!JWT_SECRET) return reply.code(503).send({ error: 'AUTH_NOT_CONFIGURED' });
  const phone = normalizePhone(request.body?.phone);
  if (!phone) return reply.code(400).send({ error: 'INVALID_PHONE' });

  let result = await pool.query('SELECT id,email,display_name,role,phone,phone_verified FROM users WHERE phone=$1', [phone]);
  if (!result.rowCount) {
    const id = randomBytes(12).toString('hex');
    const passwordHash = hashPassword(randomUUID());
    try {
      result = await pool.query(
        'INSERT INTO users(id,email,display_name,password_hash,role,phone,phone_verified) VALUES($1,NULL,$2,$3,$4,$5,false) RETURNING id,email,display_name,role,phone,phone_verified',
        [id, 'AARVO User', passwordHash, 'BUYER', phone]
      );
    } catch (error) {
      if (error.code !== '23505') throw error;
      result = await pool.query('SELECT id,email,display_name,role,phone,phone_verified FROM users WHERE phone=$1', [phone]);
    }
  }
  if (!result.rowCount) return reply.code(404).send({ error: 'USER_NOT_FOUND' });

  const user = result.rows[0];
  const otp = String(Math.floor(100000 + Math.random() * 900000));
  const otpHash = hashPassword(otp);
  await pool.query('UPDATE phone_verification_challenges SET verified_at=COALESCE(verified_at,now()) WHERE phone=$1 AND verified_at IS NULL', [phone]);
  try {
    await sendPhoneOtp({ phone, otp });
  } catch (error) {
    request.log.error({ err: error, phone }, 'phone OTP delivery failed');
    return reply.code(error?.code === 'OTP_PROVIDER_NOT_CONFIGURED' ? 503 : 502).send({ error: error?.code === 'OTP_PROVIDER_NOT_CONFIGURED' ? 'OTP_PROVIDER_NOT_CONFIGURED' : 'OTP_DELIVERY_FAILED' });
  }
  await pool.query(
    "INSERT INTO phone_verification_challenges(user_id,phone,otp_hash,expires_at,attempts) VALUES($1,$2,$3,now()+interval '10 minutes',0)",
    [user.id, phone, otpHash]
  );
  return { sent: true, verificationRequired: true, expiresInSeconds: 600, user: { id: user.id, display_name: user.display_name, role: user.role, phone: user.phone, phone_verified: user.phone_verified } };
});`;
  source = source.slice(0, start) + route + source.slice(end);
}

if (!source.includes("POST /v1/auth/verify-msg91-token DIRECT")) {
  const listenMarker = "app.listen(PORT, '0.0.0.0', () => {";
  const directRoute = `app.post('/v1/auth/verify-msg91-token', { config: { rateLimit: { max: 10, timeWindow: '1 minute' } } }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  if (!JWT_SECRET) return reply.code(503).send({ error: 'AUTH_NOT_CONFIGURED' });
  const authKey = String(process.env.MSG91_AUTH_KEY || process.env.MSG91_AUTHKEY || process.env.MSG91_AUTH_KEY_ID || '').trim();
  if (!authKey) return reply.code(503).send({ error: 'MSG91_AUTH_NOT_CONFIGURED' });
  const phone = normalizePhone(request.body?.phone);
  const accessToken = String(request.body?.accessToken || '').trim();
  if (!phone || !accessToken) return reply.code(400).send({ error: 'INVALID_MSG91_VERIFICATION' });
  const body = new URLSearchParams({ authkey: authKey, 'access-token': accessToken });
  let response;
  try {
    response = await fetch('https://control.msg91.com/api/v5/widget/verifyAccessToken', { method: 'POST', headers: { 'content-type': 'application/x-www-form-urlencoded' }, body, signal: AbortSignal.timeout(10000) });
  } catch (error) {
    request.log.error({ err: error }, 'MSG91 access-token verification request failed');
    return reply.code(503).send({ error: 'MSG91_VERIFICATION_UNAVAILABLE' });
  }
  let verification = {};
  try { verification = await response.json(); } catch { verification = {}; }
  const providerType = String(verification.type || '').trim().toLowerCase();
  const providerStatus = String(verification.status || '').trim().toLowerCase();
  const failedValues = ['false', '0', 'failed', 'failure', 'error', 'invalid', 'rejected'];
  const failed = failedValues.includes(providerType) || failedValues.includes(providerStatus);
  const verified = response.ok && !failed;
  if (!verified) return reply.code(401).send({ error: 'MSG91_TOKEN_INVALID' });
  let userResult = await pool.query('SELECT id,email,display_name,role,phone,phone_verified FROM users WHERE phone=$1', [phone]);
  if (!userResult.rowCount) {
    const id = randomUUID();
    try {
      userResult = await pool.query('INSERT INTO users (id, display_name, role, phone, phone_verified, phone_verified_at) VALUES ($1, $2, \'BUYER\', $3, true, now()) RETURNING id,email,display_name,role,phone,phone_verified', [id, 'AARVO User ' + phone.slice(-4), phone]);
    } catch (error) {
      if (error?.code === '23505') userResult = await pool.query('SELECT id,email,display_name,role,phone,phone_verified FROM users WHERE phone=$1', [phone]);
      else { request.log.error({ err: error }, 'Unable to create AARVO user after MSG91 verification'); return reply.code(500).send({ error: 'USER_CREATE_FAILED' }); }
    }
  }
  if (!userResult.rowCount) return reply.code(404).send({ error: 'USER_NOT_FOUND' });
  const user = userResult.rows[0];
  await pool.query('UPDATE users SET phone_verified=true,phone_verified_at=now() WHERE id=$1', [user.id]);
  const refreshed = { ...user, phone_verified: true };
  return { user: refreshed, token: issueToken(refreshed), verified: true };
}); // POST /v1/auth/verify-msg91-token DIRECT\n\n`;
  source = source.replace(listenMarker, directRoute + listenMarker);
}

// Render starts this wrapper (backend/package.json -> npm start), so marketplace routes
// must be registered here as well. Without this, /v1/addresses and the other completion
// routes can return 404 even though the route modules exist in the repository.
if (!source.includes("MARKETPLACE COMPLETION ROUTES DIRECT")) {
  const listenMarker = "app.listen(PORT, '0.0.0.0', () => {";
  const registrations = `await registerMsg91WidgetAuth({ app, pool, issueToken, normalizePhone });
await registerMarketplaceCompletion({ app, pool, requireAuth, requireRole, audit });
await registerCartCompletion({ app, pool, requireRole, audit });
await registerSettlementCompletion({ app, pool, requireRole, audit, razorpay });
await registerSellerOnboarding({ app, pool, requireRole, audit }); // MARKETPLACE COMPLETION ROUTES DIRECT
`;
  source = source.replace(listenMarker, registrations + listenMarker);
}

await fs.writeFile(serverPath, source, 'utf8');
await import('./launcher.js');
