export async function registerMsg91WidgetAuth({ app, pool, issueToken, normalizePhone }) {
  app.post('/v1/auth/verify-msg91-token', { config: { rateLimit: { max: 10, timeWindow: '1 minute' } } }, async (request, reply) => {
    if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
    if (!process.env.MSG91_AUTH_KEY) return reply.code(503).send({ error: 'MSG91_AUTH_NOT_CONFIGURED' });

    const phone = normalizePhone(request.body?.phone);
    const accessToken = String(request.body?.accessToken || '').trim();
    if (!phone || !accessToken) return reply.code(400).send({ error: 'INVALID_MSG91_VERIFICATION' });

    const body = new URLSearchParams({ authkey: process.env.MSG91_AUTH_KEY, 'access-token': accessToken });
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
    const verified = response.ok && !['false', '0', 'failed', 'failure', 'error'].includes(String(verification.type || verification.status || '').toLowerCase());
    if (!verified) return reply.code(401).send({ error: 'MSG91_TOKEN_INVALID' });

    const userResult = await pool.query('SELECT id,email,display_name,role,phone,phone_verified FROM users WHERE phone=$1', [phone]);
    if (!userResult.rowCount) return reply.code(404).send({ error: 'USER_NOT_FOUND' });
    const user = userResult.rows[0];
    await pool.query('UPDATE users SET phone_verified=true,phone_verified_at=now() WHERE id=$1', [user.id]);
    const refreshed = { ...user, phone_verified: true };
    return { user: refreshed, token: issueToken(refreshed), verified: true };
  });
}
