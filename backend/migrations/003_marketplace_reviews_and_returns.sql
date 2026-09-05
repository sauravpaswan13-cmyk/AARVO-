-- AARVO reviews, delivery, and post-order operations
-- Run after 002_marketplace_operations.sql. All statements are idempotent.

CREATE TABLE IF NOT EXISTS product_reviews (
  id BIGSERIAL PRIMARY KEY,
  product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
  buyer_id TEXT NOT NULL REFERENCES users(id),
  order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
  review_text TEXT NOT NULL DEFAULT '',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (product_id, buyer_id, order_id)
);

CREATE INDEX IF NOT EXISTS product_reviews_product_idx
  ON product_reviews (product_id, created_at DESC);

CREATE TABLE IF NOT EXISTS order_disputes (
  id UUID PRIMARY KEY,
  order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  buyer_id TEXT NOT NULL REFERENCES users(id),
  reason TEXT NOT NULL,
  details TEXT NOT NULL DEFAULT '',
  status TEXT NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','UNDER_REVIEW','RESOLVED','REJECTED')),
  resolution TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS order_disputes_status_idx
  ON order_disputes (status, created_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS order_disputes_order_open_unique
  ON order_disputes (order_id)
  WHERE status IN ('OPEN','UNDER_REVIEW');
