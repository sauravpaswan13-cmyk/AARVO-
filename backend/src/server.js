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
const PAYMENT_WINDOW_MINUTES = Math.max(5, Number(process.env.PAYMENT_WINDOW_MINUTES || 15));
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
const requireRole = (role) => async (request, reply) => {
  const result = await requireAuth(request, reply);
  if (result) return result;
  if (request.user.role !== role) return reply.code(403).send({ error: `${role}_ROLE_REQUIRED` });
};
const hashPassword = (password) => { const salt = randomBytes(16).toString('hex'); return `${salt}:${scryptSync(password, salt, 64).toString('hex')}`; };
const verifyPassword = (password, stored) => { const [salt, expected] = String(stored || '').split(':'); if (!salt || !expected) return false; const actual = scryptSync(password, salt, 64), expectedBuffer = Buffer.from(expected, 'hex'); return expectedBuffer.length === actual.length && timingSafeEqual(actual, expectedBuffer); };
const issueToken = (user) => jwt.sign({ sub: user.id, role: user.role, email: user.email }, JWT_SECRET, { expiresIn: '7d' });
const badAddress = (a) => !a || !a.fullName || !a.phone || !a.line1 || !a.city || !a.state || !a.postalCode || !a.country;
const httpError = (statusCode, message) => Object.assign(new Error(message), { statusCode, publicMessage: message });
const safeSignatureEqual = (expected, received) => { const a = Buffer.from(String(expected), 'utf8'), b = Buffer.from(String(received), 'utf8'); return a.length === b.length && timingSafeEqual(a, b); };
const audit = async (client, actor, entityType, entityId, action, metadata = {}) => client.query('INSERT INTO audit_events(actor_id,actor_role,entity_type,entity_id,action,metadata) VALUES($1,$2,$3,$4,$5,$6)', [actor?.sub || null, actor?.role || null, entityType, String(entityId), action, JSON.stringify(metadata)]);
const normalizeStatus = (value) => String(value || '').trim().toUpperCase();

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
    await audit(client, { sub: id, role: normalizedRole }, 'USER', id, 'REGISTERED');
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

app.get('/v1/products/:id/reviews', async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  return (await pool.query(`SELECT r.id,r.product_id,r.rating,r.review_text,r.created_at,u.display_name FROM product_reviews r JOIN users u ON u.id=r.buyer_id WHERE r.product_id=$1 ORDER BY r.created_at DESC LIMIT 100`, [request.params.id])).rows;
});

app.post('/v1/seller/products', { preHandler: requireRole('SELLER') }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  const { name, category, pricePaise, description, stockQuantity = 0, publish = false } = request.body || {};
  const price = Number(pricePaise), stock = Number(stockQuantity);
  if (!name || !category || !description || !Number.isInteger(price) || price <= 0 || !Number.isInteger(stock) || stock < 0) return reply.code(400).send({ error: 'INVALID_PRODUCT' });
  const seller = await pool.query('SELECT u.display_name,s.verified,s.payout_account_ready FROM users u JOIN seller_profiles s ON s.seller_id=u.id WHERE u.id=$1', [request.user.sub]);
  if (!seller.rowCount) return reply.code(403).send({ error: 'SELLER_PROFILE_REQUIRED' });
  const canPublish = seller.rows[0].verified && seller.rows[0].payout_account_ready, isPublished = Boolean(publish) && canPublish;
  const result = await pool.query('INSERT INTO products(seller_id,seller_name,name,category,price_paise,description,stock_quantity,is_published) VALUES($1,$2,$3,$4,$5,$6,$7,$8) RETURNING id,seller_id,seller_name,name,category,price_paise,rating,description,stock_quantity,is_published', [request.user.sub,seller.rows[0].display_name,String(name).trim(),String(category).trim(),price,String(description).trim(),stock,isPublished]);
  await audit(pool, request.user, 'PRODUCT', result.rows[0].id, 'CREATED', { published: isPublished });
  return reply.code(201).send({ product: result.rows[0], published: isPublished, publishBlocked: Boolean(publish) && !canPublish });
});

app.get('/v1/seller/products', { preHandler: requireRole('SELLER') }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  return (await pool.query('SELECT id,seller_id,seller_name,name,category,price_paise,rating,description,stock_quantity,is_published,created_at,updated_at FROM products WHERE seller_id=$1 ORDER BY created_at DESC', [request.user.sub])).rows;
});

app.post('/v1/seller/products/:id/inventory', { preHandler: requireRole('SELLER') }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  const stock = Number(request.body?.stockQuantity);
  if (!Number.isInteger(stock) || stock < 0 || stock > 1000000) return reply.code(400).send({ error: 'INVALID_STOCK' });
  const result = await pool.query('UPDATE products SET stock_quantity=$1,updated_at=now() WHERE id=$2 AND seller_id=$3 RETURNING id,stock_quantity,is_published', [stock, request.params.id, request.user.sub]);
  if (!result.rowCount) return reply.code(404).send({ error: 'PRODUCT_NOT_FOUND' });
  await audit(pool, request.user, 'PRODUCT', request.params.id, 'INVENTORY_UPDATED', { stockQuantity: stock });
  return result.rows[0];
});

app.get('/v1/seller/profile', { preHandler: requireRole('SELLER') }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  const result = await pool.query('SELECT s.seller_id,u.display_name,s.phone,s.verified,s.payout_account_ready FROM seller_profiles s JOIN users u ON u.id=s.seller_id WHERE s.seller_id=$1', [request.user.sub]);
  if (!result.rowCount) return reply.code(404).send({ error: 'SELLER_PROFILE_NOT_FOUND' });
  return result.rows[0];
});

app.get('/v1/admin/sellers', { preHandler: requireRole('ADMIN') }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  return (await pool.query('SELECT s.seller_id,u.email,u.display_name,s.phone,s.verified,s.payout_account_ready,s.gateway_account_id FROM seller_profiles s JOIN users u ON u.id=s.seller_id ORDER BY s.created_at DESC')).rows;
});

app.post('/v1/admin/sellers/:id/verify', { preHandler: requireRole('ADMIN') }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  const verified = Boolean(request.body?.verified);
  const result = await pool.query('UPDATE seller_profiles SET verified=$1 WHERE seller_id=$2 RETURNING seller_id,verified,payout_account_ready', [verified, request.params.id]);
  if (!result.rowCount) return reply.code(404).send({ error: 'SELLER_NOT_FOUND' });
  await audit(pool, request.user, 'SELLER', request.params.id, verified ? 'VERIFIED' : 'UNVERIFIED');
  return result.rows[0];
});

app.post('/v1/orders', { preHandler: requireRole('BUYER') }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  if (!razorpay) return reply.code(503).send({ error: 'PAYMENTS_NOT_CONFIGURED' });
  const idempotencyKey = String(request.headers['idempotency-key'] || request.body?.idempotencyKey || '').trim();
  if (idempotencyKey.length < 8 || idempotencyKey.length > 128) return reply.code(400).send({ error: 'IDEMPOTENCY_KEY_REQUIRED' });
  const existing = await pool.query('SELECT id,total_paise,gateway_order_id,payment_status,status FROM orders WHERE buyer_id=$1 AND idempotency_key=$2', [request.user.sub, idempotencyKey]);
  if (existing.rowCount) { const o = existing.rows[0]; return { orderId: o.id, amountPaise: o.total_paise, currency: 'INR', gatewayOrderId: o.gateway_order_id, keyId: process.env.RAZORPAY_KEY_ID, paymentStatus: o.payment_status, status: o.status, idempotentReplay: true }; }
  const { items, address } = request.body || {};
  if (!Array.isArray(items) || items.length < 1 || items.length > 50 || badAddress(address)) return reply.code(400).send({ error: 'INVALID_ORDER' });
  const normalized = items.map(x => ({ productId: Number(x.productId), quantity: Number(x.quantity) }));
  if (normalized.some(x => !Number.isInteger(x.productId) || !Number.isInteger(x.quantity) || x.quantity < 1 || x.quantity > 100)) return reply.code(400).send({ error: 'INVALID_ORDER_ITEMS' });
  const merged = new Map(); for (const item of normalized) merged.set(item.productId, (merged.get(item.productId) || 0) + item.quantity);
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const duplicate = await client.query('SELECT id,total_paise,gateway_order_id,payment_status,status FROM orders WHERE buyer_id=$1 AND idempotency_key=$2 FOR UPDATE', [request.user.sub, idempotencyKey]);
    if (duplicate.rowCount) { await client.query('COMMIT'); const o = duplicate.rows[0]; return { orderId: o.id, amountPaise: o.total_paise, currency: 'INR', gatewayOrderId: o.gateway_order_id, keyId: process.env.RAZORPAY_KEY_ID, paymentStatus: o.payment_status, status: o.status, idempotentReplay: true }; }
    const ids = [...merged.keys()];
    const products = await client.query('SELECT id,seller_id,price_paise,stock_quantity,is_published FROM products WHERE id=ANY($1::bigint[]) FOR UPDATE', [ids]);
    if (products.rowCount !== ids.length) throw httpError(409, 'PRODUCT_CHANGED');
    let subtotal = 0;
    for (const p of products.rows) { const qty = merged.get(Number(p.id)); if (!p.is_published || p.stock_quantity < qty) throw httpError(409, `INSUFFICIENT_STOCK:${p.id}`); subtotal += Number(p.price_paise) * qty; }
    const platformFee = Math.floor(subtotal * PLATFORM_FEE_BPS / 10000), total = subtotal + DELIVERY_FEE_PAISE + platformFee, orderId = randomUUID();
    const gatewayOrder = await razorpay.orders.create({ amount: total, currency: 'INR', receipt: orderId });
    await client.query('INSERT INTO orders(id,buyer_id,subtotal_paise,delivery_fee_paise,platform_fee_paise,total_paise,payment_status,status,address_json,gateway_order_id,payment_expires_at,idempotency_key) VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,now()+($11 * interval \'1 minute\'),$12)', [orderId,request.user.sub,subtotal,DELIVERY_FEE_PAISE,platformFee,total,'CREATED','PENDING_PAYMENT',JSON.stringify(address),gatewayOrder.id,PAYMENT_WINDOW_MINUTES,idempotencyKey]);
    for (const p of products.rows) { const qty = merged.get(Number(p.id)), lineTotal = Number(p.price_paise) * qty, sellerAmount = lineTotal - Math.floor(lineTotal * PLATFORM_FEE_BPS / 10000); await client.query('INSERT INTO order_lines(order_id,product_id,seller_id,quantity,unit_price_paise,seller_amount_paise) VALUES($1,$2,$3,$4,$5,$6)', [orderId,p.id,p.seller_id,qty,p.price_paise,sellerAmount]); await client.query('UPDATE products SET stock_quantity=stock_quantity-$1,updated_at=now() WHERE id=$2', [qty,p.id]); }
    await audit(client, request.user, 'ORDER', orderId, 'CREATED', { totalPaise: total });
    await client.query('COMMIT');
    return reply.code(201).send({ orderId, amountPaise: total, currency: 'INR', gatewayOrderId: gatewayOrder.id, keyId: process.env.RAZORPAY_KEY_ID, paymentStatus: 'CREATED', status: 'PENDING_PAYMENT', paymentExpiresInMinutes: PAYMENT_WINDOW_MINUTES });
  } catch (error) { await client.query('ROLLBACK'); if (error.code === '23505') { const duplicate = await pool.query('SELECT id,total_paise,gateway_order_id,payment_status,status FROM orders WHERE buyer_id=$1 AND idempotency_key=$2', [request.user.sub, idempotencyKey]); if (duplicate.rowCount) { const o = duplicate.rows[0]; return { orderId: o.id, amountPaise: o.total_paise, currency: 'INR', gatewayOrderId: o.gateway_order_id, keyId: process.env.RAZORPAY_KEY_ID, paymentStatus: o.payment_status, status: o.status, idempotentReplay: true }; } } throw error; }
  finally { client.release(); }
});

const orderQuery = `SELECT o.*, COALESCE(json_agg(json_build_object('productId',ol.product_id,'sellerId',ol.seller_id,'quantity',ol.quantity,'unitPricePaise',ol.unit_price_paise,'sellerAmountPaise',ol.seller_amount_paise)) FILTER (WHERE ol.order_id IS NOT NULL),'[]') AS items FROM orders o LEFT JOIN order_lines ol ON ol.order_id=o.id WHERE o.id=$1`;

app.get('/v1/orders', { preHandler: requireRole('BUYER') }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  return (await pool.query(`SELECT o.id,o.subtotal_paise,o.delivery_fee_paise,o.platform_fee_paise,o.total_paise,o.payment_status,o.status,o.address_json,o.gateway_order_id,o.gateway_payment_id,o.tracking_json,o.payment_expires_at,o.cancelled_at,o.cancel_reason,o.created_at,o.updated_at FROM orders o WHERE o.buyer_id=$1 ORDER BY o.created_at DESC`, [request.user.sub])).rows;
});

app.get('/v1/orders/:id', { preHandler: requireAuth }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  let result;
  if (request.user.role === 'ADMIN') {
    result = await pool.query(`${orderQuery} GROUP BY o.id`, [request.params.id]);
  } else {
    const ownership = request.user.role === 'BUYER' ? 'o.buyer_id=$2' : 'EXISTS (SELECT 1 FROM order_lines x WHERE x.order_id=o.id AND x.seller_id=$2)';
    result = await pool.query(`${orderQuery} AND ${ownership} GROUP BY o.id`, [request.params.id, request.user.sub]);
  }
  if (!result.rowCount) return reply.code(404).send({ error: 'ORDER_NOT_FOUND' });
  return result.rows[0];
});

app.post('/v1/payments/verify', { preHandler: requireRole('BUYER') }, async (request, reply) => {
  if (!pool || !razorpay || !process.env.RAZORPAY_KEY_SECRET) return reply.code(503).send({ error: 'PAYMENTS_NOT_CONFIGURED' });
  const { orderId, razorpayOrderId, razorpayPaymentId, razorpaySignature } = request.body || {};
  if (!orderId || !razorpayOrderId || !razorpayPaymentId || !razorpaySignature) return reply.code(400).send({ error: 'INVALID_PAYMENT' });
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const result = await client.query('SELECT id,total_paise,gateway_order_id,payment_status,status,payment_expires_at FROM orders WHERE id=$1 AND buyer_id=$2 FOR UPDATE', [orderId, request.user.sub]);
    if (!result.rowCount) throw httpError(404, 'ORDER_NOT_FOUND');
    const order = result.rows[0];
    if (order.gateway_order_id !== razorpayOrderId) throw httpError(400, 'PAYMENT_ORDER_MISMATCH');
    const expected = createHmac('sha256', process.env.RAZORPAY_KEY_SECRET).update(`${order.gateway_order_id}|${razorpayPaymentId}`).digest('hex');
    if (!safeSignatureEqual(expected, razorpaySignature)) throw httpError(400, 'INVALID_PAYMENT_SIGNATURE');
    if (order.payment_status === 'CAPTURED') { await client.query('COMMIT'); return { orderId, paymentStatus: 'CAPTURED', status: order.status }; }
    if (order.payment_expires_at && new Date(order.payment_expires_at).getTime() <= Date.now()) throw httpError(409, 'PAYMENT_WINDOW_EXPIRED');
    const payment = await razorpay.payments.fetch(razorpayPaymentId);
    if (payment.order_id !== order.gateway_order_id || Number(payment.amount) !== Number(order.total_paise)) throw httpError(400, 'PAYMENT_DETAILS_MISMATCH');
    if (!['authorized', 'captured'].includes(payment.status)) throw httpError(400, 'PAYMENT_NOT_SUCCESSFUL');
    await client.query('UPDATE orders SET payment_status=$1,status=$2,gateway_payment_id=$3,payment_expires_at=NULL,updated_at=now() WHERE id=$4', ['CAPTURED','PAID',razorpayPaymentId,orderId]);
    await client.query('INSERT INTO seller_ledger(seller_id,order_id,amount_paise,type) SELECT seller_id,order_id,seller_amount_paise,\'SALE\' FROM order_lines WHERE order_id=$1 AND NOT EXISTS (SELECT 1 FROM seller_ledger sl WHERE sl.order_id=order_lines.order_id AND sl.seller_id=order_lines.seller_id AND sl.type=\'SALE\')', [orderId]);
    await audit(client, request.user, 'ORDER', orderId, 'PAYMENT_CAPTURED', { paymentId: razorpayPaymentId });
    await client.query('COMMIT');
    return { orderId, paymentStatus: 'CAPTURED', status: 'PAID' };
  } catch (error) { await client.query('ROLLBACK'); throw error; }
  finally { client.release(); }
});

app.post('/v1/orders/:id/cancel', { preHandler: requireRole('BUYER') }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const order = await client.query('SELECT id,payment_status,status FROM orders WHERE id=$1 AND buyer_id=$2 FOR UPDATE', [request.params.id, request.user.sub]);
    if (!order.rowCount) throw httpError(404, 'ORDER_NOT_FOUND');
    if (order.rows[0].status !== 'PENDING_PAYMENT') throw httpError(409, 'ORDER_CANNOT_BE_CANCELLED');
    const reason = String(request.body?.reason || 'BUYER_CANCELLED').slice(0, 250);
    const lines = await client.query('SELECT product_id,quantity FROM order_lines WHERE order_id=$1', [request.params.id]);
    for (const line of lines.rows) await client.query('UPDATE products SET stock_quantity=stock_quantity+$1,updated_at=now() WHERE id=$2', [line.quantity,line.product_id]);
    await client.query('UPDATE orders SET status=$1,payment_status=$2,payment_expires_at=NULL,cancelled_at=now(),cancel_reason=$3,updated_at=now() WHERE id=$4', ['CANCELLED','FAILED',reason,request.params.id]);
    await audit(client, request.user, 'ORDER', request.params.id, 'CANCELLED', { reason });
    await client.query('COMMIT');
    return { orderId: request.params.id, status: 'CANCELLED', paymentStatus: 'FAILED' };
  } catch (error) { await client.query('ROLLBACK'); throw error; }
  finally { client.release(); }
});

const transitionMap = { PAID: ['PACKED','CANCELLED'], PACKED: ['SHIPPED','CANCELLED'], SHIPPED: ['OUT_FOR_DELIVERY','DELIVERED'], OUT_FOR_DELIVERY: ['DELIVERED'], DELIVERED: [] };
app.post('/v1/orders/:id/tracking', { preHandler: requireAuth }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  if (!['SELLER','ADMIN'].includes(request.user.role)) return reply.code(403).send({ error: 'SELLER_OR_ADMIN_REQUIRED' });
  const nextStatus = normalizeStatus(request.body?.status), carrier = String(request.body?.carrier || '').slice(0, 100), trackingNumber = String(request.body?.trackingNumber || '').slice(0, 150), note = String(request.body?.note || '').slice(0, 500);
  const allowed = ['PACKED','SHIPPED','OUT_FOR_DELIVERY','DELIVERED','CANCELLED'];
  if (!allowed.includes(nextStatus)) return reply.code(400).send({ error: 'INVALID_TRACKING_STATUS' });
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const order = await client.query(`SELECT o.id,o.status FROM orders o WHERE o.id=$1 AND (${request.user.role === 'ADMIN' ? 'TRUE' : 'EXISTS (SELECT 1 FROM order_lines x WHERE x.order_id=o.id AND x.seller_id=$2)'}) FOR UPDATE`, request.user.role === 'ADMIN' ? [request.params.id] : [request.params.id, request.user.sub]);
    if (!order.rowCount) throw httpError(404, 'ORDER_NOT_FOUND');
    const current = normalizeStatus(order.rows[0].status);
    if (nextStatus !== 'CANCELLED' && !(transitionMap[current] || []).includes(nextStatus)) throw httpError(409, 'INVALID_ORDER_TRANSITION');
    if (nextStatus === 'CANCELLED' && !['PAID','PACKED','SHIPPED'].includes(current)) throw httpError(409, 'ORDER_CANNOT_BE_CANCELLED');
    const tracking = { status: nextStatus, carrier, trackingNumber, note, updatedAt: new Date().toISOString() };
    await client.query('UPDATE orders SET status=$1,tracking_json=$2,updated_at=now() WHERE id=$3', [nextStatus,JSON.stringify(tracking),request.params.id]);
    await audit(client, request.user, 'ORDER', request.params.id, 'STATUS_CHANGED', { from: current, to: nextStatus, tracking });
    await client.query('COMMIT');
    return { orderId: request.params.id, status: nextStatus, tracking };
  } catch (error) { await client.query('ROLLBACK'); throw error; }
  finally { client.release(); }
});

app.post('/v1/orders/:id/reviews', { preHandler: requireRole('BUYER') }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  const rating = Number(request.body?.rating), reviewText = String(request.body?.reviewText || '').trim().slice(0, 2000), productId = Number(request.body?.productId);
  if (!Number.isInteger(productId) || !Number.isInteger(rating) || rating < 1 || rating > 5) return reply.code(400).send({ error: 'INVALID_REVIEW' });
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const order = await client.query('SELECT id FROM orders WHERE id=$1 AND buyer_id=$2 AND status=\'DELIVERED\'', [request.params.id,request.user.sub]);
    if (!order.rowCount) throw httpError(409, 'ORDER_NOT_DELIVERED');
    const line = await client.query('SELECT product_id FROM order_lines WHERE order_id=$1 AND product_id=$2', [request.params.id,productId]);
    if (!line.rowCount) throw httpError(400, 'PRODUCT_NOT_IN_ORDER');
    const inserted = await client.query('INSERT INTO product_reviews(product_id,buyer_id,order_id,rating,review_text) VALUES($1,$2,$3,$4,$5) ON CONFLICT (product_id,buyer_id,order_id) DO UPDATE SET rating=EXCLUDED.rating,review_text=EXCLUDED.review_text RETURNING id,product_id,rating,review_text,created_at', [productId,request.user.sub,request.params.id,rating,reviewText]);
    await client.query('UPDATE products SET rating=(SELECT ROUND(AVG(rating)::numeric,1) FROM product_reviews WHERE product_id=$1) WHERE id=$1', [productId]);
    await audit(client, request.user, 'REVIEW', inserted.rows[0].id, 'UPSERTED', { productId, orderId: request.params.id });
    await client.query('COMMIT');
    return reply.code(201).send(inserted.rows[0]);
  } catch (error) { await client.query('ROLLBACK'); throw error; }
  finally { client.release(); }
});

app.post('/v1/orders/:id/disputes', { preHandler: requireRole('BUYER') }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  const reason = String(request.body?.reason || '').trim().slice(0, 200), details = String(request.body?.details || '').trim().slice(0, 3000);
  if (!reason) return reply.code(400).send({ error: 'DISPUTE_REASON_REQUIRED' });
  const order = await pool.query('SELECT id,status FROM orders WHERE id=$1 AND buyer_id=$2', [request.params.id,request.user.sub]);
  if (!order.rowCount) return reply.code(404).send({ error: 'ORDER_NOT_FOUND' });
  if (!['PAID','PACKED','SHIPPED','OUT_FOR_DELIVERY','DELIVERED'].includes(normalizeStatus(order.rows[0].status))) return reply.code(409).send({ error: 'DISPUTE_NOT_ALLOWED' });
  try { const result = await pool.query('INSERT INTO order_disputes(id,order_id,buyer_id,reason,details) VALUES($1,$2,$3,$4,$5) RETURNING id,order_id,reason,details,status,created_at', [randomUUID(),request.params.id,request.user.sub,reason,details]); await audit(pool, request.user, 'ORDER', request.params.id, 'DISPUTE_OPENED', { reason }); return reply.code(201).send(result.rows[0]); }
  catch (error) { if (error.code === '23505') return reply.code(409).send({ error: 'OPEN_DISPUTE_EXISTS' }); throw error; }
});

app.get('/v1/admin/disputes', { preHandler: requireRole('ADMIN') }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  return (await pool.query(`SELECT d.*,u.email,u.display_name FROM order_disputes d JOIN users u ON u.id=d.buyer_id ORDER BY d.created_at DESC LIMIT 200`)).rows;
});

app.post('/v1/admin/disputes/:id/resolve', { preHandler: requireRole('ADMIN') }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  const status = normalizeStatus(request.body?.status), resolution = String(request.body?.resolution || '').slice(0, 2000);
  if (!['RESOLVED','REJECTED','UNDER_REVIEW'].includes(status)) return reply.code(400).send({ error: 'INVALID_DISPUTE_STATUS' });
  const result = await pool.query('UPDATE order_disputes SET status=$1,resolution=$2,updated_at=now() WHERE id=$3 RETURNING *', [status,resolution,request.params.id]);
  if (!result.rowCount) return reply.code(404).send({ error: 'DISPUTE_NOT_FOUND' });
  await audit(pool, request.user, 'DISPUTE', request.params.id, 'STATUS_CHANGED', { status });
  return result.rows[0];
});

app.post('/v1/webhooks/razorpay', { config: { rawBody: true } }, async (request, reply) => {
  if (!pool || !RAZORPAY_WEBHOOK_SECRET) return reply.code(503).send({ error: 'WEBHOOK_NOT_CONFIGURED' });
  const signature = request.headers['x-razorpay-signature'], eventId = request.headers['x-razorpay-event-id'];
  if (!signature || !eventId) return reply.code(400).send({ error: 'WEBHOOK_HEADERS_REQUIRED' });
  const raw = request.rawBody || '', expected = createHmac('sha256', RAZORPAY_WEBHOOK_SECRET).update(raw).digest('hex');
  if (!safeSignatureEqual(expected, signature)) return reply.code(400).send({ error: 'INVALID_WEBHOOK_SIGNATURE' });
  const payload = request.body || {}, event = String(payload.event || '');
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const paymentEntity = payload?.payload?.payment?.entity, orderEntity = payload?.payload?.order?.entity;
    const gatewayOrderId = paymentEntity?.order_id || orderEntity?.id, paymentId = paymentEntity?.id;
    const linked = gatewayOrderId ? await client.query('SELECT id FROM orders WHERE gateway_order_id=$1', [gatewayOrderId]) : { rowCount: 0 };
    const inserted = await client.query('INSERT INTO payment_events(event_id,order_id,event_type,payload) VALUES($1,$2,$3,$4) ON CONFLICT (event_id) DO NOTHING RETURNING event_id', [String(eventId),linked.rowCount ? linked.rows[0].id : null,event,JSON.stringify(payload)]);
    if (!inserted.rowCount) { await client.query('COMMIT'); return { received: true, duplicate: true }; }
    const captured = event === 'payment.captured' || event === 'order.paid' || paymentEntity?.status === 'captured' || orderEntity?.status === 'paid';
    const failed = event === 'payment.failed';
    const refunded = event.startsWith('refund.') || event === 'payment.refunded';
    if (gatewayOrderId) {
      const order = await client.query('SELECT id,total_paise,payment_status,status,gateway_payment_id FROM orders WHERE gateway_order_id=$1 FOR UPDATE', [gatewayOrderId]);
      if (order.rowCount) {
        const o = order.rows[0];
        if (captured && paymentId && Number(paymentEntity?.amount) === Number(o.total_paise)) {
          await client.query('UPDATE orders SET payment_status=\'CAPTURED\',status=CASE WHEN status=\'PENDING_PAYMENT\' THEN \'PAID\' ELSE status END,gateway_payment_id=$1,payment_expires_at=NULL,updated_at=now() WHERE id=$2', [paymentId,o.id]);
          await client.query('INSERT INTO seller_ledger(seller_id,order_id,amount_paise,type) SELECT seller_id,order_id,seller_amount_paise,\'SALE\' FROM order_lines WHERE order_id=$1 AND NOT EXISTS (SELECT 1 FROM seller_ledger sl WHERE sl.order_id=order_lines.order_id AND sl.seller_id=order_lines.seller_id AND sl.type=\'SALE\')', [o.id]);
        } else if (failed && o.payment_status !== 'CAPTURED' && o.status === 'PENDING_PAYMENT') {
          const lines = await client.query('SELECT product_id,quantity FROM order_lines WHERE order_id=$1', [o.id]);
          for (const line of lines.rows) await client.query('UPDATE products SET stock_quantity=stock_quantity+$1,updated_at=now() WHERE id=$2', [line.quantity,line.product_id]);
          await client.query('UPDATE orders SET payment_status=\'FAILED\',status=\'CANCELLED\',payment_expires_at=NULL,updated_at=now() WHERE id=$1', [o.id]);
        } else if (refunded && o.gateway_payment_id) {
          await client.query('UPDATE orders SET payment_status=\'REFUNDED\',status=CASE WHEN status=\'DELIVERED\' THEN status ELSE \'REFUNDED\' END,updated_at=now() WHERE id=$1', [o.id]);
          await client.query('INSERT INTO seller_ledger(seller_id,order_id,amount_paise,type) SELECT seller_id,order_id,seller_amount_paise,\'REFUND\' FROM order_lines WHERE order_id=$1 AND NOT EXISTS (SELECT 1 FROM seller_ledger sl WHERE sl.order_id=order_lines.order_id AND sl.seller_id=order_lines.seller_id AND sl.type=\'REFUND\')', [o.id]);
        }
      }
    }
    await client.query('COMMIT');
    return { received: true };
  } catch (error) { await client.query('ROLLBACK'); throw error; }
  finally { client.release(); }
});

const expirePendingOrders = async () => {
  if (!pool) return;
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const expired = await client.query(`SELECT id FROM orders WHERE payment_status='CREATED' AND status='PENDING_PAYMENT' AND payment_expires_at IS NOT NULL AND payment_expires_at<=now() FOR UPDATE SKIP LOCKED LIMIT 50`);
    for (const order of expired.rows) {
      const lines = await client.query('SELECT product_id,quantity FROM order_lines WHERE order_id=$1', [order.id]);
      for (const line of lines.rows) await client.query('UPDATE products SET stock_quantity=stock_quantity+$1,updated_at=now() WHERE id=$2', [line.quantity,line.product_id]);
      await client.query('UPDATE orders SET payment_status=\'FAILED\',status=\'CANCELLED\',payment_expires_at=NULL,cancel_reason=\'PAYMENT_EXPIRED\',cancelled_at=now(),updated_at=now() WHERE id=$1', [order.id]);
      await audit(client, null, 'ORDER', order.id, 'PAYMENT_EXPIRED');
    }
    await client.query('COMMIT');
  } catch (error) { await client.query('ROLLBACK'); app.log.error(error); }
  finally { client.release(); }
};
setInterval(expirePendingOrders, 60_000).unref();

app.setErrorHandler((error, request, reply) => { request.log.error(error); const status = error.statusCode && error.statusCode < 500 ? error.statusCode : 500; reply.code(status).send({ error: status < 500 && error.publicMessage ? error.publicMessage : 'INTERNAL_SERVER_ERROR' }); });
const port = Number(process.env.PORT || 8080);
await app.listen({ port, host: '0.0.0.0' });
