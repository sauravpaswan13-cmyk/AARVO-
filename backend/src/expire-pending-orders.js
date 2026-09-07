import pg from 'pg';
import { randomUUID } from 'node:crypto';

const { Pool } = pg;
const pool = process.env.DATABASE_URL
  ? new Pool({
      connectionString: process.env.DATABASE_URL,
      ssl: process.env.DATABASE_SSL === 'true' ? { rejectUnauthorized: false } : undefined,
    })
  : null;

if (!pool) {
  console.error('DATABASE_NOT_CONFIGURED');
  process.exit(1);
}

const client = await pool.connect();
let expired = 0;
try {
  await client.query('BEGIN');

  const orders = await client.query(`
    SELECT id
    FROM orders
    WHERE status = 'PENDING_PAYMENT'
      AND payment_status = 'PENDING'
      AND payment_expires_at IS NOT NULL
      AND payment_expires_at <= now()
    FOR UPDATE SKIP LOCKED
  `);

  for (const order of orders.rows) {
    const lines = await client.query(
      'SELECT product_id, quantity FROM order_lines WHERE order_id = $1',
      [order.id],
    );

    for (const line of lines.rows) {
      await client.query(
        'UPDATE products SET stock_quantity = stock_quantity + $1, updated_at = now() WHERE id = $2',
        [line.quantity, line.product_id],
      );
    }

    await client.query(
      `UPDATE orders
       SET status = 'CANCELLED',
           payment_status = 'EXPIRED',
           cancelled_at = now(),
           cancel_reason = 'PAYMENT_WINDOW_EXPIRED',
           updated_at = now()
       WHERE id = $1
         AND status = 'PENDING_PAYMENT'
         AND payment_status = 'PENDING'`,
      [order.id],
    );

    await client.query(
      `INSERT INTO payment_events(event_id, order_id, event_type, payload)
       VALUES ($1, $2, 'PAYMENT_WINDOW_EXPIRED', $3::jsonb)
       ON CONFLICT (event_id) DO NOTHING`,
      [`expiry-${order.id}`, order.id, JSON.stringify({ reason: 'PAYMENT_WINDOW_EXPIRED' })],
    );

    expired += 1;
  }

  await client.query('COMMIT');
  console.log(JSON.stringify({ ok: true, expiredOrders: expired }));
} catch (error) {
  await client.query('ROLLBACK');
  console.error(error);
  process.exitCode = 1;
} finally {
  client.release();
  await pool.end();
}
