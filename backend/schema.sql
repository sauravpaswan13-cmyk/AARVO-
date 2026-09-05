CREATE TABLE IF NOT EXISTS users (
  id TEXT PRIMARY KEY,
  email TEXT NOT NULL UNIQUE,
  display_name TEXT NOT NULL,
  role TEXT NOT NULL CHECK (role IN ('BUYER','SELLER','ADMIN')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS seller_profiles (
  seller_id TEXT PRIMARY KEY REFERENCES users(id),
  phone TEXT NOT NULL,
  verified BOOLEAN NOT NULL DEFAULT false,
  payout_account_ready BOOLEAN NOT NULL DEFAULT false,
  gateway_account_id TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS products (
  id BIGSERIAL PRIMARY KEY,
  seller_id TEXT NOT NULL REFERENCES users(id),
  seller_name TEXT NOT NULL,
  name TEXT NOT NULL,
  category TEXT NOT NULL,
  price_paise INTEGER NOT NULL CHECK (price_paise > 0),
  rating NUMERIC(2,1) NOT NULL DEFAULT 0,
  description TEXT NOT NULL,
  stock_quantity INTEGER NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
  is_published BOOLEAN NOT NULL DEFAULT false,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS orders (
  id UUID PRIMARY KEY,
  buyer_id TEXT NOT NULL REFERENCES users(id),
  subtotal_paise INTEGER NOT NULL,
  delivery_fee_paise INTEGER NOT NULL DEFAULT 0,
  platform_fee_paise INTEGER NOT NULL DEFAULT 0,
  total_paise INTEGER NOT NULL,
  payment_status TEXT NOT NULL,
  status TEXT NOT NULL,
  address_json JSONB NOT NULL,
  gateway_order_id TEXT,
  gateway_payment_id TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS order_lines (
  order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  product_id BIGINT NOT NULL REFERENCES products(id),
  seller_id TEXT NOT NULL REFERENCES users(id),
  quantity INTEGER NOT NULL CHECK (quantity > 0),
  unit_price_paise INTEGER NOT NULL CHECK (unit_price_paise > 0),
  seller_amount_paise INTEGER NOT NULL CHECK (seller_amount_paise >= 0),
  PRIMARY KEY (order_id, product_id)
);

CREATE TABLE IF NOT EXISTS payment_events (
  event_id TEXT PRIMARY KEY,
  order_id UUID REFERENCES orders(id),
  event_type TEXT NOT NULL,
  payload JSONB NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS seller_ledger (
  id BIGSERIAL PRIMARY KEY,
  seller_id TEXT NOT NULL REFERENCES users(id),
  order_id UUID NOT NULL REFERENCES orders(id),
  amount_paise INTEGER NOT NULL,
  type TEXT NOT NULL CHECK (type IN ('SALE','REFUND','PAYOUT','REVERSAL')),
  gateway_transfer_id TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS products_search_idx ON products (category, is_published, created_at DESC);
CREATE INDEX IF NOT EXISTS orders_buyer_idx ON orders (buyer_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ledger_seller_idx ON seller_ledger (seller_id, created_at DESC);
