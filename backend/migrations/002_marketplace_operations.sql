-- AARVO marketplace operations migration
-- Run after 001_production_integrity.sql. All statements are idempotent.

ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_expires_at TIMESTAMPTZ;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS tracking_json JSONB;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMPTZ;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS cancel_reason TEXT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS idempotency_key TEXT;

CREATE UNIQUE INDEX IF NOT EXISTS orders_buyer_idempotency_unique
  ON orders (buyer_id, idempotency_key)
  WHERE idempotency_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS orders_payment_expiry_idx
  ON orders (payment_expires_at)
  WHERE payment_status = 'CREATED' AND status = 'PENDING_PAYMENT';

CREATE INDEX IF NOT EXISTS orders_status_idx
  ON orders (status, updated_at DESC);

CREATE INDEX IF NOT EXISTS products_published_category_idx
  ON products (category, created_at DESC)
  WHERE is_published = true;

CREATE TABLE IF NOT EXISTS audit_events (
  id BIGSERIAL PRIMARY KEY,
  actor_id TEXT,
  actor_role TEXT,
  entity_type TEXT NOT NULL,
  entity_id TEXT NOT NULL,
  action TEXT NOT NULL,
  metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS audit_events_entity_idx
  ON audit_events (entity_type, entity_id, created_at DESC);

CREATE INDEX IF NOT EXISTS audit_events_actor_idx
  ON audit_events (actor_id, created_at DESC);
