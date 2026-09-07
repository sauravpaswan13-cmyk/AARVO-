export async function registerSettlementCompletion({ app, pool, requireRole, audit }) {
  app.get('/v1/seller/settlement', { preHandler: requireRole('SELLER') }, async (request, reply) => {
    if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
    const sellerId = request.user.sub;
    const result = await pool.query(`
      SELECT COALESCE(SUM(CASE WHEN type='SALE' THEN amount_paise WHEN type IN ('REFUND','REVERSAL') THEN -amount_paise WHEN type='PAYOUT' THEN -amount_paise ELSE 0 END),0)::bigint AS "availablePaise",
             COALESCE(SUM(CASE WHEN type='PAYOUT' THEN amount_paise ELSE 0 END),0)::bigint AS "paidOutPaise"
      FROM seller_ledger WHERE seller_id=$1
    `, [sellerId]);
    const row = result.rows[0];
    return { availablePaise: Math.max(0, Number(row.availablePaise)), paidOutPaise: Number(row.paidOutPaise) };
  });

  app.post('/v1/seller/payout', { preHandler: requireRole('SELLER') }, async (request, reply) => {
    if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
    const sellerId = request.user.sub;
    const amountPaise = Number(request.body?.amountPaise);
    if (!Number.isSafeInteger(amountPaise) || amountPaise <= 0) return reply.code(400).send({ error: 'INVALID_PAYOUT_AMOUNT' });
    const account = await pool.query('SELECT payout_account_ready,gateway_account_id FROM seller_profiles WHERE seller_id=$1', [sellerId]);
    if (!account.rowCount || !account.rows[0].payout_account_ready || !account.rows[0].gateway_account_id) return reply.code(409).send({ error: 'PAYOUT_ACCOUNT_NOT_READY' });
    const balance = await pool.query(`
      SELECT COALESCE(SUM(CASE WHEN type='SALE' THEN amount_paise WHEN type IN ('REFUND','REVERSAL','PAYOUT') THEN -amount_paise ELSE 0 END),0)::bigint AS available
      FROM seller_ledger WHERE seller_id=$1
    `, [sellerId]);
    const available = Number(balance.rows[0].available);
    if (amountPaise > available) return reply.code(409).send({ error: 'INSUFFICIENT_SETTLEMENT_BALANCE', availablePaise: Math.max(0, available) });
    const result = await pool.query(`
      INSERT INTO seller_ledger(seller_id,order_id,amount_paise,type)
      VALUES($1,NULL,$2,'PAYOUT')
      RETURNING id,amount_paise,created_at
    `, [sellerId, amountPaise]);
    await audit(pool, request.user, 'SELLER_SETTLEMENT', sellerId, 'PAYOUT_REQUESTED', { amountPaise, ledgerId: result.rows[0].id });
    return reply.code(202).send({ status: 'PENDING_TRANSFER', payoutId: result.rows[0].id, amountPaise });
  });
}
