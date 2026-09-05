import Fastify from 'fastify';
import cors from '@fastify/cors';
import helmet from '@fastify/helmet';
import pg from 'pg';
import jwt from 'jsonwebtoken';
import { randomBytes, scryptSync, timingSafeEqual } from 'node:crypto';

const app = Fastify({ logger: true });
const { Pool } = pg;
const pool = process.env.DATABASE_URL ? new Pool({ connectionString: process.env.DATABASE_URL, ssl: process.env.DATABASE_SSL === 'true' ? { rejectUnauthorized: false } : undefined }) : null;
const JWT_SECRET = process.env.JWT_SECRET;
const DELIVERY_FEE_PAISE = Number(process.env.DELIVERY_FEE_PAISE || 0);
const PLATFORM_FEE_BPS = Number(process.env.PLATFORM_FEE_BPS || 0);

await app.register(helmet);
await app.register(cors, { origin: process.env.CORS_ORIGIN ? process.env.CORS_ORIGIN.split(',') : true });

const requireAuth = async (request, reply) => {
  if (!JWT_SECRET) return reply.code(503).send({ error: 'AUTH_NOT_CONFIGURED' });
  const header = request.headers.authorization || '';
  if (!header.startsWith('Bearer ')) return reply.code(401).send({ error: 'AUTH_REQUIRED' });
  try { request.user = jwt.verify(header.slice(7), JWT_SECRET); }
  catch { return reply.code(401).send({ error: 'INVALID_TOKEN' }); }
};
const hashPassword = (password) => {
  const salt = randomBytes(16).toString('hex');
  return `${salt}:${scryptSync(password, salt, 64).toString('hex')}`;
};
const verifyPassword = (password, stored) => {
  const [salt, expected] = String(stored || '').split(':');
  if (!salt || !expected) return false;
  const actual = scryptSync(password, salt, 64);
  const expectedBuffer = Buffer.from(expected, 'hex');
  return expectedBuffer.length === actual.length && timingSafeEqual(actual, expectedBuffer);
};
const issueToken = (user) => jwt.sign({ sub: user.id, role: user.role, email: user.email }, JWT_SECRET, { expiresIn: '7d' });
const badAddress = (a) => !a || !a.fullName || !a.phone || !a.line1 || !a.city || !a.state || !a.postalCode || !a.country;

app.get('/health', async () => ({ service: 'aarvo-api', status: 'ok', database: Boolean(pool), auth: Boolean(JWT_SECRET) }));

app.post('/v1/auth/register', async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  if (!JWT_SECRET) return reply.code(503).send({ error: 'AUTH_NOT_CONFIGURED' });
  const { email, password, displayName, role = 'BUYER', phone = '' } = request.body || {};
  const normalizedEmail = String(email || '').trim().toLowerCase();
  const normalizedRole = String(role).toUpperCase();
  if (!normalizedEmail || !password || String(password).length < 8 || !displayName) return reply.code(400).send({ error: 'INVALID_REGISTRATION' });
  if (!['BUYER', 'SELLER'].includes(normalizedRole)) return reply.code(400).send({ error: 'INVALID_ROLE' });
  const id = randomBytes(12).toString('hex');
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const user = await client.query('INSERT INTO users(id,email,display_name,password_hash,role) VALUES($1,$2,$3,$4,$5) RETURNING id,email,display_name,role', [id, normalizedEmail, String(displayName).trim(), hashPassword(String(password)), normalizedRole]);
    if (normalizedRole === 'SELLER') await client.query('INSERT INTO seller_profiles(seller_id,phone) VALUES($1,$2)', [id, String(phone).trim()]);
    await client.query('COMMIT');
    return reply.code(201).send({ user: user.rows[0], token: issueToken(user.rows[0]) });
  } catch (error) {
    await client.query('ROLLBACK');
    if (error.code === '23505') return reply.code(409).send({ error: 'EMAIL_ALREADY_REGISTERED' });
    throw error;
  } finally { client.release(); }
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
  return (await pool.query(`SELECT id, seller_id, seller_name, name, category, price_paise, rating, description, stock_quantity, is_published FROM products WHERE is_published=true AND stock_quantity>0 AND ($1='' OR name ILIKE '%'||$1||'%' OR description ILIKE '%'||$1||'%') AND ($2='' OR category=$2) ORDER BY created_at DESC LIMIT 100`, [q, category])).rows;
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
  const canPublish = seller.rows[0].verified && seller.rows[0].payout_account_ready;
  const isPublished = Boolean(publish) && canPublish;
  const result = await pool.query('INSERT INTO products(seller_id,seller_name,name,category,price_paise,description,stock_quantity,is_published) VALUES($1,$2,$3,$4,$5,$6,$7,$8) RETURNING id,seller_id,seller_name,name,category,price_paise,rating,description,stock_quantity,is_published', [request.user.sub,seller.rows[0].display_name,String(name).trim(),String(category).trim(),price,String(description).trim(),stock,isPublished]);
  return reply.code(201).send({ product: result.rows[0], published: isPublished, publishBlocked: Boolean(publish) && !canPublish });
});

app.get('/v1/seller/profile', { preHandler: requireAuth }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  if (request.user.role !== 'SELLER') return reply.code(403).send({ error: 'SELLER_ROLE_REQUIRED' });
  const result = await pool.query('SELECT seller_id,phone,verified,payout_account_ready FROM seller_profiles WHERE seller_id=$1', [request.user.sub]);
  if (!result.rowCount) return reply.code(404).send({ error: 'SELLER_PROFILE_NOT_FOUND' });
  return result.rows[0];
});

app.post('/v1/orders', { preHandler: requireAuth }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  if (request.user.role !== 'BUYER') return reply.code(403).send({ error: 'BUYER_ROLE_REQUIRED' });
  const { items, address } = request.body || {};
  if (!Array.isArray(items) || items.length < 1 || items.length > 50 || badAddress(address)) return reply.code(400).send({ error: 'INVALID_ORDER' });
  const normalized = items.map(x => ({ productId: Number(x.productId), quantity: Number(x.quantity) }));
  if (normalized.some(x => !Number.isInteger(x.productId) || !Number.isInteger(x.quantity) || x.quantity < 1 || x.quantity > 100)) return reply.code(400).send({ error: 'INVALID_ORDER_ITEMS' });
  const merged = new Map();
  for (const item of normalized) merged.set(item.productId, (merged.get(item.productId) || 0) + item.quantity);
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const ids = [...merged.keys()];
    const products = await client.query('SELECT id,seller_id,price_paise,stock_quantity,is_published FROM products WHERE id=ANY($1::bigint[]) FOR UPDATE', [ids]);
    if (products.rowCount !== ids.length) return reply.code(409).send({ error: 'PRODUCT_CHANGED' });
    let subtotal = 0;
    for (const p of products.rows) {
      const qty = merged.get(Number(p.id));
      if (!p.is_published || p.stock_quantity < qty) return reply.code(409).send({ error: 'INSUFFICIENT_STOCK', productId: p.id });
      subtotal += Number(p.price_paise) * qty;
    }
    const platformFee = Math.floor(subtotal * PLATFORM_FEE_BPS / 10000);
    const total = subtotal + DELIVERY_FEE_PAISE + platformFee;
    const orderId = crypto.randomUUID();
    await client.query('INSERT INTO orders(id,buyer_id,subtotal_paise,delivery_fee_paise,platform_fee_paise,total_paise,payment_status,status,address_json) VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9,$10)', [orderId,request.user.sub,subtotal,DELIVERY_FEE_PAISE,platformFee,total,'CREATED','PENDING_PAYMENT',JSON.stringify(address)]);
    for (const p of products.rows) {
      const qty = merged.get(Number(p.id));
      const lineTotal = Number(p.price_paise) * qty;
      await client.query('INSERT INTO order_lines(order_id,product_id,seller_id,quantity,unit_price_paise,seller_amount_paise) VALUES($1,$2,$3,$4,$5,$6)', [orderId,p.id,p.seller_id,qty,p.price_paise,lineTotal]);
      await client.query('UPDATE products SET stock_quantity=stock_quantity-$1,updated_at=now() WHERE id=$2', [qty,p.id]);
    }
    await client.query('COMMIT');
    return reply.code(201).send({ orderId, subtotalPaise: subtotal, deliveryFeePaise: DELIVERY_FEE_PAISE, platformFeePaise: platformFee, totalPaise: total, paymentStatus: 'CREATED', status: 'PENDING_PAYMENT', paymentProvider: process.env.RAZORPAY_KEY_ID ? 'RAZORPAY' : 'NOT_CONFIGURED' });
  } catch (error) {
    await client.query('ROLLBACK');
    throw error;
  } finally { client.release(); }
});

app.get('/v1/orders', { preHandler: requireAuth }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  if (request.user.role !== 'BUYER') return reply.code(403).send({ error: 'BUYER_ROLE_REQUIRED' });
  return (await pool.query('SELECT * FROM orders WHERE buyer_id=$1 ORDER BY created_at DESC', [request.user.sub])).rows;
});

app.setErrorHandler((error, request, reply) => { request.log.error(error); reply.code(error.statusCode && error.statusCode < 500 ? error.statusCode : 500).send({ error: 'INTERNAL_SERVER_ERROR' }); });
const port = Number(process.env.PORT || 8080);
await app.listen({ port, host: '0.0.0.0' });
