CREATE TABLE IF NOT EXISTS users (
  id TEXT PRIMARY KEY,
  email TEXT UNIQUE,
  display_name TEXT NOT NULL,
  password_hash TEXT,
  role TEXT NOT NULL CHECK (role IN ('BUYER','SELLER','ADMIN')),
  phone TEXT,
  phone_verified BOOLEAN NOT NULL DEFAULT false,
  phone_verified_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_verified BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_verified_at TIMESTAMPTZ;
ALTER TABLE users ALTER COLUMN email DROP NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS users_phone_uidx ON users (phone) WHERE phone IS NOT NULL AND phone <> '';

CREATE TABLE IF NOT EXISTS phone_verification_challenges (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  phone TEXT NOT NULL,
  otp_hash TEXT NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
  verified_at TIMESTAMPTZ,
  last_sent_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='phone_verification_challenges' AND column_name='code_hash')
     AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='phone_verification_challenges' AND column_name='otp_hash') THEN
    EXECUTE 'UPDATE phone_verification_challenges SET otp_hash = COALESCE(otp_hash, code_hash) WHERE otp_hash IS NULL';
    EXECUTE 'ALTER TABLE phone_verification_challenges DROP COLUMN code_hash';
  ELSIF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='phone_verification_challenges' AND column_name='code_hash') THEN
    EXECUTE 'ALTER TABLE phone_verification_challenges RENAME COLUMN code_hash TO otp_hash';
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='phone_verification_challenges' AND column_name='consumed_at')
     AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='phone_verification_challenges' AND column_name='verified_at') THEN
    EXECUTE 'UPDATE phone_verification_challenges SET verified_at = COALESCE(verified_at, consumed_at) WHERE verified_at IS NULL';
    EXECUTE 'ALTER TABLE phone_verification_challenges DROP COLUMN consumed_at';
  ELSIF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='phone_verification_challenges' AND column_name='consumed_at') THEN
    EXECUTE 'ALTER TABLE phone_verification_challenges RENAME COLUMN consumed_at TO verified_at';
  END IF;
END $$;
ALTER TABLE phone_verification_challenges ADD COLUMN IF NOT EXISTS otp_hash TEXT;
ALTER TABLE phone_verification_challenges ADD COLUMN IF NOT EXISTS verified_at TIMESTAMPTZ;
ALTER TABLE phone_verification_challenges ALTER COLUMN id SET DEFAULT gen_random_uuid();
UPDATE phone_verification_challenges SET otp_hash = '' WHERE otp_hash IS NULL;
ALTER TABLE phone_verification_challenges ALTER COLUMN otp_hash SET NOT NULL;
CREATE INDEX IF NOT EXISTS phone_verification_user_idx ON phone_verification_challenges (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS phone_verification_expiry_idx ON phone_verification_challenges (expires_at);

CREATE TABLE IF NOT EXISTS seller_profiles (
  seller_id TEXT PRIMARY KEY REFERENCES users(id), phone TEXT NOT NULL,
  verified BOOLEAN NOT NULL DEFAULT false, payout_account_ready BOOLEAN NOT NULL DEFAULT false,
  gateway_account_id TEXT, created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
ALTER TABLE seller_profiles ADD COLUMN IF NOT EXISTS verified_at TIMESTAMPTZ;
ALTER TABLE seller_profiles ADD COLUMN IF NOT EXISTS verified_by TEXT REFERENCES users(id);

CREATE TABLE IF NOT EXISTS products (
  id BIGSERIAL PRIMARY KEY, seller_id TEXT NOT NULL REFERENCES users(id), seller_name TEXT NOT NULL,
  name TEXT NOT NULL, category TEXT NOT NULL, price_paise INTEGER NOT NULL CHECK (price_paise > 0),
  rating NUMERIC(2,1) NOT NULL DEFAULT 0, description TEXT NOT NULL,
  stock_quantity INTEGER NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
  is_published BOOLEAN NOT NULL DEFAULT false, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
ALTER TABLE products ADD COLUMN IF NOT EXISTS image_url TEXT;
ALTER TABLE products ADD COLUMN IF NOT EXISTS seller_id TEXT;

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
ALTER TABLE product_images ADD COLUMN IF NOT EXISTS is_primary BOOLEAN NOT NULL DEFAULT false;
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

CREATE TABLE IF NOT EXISTS orders (
  id UUID PRIMARY KEY, buyer_id TEXT NOT NULL REFERENCES users(id), subtotal_paise INTEGER NOT NULL,
  delivery_fee_paise INTEGER NOT NULL DEFAULT 0, platform_fee_paise INTEGER NOT NULL DEFAULT 0,
  total_paise INTEGER NOT NULL, payment_status TEXT NOT NULL, status TEXT NOT NULL,
  address_json JSONB NOT NULL, gateway_order_id TEXT, gateway_payment_id TEXT,
  tracking_json JSONB,
  payment_expires_at TIMESTAMPTZ,
  idempotency_key TEXT,
  cancelled_at TIMESTAMPTZ,
  cancel_reason TEXT,
  refund_status TEXT,
  refunded_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS tracking_json JSONB;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_expires_at TIMESTAMPTZ;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS idempotency_key TEXT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMPTZ;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS cancel_reason TEXT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS refund_status TEXT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS refunded_at TIMESTAMPTZ;

CREATE UNIQUE INDEX IF NOT EXISTS orders_buyer_idempotency_uidx ON orders (buyer_id, idempotency_key) WHERE idempotency_key IS NOT NULL;

CREATE TABLE IF NOT EXISTS order_lines (
  order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE, product_id BIGINT NOT NULL REFERENCES products(id),
  seller_id TEXT NOT NULL REFERENCES users(id), quantity INTEGER NOT NULL CHECK (quantity > 0),
  unit_price_paise INTEGER NOT NULL CHECK (unit_price_paise > 0), seller_amount_paise INTEGER NOT NULL CHECK (seller_amount_paise >= 0),
  PRIMARY KEY (order_id, product_id)
);
CREATE TABLE IF NOT EXISTS payment_events (
  event_id TEXT PRIMARY KEY, order_id UUID REFERENCES orders(id), event_type TEXT NOT NULL,
  payload JSONB NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE IF NOT EXISTS seller_ledger (
  id BIGSERIAL PRIMARY KEY, seller_id TEXT NOT NULL REFERENCES users(id), order_id UUID NOT NULL REFERENCES orders(id),
  amount_paise INTEGER NOT NULL, type TEXT NOT NULL CHECK (type IN ('SALE','REFUND','PAYOUT','REVERSAL')),
  gateway_transfer_id TEXT, created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE IF NOT EXISTS delivery_events (
  id BIGSERIAL PRIMARY KEY,
  order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  status TEXT NOT NULL,
  tracking_code TEXT,
  carrier TEXT,
  note TEXT,
  actor_id TEXT REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS delivery_events_order_idx ON delivery_events(order_id, created_at ASC);
CREATE TABLE IF NOT EXISTS order_disputes (
  id UUID PRIMARY KEY,
  order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  buyer_id TEXT NOT NULL REFERENCES users(id),
  reason TEXT NOT NULL,
  details TEXT,
  status TEXT NOT NULL DEFAULT 'OPEN',
  resolution TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS order_disputes_open_uidx ON order_disputes(order_id) WHERE status IN ('OPEN','UNDER_REVIEW');

CREATE UNIQUE INDEX IF NOT EXISTS orders_gateway_order_uidx ON orders (gateway_order_id) WHERE gateway_order_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS orders_gateway_payment_uidx ON orders (gateway_payment_id) WHERE gateway_payment_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS seller_ledger_sale_uidx ON seller_ledger (seller_id, order_id, type) WHERE type = 'SALE';
CREATE INDEX IF NOT EXISTS products_search_idx ON products (category, is_published, created_at DESC);
CREATE INDEX IF NOT EXISTS products_seller_idx ON products (seller_id, updated_at DESC);
CREATE INDEX IF NOT EXISTS orders_buyer_idx ON orders (buyer_id, created_at DESC);
CREATE INDEX IF NOT EXISTS orders_status_idx ON orders (status, updated_at DESC);
CREATE INDEX IF NOT EXISTS ledger_seller_idx ON seller_ledger (seller_id, created_at DESC);
CREATE INDEX IF NOT EXISTS payment_events_order_idx ON payment_events (order_id, created_at DESC);
