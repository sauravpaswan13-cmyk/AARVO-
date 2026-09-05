import Fastify from 'fastify';
import cors from '@fastify/cors';
import helmet from '@fastify/helmet';
import pg from 'pg';

const app = Fastify({ logger: true });
const { Pool } = pg;
const pool = process.env.DATABASE_URL ? new Pool({ connectionString: process.env.DATABASE_URL, ssl: process.env.DATABASE_SSL === 'true' ? { rejectUnauthorized: false } : undefined }) : null;

await app.register(helmet);
await app.register(cors, { origin: process.env.CORS_ORIGIN ? process.env.CORS_ORIGIN.split(',') : true });

app.get('/health', async () => ({ service: 'aarvo-api', status: 'ok', database: Boolean(pool) }));

app.get('/v1/products', async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  const { q = '', category = '' } = request.query;
  const result = await pool.query(`
    SELECT id, seller_id, seller_name, name, category, price_paise, rating, description, stock_quantity, is_published
    FROM products
    WHERE is_published = true AND stock_quantity > 0
      AND ($1 = '' OR name ILIKE '%' || $1 || '%' OR description ILIKE '%' || $1 || '%')
      AND ($2 = '' OR category = $2)
    ORDER BY created_at DESC LIMIT 100`, [q, category]);
  return result.rows;
});

app.get('/v1/products/:id', async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  const result = await pool.query('SELECT id, seller_id, seller_name, name, category, price_paise, rating, description, stock_quantity, is_published FROM products WHERE id=$1 AND is_published=true', [request.params.id]);
  if (!result.rowCount) return reply.code(404).send({ error: 'PRODUCT_NOT_FOUND' });
  return result.rows[0];
});

app.get('/v1/orders', async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  const buyerId = request.headers['x-user-id'];
  if (!buyerId) return reply.code(401).send({ error: 'AUTH_REQUIRED' });
  const result = await pool.query('SELECT * FROM orders WHERE buyer_id=$1 ORDER BY created_at DESC', [buyerId]);
  return result.rows;
});

app.setErrorHandler((error, request, reply) => {
  request.log.error(error);
  reply.code(error.statusCode && error.statusCode < 500 ? error.statusCode : 500).send({ error: 'INTERNAL_SERVER_ERROR' });
});

const port = Number(process.env.PORT || 8080);
await app.listen({ port, host: '0.0.0.0' });
