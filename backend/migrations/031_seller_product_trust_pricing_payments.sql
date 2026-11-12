-- Seller-controlled product pricing, delivery, warranty and payment choices.
-- Additive migration: preserves all existing product and order data.
ALTER TABLE products
  ADD COLUMN IF NOT EXISTS mrp_price_paise BIGINT,
  ADD COLUMN IF NOT EXISTS delivery_fee_paise INTEGER NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS warranty_period_months INTEGER,
  ADD COLUMN IF NOT EXISTS guarantee_period_months INTEGER,
  ADD COLUMN IF NOT EXISTS online_payment_enabled BOOLEAN,
  ADD COLUMN IF NOT EXISTS cod_enabled BOOLEAN;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='products_mrp_price_positive_chk' AND conrelid='products'::regclass) THEN
    ALTER TABLE products ADD CONSTRAINT products_mrp_price_positive_chk
      CHECK (mrp_price_paise IS NULL OR mrp_price_paise >= price_paise);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='products_delivery_fee_nonnegative_chk' AND conrelid='products'::regclass) THEN
    ALTER TABLE products ADD CONSTRAINT products_delivery_fee_nonnegative_chk
      CHECK (delivery_fee_paise >= 0);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='products_warranty_months_chk' AND conrelid='products'::regclass) THEN
    ALTER TABLE products ADD CONSTRAINT products_warranty_months_chk
      CHECK ((warranty_period_months IS NULL OR warranty_period_months BETWEEN 1 AND 120)
         AND (guarantee_period_months IS NULL OR guarantee_period_months BETWEEN 1 AND 120));
  END IF;
END $$;

-- Do not infer a warranty, guarantee, discount or payment method for old products.
-- Sellers must explicitly set these values when creating or editing a listing.
