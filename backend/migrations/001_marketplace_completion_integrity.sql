-- Internal marketplace completion migration.
-- Keeps the existing API contract compatible while repairing schema/API drift.

-- OTP compatibility: server uses otp_hash + verified_at; legacy schema used code_hash + consumed_at.
ALTER TABLE phone_verification_challenges ADD COLUMN IF NOT EXISTS otp_hash TEXT;
ALTER TABLE phone_verification_challenges ADD COLUMN IF NOT EXISTS verified_at TIMESTAMPTZ;
ALTER TABLE phone_verification_challenges ALTER COLUMN code_hash DROP NOT NULL;
ALTER TABLE phone_verification_challenges ALTER COLUMN id SET DEFAULT md5(random()::text || clock_timestamp()::text)::uuid;
UPDATE phone_verification_challenges
SET otp_hash = COALESCE(otp_hash, code_hash),
    verified_at = COALESCE(verified_at, consumed_at)
WHERE otp_hash IS NULL OR verified_at IS NULL;

-- Product images are stored as ordered URLs/references. Binary storage remains an external storage concern.
CREATE TABLE IF NOT EXISTS product_images (
  id BIGSERIAL PRIMARY KEY,
  product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
  image_url TEXT NOT NULL,
  sort_order INTEGER NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
  alt_text TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS product_images_product_idx ON product_images(product_id, sort_order, id);

-- Buyer reviews used by product detail/review endpoints.
CREATE TABLE IF NOT EXISTS product_reviews (
  id UUID PRIMARY KEY,
  order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
  buyer_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
  review_text TEXT NOT NULL DEFAULT '',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(order_id, product_id, buyer_id)
);
CREATE INDEX IF NOT EXISTS product_reviews_product_idx ON product_reviews(product_id, created_at DESC);

-- Audit trail used by every sensitive marketplace mutation.
CREATE TABLE IF NOT EXISTS audit_events (
  id BIGSERIAL PRIMARY KEY,
  actor_id TEXT REFERENCES users(id) ON DELETE SET NULL,
  actor_role TEXT,
  entity_type TEXT NOT NULL,
  entity_id TEXT NOT NULL,
  action TEXT NOT NULL,
  metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS audit_events_entity_idx ON audit_events(entity_type, entity_id, created_at DESC);
CREATE INDEX IF NOT EXISTS audit_events_actor_idx ON audit_events(actor_id, created_at DESC);

-- Address book foundation: orders remain immutable snapshots through address_json.
CREATE TABLE IF NOT EXISTS buyer_addresses (
  id UUID PRIMARY KEY,
  buyer_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  label TEXT NOT NULL DEFAULT 'Home',
  full_name TEXT NOT NULL,
  phone TEXT NOT NULL,
  line1 TEXT NOT NULL,
  line2 TEXT NOT NULL DEFAULT '',
  city TEXT NOT NULL,
  state TEXT NOT NULL,
  postal_code TEXT NOT NULL,
  country TEXT NOT NULL DEFAULT 'IN',
  is_default BOOLEAN NOT NULL DEFAULT false,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS buyer_addresses_buyer_idx ON buyer_addresses(buyer_id, is_default DESC, updated_at DESC);
