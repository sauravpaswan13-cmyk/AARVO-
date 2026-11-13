import { randomInt, randomUUID } from 'node:crypto';
import { sendPhoneOtp } from './otp-delivery.js';

const genericRequest = { sent: true, expiresInSeconds: 600, message: 'If this number belongs to an AARVO seller account, an OTP has been sent.' };

export async function registerSellerPasswordReset({ app, pool, normalizePhone, hashPassword, verifyPassword }) {
  app.post('/v1/seller/auth/password-reset/request', { config: { rateLimit: { max: 5, timeWindow: '15 minutes' } } }, async (request, reply) => {
    if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
    const phone = normalizePhone(request.body?.phone);
    if (!phone) return reply.code(400).send({ error: 'INVALID_PHONE' });

    const found = await pool.query("SELECT id FROM users WHERE phone=$1 AND UPPER(role)='SELLER' LIMIT 1", [phone]);
    if (!found.rowCount) return genericRequest;

    const userId = found.rows[0].id;
    const recent = await pool.query(
      "SELECT COUNT(*)::int AS count FROM phone_verification_challenges WHERE user_id=$1 AND created_at > now() - interval '1 hour'",
      [userId]
    );
    if (Number(recent.rows[0]?.count || 0) >= 5) return genericRequest;

    const otp = String(randomInt(100000, 1000000));
    try {
      await sendPhoneOtp({ phone, otp, forceServer: true });
    } catch (error) {
      request.log.error({ err: error, phoneLast4: phone.slice(-4) }, 'seller password reset OTP delivery failed');
      return genericRequest;
    }

    await pool.query(
      'UPDATE phone_verification_challenges SET consumed_at=COALESCE(consumed_at,now()) WHERE user_id=$1 AND consumed_at IS NULL',
      [userId]
    );
    await pool.query(
      "INSERT INTO phone_verification_challenges(id,user_id,phone,code_hash,expires_at,attempts) VALUES($1,$2,$3,$4,now()+interval '10 minutes',0)",
      [randomUUID(), userId, phone, hashPassword(otp)]
    );
    request.log.info({ phoneLast4: phone.slice(-4) }, 'seller password reset OTP requested');
    return genericRequest;
  });

  app.post('/v1/seller/auth/password-reset/confirm', { config: { rateLimit: { max: 8, timeWindow: '15 minutes' } } }, async (request, reply) => {
    if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
    const phone = normalizePhone(request.body?.phone);
    const otp = String(request.body?.otp || '').trim();
    const newPassword = String(request.body?.newPassword || '');
    if (!phone || !/^\d{6}$/.test(otp) || newPassword.length < 8 || newPassword.length > 128) {
      return reply.code(400).send({ error: 'INVALID_RESET_DETAILS' });
    }

    const client = await pool.connect();
    try {
      await client.query('BEGIN');
      const challenge = await client.query(
        "SELECT id,user_id,code_hash,expires_at,attempts FROM phone_verification_challenges WHERE phone=$1 AND consumed_at IS NULL ORDER BY created_at DESC LIMIT 1 FOR UPDATE",
        [phone]
      );
      if (!challenge.rowCount) {
        await client.query('ROLLBACK');
        return reply.code(400).send({ error: 'OTP_EXPIRED_OR_INVALID' });
      }
      const row = challenge.rows[0];
      if (Number(row.attempts) >= 5 || new Date(row.expires_at).getTime() <= Date.now()) {
        await client.query('UPDATE phone_verification_challenges SET consumed_at=now() WHERE id=$1', [row.id]);
        await client.query('COMMIT');
        return reply.code(400).send({ error: 'OTP_EXPIRED_OR_INVALID' });
      }
      if (!verifyPassword(otp, row.code_hash)) {
        await client.query('UPDATE phone_verification_challenges SET attempts=attempts+1 WHERE id=$1', [row.id]);
        await client.query('COMMIT');
        return reply.code(400).send({ error: 'OTP_EXPIRED_OR_INVALID' });
      }
      const updated = await client.query(
        "UPDATE users SET password_hash=$1 WHERE id=$2 AND phone=$3 AND UPPER(role)='SELLER' RETURNING id",
        [hashPassword(newPassword), row.user_id, phone]
      );
      if (!updated.rowCount) {
        await client.query('ROLLBACK');
        return reply.code(400).send({ error: 'OTP_EXPIRED_OR_INVALID' });
      }
      await client.query('UPDATE phone_verification_challenges SET consumed_at=now() WHERE id=$1', [row.id]);
      await client.query('COMMIT');
      request.log.info({ phoneLast4: phone.slice(-4) }, 'seller password reset completed');
      return { reset: true };
    } catch (error) {
      await client.query('ROLLBACK');
      throw error;
    } finally {
      client.release();
    }
  });
}
