import Fastify from 'fastify';
import cors from '@fastify/cors';
import helmet from '@fastify/helmet';
import rawBody from 'fastify-raw-body';
import pg from 'pg';
import jwt from 'jsonwebtoken';
import Razorpay from 'razorpay';
import { createHmac, randomBytes, randomUUID, scryptSync, timingSafeEqual } from 'node:crypto';

const app = Fastify({ logger: true });
const { Pool } = pg;
const pool = process.env.DATABASE_URL ? new Pool({ connectionString: process.env.DATABASE_URL, ssl: process.env.DATABASE_SSL === 'true' ? { rejectUnauthorized: false } : undefined }) : null;
const JWT_SECRET = process.env.JWT_SECRET;
const DELIVERY_FEE_PAISE = Number(process.env.DELIVERY_FEE_PAISE || 0);
const PLATFORM_FEE_BPS = Number(process.env.PLATFORM_FEE_BPS || 0);
const RAZORPAY_WEBHOOK_SECRET = process.env.RAZORPAY_WEBHOOK_SECRET;
const razorpay = process.env.RAZORPAY_KEY_ID && process.env.RAZORPAY_KEY_SECRET ? new Razorpay({ key_id: process.env.RAZORPAY_KEY_ID, key_secret: process.env.RAZORPAY_KEY_SECRET }) : null;

await app.register(helmet);
await app.register(cors, { origin: process.env.CORS_ORIGIN ? process.env.CORS_ORIGIN.split(',') : true });
await app.register(rawBody, { field: 'rawBody', global: false, encoding: 'utf8', runFirst: true });

const requireAuth = async (request, reply) => {
  if (!JWT_SECRET) return reply.code(503).send({ error: 'AUTH_NOT_CONFIGURED' });
  const header = request.headers.authorization || '';
  if (!header.startsWith('Bearer ')) return reply.code(401).send({ error: 'AUTH_REQUIRED' });
  try { request.user = jwt.verify(header.slice(7), JWT_SECRET); }
  catch { return reply.code(401).send({ error: 'INVALID_TOKEN' }); }
};
const hashPassword = (password) => { const salt = randomBytes(16).toString('hex'); return `${salt}:${scryptSync(password, salt, 64).toString('hex')}`; };
const verifyPassword = (password, stored) => { const [salt, expected] = String(stored || '').split(':'); if (!salt || !expected) return false; const actual = scryptSync(password, salt, 64), expectedBuffer = Buffer.from(expected, 'hex'); return expectedBuffer.length === actual.length && timingSafeEqual(actual, expectedBuffer); };
const issueToken = (user) => jwt.sign({ sub: user.id, role: user.role, email: user.email }, JWT_SECRET, { expiresIn: '7d' });
const badAddress = (a) => !a || !a.fullName || !a.phone || !a.line1 || !a.city || !a.state || !a.postalCode || !a.country;
const httpError = (statusCode, message) => Object.assign(new Error(message), { statusCode, publicMessage: message });
const safeSignatureEqual = (expected, received) => { const a = Buffer.from(String(expected), 'utf8'), b = Buffer.from(String(received), 'utf8'); return a.length === b.length && timingSafeEqual(a, b); };

app.get('/health', async () => ({ service: 'aarvo-api', status: 'ok', database: Boolean(pool), auth: Boolean(JWT_SECRET), payments: Boolean(razorpay), webhooks: Boolean(RAZORPAY_WEBHOOK_SECRET) }));

app.post('/v1/auth/register', async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  if (!JWT_SECRET) return reply.code(503).send({ error: 'AUTH_NOT_CONFIGURED' });
  const { email, password, displayName, role = 'BUYER', phone = '' } = request.body || {};
  const normalizedEmail = String(email || '').trim().toLowerCase(), normalizedRole = String(role).toUpperCase();
  if (!normalizedEmail || !password || String(password).length < 8 || !displayName) return reply.code(400).send({ error: 'INVALID_REGISTRATION' });
  if (!['BUYER', 'SELLER'].includes(normalizedRole)) return reply.code(400).send({ error: 'INVALID_ROLE' });
  if (normalizedRole === 'SELLER' && String(phone).trim().length < 10) return reply.code(400).send({ error: 'SELLER_PHONE_REQUIRED' });
  const id = randomBytes(12).toString('hex'), client = await pool.connect();
  try {
    await client.query('BEGIN');
    const user = await client.query('INSERT INTO users(id,email,display_name,password_hash,role) VALUES($1,$2,$3,$4,$5) RETURNING id,email,display_name,role', [id, normalizedEmail, String(displayName).trim(), hashPassword(String(password)), normalizedRole]);
    if (normalizedRole === 'SELLER') await client.query('INSERT INTO seller_profiles(seller_id,phone) VALUES($1,$2)', [id, String(phone).trim()]);
    await client.query('COMMIT');
    return reply.code(201).send({ user: user.rows[0], token: issueToken(user.rows[0]) });
  } catch (error) { await client.query('ROLLBACK'); if (error.code === '23505') return reply.code(409).send({ error: 'EMAIL_ALREADY_REGISTERED' }); throw error; }
  finally { client.release(); }
});

app.post('/v1/auth/login', async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  if (!JWT_SECRET) return reply.code(503).send({ error: 'AUTH_NOT_CONFIGURED' });
  const { email, password } = request.body || {};
  const result = await pool.query('SELECT id,email,display_name,role,password_hash FROM users WHERE email=$1', [String(email || '').trim().toLowerCase()]);
  if (!result.rowCount || !verifyPassword(String(password || ''), result.rows[0].password_hash)) return reply.code(401).send({ error: 'INVALID_CREDENTIALS' });
  const { password_hash, ...user } = result.rows[0];
  return { user, token: issueToken(user) };
});

app.get('/v1/products', async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  const { q = '', category = '' } = request.query;
  return (await pool.query(`SELECT id,seller_id,seller_name,name,category,price_paise,rating,description,stock_quantity,is_published FROM products WHERE is_published=true AND stock_quantity>0 AND ($1='' OR name ILIKE '%'||$1||'%' OR description ILIKE '%'||$1||'%') AND ($2='' OR category=$2) ORDER BY created_at DESC LIMIT 100`, [q, category])).rows;
});

app.get('/v1/products/:id', async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  const result = await pool.query('SELECT id,seller_id,seller_name,name,category,price_paise,rating,description,stock_quantity,is_published FROM products WHERE id=$1 AND is_published=true', [request.params.id]);
  if (!result.rowCount) return reply.code(404).send({ error: 'PRODUCT_NOT_FOUND' });
  return result.rows[0];
});

app.post('/v1/seller/products', { preHandler: requireAuth }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  if (request.user.role !== 'SELLER') return reply.code(403).send({ error: 'SELLER_ROLE_REQUIRED' });
  const { name, category, pricePaise, description, stockQuantity = 0, publish = false } = request.body || {};
  const price = Number(pricePaise), stock = Number(stockQuantity);
  if (!name || !category || !description || !Number.isInteger(price) || price <= 0 || !Number.isInteger(stock) || stock < 0) return reply.code(400).send({ error: 'INVALID_PRODUCT' });
  const seller = await pool.query('SELECT u.display_name,s.verified,s.payout_account_ready FROM users u JOIN seller_profiles s ON s.seller_id=u.id WHERE u.id=$1', [request.user.sub]);
  if (!seller.rowCount) return reply.code(403).send({ error: 'SELLER_PROFILE_REQUIRED' });
  const canPublish = seller.rows[0].verified && seller.rows[0].payout_account_ready, isPublished = Boolean(publish) && canPublish;
  const result = await pool.query('INSERT INTO products(seller_id,seller_name,name,category,price_paise,description,stock_quantity,is_published) VALUES($1,$2,$3,$4,$5,$6,$7,$8) RETURNING id,seller_id,seller_name,name,category,price_paise,rating,description,stock_quantity,is_published', [request.user.sub,seller.rows[0].display_name,String(name).trim(),String(category).trim(),price,String(description).trim(),stock,isPublished]);
  return reply.code(201).send({ product: result.rows[0], published: isPublished, publishBlocked: Boolean(publish) && !canPublish });
});

app.get('/v1/seller/profile', { preHandler: requireAuth }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  if (request.user.role !== 'SELLER') return reply.code(403).send({ error: 'SELLER_ROLE_REQUIRED' });
  const result = await pool.query('SELECT s.seller_id,u.display_name,s.phone,s.verified,s.payout_account_ready FROM seller_profiles s JOIN users u ON u.id=s.seller_id WHERE s.seller_id=$1', [request.user.sub]);
  if (!result.rowCount) return reply.code(404).send({ error: 'SELLER_PROFILE_NOT_FOUND' });
  return result.rows[0];
});

app.post('/v1/orders', { preHandler: requireAuth }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  if (!razorpay) return reply.code(503).send({ error: 'PAYMENTS_NOT_CONFIGURED' });
  if (request.user.role !== 'BUYER') return reply.code(403).send({ error: 'BUYER_ROLE_REQUIRED' });
  const { items, address } = request.body || {};
  if (!Array.isArray(items) || items.length < 1 || items.length > 50 || badAddress(address)) return reply.code(400).send({ error: 'INVALID_ORDER' });
  const normalized = items.map(x => ({ productId: Number(x.productId), quantity: Number(x.quantity) }));
  if (normalized.some(x => !Number.isInteger(x.productId) || !Number.isInteger(x.quantity) || x.quantity < 1 || x.quantity > 100)) return reply.code(400).send({ error: 'INVALID_ORDER_ITEMS' });
  const merged = new Map(); for (const item of normalized) merged.set(item.productId, (merged.get(item.productId) || 0) + item.quantity);
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const ids = [...merged.keys()];
    const products = await client.query('SELECT id,seller_id,price_paise,stock_quantity,is_published FROM products WHERE id=ANY($1::bigint[]) FOR UPDATE', [ids]);
    if (products.rowCount !== ids.length) throw httpError(409, 'PRODUCT_CHANGED');
    let subtotal = 0;
    for (const p of products.rows) { const qty = merged.get(Number(p.id)); if (!p.is_published || p.stock_quantity < qty) throw httpError(409, `INSUFFICIENT_STOCK:${p.id}`); subtotal += Number(p.price_paise) * qty; }
    const platformFee = Math.floor(subtotal * PLATFORM_FEE_BPS / 10000), total = subtotal + DELIVERY_FEE_PAISE + platformFee, orderId = randomUUID();
    const gatewayOrder = await razorpay.orders.create({ amount: total, currency: 'INR', receipt: orderId });
    await client.query('INSERT INTO orders(id,buyer_id,subtotal_paise,delivery_fee_paise,platform_fee_paise,total_paise,payment_status,status,address_json,gateway_order_id) VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9,$10)', [orderId,request.user.sub,subtotal,DELIVERY_FEE_PAISE,platformFee,total,'CREATED','PENDING_PAYMENT',JSON.stringify(address),gatewayOrder.id]);
    for (const p of products.rows) { const qty = merged.get(Number(p.id)), lineTotal = Number(p.price_paise) * qty, sellerAmount = lineTotal - Math.floor(lineTotal * PLATFORM_FEE_BPS / 10000); await client.query('INSERT INTO order_lines(order_id,product_id,seller_id,quantity,unit_price_paise,seller_amount_paise) VALUES($1,$2,$3,$4,$5,$6)', [orderId,p.id,p.seller_id,qty,p.price_paise,sellerAmount]); await client.query('UPDATE products SET stock_quantity=stock_quantity-$1,updated_at=now() WHERE id=$2', [qty,p.id]); }
    await client.query('COMMIT');
    return reply.code(201).send({ orderId, amountPaise: total, currency: 'INR', gatewayOrderId: gatewayOrder.id, keyId: process.env.RAZORPAY_KEY_ID, paymentStatus: 'CREATED', status: 'PENDING_PAYMENT' });
  } catch (error) { await client.query('ROLLBACK'); throw error; }
  finally { client.release(); }
});

app.post('/v1/payments/verify', { preHandler: requireAuth }, async (request, reply) => {
  if (!pool || !razorpay || !process.env.RAZORPAY_KEY_SECRET) return reply.code(503).send({ error: 'PAYMENTS_NOT_CONFIGURED' });
  if (request.user.role !== 'BUYER') return reply.code(403).send({ error: 'BUYER_ROLE_REQUIRED' });
  const { orderId, razorpayOrderId, razorpayPaymentId, razorpaySignature } = request.body || {};
  if (!orderId || !razorpayOrderId || !razorpayPaymentId || !razorpaySignature) return reply.code(400).send({ error: 'INVALID_PAYMENT' });
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const result = await client.query('SELECT id,total_paise,gateway_order_id,payment_status,status FROM orders WHERE id=$1 AND buyer_id=$2 FOR UPDATE', [orderId, request.user.sub]);
    if (!result.rowCount) throw httpError(404, 'ORDER_NOT_FOUND');
    const order = result.rows[0];
    if (order.gateway_order_id !== razorpayOrderId) throw httpError(400, 'PAYMENT_ORDER_MISMATCH');
    const expected = createHmac('sha256', process.env.RAZORPAY_KEY_SECRET).update(`${order.gateway_order_id}|${razorpayPaymentId}`).digest('hex');
    if (!safeSignatureEqual(expected, razorpaySignature)) throw httpError(400, 'INVALID_PAYMENT_SIGNATURE');
    if (order.payment_status === 'CAPTURED') { await client.query('COMMIT'); return { orderId, paymentStatus: 'CAPTURED', status: order.status }; }
    const payment = await razorpay.payments.fetch(razorpayPaymentId);
    if (payment.order_id !== order.gateway_order_id || Number(payment.amount) !== Number(order.total_paise)) throw httpError(400, 'PAYMENT_DETAILS_MISMATCH');
    if (!['authorized', 'captured'].includes(payment.status)) throw httpError(400, 'PAYMENT_NOT_SUCCESSFUL');
    await client.query('UPDATE orders SET payment_status=$1,status=$2,gateway_payment_id=$3,updated_at=now() WHERE id=$4', ['CAPTURED','PAID',razorpayPaymentId,orderId]);
    await client.query('INSERT INTO seller_ledger(seller_id,order_id,amount_paise,type) SELECT seller_id,order_id,seller_amount_paise,\'SALE\' FROM order_lines WHERE order_id=$1 AND NOT EXISTS (SELECT 1 FROM seller_ledger sl WHERE sl.order_id=order_lines.order_id AND sl.seller_id=order_lines.seller_id AND sl.type=\'SALE\')', [orderId]);
    await client.query('COMMIT');
    return { orderId, paymentStatus: 'CAPTURED', status: 'PAID' };
  } catch (error) { await client.query('ROLLBACK'); throw error; }
  finally { client.release(); }
});

app.post('/v1/orders/:id/cancel', { preHandler: requireAuth }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  if (request.user.role !== 'BUYER') return reply.code(403).send({ error: 'BUYER_ROLE_REQUIRED' });
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const order = await client.query('SELECT id,payment_status,status FROM orders WHERE id=$1 AND buyer_id=$2 FOR UPDATE', [request.params.id, request.user.sub]);
    if (!order.rowCount) throw httpError(404, 'ORDER_NOT_FOUND');
    if (order.rows[0].status !== 'PENDING_PAYMENT') throw httpError(409, 'ORDER_CANNOT_BE_CANCELLED');
    const lines = await client.query('SELECT product_id,quantity FROM order_lines WHERE order_id=$1', [request.params.id]);
    for (const line of lines.rows) await client.query('UPDATE products SET stock_quantity=stock_quantity+$1,updated_at=now() WHERE id=$2', [line.quantity,line.product_id]);
    await client.query('UPDATE orders SET status=$1,payment_status=$2,updated_at=now() WHERE id=$3', ['CANCELLED','FAILED',request.params.id]);
    await client.query('COMMIT');
    return { orderId: request.params.id, status: 'CANCELLED', paymentStatus: 'FAILED' };
  } catch (error) { await client.query('ROLLBACK'); throw error; }
  finally { client.release(); }
});

app.post('/v1/webhooks/razorpay', { config: { rawBody: true } }, async (request, reply) => {
  if (!pool || !RAZORPAY_WEBHOOK_SECRET) return reply.code(503).send({ error: 'WEBHOOK_NOT_CONFIGURED' });
  const signature = request.headers['x-razorpay-signature'];
  const eventId = request.headers['x-razorpay-event-id'];
  if (!signature || !eventId) return reply.code(400).send({ error: 'WEBHOOK_HEADERS_REQUIRED' });
  const raw = request.rawBody || '';
  const expected = createHmac('sha256', RAZORPAY_WEBHOOK_SECRET).update(raw).digest('hex');
  if (!safeSignatureEqual(expected, signature)) return reply.code(400).send({ error: 'INVALID_WEBHOOK_SIGNATURE' });
  const payload = request.body || {}, event = String(payload.event || '');
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const inserted = await client.query('INSERT INTO payment_events(event_id,order_id,event_type,payload) VALUES($1,NULL,$2,$3) ON CONFLICT (event_id) DO NOTHING RETURNING event_id', [String(eventId), event, JSON.stringify(payload)]);
    if (!inserted.rowCount) { await client.query('COMMIT'); return { received: true, duplicate: true }; }
    const paymentEntity = payload?.payload?.payment?.entity;
    const orderEntity = payload?.payload?.order?.entity;
    const gatewayOrderId = paymentEntity?.order_id || orderEntity?.id;
    const paymentId = paymentEntity?.id;
    const captured = event === 'payment.captured' || event === 'order.paid' || paymentEntity?.status === 'captured' || orderEntity?.status === 'paid';
    if (captured && gatewayOrderId && paymentId) {
      const order = await client.query('SELECT id,total_paise,payment_status FROM orders WHERE gateway_order_id=$1 FOR UPDATE', [gatewayOrderId]);
      if (order.rowCount && order.rows[0].payment_status !== 'CAPTURED' && Number(paymentEntity.amount) === Number(order.rows[0].total_paise)) {
        await client.query('UPDATE orders SET payment_status=\'CAPTURED\',status=\'PAID\',gateway_payment_id=$1,updated_at=now() WHERE id=$2', [paymentId, order.rows[0].id]);
        await client.query('INSERT INTO seller_ledger(seller_id,order_id,amount_paise,type) SELECT seller_id,order_id,seller_amount_paise,\'SALE\' FROM order_lines WHERE order_id=$1 AND NOT EXISTS (SELECT 1 FROM seller_ledger sl WHERE sl.order_id=order_lines.order_id AND sl.seller_id=order_lines.seller_id AND sl.type=\'SALE\')', [order.rows[0].id]);
      }
    }
    await client.query('COMMIT');
    return { received: true };
  } catch (error) { await client.query('ROLLBACK'); throw error; }
  finally { client.release(); }
});

app.get('/v1/orders', { preHandler: requireAuth }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  if (request.user.role !== 'BUYER') return reply.code(403).send({ error: 'BUYER_ROLE_REQUIRED' });
  return (await pool.query('SELECT * FROM orders WHERE buyer_id=$1 ORDER BY created_at DESC', [request.user.sub])).rows;
});

app.setErrorHandler((error, request, reply) => { request.log.error(error); const status = error.statusCode && error.statusCode < 500 ? error.statusCode : 500; reply.code(status).send({ error: status < 500 && error.publicMessage ? error.publicMessage : 'INTERNAL_SERVER_ERROR' }); });
const port = Number(process.env.PORT || 8080);
await app.listen({ port, host: '0.0.0.0' });
