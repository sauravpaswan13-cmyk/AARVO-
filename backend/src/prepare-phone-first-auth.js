import fs from 'node:fs';

const file = new URL('./server.js', import.meta.url);
let source = fs.readFileSync(file, 'utf8');

if (source.includes("/v1/auth/verify-phone")) {
  console.log('Phone-first auth already prepared');
  process.exit(0);
}

const authStart = source.indexOf("app.post('/v1/auth/register'");
const productsStart = source.indexOf("app.get('/v1/products'");
if (authStart < 0 || productsStart < 0 || productsStart <= authStart) throw new Error('Unable to locate auth route block');

const helpers = `
const normalizeIndianPhone = (value) => {
  const raw = String(value || '').trim().replace(/\\s+/g, '');
  const digits = raw.replace(/^\\+/, '');
  const phone = digits.startsWith('91') && digits.length === 12 ? digits.slice(2) : digits;
  if (!/^[6-9]\\d{9}$/.test(phone)) throw httpError(400, 'INVALID_PHONE');
  return phone;
};
const optionalEmail = (value) => {
  const email = String(value || '').trim().toLowerCase();
  if (!email) return null;
  if (!/^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$/.test(email)) throw httpError(400, 'INVALID_EMAIL');
  return email;
};
const otpHash = (code) => createHmac('sha256', JWT_SECRET).update(String(code)).digest('hex');
const makeOtp = () => String(Math.floor(100000 + Math.random() * 900000));
const sendOtp = async (phone, code) => {
  if (process.env.OTP_DEV_MODE === 'true') return { delivered: false, preview: code };
  const authkey = process.env.MSG91_AUTH_KEY, sender = process.env.MSG91_SENDER_ID;
  if (!authkey || !sender) throw httpError(503, 'OTP_PROVIDER_NOT_CONFIGURED');
  const params = new URLSearchParams({ authkey, mobile: '91' + phone, message: 'Your AARVO verification code is ' + code + '. It expires in 10 minutes.', sender, otp: code, otp_expiry: '10', otp_length: '6' });
  const response = await fetch('https://api.msg91.com/api/sendotp.php?' + params.toString());
  const body = await response.json().catch(() => ({}));
  if (!response.ok || body.type !== 'success') throw httpError(502, 'OTP_DELIVERY_FAILED');
  return { delivered: true };
};
const createPhoneChallenge = async (userId, phone) => {
  const recent = await pool.query('SELECT last_sent_at FROM phone_verification_challenges WHERE user_id=$1 AND consumed_at IS NULL ORDER BY created_at DESC LIMIT 1', [userId]);
  if (recent.rowCount && Date.now() - new Date(recent.rows[0].last_sent_at).getTime() < 60000) throw httpError(429, 'OTP_RESEND_TOO_SOON');
  const code = makeOtp(), id = randomUUID();
  await pool.query('UPDATE phone_verification_challenges SET consumed_at=now() WHERE user_id=$1 AND consumed_at IS NULL', [userId]);
  await pool.query('INSERT INTO phone_verification_challenges(id,user_id,phone,code_hash,expires_at) VALUES($1,$2,$3,$4,now()+interval \'10 minutes\')', [id, userId, phone, otpHash(code)]);
  const delivery = await sendOtp(phone, code);
  return { challengeId: id, ...delivery };
};
`;

const authRoutes = `
app.post('/v1/auth/register', { config: { rateLimit: { max: 10, timeWindow: '1 minute' } } }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  if (!JWT_SECRET) return reply.code(503).send({ error: 'AUTH_NOT_CONFIGURED' });
  const { email, password, displayName, role = 'BUYER', phone } = request.body || {};
  const normalizedPhone = normalizeIndianPhone(phone), normalizedEmail = optionalEmail(email), normalizedRole = String(role).toUpperCase();
  if (!password || String(password).length < 8 || !String(displayName || '').trim()) return reply.code(400).send({ error: 'INVALID_REGISTRATION' });
  if (!['BUYER', 'SELLER'].includes(normalizedRole)) return reply.code(400).send({ error: 'INVALID_ROLE' });
  const id = randomBytes(12).toString('hex'), client = await pool.connect();
  try {
    await client.query('BEGIN');
    const user = await client.query('INSERT INTO users(id,email,display_name,password_hash,role,phone,phone_verified) VALUES($1,$2,$3,$4,$5,$6,false) RETURNING id,email,display_name,role,phone,phone_verified', [id, normalizedEmail, String(displayName).trim(), hashPassword(String(password)), normalizedRole, normalizedPhone]);
    if (normalizedRole === 'SELLER') await client.query('INSERT INTO seller_profiles(seller_id,phone) VALUES($1,$2)', [id, normalizedPhone]);
    await audit(client, { sub: id, role: normalizedRole }, 'USER', id, 'REGISTERED');
    await client.query('COMMIT');
    try { const challenge = await createPhoneChallenge(id, normalizedPhone); return reply.code(201).send({ verificationRequired: true, challengeId: challenge.challengeId, otpPreview: challenge.preview || null, user: user.rows[0] }); }
    catch (deliveryError) { return reply.code(deliveryError.statusCode || 503).send({ error: deliveryError.publicMessage || 'OTP_DELIVERY_FAILED', user: user.rows[0] }); }
  } catch (error) { await client.query('ROLLBACK'); if (error.code === '23505') return reply.code(409).send({ error: error.constraint?.includes('phone') ? 'PHONE_ALREADY_REGISTERED' : 'EMAIL_ALREADY_REGISTERED' }); throw error; } finally { client.release(); }
});

app.post('/v1/auth/verify-phone', { config: { rateLimit: { max: 10, timeWindow: '1 minute' } } }, async (request, reply) => {
  if (!pool || !JWT_SECRET) return reply.code(503).send({ error: 'AUTH_NOT_CONFIGURED' });
  const phone = normalizeIndianPhone(request.body?.phone), otp = String(request.body?.otp || '').trim();
  if (!/^\\d{6}$/.test(otp)) return reply.code(400).send({ error: 'INVALID_OTP' });
  const client = await pool.connect();
  try { await client.query('BEGIN'); const challenge = await client.query('SELECT id,user_id,phone,code_hash,expires_at,attempts FROM phone_verification_challenges WHERE phone=$1 AND consumed_at IS NULL ORDER BY created_at DESC LIMIT 1 FOR UPDATE', [phone]); if (!challenge.rowCount) throw httpError(400, 'OTP_NOT_FOUND'); const c=challenge.rows[0]; if (new Date(c.expires_at).getTime() < Date.now()) throw httpError(400,'OTP_EXPIRED'); if (c.attempts >= 5) throw httpError(429,'OTP_TOO_MANY_ATTEMPTS'); if (otpHash(otp) !== c.code_hash) { await client.query('UPDATE phone_verification_challenges SET attempts=attempts+1 WHERE id=$1',[c.id]); throw httpError(400,'INVALID_OTP'); } await client.query('UPDATE phone_verification_challenges SET consumed_at=now() WHERE id=$1',[c.id]); const result=await client.query('UPDATE users SET phone_verified=true,phone_verified_at=now() WHERE id=$1 RETURNING id,email,display_name,role,phone,phone_verified',[c.user_id]); if(!result.rowCount) throw httpError(404,'USER_NOT_FOUND'); const user=result.rows[0]; await audit(client,{sub:user.id,role:user.role},'USER',user.id,'PHONE_VERIFIED'); await client.query('COMMIT'); return { user, token: issueToken(user) }; } catch(error){await client.query('ROLLBACK'); throw error;} finally{client.release();}
});

app.post('/v1/auth/resend-phone-otp', { config: { rateLimit: { max: 5, timeWindow: '10 minutes' } } }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  const phone = normalizeIndianPhone(request.body?.phone); const result = await pool.query('SELECT id,phone,phone_verified FROM users WHERE phone=$1',[phone]); if(!result.rowCount)return reply.code(404).send({error:'USER_NOT_FOUND'}); if(result.rows[0].phone_verified)return {alreadyVerified:true}; const challenge=await createPhoneChallenge(result.rows[0].id,phone); return {verificationRequired:true,challengeId:challenge.challengeId,otpPreview:challenge.preview||null};
});

app.post('/v1/auth/login', { config: { rateLimit: { max: 10, timeWindow: '1 minute' } } }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' }); if (!JWT_SECRET) return reply.code(503).send({ error: 'AUTH_NOT_CONFIGURED' });
  const phone = normalizeIndianPhone(request.body?.phone), password = String(request.body?.password || '');
  const result = await pool.query('SELECT id,email,display_name,role,phone,phone_verified,password_hash FROM users WHERE phone=$1',[phone]);
  if (!result.rowCount || !verifyPassword(password,result.rows[0].password_hash)) return reply.code(401).send({error:'INVALID_CREDENTIALS'});
  const {password_hash,...user}=result.rows[0]; if(!user.phone_verified){ return reply.code(403).send({error:'PHONE_NOT_VERIFIED'}); }
  return {user,token:issueToken(user)};
});
`;

source = source.slice(0, authStart) + helpers + authRoutes + source.slice(productsStart);
fs.writeFileSync(file, source);
console.log('Prepared phone-first authentication routes');
