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

s = SERVER.read_text(encoding='utf-8')
if "from './ai.js'" not in s:
    anchor = "import Razorpay from 'razorpay';"
    if anchor not in s: raise SystemExit('server import anchor not found')
    s = s.replace(anchor, anchor + '\n' + AI_IMPORT, 1)
if "app.post('/v1/ai/assistant'" not in s:
    anchor = "app.post('/v1/seller/products'"
    if anchor not in s: raise SystemExit('server route anchor not found')
    s = s.replace(anchor, AI_ROUTES + '\n' + anchor, 1)
SERVER.write_text(s, encoding='utf-8')

s = MAIN.read_text(encoding='utf-8')
if 'AarvoAiActivity::class.java' not in s:
    anchor = 'actions = { BadgedBox(badge = {'
    if anchor not in s: raise SystemExit('MainActivity AI toolbar anchor not found')
    s = s.replace(anchor, 'actions = { TextButton(onClick = { activity.startActivity(Intent(activity, AarvoAiActivity::class.java)) }) { Text("AI") }; BadgedBox(badge = {', 1)
MAIN.write_text(s, encoding='utf-8')
print('AI integration applied')
