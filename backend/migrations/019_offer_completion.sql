CREATE TABLE IF NOT EXISTS marketplace_offers (
  id BIGSERIAL PRIMARY KEY,
  code TEXT UNIQUE,
  title TEXT NOT NULL,
  description TEXT NOT NULL DEFAULT '',
  discount_type TEXT NOT NULL CHECK (discount_type IN ('FLAT_PAISE','PERCENT')),
  discount_value INTEGER NOT NULL CHECK (discount_value > 0),
  min_order_paise BIGINT NOT NULL DEFAULT 0 CHECK (min_order_paise >= 0),
  max_discount_paise BIGINT,
  active BOOLEAN NOT NULL DEFAULT true,
  starts_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  ends_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS marketplace_offers_active_idx ON marketplace_offers(active, starts_at, ends_at);
