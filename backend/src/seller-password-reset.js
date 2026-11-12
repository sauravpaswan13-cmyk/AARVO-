export async function registerSellerPasswordReset({ app, pool, hashPassword, verifyPassword, sendPhoneOtp, normalizePhone }) {
  app.post('/v1/seller/password-reset/request', { config: { rateLimit: { max: 3, timeWindow: '10 minutes' } } }, async (request, reply) => {
    if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
    const phone = normalizePhone(request.body?.phone);
    if (!phone) return reply.code(400).send({ error: 'INVALID_PHONE' });
    const result = await pool.query("SELECT id,phone FROM users WHERE phone=$1 AND role='SELLER' LIMIT 1", [phone]);
    // Keep the response generic so the endpoint cannot be used to enumerate seller accounts.
    if (!result.rowCount) return { sent: true, expiresInSeconds: 600 };
    const user = result.rows[0];
    const recent = await pool.query("SELECT created_at FROM phone_verification_challenges WHERE user_id=$1 AND verified_at IS NULL ORDER BY created_at DESC LIMIT 1", [user.id]);
    if (recent.rowCount && Date.now() - new Date(recent.rows[0].created_at).getTime() < 60000) {
      return reply.code(429).send({ error: 'OTP_RESEND_TOO_SOON' });
    }
    const otp = String(Math.floor(100000 + Math.random() * 900000));
    try {
      await sendPhoneOtp({ phone, otp, forceServer: true });
    } catch (error) {
      request.log.error({ err: error, phoneLast4: phone.slice(-4) }, 'seller password reset OTP delivery failed');
      return reply.code(503).send({ error: error?.code === 'OTP_PROVIDER_NOT_CONFIGURED' ? 'OTP_PROVIDER_NOT_CONFIGURED' : 'OTP_DELIVERY_FAILED' });
    }
    await pool.query("UPDATE phone_verification_challenges SET verified_at=COALESCE(verified_at,now()) WHERE user_id=$1 AND verified_at IS NULL", [user.id]);
    await pool.query("INSERT INTO phone_verification_challenges(user_id,phone,otp_hash,expires_at,attempts) VALUES($1,$2,$3,now()+interval '10 minutes',0)", [user.id, phone, hashPassword(otp)]);
    return { sent: true, expiresInSeconds: 600 };
  });

  app.post('/v1/seller/password-reset/confirm', { config: { rateLimit: { max: 5, timeWindow: '10 minutes' } } }, async (request, reply) => {
    if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
    const phone = normalizePhone(request.body?.phone);
    const otp = String(request.body?.otp || '').trim();
    const password = String(request.body?.newPassword || '');
    if (!phone || !/^\\d{6}$/.test(otp) || password.length < 8 || password.length > 128) {
      return reply.code(400).send({ error: 'INVALID_RESET_DETAILS' });
    }
    const client = await pool.connect();
    try {
      await client.query('BEGIN');
      const challenge = await client.query(
        "SELECT c.id,c.user_id,c.otp_hash,c.expires_at,c.attempts FROM phone_verification_challenges c JOIN users u ON u.id=c.user_id WHERE c.phone=$1 AND u.role='SELLER' AND c.verified_at IS NULL ORDER BY c.created_at DESC LIMIT 1 FOR UPDATE OF c",
        [phone]
      );
      if (!challenge.rowCount) { await client.query('ROLLBACK'); return reply.code(400).send({ error: 'OTP_NOT_FOUND_OR_EXPIRED' }); }
      const c = challenge.rows[0];
      if (new Date(c.expires_at).getTime() < Date.now()) { await client.query('ROLLBACK'); return reply.code(400).send({ error: 'OTP_EXPIRED' }); }
      if (Number(c.attempts) >= 5) { await client.query('ROLLBACK'); return reply.code(429).send({ error: 'OTP_TOO_MANY_ATTEMPTS' }); }
      if (!verifyPassword(otp, c.otp_hash)) {
        await client.query('UPDATE phone_verification_challenges SET attempts=attempts+1 WHERE id=$1', [c.id]);
        await client.query('COMMIT');
        return reply.code(400).send({ error: 'INVALID_OTP' });
      }
      await client.query('UPDATE users SET password_hash=$1 WHERE id=$2 AND role=$3', [hashPassword(password), c.user_id, 'SELLER']);
      await client.query('UPDATE phone_verification_challenges SET verified_at=now() WHERE id=$1', [c.id]);
      await client.query("UPDATE phone_verification_challenges SET verified_at=COALESCE(verified_at,now()) WHERE user_id=$1 AND id<>$2 AND verified_at IS NULL", [c.user_id, c.id]);
      await client.query('COMMIT');
      return { reset: true };
    } catch (error) {
      await client.query('ROLLBACK');
      request.log.error({ err: error }, 'seller password reset failed');
      return reply.code(500).send({ error: 'PASSWORD_RESET_FAILED' });
    } finally {
      client.release();
    }
  });
}
