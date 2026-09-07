-- Settlement safety primitives. Money movement remains disabled until a gateway transfer
-- is explicitly configured; this migration only makes seller settlement accounting
-- idempotent and auditable.

CREATE UNIQUE INDEX IF NOT EXISTS seller_ledger_payout_transfer_uidx
  ON seller_ledger (gateway_transfer_id)
  WHERE gateway_transfer_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS seller_ledger_order_type_idx
  ON seller_ledger (order_id, type);

-- Prevent a duplicate refund ledger entry for the same order.
CREATE UNIQUE INDEX IF NOT EXISTS seller_ledger_refund_uidx
  ON seller_ledger (seller_id, order_id, type)
  WHERE type = 'REFUND';
