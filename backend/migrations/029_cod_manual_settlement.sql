-- COD and audited manual seller settlement foundation.
-- Additive only: existing Razorpay orders retain payment_method='RAZORPAY'.
ALTER TABLE orders
  ADD COLUMN IF NOT EXISTS payment_method TEXT NOT NULL DEFAULT 'RAZORPAY',
  ADD COLUMN IF NOT EXISTS cod_collected_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS cod_collected_by TEXT REFERENCES users(id),
  ADD COLUMN IF NOT EXISTS cod_collection_reference TEXT;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conname = 'orders_payment_method_check'
      AND conrelid = 'orders'::regclass
  ) THEN
    ALTER TABLE orders
      ADD CONSTRAINT orders_payment_method_check
      CHECK (payment_method IN ('RAZORPAY', 'COD'));
  END IF;
END
$$;

CREATE INDEX IF NOT EXISTS orders_payment_method_status_idx
  ON orders (payment_method, payment_status, status, created_at DESC);

-- Manual payouts are seller-level ledger entries, not tied to one order.
-- Existing installations may still have order_id NOT NULL from an earlier schema.
ALTER TABLE seller_ledger ALTER COLUMN order_id DROP NOT NULL;

-- Preserve all existing ledger types and add a distinct manual-payout record.
ALTER TABLE seller_ledger
  ADD COLUMN IF NOT EXISTS settlement_reference TEXT,
  ADD COLUMN IF NOT EXISTS settlement_memo TEXT,
  ADD COLUMN IF NOT EXISTS settled_by TEXT REFERENCES users(id);

DO $$
DECLARE
  type_attnum SMALLINT;
  constraint_row RECORD;
BEGIN
  SELECT attnum INTO type_attnum
  FROM pg_attribute
  WHERE attrelid = 'seller_ledger'::regclass
    AND attname = 'type'
    AND NOT attisdropped;

  IF type_attnum IS NULL THEN
    RAISE EXCEPTION 'seller_ledger.type column is missing';
  END IF;

  -- Drop only CHECK constraints that apply exclusively to the type column.
  -- This avoids removing unrelated ledger validation constraints.
  FOR constraint_row IN
    SELECT conname
    FROM pg_constraint
    WHERE conrelid = 'seller_ledger'::regclass
      AND contype = 'c'
      AND conkey = ARRAY[type_attnum]::SMALLINT[]
  LOOP
    EXECUTE format('ALTER TABLE seller_ledger DROP CONSTRAINT %I', constraint_row.conname);
  END LOOP;
END
$$;

ALTER TABLE seller_ledger
  ADD CONSTRAINT seller_ledger_type_check
  CHECK (type IN ('SALE', 'REFUND', 'PAYOUT', 'REVERSAL', 'MANUAL_PAYOUT'));

CREATE INDEX IF NOT EXISTS seller_ledger_manual_settlement_idx
  ON seller_ledger (seller_id, created_at DESC)
  WHERE type = 'MANUAL_PAYOUT';

-- Prevent reusing a manual payout reference across any seller.
CREATE UNIQUE INDEX IF NOT EXISTS seller_ledger_manual_settlement_reference_uidx
  ON seller_ledger (settlement_reference)
  WHERE type = 'MANUAL_PAYOUT' AND settlement_reference IS NOT NULL;
