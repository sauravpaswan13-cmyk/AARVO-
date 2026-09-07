export async function registerSettlementCompletion({ app, pool, requireRole, audit, razorpay }) {
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
    if (!razorpay) return reply.code(503).send({ error: 'PAYMENTS_NOT_CONFIGURED' });

    const sellerId = request.user.sub;
    const amountPaise = Number(request.body?.amountPaise);
    if (!Number.isSafeInteger(amountPaise) || amountPaise <= 0) return reply.code(400).send({ error: 'INVALID_PAYOUT_AMOUNT' });

    const account = await pool.query('SELECT payout_account_ready,gateway_account_id FROM seller_profiles WHERE seller_id=$1', [sellerId]);
    if (!account.rowCount || !account.rows[0].payout_account_ready || !account.rows[0].gateway_account_id) {
      return reply.code(409).send({ error: 'PAYOUT_ACCOUNT_NOT_READY' });
    }
    const gatewayAccountId = account.rows[0].gateway_account_id;

    const candidates = await pool.query(`
      SELECT sl.id, sl.order_id, sl.amount_paise, o.gateway_payment_id,
             GREATEST(0, sl.amount_paise - COALESCE((
               SELECT SUM(p.amount_paise) FROM seller_ledger p
               WHERE p.seller_id=sl.seller_id AND p.order_id=sl.order_id AND p.type='PAYOUT'
             ),0))::bigint AS remaining_paise
      FROM seller_ledger sl
      JOIN orders o ON o.id=sl.order_id
      WHERE sl.seller_id=$1 AND sl.type='SALE'
        AND o.gateway_payment_id IS NOT NULL
        AND o.payment_status='CAPTURED'
      ORDER BY sl.created_at ASC, sl.id ASC
    `, [sellerId]);

    const available = candidates.rows.reduce((sum, row) => sum + Number(row.remaining_paise), 0);
    if (amountPaise > available) {
      return reply.code(409).send({ error: 'INSUFFICIENT_SETTLEMENT_BALANCE', availablePaise: available });
    }

    let remaining = amountPaise;
    let transferred = 0;
    const transfers = [];

    for (const row of candidates.rows) {
      if (remaining <= 0) break;
      const rowRemaining = Number(row.remaining_paise);
      if (rowRemaining <= 0) continue;
      const transferAmount = Math.min(remaining, rowRemaining);

      try {
        const transfer = await razorpay.payments.transfer(row.gateway_payment_id, {
          transfers: [{
            account: gatewayAccountId,
            amount: transferAmount,
            currency: 'INR'
          }]
        });
        const transferId = transfer?.items?.[0]?.id || transfer?.id;
        if (!transferId) throw new Error('RAZORPAY_TRANSFER_ID_MISSING');

        const ledger = await pool.query(`
          INSERT INTO seller_ledger(seller_id,order_id,amount_paise,type,gateway_transfer_id)
          VALUES($1,$2,$3,'PAYOUT',$4)
          RETURNING id,amount_paise,created_at,gateway_transfer_id
        `, [sellerId, row.order_id, transferAmount, transferId]);
        await audit(pool, request.user, 'SELLER_SETTLEMENT', sellerId, 'PAYOUT_TRANSFER_CREATED', {
          amountPaise: transferAmount, ledgerId: ledger.rows[0].id, gatewayTransferId: transferId, orderId: row.order_id
        });
        transfers.push({ payoutId: ledger.rows[0].id, orderId: row.order_id, transferId, amountPaise: transferAmount });
        transferred += transferAmount;
        remaining -= transferAmount;
      } catch (error) {
        app.log.error(error, 'seller payout transfer failed');
        if (transferred === 0) return reply.code(502).send({ error: 'PAYOUT_TRANSFER_FAILED' });
        break;
      }
    }

    return reply.code(202).send({
      status: remaining === 0 ? 'TRANSFER_CREATED' : 'PARTIALLY_TRANSFERRED',
      requestedPaise: amountPaise,
      transferredPaise: transferred,
      remainingPaise: remaining,
      transfers
    });
  });
}
