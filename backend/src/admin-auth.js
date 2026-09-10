import { timingSafeEqual } from 'node:crypto';

export function registerAdminAuth({ app, pool, issueToken, verifyPassword }) {
  app.post('/v1/auth/admin/login', { config: { rateLimit: { max: 5, timeWindow: '1 minute' } } }, async (request, reply) => {
    if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
    if (!process.env.JWT_SECRET) return reply.code(503).send({ error: 'AUTH_NOT_CONFIGURED' });

    const email = String(request.body?.email || '').trim().toLowerCase();
    const password = String(request.body?.password || '');
    if (!email || !password) return reply.code(400).send({ error: 'INVALID_ADMIN_LOGIN' });

    const result = await pool.query(
      'SELECT id,email,display_name,role,password_hash,phone,phone_verified FROM users WHERE email=$1 AND role=\'ADMIN\' LIMIT 1',
      [email],
    );
    if (!result.rowCount) return reply.code(401).send({ error: 'INVALID_ADMIN_CREDENTIALS' });

    const user = result.rows[0];
    if (!verifyPassword(password, user.password_hash)) return reply.code(401).send({ error: 'INVALID_ADMIN_CREDENTIALS' });
    if (String(user.role).toUpperCase() !== 'ADMIN') return reply.code(403).send({ error: 'ADMIN_ROLE_REQUIRED' });

    const { password_hash: _passwordHash, ...safeUser } = user;
    return { user: safeUser, token: issueToken(safeUser), admin: true };
  });
}
