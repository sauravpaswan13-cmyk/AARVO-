export function enforceOrderActionReasons(source) {
  if (source.includes('AARVO_ACTION_REASON_V2')) return source;

  const helpers = `\nconst AARVO_ACTION_REASON_V2 = true;\nconst ACTION_REASONS = new Set(['PRODUCT_NOT_NEEDED','FOUND_BETTER_PRICE','ORDERED_BY_MISTAKE','DELIVERY_TOO_LATE','WRONG_PRODUCT','DAMAGED_OR_DEFECTIVE','SIZE_OR_FIT_ISSUE','DESCRIPTION_MISMATCH','QUALITY_ISSUE','CHANGED_MIND','OTHER']);\nconst normalizeActionReason = (value) => String(value || '').trim().toUpperCase().slice(0, 80);\nconst normalizeActionDetails = (value) => String(value || '').trim().slice(0, 500);\n`;
  source = source.replace("const normalizeStatus = (value) => String(value || '').trim().toUpperCase();", "const normalizeStatus = (value) => String(value || '').trim().toUpperCase();" + helpers);

  const replaceRoute = (route, replacement) => {
    const start = source.indexOf(`app.post('/v1/orders/:id/${route}'`);
    const end = start >= 0 ? source.indexOf('\napp.', start + 10) : -1;
    if (start >= 0 && end > start) source = source.slice(0, start) + replacement + source.slice(end);
  };

  replaceRoute('cancel', `app.post('/v1/orders/:id/cancel', { preHandler: requireRole('BUYER') }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  const reason = normalizeActionReason(request.body?.reason);
  const details = normalizeActionDetails(request.body?.details);
  if (!ACTION_REASONS.has(reason)) return reply.code(400).send({ error: 'CANCEL_REASON_REQUIRED', allowedReasons: [...ACTION_REASONS] });
  if (reason === 'OTHER' && !details) return reply.code(400).send({ error: 'CANCEL_REASON_DETAILS_REQUIRED' });
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const result = await client.query('SELECT id,status FROM orders WHERE id=$1 AND buyer_id=$2 FOR UPDATE', [request.params.id, request.user.sub]);
    if (!result.rowCount) throw httpError(404, 'ORDER_NOT_FOUND');
    if (normalizeStatus(result.rows[0].status) !== 'PENDING_PAYMENT') throw httpError(409, 'ORDER_NOT_CANCELLABLE');
    await client.query('UPDATE orders SET status=$1,cancel_reason=$2,cancelled_at=now(),updated_at=now() WHERE id=$3', ['CANCELLED', reason, request.params.id]);
    const lines = await client.query('SELECT product_id,quantity FROM order_lines WHERE order_id=$1', [request.params.id]);
    for (const line of lines.rows) await client.query('UPDATE products SET stock_quantity=stock_quantity+$1,updated_at=now() WHERE id=$2', [line.quantity, line.product_id]);
    await audit(client, request.user, 'ORDER', request.params.id, 'CANCELLED', { reason, details: details || null });
    await client.query('COMMIT');
    return { orderId: request.params.id, status: 'CANCELLED', cancelReason: reason, details: details || null };
  } catch (error) { await client.query('ROLLBACK'); throw error; } finally { client.release(); }
});`);

  replaceRoute('return', `app.post('/v1/orders/:id/return', { preHandler: requireRole('BUYER') }, async (request, reply) => {
  if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
  const reason = normalizeActionReason(request.body?.reason);
  const details = normalizeActionDetails(request.body?.details);
  if (!ACTION_REASONS.has(reason)) return reply.code(400).send({ error: 'RETURN_REASON_REQUIRED', allowedReasons: [...ACTION_REASONS] });
  if (reason === 'OTHER' && !details) return reply.code(400).send({ error: 'RETURN_REASON_DETAILS_REQUIRED' });
  const order = await pool.query('SELECT id,status FROM orders WHERE id=$1 AND buyer_id=$2', [request.params.id, request.user.sub]);
  if (!order.rowCount) return reply.code(404).send({ error: 'ORDER_NOT_FOUND' });
  if (!['DELIVERED','SHIPPED','OUT_FOR_DELIVERY'].includes(normalizeStatus(order.rows[0].status))) return reply.code(409).send({ error: 'RETURN_NOT_ALLOWED' });
  try {
    const result = await pool.query('INSERT INTO order_disputes(id,order_id,buyer_id,reason,details) VALUES($1,$2,$3,$4,$5) RETURNING id,order_id,reason,details,status,created_at', [randomUUID(), request.params.id, request.user.sub, reason, details || null]);
    await pool.query('UPDATE orders SET return_reason=$1,return_details=$2,return_requested_at=now(),updated_at=now() WHERE id=$3 AND buyer_id=$4', [reason, details || null, request.params.id, request.user.sub]);
    await audit(pool, request.user, 'ORDER', request.params.id, 'RETURN_REQUESTED', { reason, details: details || null });
    return reply.code(201).send(result.rows[0]);
  } catch (error) { if (error.code === '23505') return reply.code(409).send({ error: 'OPEN_RETURN_EXISTS' }); throw error; }
});`);

  return source;
}
