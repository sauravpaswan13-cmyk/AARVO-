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

// Register MSG91 access-token verification directly in the runtime server.
// This avoids relying on a second launcher transformation, which previously
// left POST /v1/auth/verify-msg91-token unavailable on Render.
if (!source.includes("import { registerMsg91WidgetAuth } from './msg91-widget-auth.js';")) {
  source = source.replace(
    "import { createHmac, randomBytes, randomUUID, scryptSync, timingSafeEqual } from 'node:crypto';",
    "import { createHmac, randomBytes, randomUUID, scryptSync, timingSafeEqual } from 'node:crypto';\nimport { registerMsg91WidgetAuth } from './msg91-widget-auth.js';"
  );
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

if (!source.includes('await registerMsg91WidgetAuth({ app, pool, issueToken, normalizePhone });')) {
  source = source.replace(
    "app.listen(PORT, '0.0.0.0', () => {",
    "await registerMsg91WidgetAuth({ app, pool, issueToken, normalizePhone });\napp.listen(PORT, '0.0.0.0', () => {"
  );
}

await fs.writeFile(serverPath, source, 'utf8');
await import('./launcher.js');
