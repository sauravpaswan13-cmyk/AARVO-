-- Marketplace completion migration: product media and reusable buyer addresses.
-- Idempotent and safe to apply after migrations 001-008, including legacy tables.

CREATE TABLE IF NOT EXISTS product_images (
  id BIGSERIAL PRIMARY KEY,
  product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
  seller_id TEXT,
  image_url TEXT NOT NULL,
  alt_text TEXT NOT NULL DEFAULT '',
  sort_order INTEGER NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
  is_primary BOOLEAN NOT NULL DEFAULT false,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
ALTER TABLE product_images ADD COLUMN IF NOT EXISTS seller_id TEXT;
CREATE INDEX IF NOT EXISTS product_images_product_idx ON product_images(product_id, sort_order, id);
CREATE INDEX IF NOT EXISTS product_images_seller_idx ON product_images(seller_id, product_id);
CREATE UNIQUE INDEX IF NOT EXISTS product_images_primary_uidx ON product_images(product_id) WHERE is_primary;

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
CREATE INDEX IF NOT EXISTS buyer_addresses_buyer_idx ON buyer_addresses(buyer_id, updated_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS buyer_addresses_default_uidx ON buyer_addresses(buyer_id) WHERE is_default;

ALTER TABLE products ADD COLUMN IF NOT EXISTS image_url TEXT;
ALTER TABLE products ADD COLUMN IF NOT EXISTS seller_id TEXT;
CREATE INDEX IF NOT EXISTS products_seller_idx ON products(seller_id, updated_at DESC);
