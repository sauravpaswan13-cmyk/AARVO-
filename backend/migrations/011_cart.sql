-- Persistent buyer cart. Idempotent so startup migrations remain safe on legacy databases.
CREATE TABLE IF NOT EXISTS buyer_carts (
  buyer_id TEXT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INTEGER NOT NULL DEFAULT 1,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (buyer_id, product_id),
  FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
  CHECK (quantity > 0 AND quantity <= 100)
);
CREATE INDEX IF NOT EXISTS buyer_carts_buyer_idx ON buyer_carts(buyer_id, updated_at DESC);
