-- AARVO production integrity migration
-- Run after schema.sql. All statements are idempotent.

CREATE INDEX IF NOT EXISTS products_seller_idx ON products (seller_id, created_at DESC);
CREATE INDEX IF NOT EXISTS order_lines_seller_idx ON order_lines (seller_id, order_id);
CREATE INDEX IF NOT EXISTS orders_gateway_order_idx ON orders (gateway_order_id) WHERE gateway_order_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS payment_events_order_idx ON payment_events (order_id, created_at DESC);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conname = 'orders_total_nonnegative'
      AND conrelid = 'orders'::regclass
  ) THEN
    ALTER TABLE orders ADD CONSTRAINT orders_total_nonnegative CHECK (total_paise >= 0);
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conname = 'orders_subtotal_nonnegative'
      AND conrelid = 'orders'::regclass
  ) THEN
    ALTER TABLE orders ADD CONSTRAINT orders_subtotal_nonnegative CHECK (subtotal_paise >= 0);
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conname = 'orders_delivery_nonnegative'
      AND conrelid = 'orders'::regclass
  ) THEN
    ALTER TABLE orders ADD CONSTRAINT orders_delivery_nonnegative CHECK (delivery_fee_paise >= 0);
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conname = 'orders_platform_fee_nonnegative'
      AND conrelid = 'orders'::regclass
  ) THEN
    ALTER TABLE orders ADD CONSTRAINT orders_platform_fee_nonnegative CHECK (platform_fee_paise >= 0);
  END IF;
END $$;

-- Prevent the same gateway order from being attached to multiple AARVO orders.
CREATE UNIQUE INDEX IF NOT EXISTS orders_gateway_order_unique
  ON orders (gateway_order_id) WHERE gateway_order_id IS NOT NULL;

-- Payment IDs are globally unique once captured.
CREATE UNIQUE INDEX IF NOT EXISTS orders_gateway_payment_unique
  ON orders (gateway_payment_id) WHERE gateway_payment_id IS NOT NULL;
