import fs from 'node:fs/promises';
import path from 'path';
import { fileURLToPath, pathToFileURL } from 'url';

const here = path.dirname(fileURLToPath(import.meta.url));
const serverPath = path.join(here, 'server.js');
// The compatibility wrapper lives beside server.js so Node resolves package imports
// (fastify, pg, etc.) and relative imports from the correct /app/src module scope.
const runtimePath = path.join(here, '.aarvo-runtime-server.mjs');
let source = await fs.readFile(serverPath, 'utf8');

if (!source.includes('registerMarketplaceCompletion')) {
  source = source.replace(
    "import { createHmac, randomBytes, randomUUID, scryptSync, timingSafeEqual } from 'node:crypto';",
    "import { createHmac, randomBytes, randomUUID, scryptSync, timingSafeEqual } from 'node:crypto';\nimport { registerMarketplaceCompletion } from './marketplace-completion.js';\nimport { registerCartCompletion } from './cart-completion.js';\nimport { registerSettlementCompletion } from './settlement-completion.js';"
  );
  source = source.replace(
    "const port=Number(process.env.PORT||8080);",
    "await registerMarketplaceCompletion({ app, pool, requireAuth, requireRole, audit });\nawait registerCartCompletion({ app, pool, requireRole, audit });\nawait registerSettlementCompletion({ app, pool, requireRole, audit, razorpay });\nconst port=Number(process.env.PORT||8080);"
  );
}

// Harden the legacy refund handler without mutating the canonical server source at build time.
const refundStart = source.indexOf("app.post('/v1/admin/orders/:id/refund'");
const refundEnd = refundStart >= 0 ? source.indexOf("\napp.get('/v1/admin/sellers'", refundStart) : -1;
if (refundStart >= 0 && refundEnd > refundStart) {
  const safeRefundRoute = `app.post('/v1/admin/orders/:id/refund', { preHandler: requireRole('ADMIN') }, async (request, reply) => {
  if (!pool || !razorpay) return reply.code(503).send({ error: 'PAYMENTS_NOT_CONFIGURED' });
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const result = await client.query('SELECT id,total_paise,gateway_payment_id,payment_status,status,refund_status FROM orders WHERE id=$1 FOR UPDATE', [request.params.id]);
    if (!result.rowCount) throw httpError(404, 'ORDER_NOT_FOUND');
    const order = result.rows[0];
    if (order.payment_status !== 'CAPTURED' || !order.gateway_payment_id) throw httpError(409, 'ORDER_NOT_REFUNDABLE');
    if (order.refund_status) throw httpError(409, 'REFUND_ALREADY_PROCESSED');
    const amount = request.body?.amountPaise === undefined ? Number(order.total_paise) : Number(request.body.amountPaise);
    if (!Number.isSafeInteger(amount) || amount <= 0 || amount > Number(order.total_paise)) throw httpError(400, 'INVALID_REFUND_AMOUNT');
    const refund = await razorpay.payments.refund(order.gateway_payment_id, { amount });
    const lines = await client.query('SELECT seller_id,order_id,seller_amount_paise FROM order_lines WHERE order_id=$1 ORDER BY seller_id,product_id', [request.params.id]);
    const sellerTotal = lines.rows.reduce((sum, line) => sum + Number(line.seller_amount_paise), 0);
    let remainingSellerRefund = Math.min(amount, sellerTotal);
    for (let i = 0; i < lines.rows.length; i++) {
      const line = lines.rows[i];
      const sellerAmount = Number(line.seller_amount_paise);
      const allocation = i === lines.rows.length - 1 ? remainingSellerRefund : Math.min(sellerAmount, Math.floor(amount * sellerAmount / Number(order.total_paise)));
      if (allocation > 0) {
        await client.query('INSERT INTO seller_ledger(seller_id,order_id,amount_paise,type,gateway_transfer_id) VALUES($1,$2,$3,\'REFUND\',$4)', [line.seller_id, line.order_id, allocation, refund.id]);
        remainingSellerRefund -= allocation;
      }
    }
    const fullRefund = amount === Number(order.total_paise);
    await client.query('UPDATE orders SET refund_status=$1,payment_status=CASE WHEN $2 THEN \'REFUNDED\' ELSE payment_status END,refunded_at=CASE WHEN $2 THEN now() ELSE refunded_at END,status=CASE WHEN $2 AND status<>\'DELIVERED\' THEN \'REFUNDED\' ELSE status END,updated_at=now() WHERE id=$3', [fullRefund ? 'PROCESSED' : 'PARTIAL', fullRefund, request.params.id]);
    await audit(client, request.user, 'ORDER', request.params.id, 'REFUND_PROCESSED', { refundId: refund.id, amountPaise: amount, fullRefund });
    await client.query('COMMIT');
    return { orderId: request.params.id, refundId: refund.id, amountPaise: amount, refundStatus: fullRefund ? 'PROCESSED' : 'PARTIAL' };
  } catch (error) {
    await client.query('ROLLBACK');
    throw error;
  } finally { client.release(); }
});`;
  source = source.slice(0, refundStart) + safeRefundRoute + source.slice(refundEnd);
}

await fs.writeFile(runtimePath, source, 'utf8');
await import(`${pathToFileURL(runtimePath).href}?v=${Date.now()}`);
