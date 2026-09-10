import crypto from 'node:crypto';

function findAccessToken(value) {
  if (value == null) return null;
  if (typeof value === 'string') {
    const v = value.trim().replace(/^"|"$/g, '');
    if (v.startsWith('eyJ') && v.split('.').length === 3) return v;
    try { return findAccessToken(JSON.parse(v)); } catch { return null; }
  }
  if (Array.isArray(value)) {
    for (const item of value) { const token = findAccessToken(item); if (token) return token; }
    return null;
  }
  if (typeof value === 'object') {
    for (const [key, item] of Object.entries(value)) {
      if (/^(access[-_]?token|jwt|token)$/i.test(key) && typeof item === 'string' && item.trim()) return item.trim();
    }
    for (const item of Object.values(value)) { const token = findAccessToken(item); if (token) return token; }
  }
  return null;
}

function findAccessTokenInHeaders(headers) {
  const candidates = [
    headers.get('access-token'),
    headers.get('access_token'),
    headers.get('x-access-token'),
    headers.get('authorization')
  ];
  for (const value of candidates) {
    if (!value) continue;
    const token = String(value).replace(/^Bearer\s+/i, '').trim();
    if (token.startsWith('eyJ') && token.split('.').length === 3) return token;
  }
  return null;
}

async function createVerifiedUserSession({ pool, issueToken, phone }) {
  let userResult = await pool.query('SELECT id,email,display_name,role,phone,phone_verified FROM users WHERE phone=$1', [phone]);
  if (!userResult.rowCount) {
    const id = crypto.randomUUID();
    try {
      userResult = await pool.query(
        `INSERT INTO users (id, display_name, role, phone, phone_verified, phone_verified_at)
         VALUES ($1, $2, 'BUYER', $3, true, now())
         RETURNING id,email,display_name,role,phone,phone_verified`,
        [id, `AARVO User ${phone.slice(-4)}`, phone]
      );
    } catch (error) {
      if (error?.code === '23505') {
        userResult = await pool.query('SELECT id,email,display_name,role,phone,phone_verified FROM users WHERE phone=$1', [phone]);
      } else {
        throw error;
      }
    }
  }
  if (!userResult.rowCount) return null;
  const user = userResult.rows[0];
  await pool.query('UPDATE users SET phone_verified=true,phone_verified_at=now() WHERE id=$1', [user.id]);
  const refreshed = { ...user, phone_verified: true };
  return { user: refreshed, token: issueToken(refreshed), verified: true };
}

export async function registerMsg91WidgetAuth({ app, pool, issueToken, normalizePhone }) {
  app.post('/v1/auth/verify-msg91-token', { config: { rateLimit: { max: 10, timeWindow: '1 minute' } } }, async (request, reply) => {
    if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
    const authKey = String(process.env.MSG91_AUTH_KEY || process.env.MSG91_AUTHKEY || process.env.MSG91_AUTH_KEY_ID || '').trim();
    if (!authKey) {
      request.log.error('MSG91 auth key is not configured on AARVO API');
      return reply.code(503).send({ error: 'MSG91_AUTH_NOT_CONFIGURED' });
    }

    const phone = normalizePhone(request.body?.phone);
    const accessToken = String(request.body?.accessToken || '').trim();
    request.log.info({ phoneLast4: phone ? phone.slice(-4) : '', accessTokenPresent: Boolean(accessToken) }, 'AARVO MSG91 verification started');
    if (!phone || !accessToken) return reply.code(400).send({ error: 'INVALID_MSG91_VERIFICATION' });

    const body = new URLSearchParams({ authkey: authKey, 'access-token': accessToken });
    let response;
    try {
      response = await fetch('https://control.msg91.com/api/v5/widget/verifyAccessToken', {
        method: 'POST',
        headers: { 'content-type': 'application/x-www-form-urlencoded' },
        body,
        signal: AbortSignal.timeout(10000)
      });
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
    request.log.info({ providerHttpStatus: response.status, providerType, providerStatus, verified }, 'AARVO MSG91 verification result');
    if (!verified) {
      request.log.warn({ providerHttpStatus: response.status, providerType, providerStatus }, 'MSG91 access-token rejected');
      return reply.code(401).send({ error: 'MSG91_TOKEN_INVALID' });
    }

    try {
      const session = await createVerifiedUserSession({ pool, issueToken, phone });
      if (!session) return reply.code(404).send({ error: 'USER_NOT_FOUND' });
      request.log.info({ phoneLast4: phone.slice(-4), userId: session.user.id }, 'AARVO MSG91 login session created');
      return session;
    } catch (error) {
      request.log.error({ err: error }, 'Unable to create AARVO user after MSG91 verification');
      return reply.code(500).send({ error: 'USER_CREATE_FAILED' });
    }
  });

  // Verify the OTP on AARVO's server so the Android client never needs the MSG91 authkey.
  // Some MSG91 widget responses confirm the OTP but do not return the optional JWT access-token.
  // A successful verifyOtp response is already sufficient proof of OTP ownership, so in that
  // case we create the AARVO session directly instead of returning MSG91_ACCESS_TOKEN_MISSING.
  app.post('/v1/auth/verify-msg91-otp', { config: { rateLimit: { max: 10, timeWindow: '1 minute' } } }, async (request, reply) => {
    const authKey = String(process.env.MSG91_AUTH_KEY || process.env.MSG91_AUTHKEY || process.env.MSG91_AUTH_KEY_ID || '').trim();
    const phone = normalizePhone(request.body?.phone);
    const reqId = String(request.body?.reqId || request.body?.requestId || '').trim();
    const otp = String(request.body?.otp || '').trim();
    if (!authKey) return reply.code(503).send({ error: 'MSG91_AUTH_NOT_CONFIGURED' });
    if (!phone || !reqId || !/^\d{4,8}$/.test(otp)) return reply.code(400).send({ error: 'INVALID_MSG91_OTP' });

    let response;
    try {
      response = await fetch('https://api.msg91.com/api/v5/widget/verifyOtp', {
        method: 'POST',
        headers: { 'content-type': 'application/json', authkey: authKey },
        body: JSON.stringify({ reqId, otp }),
        signal: AbortSignal.timeout(10000)
      });
    } catch (error) {
      request.log.error({ err: error }, 'MSG91 OTP verification request failed');
      return reply.code(503).send({ error: 'MSG91_VERIFICATION_UNAVAILABLE' });
    }

    const rawBody = await response.text();
    let result = {};
    try { result = JSON.parse(rawBody || '{}'); } catch { result = rawBody; }
    if (!response.ok) {
      request.log.warn({ providerHttpStatus: response.status }, 'MSG91 OTP verification rejected');
      return reply.code(401).send({ error: 'MSG91_OTP_INVALID' });
    }

    const accessToken = findAccessToken(result) || findAccessTokenInHeaders(response.headers);
    if (!accessToken) {
      request.log.info({ providerHttpStatus: response.status }, 'MSG91 OTP verified without access token; creating AARVO session directly');
    } else {
      const injected = await app.inject({
        method: 'POST',
        url: '/v1/auth/verify-msg91-token',
        payload: { phone, accessToken }
      });
      let payload = {};
      try { payload = JSON.parse(injected.body || '{}'); } catch { payload = {}; }
      return reply.code(injected.statusCode).send(payload);
    }

    try {
      const session = await createVerifiedUserSession({ pool, issueToken, phone });
      if (!session) return reply.code(404).send({ error: 'USER_NOT_FOUND' });
      request.log.info({ phoneLast4: phone.slice(-4), userId: session.user.id }, 'AARVO MSG91 OTP login session created');
      return session;
    } catch (error) {
      request.log.error({ err: error }, 'Unable to create AARVO user after successful MSG91 OTP');
      return reply.code(500).send({ error: 'USER_CREATE_FAILED' });
    }
  });
}
