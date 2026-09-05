-- AARVO operational query indexes
-- Run after 004_marketplace_hardening.sql. Statements are idempotent.

CREATE INDEX IF NOT EXISTS orders_created_at_idx
  ON orders (created_at DESC);

CREATE INDEX IF NOT EXISTS delivery_events_status_idx
  ON delivery_events (status, created_at DESC);

CREATE INDEX IF NOT EXISTS product_reviews_rating_idx
  ON product_reviews (product_id, rating, created_at DESC);

CREATE INDEX IF NOT EXISTS audit_events_action_idx
  ON audit_events (action, created_at DESC);
