export async function registerCartCompletion({ app, pool, requireRole, audit }) {
  app.get('/v1/cart', { preHandler: requireRole('BUYER') }, async (request, reply) => {
    if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
    return (await pool.query(`
      SELECT c.product_id AS "productId", c.quantity,
             p.name, p.category, p.price_paise AS "pricePaise", p.image_url AS "imageUrl",
             p.stock_quantity AS "stockQuantity", p.is_published AS "isPublished"
      FROM buyer_carts c
      JOIN products p ON p.id=c.product_id
      WHERE c.buyer_id=$1
      ORDER BY c.updated_at DESC
    `, [request.user.sub])).rows;
  });

  app.post('/v1/cart/items', { preHandler: requireRole('BUYER') }, async (request, reply) => {
    if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
    const productId = Number(request.body?.productId);
    const quantity = Number(request.body?.quantity ?? 1);
    if (!Number.isInteger(productId) || !Number.isInteger(quantity) || quantity < 1 || quantity > 100) return reply.code(400).send({ error: 'INVALID_CART_ITEM' });
    const product = await pool.query('SELECT id,stock_quantity,is_published FROM products WHERE id=$1', [productId]);
    if (!product.rowCount || !product.rows[0].is_published) return reply.code(404).send({ error: 'PRODUCT_NOT_FOUND' });
    if (product.rows[0].stock_quantity < quantity) return reply.code(409).send({ error: 'INSUFFICIENT_STOCK' });
    const result = await pool.query(`
      INSERT INTO buyer_carts(buyer_id,product_id,quantity,updated_at)
      VALUES($1,$2,$3,now())
      ON CONFLICT (buyer_id,product_id)
      DO UPDATE SET quantity=EXCLUDED.quantity,updated_at=now()
      RETURNING buyer_id,product_id,quantity,updated_at
    `, [request.user.sub, productId, quantity]);
    await audit(pool, request.user, 'CART', `${request.user.sub}:${productId}`, 'ITEM_UPDATED', { quantity });
    return reply.code(201).send({ productId, quantity, updatedAt: result.rows[0].updated_at });
  });

  app.patch('/v1/cart/items/:productId', { preHandler: requireRole('BUYER') }, async (request, reply) => {
    if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
    const productId = Number(request.params.productId);
    const quantity = Number(request.body?.quantity);
    if (!Number.isInteger(productId) || !Number.isInteger(quantity) || quantity < 1 || quantity > 100) return reply.code(400).send({ error: 'INVALID_CART_ITEM' });
    const product = await pool.query('SELECT stock_quantity,is_published FROM products WHERE id=$1', [productId]);
    if (!product.rowCount || !product.rows[0].is_published) return reply.code(404).send({ error: 'PRODUCT_NOT_FOUND' });
    if (product.rows[0].stock_quantity < quantity) return reply.code(409).send({ error: 'INSUFFICIENT_STOCK' });
    const result = await pool.query('UPDATE buyer_carts SET quantity=$1,updated_at=now() WHERE buyer_id=$2 AND product_id=$3 RETURNING product_id,quantity,updated_at', [quantity, request.user.sub, productId]);
    if (!result.rowCount) return reply.code(404).send({ error: 'CART_ITEM_NOT_FOUND' });
    return result.rows[0];
  });

  app.delete('/v1/cart/items/:productId', { preHandler: requireRole('BUYER') }, async (request, reply) => {
    if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
    const productId = Number(request.params.productId);
    if (!Number.isInteger(productId)) return reply.code(400).send({ error: 'INVALID_PRODUCT_ID' });
    const result = await pool.query('DELETE FROM buyer_carts WHERE buyer_id=$1 AND product_id=$2 RETURNING product_id', [request.user.sub, productId]);
    if (!result.rowCount) return reply.code(404).send({ error: 'CART_ITEM_NOT_FOUND' });
    return { deleted: true, productId };
  });

  app.delete('/v1/cart', { preHandler: requireRole('BUYER') }, async (request, reply) => {
    if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
    await pool.query('DELETE FROM buyer_carts WHERE buyer_id=$1', [request.user.sub]);
    return { cleared: true };
  });
}
