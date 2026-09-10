import crypto from 'node:crypto';

export async function registerMsg91WidgetAuth({ app, pool, issueToken, normalizePhone }) {
  app.post('/v1/auth/verify-msg91-token', { config: { rateLimit: { max: 10, timeWindow: '1 minute' } } }, async (request, reply) => {
    if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
    const authKey = String(process.env.MSG91_AUTH_KEY || process.env.MSG91_AUTHKEY || '').trim();
    if (!authKey) return reply.code(503).send({ error: 'MSG91_AUTH_NOT_CONFIGURED' });

    const phone = normalizePhone(request.body?.phone);
    const accessToken = String(request.body?.accessToken || '').trim();
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
    const providerType = String(verification.type || '').toLowerCase();
    const providerStatus = String(verification.status || '').toLowerCase();
    const verified = response.ok && !['false', '0', 'failed', 'failure', 'error'].includes(providerType || providerStatus);
    if (!verified) {
      request.log.warn({ providerHttpStatus: response.status, providerType, providerStatus }, 'MSG91 access-token rejected');
      return reply.code(401).send({ error: 'MSG91_TOKEN_INVALID' });
    }

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
          request.log.error({ err: error }, 'Unable to create AARVO user after MSG91 verification');
          return reply.code(500).send({ error: 'USER_CREATE_FAILED' });
        }
      }
    }

    if (!userResult.rowCount) return reply.code(404).send({ error: 'USER_NOT_FOUND' });
    const user = userResult.rows[0];
    await pool.query('UPDATE users SET phone_verified=true,phone_verified_at=now() WHERE id=$1', [user.id]);
    const refreshed = { ...user, phone_verified: true };
    return { user: refreshed, token: issueToken(refreshed), verified: true };
  });
}
