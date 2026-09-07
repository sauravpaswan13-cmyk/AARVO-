from pathlib import Path

MAIN = Path('app/src/main/java/com/aarvo/MainActivity.kt')
SERVER = Path('backend/src/server.js')

AI_IMPORT = "import { assistant, smartSearch, summarizeReviews, compareProducts, sellerListing, smartDealHints, aiConfigured } from './ai.js';"
AI_ROUTES = r'''

app.post('/v1/ai/assistant', async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  const message = String(request.body?.message || '').trim();
  if (!message || message.length > 1000) return reply.code(400).send({ error: 'INVALID_AI_MESSAGE' });
  const products = (await pool.query('SELECT id,seller_id,seller_name,name,category,price_paise,rating,description,stock_quantity,is_published FROM products WHERE is_published=true AND stock_quantity>0 ORDER BY rating DESC,created_at DESC LIMIT 100')).rows;
  return assistant({ message, products, user: request.user || null });
});

app.post('/v1/ai/search', async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  const query = String(request.body?.query || '').trim();
  if (!query || query.length > 500) return reply.code(400).send({ error: 'INVALID_AI_QUERY' });
  const parsed = await smartSearch({ query });
  const q = String(parsed.normalizedQuery || query).slice(0,200);
  const maxPrice = Number(parsed.maxPricePaise || 0);
  const result = await pool.query(`SELECT id,seller_id,seller_name,name,category,price_paise,rating,description,stock_quantity,is_published FROM products WHERE is_published=true AND stock_quantity>0 AND ($1='' OR name ILIKE '%'||$1||'%' OR description ILIKE '%'||$1||'%' OR category ILIKE '%'||$1||'%') AND ($2=0 OR price_paise<=$2) AND ($3=0 OR rating>=$3) ORDER BY rating DESC,created_at DESC LIMIT 100`, [q, maxPrice, Number(parsed.minRating || 0)]);
  return { intent: parsed.intent || 'SEARCH', normalizedQuery: q, products: result.rows };
});

app.post('/v1/ai/compare', async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  const ids = Array.isArray(request.body?.productIds) ? [...new Set(request.body.productIds.map(Number))].filter(Number.isInteger).slice(0,3) : [];
  if (ids.length < 2) return reply.code(400).send({ error: 'TWO_OR_MORE_PRODUCTS_REQUIRED' });
  const result = await pool.query('SELECT id,seller_id,seller_name,name,category,price_paise,rating,description,stock_quantity,is_published FROM products WHERE id=ANY($1::int[]) AND is_published=true', [ids]);
  return compareProducts(result.rows);
});

app.get('/v1/ai/reviews/:id', async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  const product = await pool.query('SELECT id,name,rating FROM products WHERE id=$1 AND is_published=true', [request.params.id]);
  if (!product.rowCount) return reply.code(404).send({ error: 'PRODUCT_NOT_FOUND' });
  const reviews = await pool.query('SELECT rating,review_text FROM product_reviews WHERE product_id=$1 ORDER BY created_at DESC LIMIT 100', [request.params.id]);
  return summarizeReviews({ product: product.rows[0], reviews: reviews.rows });
});

app.post('/v1/ai/seller-listing', { preHandler: requireRole('SELLER') }, async (request, reply) => {
  const body = request.body || {};
  if (!String(body.name || '').trim()) return reply.code(400).send({ error: 'PRODUCT_NAME_REQUIRED' });
  return sellerListing(body);
});

app.post('/v1/ai/deals', async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  const products = (await pool.query('SELECT id,name,price_paise,rating,stock_quantity FROM products WHERE is_published=true AND stock_quantity>0 ORDER BY rating DESC,created_at DESC LIMIT 50')).rows;
  return { aiConfigured: aiConfigured(), deals: smartDealHints(products) };
});
'''

MARKETPLACE_ROUTES = r'''

// Marketplace completion layer: seller catalog lifecycle, returns/refunds and settlement visibility.
app.put('/v1/seller/products/:id', { preHandler: requireRole('SELLER') }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  const { name, category, pricePaise, description, stockQuantity, publish } = request.body || {};
  const price = Number(pricePaise), stock = Number(stockQuantity);
  if (!String(name || '').trim() || !String(category || '').trim() || !String(description || '').trim() || !Number.isInteger(price) || price <= 0 || !Number.isInteger(stock) || stock < 0) return reply.code(400).send({ error: 'INVALID_PRODUCT' });
  const seller = await pool.query('SELECT verified,payout_account_ready FROM seller_profiles WHERE seller_id=$1', [request.user.sub]);
  if (!seller.rowCount) return reply.code(403).send({ error: 'SELLER_PROFILE_REQUIRED' });
  const wantsPublish = publish === undefined ? undefined : Boolean(publish);
  const isPublished = wantsPublish === undefined ? undefined : (wantsPublish && seller.rows[0].verified && seller.rows[0].payout_account_ready);
  const result = isPublished === undefined
    ? await pool.query('UPDATE products SET name=$1,category=$2,price_paise=$3,description=$4,stock_quantity=$5,updated_at=now() WHERE id=$6 AND seller_id=$7 RETURNING id,seller_id,seller_name,name,category,price_paise,rating,description,stock_quantity,is_published,updated_at', [String(name).trim(),String(category).trim(),price,String(description).trim(),stock,request.params.id,request.user.sub])
    : await pool.query('UPDATE products SET name=$1,category=$2,price_paise=$3,description=$4,stock_quantity=$5,is_published=$6,updated_at=now() WHERE id=$7 AND seller_id=$8 RETURNING id,seller_id,seller_name,name,category,price_paise,rating,description,stock_quantity,is_published,updated_at', [String(name).trim(),String(category).trim(),price,String(description).trim(),stock,isPublished,request.params.id,request.user.sub]);
  if (!result.rowCount) return reply.code(404).send({ error: 'PRODUCT_NOT_FOUND' });
  await audit(pool, request.user, 'PRODUCT', request.params.id, 'UPDATED', { published: result.rows[0].is_published });
  return { product: result.rows[0], publishBlocked: wantsPublish === true && !result.rows[0].is_published };
});

app.delete('/v1/seller/products/:id', { preHandler: requireRole('SELLER') }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  const result = await pool.query('DELETE FROM products WHERE id=$1 AND seller_id=$2 AND NOT EXISTS (SELECT 1 FROM order_lines WHERE product_id=$1) RETURNING id', [request.params.id, request.user.sub]);
  if (!result.rowCount) return reply.code(409).send({ error: 'PRODUCT_NOT_FOUND_OR_ALREADY_ORDERED' });
  await audit(pool, request.user, 'PRODUCT', request.params.id, 'DELETED');
  return { deleted: true, productId: Number(request.params.id) };
});

app.post('/v1/seller/products/:id/publish', { preHandler: requireRole('SELLER') }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  const publish = request.body?.publish !== false;
  const seller = await pool.query('SELECT verified,payout_account_ready FROM seller_profiles WHERE seller_id=$1', [request.user.sub]);
  if (!seller.rowCount) return reply.code(403).send({ error: 'SELLER_PROFILE_REQUIRED' });
  const allowed = seller.rows[0].verified && seller.rows[0].payout_account_ready;
  if (publish && !allowed) return reply.code(403).send({ error: 'SELLER_VERIFICATION_AND_PAYOUT_REQUIRED' });
  const result = await pool.query('UPDATE products SET is_published=$1,updated_at=now() WHERE id=$2 AND seller_id=$3 RETURNING id,is_published', [publish, request.params.id, request.user.sub]);
  if (!result.rowCount) return reply.code(404).send({ error: 'PRODUCT_NOT_FOUND' });
  await audit(pool, request.user, 'PRODUCT', request.params.id, publish ? 'PUBLISHED' : 'UNPUBLISHED');
  return result.rows[0];
});

app.get('/v1/seller/settlements', { preHandler: requireRole('SELLER') }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  const rows = (await pool.query(`SELECT type,COALESCE(SUM(amount_paise),0)::bigint AS amount_paise,COUNT(*)::int AS entries FROM seller_ledger WHERE seller_id=$1 GROUP BY type ORDER BY type`, [request.user.sub])).rows;
  const ledger = (await pool.query(`SELECT id,order_id,amount_paise,type,gateway_transfer_id,created_at FROM seller_ledger WHERE seller_id=$1 ORDER BY created_at DESC LIMIT 200`, [request.user.sub])).rows;
  return { summary: rows, ledger };
});

app.post('/v1/orders/:id/return', { preHandler: requireRole('BUYER') }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  const reason = String(request.body?.reason || 'RETURN_REQUESTED').trim().slice(0,200);
  const details = String(request.body?.details || '').trim().slice(0,3000);
  const order = await pool.query('SELECT id,status FROM orders WHERE id=$1 AND buyer_id=$2', [request.params.id, request.user.sub]);
  if (!order.rowCount) return reply.code(404).send({ error: 'ORDER_NOT_FOUND' });
  if (!['DELIVERED','SHIPPED','OUT_FOR_DELIVERY'].includes(normalizeStatus(order.rows[0].status))) return reply.code(409).send({ error: 'RETURN_NOT_ALLOWED' });
  try {
    const result = await pool.query('INSERT INTO order_disputes(id,order_id,buyer_id,reason,details) VALUES($1,$2,$3,$4,$5) RETURNING id,order_id,reason,details,status,created_at', [randomUUID(),request.params.id,request.user.sub,reason,details]);
    await audit(pool,request.user,'ORDER',request.params.id,'RETURN_REQUESTED',{reason});
    return reply.code(201).send(result.rows[0]);
  } catch (error) { if (error.code === '23505') return reply.code(409).send({ error: 'OPEN_RETURN_EXISTS' }); throw error; }
});

app.post('/v1/admin/orders/:id/refund', { preHandler: requireRole('ADMIN') }, async (request, reply) => {
  if (!pool || !razorpay) return reply.code(503).send({ error: 'PAYMENTS_NOT_CONFIGURED' });
  const order = await pool.query('SELECT id,total_paise,gateway_payment_id,payment_status,status FROM orders WHERE id=$1', [request.params.id]);
  if (!order.rowCount) return reply.code(404).send({ error: 'ORDER_NOT_FOUND' });
  const o = order.rows[0];
  if (o.payment_status !== 'CAPTURED' || !o.gateway_payment_id) return reply.code(409).send({ error: 'ORDER_NOT_REFUNDABLE' });
  const amount = request.body?.amountPaise === undefined ? Number(o.total_paise) : Number(request.body.amountPaise);
  if (!Number.isInteger(amount) || amount <= 0 || amount > Number(o.total_paise)) return reply.code(400).send({ error: 'INVALID_REFUND_AMOUNT' });
  const refund = await razorpay.payments.refund(o.gateway_payment_id, { amount });
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    await client.query('UPDATE orders SET refund_status=$1,payment_status=CASE WHEN $2=$3 THEN \'REFUNDED\' ELSE payment_status END,refunded_at=CASE WHEN $2=$3 THEN now() ELSE refunded_at END,status=CASE WHEN $2=$3 AND status<>\'DELIVERED\' THEN \'REFUNDED\' ELSE status END,updated_at=now() WHERE id=$4', [amount === Number(o.total_paise) ? 'PROCESSED' : 'PARTIAL', amount, Number(o.total_paise), request.params.id]);
    await client.query('INSERT INTO seller_ledger(seller_id,order_id,amount_paise,type,gateway_transfer_id) SELECT seller_id,order_id,LEAST(seller_amount_paise,$1),\'REFUND\',$2 FROM order_lines WHERE order_id=$3', [amount, refund.id, request.params.id]);
    await audit(client,request.user,'ORDER',request.params.id,'REFUND_PROCESSED',{refundId:refund.id,amountPaise:amount});
    await client.query('COMMIT');
  } catch (error) { await client.query('ROLLBACK'); throw error; } finally { client.release(); }
  return { orderId: request.params.id, refundId: refund.id, amountPaise: amount, refundStatus: amount === Number(o.total_paise) ? 'PROCESSED' : 'PARTIAL' };
});
'''

s = SERVER.read_text(encoding='utf-8')
if "from './ai.js'" not in s:
    anchor = "import Razorpay from 'razorpay';"
    if anchor not in s: raise SystemExit('server import anchor not found')
    s = s.replace(anchor, anchor + '\n' + AI_IMPORT, 1)
if "app.post('/v1/ai/assistant'" not in s:
    anchor = "app.post('/v1/seller/products'"
    if anchor not in s: raise SystemExit('server route anchor not found')
    s = s.replace(anchor, AI_ROUTES + '\n' + anchor, 1)
if "app.put('/v1/seller/products/:id'" not in s:
    anchor = "app.get('/v1/admin/sellers'"
    if anchor not in s: raise SystemExit('marketplace route anchor not found')
    s = s.replace(anchor, MARKETPLACE_ROUTES + '\n' + anchor, 1)
SERVER.write_text(s, encoding='utf-8')

s = MAIN.read_text(encoding='utf-8')
# Ensure the app's unauthenticated state always uses the phone-auth flow, so the
# requiresPhoneVerification response can never be bypassed by the legacy screen.
old = '!signedIn -> SignInScreen(api) { name, token, userRole -> userName = name; role = userRole; prefs.edit().putBoolean("signed_in", true).putBoolean("guest_mode", false).putString("user_name", name).putString("user_role", userRole).putString("auth_token", token).apply(); signedIn = true }'
new = '!signedIn -> { LaunchedEffect(Unit) { openOtpLogin() } }'
if old in s:
    s = s.replace(old, new, 1)
MAIN.write_text(s, encoding='utf-8')
print('AARVO marketplace completion integration applied')
