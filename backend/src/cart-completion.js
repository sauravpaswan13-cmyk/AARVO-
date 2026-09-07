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

  // Checkout uses the persistent cart and saved delivery address, then delegates
  // pricing, stock reservation, idempotency and Razorpay order creation to /v1/orders.
  app.post('/v1/checkout', { preHandler: requireRole('BUYER') }, async (request, reply) => {
    if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
    const idempotencyKey = String(request.headers['idempotency-key'] || request.body?.idempotencyKey || '').trim();
    if (idempotencyKey.length < 8 || idempotencyKey.length > 128) return reply.code(400).send({ error: 'IDEMPOTENCY_KEY_REQUIRED' });

    const requestedAddressId = request.body?.addressId;
    const addressResult = requestedAddressId
      ? await pool.query('SELECT id,label,full_name,phone,line1,line2,city,state,postal_code,country,is_default FROM buyer_addresses WHERE id=$1 AND buyer_id=$2', [requestedAddressId, request.user.sub])
      : await pool.query('SELECT id,label,full_name,phone,line1,line2,city,state,postal_code,country,is_default FROM buyer_addresses WHERE buyer_id=$1 ORDER BY is_default DESC,updated_at DESC LIMIT 1', [request.user.sub]);
    if (!addressResult.rowCount) return reply.code(400).send({ error: 'DELIVERY_ADDRESS_REQUIRED' });
    const a = addressResult.rows[0];
    const cart = await pool.query('SELECT product_id AS "productId",quantity FROM buyer_carts WHERE buyer_id=$1 ORDER BY updated_at ASC', [request.user.sub]);
    if (!cart.rowCount) return reply.code(400).send({ error: 'CART_EMPTY' });

    const internal = await app.inject({
      method: 'POST',
      url: '/v1/orders',
      headers: { authorization: request.headers.authorization || '', 'idempotency-key': idempotencyKey },
      payload: {
        items: cart.rows,
        address: {
          id: a.id,
          label: a.label,
          fullName: a.full_name,
          phone: a.phone,
          line1: a.line1,
          line2: a.line2,
          city: a.city,
          state: a.state,
          postalCode: a.postal_code,
          country: a.country
        }
      }
    });
    if (internal.statusCode >= 400) return reply.code(internal.statusCode).send(internal.json());
    return reply.code(internal.statusCode).send(internal.json());
  });

  // Once payment verification succeeds, remove only the products belonging to
  // that paid order. Other items added to the cart remain untouched.
  app.addHook('onResponse', async (request, reply) => {
    if (!pool || request.url.split('?')[0] !== '/v1/payments/verify' || reply.statusCode < 200 || reply.statusCode >= 300) return;
    try {
      const body = request.body || {};
      const orderId = String(body.orderId || '').trim();
      if (!orderId || !request.user?.sub) return;
      await pool.query(`
        DELETE FROM buyer_carts c
        USING order_lines ol
        WHERE c.buyer_id=$1 AND ol.order_id=$2::uuid AND c.product_id=ol.product_id
      `, [request.user.sub, orderId]);
    } catch (error) {
      app.log.error(error, 'cart cleanup after payment failed');
    }
  });
}
