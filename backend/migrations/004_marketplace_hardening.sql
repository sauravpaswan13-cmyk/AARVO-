-- AARVO marketplace data-integrity hardening
-- Run after 003_reviews_delivery.sql. Statements are idempotent.

DO $$ BEGIN
  ALTER TABLE orders ADD CONSTRAINT orders_payment_status_chk
    CHECK (payment_status IN ('CREATED','AUTHORIZED','CAPTURED','FAILED','REFUNDED'));
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
  ALTER TABLE orders ADD CONSTRAINT orders_status_chk
    CHECK (status IN ('PENDING_PAYMENT','PAID','CONFIRMED','PACKED','SHIPPED','OUT_FOR_DELIVERY','DELIVERED','CANCELLED','REFUNDED'));
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
  ALTER TABLE orders ADD CONSTRAINT orders_refund_status_chk
    CHECK (refund_status IN ('NONE','PENDING','PROCESSED','FAILED'));
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
  ALTER TABLE orders ADD CONSTRAINT orders_money_nonnegative_chk
    CHECK (subtotal_paise >= 0 AND delivery_fee_paise >= 0 AND platform_fee_paise >= 0 AND total_paise >= 0);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
  ALTER TABLE products ADD CONSTRAINT products_rating_chk
    CHECK (rating >= 0 AND rating <= 5);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

CREATE INDEX IF NOT EXISTS orders_buyer_status_idx
  ON orders (buyer_id, status, updated_at DESC);

CREATE INDEX IF NOT EXISTS order_lines_product_idx
  ON order_lines (product_id, order_id);
