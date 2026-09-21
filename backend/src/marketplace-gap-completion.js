import { randomUUID } from 'node:crypto';

export async function registerMarketplaceGapCompletion({ app, pool, requireRole, audit }) {
  app.post('/v1/products/:id/viewed', { preHandler: requireRole('BUYER') }, async (request, reply) => {
    if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
    const productId = Number(request.params.id);
    if (!Number.isInteger(productId) || productId <= 0) return reply.code(400).send({ error: 'INVALID_PRODUCT_ID' });
    const product = await pool.query('SELECT id FROM products WHERE id=$1 AND is_published=true', [productId]);
    if (!product.rowCount) return reply.code(404).send({ error: 'PRODUCT_NOT_FOUND' });
    await pool.query('INSERT INTO recently_viewed_products(user_id,product_id,viewed_at) VALUES($1,$2,now()) ON CONFLICT(user_id,product_id) DO UPDATE SET viewed_at=now()', [request.user.sub, productId]);
    await pool.query('DELETE FROM recently_viewed_products WHERE user_id=$1 AND product_id NOT IN (SELECT product_id FROM recently_viewed_products WHERE user_id=$1 ORDER BY viewed_at DESC LIMIT 30)', [request.user.sub]);
    return { saved: true };
  });

  app.get('/v1/products/recently-viewed', { preHandler: requireRole('BUYER') }, async (request, reply) => {
    if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
    return (await pool.query(`SELECT p.id,p.seller_id,p.seller_name,p.name,p.category,p.price_paise,p.rating,p.description,p.stock_quantity,p.is_published,p.image_url
      FROM recently_viewed_products rv JOIN products p ON p.id=rv.product_id
      WHERE rv.user_id=$1 ORDER BY rv.viewed_at DESC LIMIT 30`, [request.user.sub])).rows;
  });

  app.post('/v1/orders/:id/return-request', { preHandler: requireRole('BUYER') }, async (request, reply) => {
    if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
    const reason = String(request.body?.reason || '').trim().slice(0,120);
    const details = String(request.body?.details || '').trim().slice(0,3000);
    if (!reason) return reply.code(400).send({ error: 'RETURN_REASON_REQUIRED' });
    const order = await pool.query('SELECT id,status,buyer_id FROM orders WHERE id=$1 AND buyer_id=$2', [request.params.id, request.user.sub]);
    if (!order.rowCount) return reply.code(404).send({ error: 'ORDER_NOT_FOUND' });
    if (!['DELIVERED','PAID','CONFIRMED'].includes(order.rows[0].status)) return reply.code(409).send({ error: 'ORDER_NOT_ELIGIBLE_FOR_RETURN' });
    const id = randomUUID();
    try {
      const r = await pool.query('INSERT INTO order_returns(id,order_id,buyer_id,reason,details) VALUES($1,$2,$3,$4,$5) RETURNING id,order_id,reason,details,status,created_at', [id, request.params.id, request.user.sub, reason, details]);
      await audit(pool, request.user, 'ORDER_RETURN', id, 'RETURN_REQUESTED', { orderId: request.params.id, reason });
      return reply.code(201).send(r.rows[0]);
    } catch (e) {
      if (e.code === '23505') return reply.code(409).send({ error: 'RETURN_ALREADY_OPEN' });
      throw e;
    }
  });

  app.get('/v1/orders/:id/invoice', { preHandler: requireRole('BUYER') }, async (request, reply) => {
    if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
    const order = await pool.query(`SELECT o.id,o.created_at,o.status,o.payment_status,o.subtotal_paise,o.delivery_fee_paise,o.platform_fee_paise,o.total_paise,o.address_json,
      COALESCE(json_agg(json_build_object('productId',ol.product_id,'quantity',ol.quantity,'unitPricePaise',ol.unit_price_paise,'sellerAmountPaise',ol.seller_amount_paise)) FILTER (WHERE ol.order_id IS NOT NULL),'[]') AS items
      FROM orders o LEFT JOIN order_lines ol ON ol.order_id=o.id WHERE o.id=$1 AND o.buyer_id=$2 GROUP BY o.id`, [request.params.id, request.user.sub]);
    if (!order.rowCount) return reply.code(404).send({ error: 'ORDER_NOT_FOUND' });
    return { invoiceVersion: 1, invoice: order.rows[0] };
  });

  app.post('/v1/support/tickets', { preHandler: requireRole('BUYER') }, async (request, reply) => {
    if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
    const subject = String(request.body?.subject || '').trim().slice(0,160);
    const details = String(request.body?.details || '').trim().slice(0,5000);
    if (!subject || details.length < 5) return reply.code(400).send({ error: 'INVALID_SUPPORT_TICKET' });
    const id = randomUUID();
    const r = await pool.query('INSERT INTO support_tickets(id,buyer_id,order_id,subject,details) VALUES($1,$2,$3,$4,$5) RETURNING id,subject,details,status,created_at', [id,request.user.sub,request.body?.orderId || null,subject,details]);
    await audit(pool, request.user, 'SUPPORT_TICKET', id, 'CREATED');
    return reply.code(201).send(r.rows[0]);
  });

  app.get('/v1/notifications', { preHandler: requireRole('BUYER') }, async (request, reply) => {
    if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
    return (await pool.query('SELECT id,type,title,body,data,read_at,created_at FROM notifications WHERE user_id=$1 ORDER BY created_at DESC LIMIT 100', [request.user.sub])).rows;
  });

  app.post('/v1/notifications/:id/read', { preHandler: requireRole('BUYER') }, async (request, reply) => {
    if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
    const r = await pool.query('UPDATE notifications SET read_at=COALESCE(read_at,now()) WHERE id=$1 AND user_id=$2 RETURNING id,read_at', [request.params.id, request.user.sub]);
    if (!r.rowCount) return reply.code(404).send({ error: 'NOTIFICATION_NOT_FOUND' });
    return r.rows[0];
  });
}
