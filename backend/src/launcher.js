import fs from 'node:fs/promises';
import path from 'path';
import { fileURLToPath, pathToFileURL } from 'url';

const here = path.dirname(fileURLToPath(import.meta.url));
const serverPath = path.join(here, 'server.js');
const runtimePath = path.join(here, '.aarvo-runtime-server.mjs');
let source = await fs.readFile(serverPath, 'utf8');

if (!source.includes('registerMarketplaceCompletion')) {
  source = source.replace(
    "import { createHmac, randomBytes, randomUUID, scryptSync, timingSafeEqual } from 'node:crypto';",
    "import { createHmac, randomBytes, randomUUID, scryptSync, timingSafeEqual } from 'node:crypto';\nimport { registerMarketplaceCompletion } from './marketplace-completion.js';\nimport { registerCartCompletion } from './cart-completion.js';\nimport { registerSettlementCompletion } from './settlement-completion.js'"
  );
  source = source.replace(
    "app.listen(PORT, '0.0.0.0', () => {",
    "await registerMarketplaceCompletion({ app, pool, requireAuth, requireRole, audit });\nawait registerCartCompletion({ app, pool, requireRole, audit });\nawait registerSettlementCompletion({ app, pool, requireRole, audit, razorpay });\napp.listen(PORT, '0.0.0.0', () => {"
  );
}

if (!source.includes("commission-rules.js")) {
  source = source.replace(
    "import { registerSettlementCompletion } from './settlement-completion.js';",
    "import { registerSettlementCompletion } from './settlement-completion.js';\nimport { commissionBpsForCategory, commissionPaise } from './commission-rules.js';"
  );
  source = source.replace("const PLATFORM_FEE_BPS = Number(process.env.PLATFORM_FEE_BPS || 0);", "const PLATFORM_FEE_BPS = 0;");
  source = source.replace("SELECT id,seller_id,price_paise,stock_quantity,is_published FROM products WHERE id=ANY($1::bigint[]) FOR UPDATE", "SELECT id,seller_id,category,price_paise,stock_quantity,is_published FROM products WHERE id=ANY($1::bigint[]) FOR UPDATE");
  source = source.replace("const platformFee=Math.floor(subtotal*PLATFORM_FEE_BPS/10000),total=subtotal+DELIVERY_FEE_PAISE+platformFee,orderId=randomUUID();", "const platformFee=products.rows.reduce((sum,p)=>sum+commissionPaise(Number(p.price_paise)*merged.get(Number(p.id)),p.category),0),total=subtotal+DELIVERY_FEE_PAISE+platformFee,orderId=randomUUID();");
  source = source.replace("const qty=merged.get(Number(p.id)),lineTotal=Number(p.price_paise)*qty,sellerAmount=lineTotal-Math.floor(lineTotal*PLATFORM_FEE_BPS/10000);", "const qty=merged.get(Number(p.id)),lineTotal=Number(p.price_paise)*qty,sellerAmount=lineTotal-commissionPaise(lineTotal,p.category);");
}

const refundStart = source.indexOf("app.post('/v1/admin/orders/:id/refund'");
const refundEnd = refundStart >= 0 ? source.indexOf("\napp.get('/v1/admin/sellers'", refundStart) : -1;
if (refundStart >= 0 && refundEnd > refundStart) {
  const safeRefundRoute = `app.post('/v1/admin/orders/:id/refund', { preHandler: requireRole('ADMIN') }, async (request, reply) => {
  if (!pool || !razorpay) return reply.code(503).send({ error: 'PAYMENTS_NOT_CONFIGURED' });
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const result = await client.query('SELECT id,total_paise,gateway_payment_id,payment_status,status,refund_status FROM orders WHERE id=$1 FOR UPDATE', [request.params.id]);
    if (!result.rowCount) throw httpError(404, 'ORDER_NOT_FOUND');
    const order = result.rows[0];
    if (order.payment_status !== 'CAPTURED' || !order.gateway_payment_id) throw httpError(409, 'ORDER_NOT_REFUNDABLE');
    if (order.refund_status) throw httpError(409, 'REFUND_ALREADY_PROCESSED');
    const amount = request.body?.amountPaise === undefined ? Number(order.total_paise) : Number(request.body.amountPaise);
    if (!Number.isSafeInteger(amount) || amount <= 0 || amount > Number(order.total_paise)) throw httpError(400, 'INVALID_REFUND_AMOUNT');
    const refund = await razorpay.payments.refund(order.gateway_payment_id, { amount });
    const lines = await client.query('SELECT seller_id,order_id,seller_amount_paise FROM order_lines WHERE order_id=$1 ORDER BY seller_id,product_id', [request.params.id]);
    const sellerTotal = lines.rows.reduce((sum, line) => sum + Number(line.seller_amount_paise), 0);
    let remainingSellerRefund = Math.min(amount, sellerTotal);
    for (let i = 0; i < lines.rows.length; i++) {
      const line = lines.rows[i];
      const sellerAmount = Number(line.seller_amount_paise);
      const allocation = i === lines.rows.length - 1 ? remainingSellerRefund : Math.min(sellerAmount, Math.floor(amount * sellerAmount / Number(order.total_paise)));
      if (allocation > 0) {
        await client.query("INSERT INTO seller_ledger(seller_id,order_id,amount_paise,type,gateway_transfer_id) VALUES($1,$2,$3,'REFUND',$4)", [line.seller_id, line.order_id, allocation, refund.id]);
        remainingSellerRefund -= allocation;
      }
    }
    const fullRefund = amount === Number(order.total_paise);
    await client.query("UPDATE orders SET refund_status=$1,payment_status=CASE WHEN $2 THEN 'REFUNDED' ELSE payment_status END,refunded_at=CASE WHEN $2 THEN now() ELSE refunded_at END,status=CASE WHEN $2 AND status<>'DELIVERED' THEN 'REFUNDED' ELSE status END,updated_at=now() WHERE id=$3", [fullRefund ? 'PROCESSED' : 'PARTIAL', fullRefund, request.params.id]);
    await audit(client, request.user, 'ORDER', request.params.id, 'REFUND_PROCESSED', { refundId: refund.id, amountPaise: amount, fullRefund });
    await client.query('COMMIT');
    return { orderId: request.params.id, refundId: refund.id, amountPaise: amount, refundStatus: fullRefund ? 'PROCESSED' : 'PARTIAL' };
  } catch (error) { await client.query('ROLLBACK'); throw error; }
  finally { client.release(); }
});`;
  source = source.slice(0, refundStart) + safeRefundRoute + source.slice(refundEnd);
}

if (!source.includes("otp-delivery.js")) {
  source = source.replace(
    "import { registerSettlementCompletion } from './settlement-completion.js';",
    "import { registerSettlementCompletion } from './settlement-completion.js';\nimport { sendPhoneOtp } from './otp-delivery.js';"
  );
  const otpStart = source.indexOf("app.post('/v1/auth/resend-phone-otp'");
  const otpEnd = otpStart >= 0 ? source.indexOf("\napp.post('/v1/ai/assistant'", otpStart) : -1;
  if (otpStart >= 0 && otpEnd > otpStart) {
    const otpRoute = `app.post('/v1/auth/resend-phone-otp', { config: { rateLimit: { max: 5, timeWindow: '1 minute' } } }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  const phone = normalizePhone(request.body?.phone);
  if (!phone) return reply.code(400).send({ error: 'INVALID_PHONE' });
  const user = await pool.query('SELECT id FROM users WHERE phone=$1', [phone]);
  if (!user.rowCount) return reply.code(404).send({ error: 'USER_NOT_FOUND' });
  const otp = String(Math.floor(100000 + Math.random() * 900000));
  const otpHash = hashPassword(otp);
  await pool.query('UPDATE phone_verification_challenges SET verified_at=COALESCE(verified_at,now()) WHERE phone=$1 AND verified_at IS NULL', [phone]);
  try { await sendPhoneOtp({ phone, otp }); }
  catch (error) { request.log.error({ err: error }, 'phone OTP delivery failed'); return reply.code(503).send({ error: error?.code === 'OTP_PROVIDER_NOT_CONFIGURED' ? 'OTP_PROVIDER_NOT_CONFIGURED' : 'OTP_DELIVERY_FAILED' }); }
  await pool.query("INSERT INTO phone_verification_challenges(user_id,phone,otp_hash,expires_at,attempts) VALUES($1,$2,$3,now()+interval '10 minutes',0)", [user.rows[0].id, phone, otpHash]);
  return { sent: true, expiresInSeconds: 600 };
});`;
    source = source.slice(0, otpStart) + otpRoute + source.slice(otpEnd);
  }
}

if (!source.includes('AARVO_LOGIN_OTP_ENABLED')) {
  const loginStart = source.indexOf("app.post('/v1/auth/login'");
  const loginEnd = loginStart >= 0 ? source.indexOf("\napp.get('/v1/products'", loginStart) : -1;
  if (loginStart >= 0 && loginEnd > loginStart) {
    const loginRoute = `app.post('/v1/auth/login', { config: { rateLimit: { max: 10, timeWindow: '1 minute' } } }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  if (!JWT_SECRET) return reply.code(503).send({ error: 'AUTH_NOT_CONFIGURED' });
  const { phone, email, password } = request.body || {};
  const normalizedPhone = normalizePhone(phone);
  const normalizedEmail = String(email || '').trim().toLowerCase();
  if ((!normalizedPhone && !normalizedEmail) || !password) return reply.code(400).send({ error: 'INVALID_LOGIN' });
  const result = normalizedPhone ? await pool.query('SELECT id,email,display_name,role,password_hash,phone,phone_verified FROM users WHERE phone=$1', [normalizedPhone]) : await pool.query('SELECT id,email,display_name,role,password_hash,phone,phone_verified FROM users WHERE email=$1', [normalizedEmail]);
  if (!result.rowCount || !verifyPassword(String(password || ''), result.rows[0].password_hash)) return reply.code(401).send({ error: 'INVALID_CREDENTIALS' });
  const user = result.rows[0];
  if (!user.phone) return reply.code(400).send({ error: 'PHONE_REQUIRED_FOR_OTP' });
  const otp = String(Math.floor(100000 + Math.random() * 900000));
  const otpHash = hashPassword(otp);
  await pool.query('UPDATE phone_verification_challenges SET verified_at=COALESCE(verified_at,now()) WHERE phone=$1 AND verified_at IS NULL', [user.phone]);
  try { await sendPhoneOtp({ phone: user.phone, otp }); }
  catch (error) { request.log.error({ err: error }, 'login OTP delivery failed'); return reply.code(503).send({ error: error?.code === 'OTP_PROVIDER_NOT_CONFIGURED' ? 'OTP_PROVIDER_NOT_CONFIGURED' : 'OTP_DELIVERY_FAILED' }); }
  await pool.query("INSERT INTO phone_verification_challenges(user_id,phone,otp_hash,expires_at,attempts) VALUES($1,$2,$3,now()+interval '10 minutes',0)", [user.id, user.phone, otpHash]);
  const { password_hash, ...safeUser } = user;
  return { user: safeUser, requiresPhoneVerification: true, otpRequired: true, expiresInSeconds: 600 };
});`;
    source = source.slice(0, loginStart) + loginRoute + source.slice(loginEnd);
  }
}

if (!source.includes('msg91-widget-auth.js')) {
  source = source.replace(
    "import { registerSettlementCompletion } from './settlement-completion.js';",
    "import { registerSettlementCompletion } from './settlement-completion.js';\nimport { registerMsg91WidgetAuth } from './msg91-widget-auth.js';"
  );
  source = source.replace(
    "app.listen(PORT, '0.0.0.0', () => {",
    "await registerMsg91WidgetAuth({ app, pool, issueToken, normalizePhone });\napp.listen(PORT, '0.0.0.0', () => {"
  );
}

await fs.writeFile(runtimePath, source, 'utf8');
await import(`${pathToFileURL(runtimePath).href}?v=${Date.now()}`);
