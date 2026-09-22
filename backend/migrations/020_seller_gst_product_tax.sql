-- Seller GST status and product-level tax metadata.
ALTER TABLE seller_profiles
  ADD COLUMN IF NOT EXISTS gst_status TEXT NOT NULL DEFAULT 'NOT_REGISTERED'
    CHECK (gst_status IN ('REGISTERED','NOT_REGISTERED'));

ALTER TABLE products
  ADD COLUMN IF NOT EXISTS hsn_code TEXT,
  ADD COLUMN IF NOT EXISTS gst_rate NUMERIC(5,2) NOT NULL DEFAULT 0
    CHECK (gst_rate >= 0 AND gst_rate <= 100),
  ADD COLUMN IF NOT EXISTS gst_rate_source TEXT NOT NULL DEFAULT 'PENDING_REVIEW'
    CHECK (gst_rate_source IN ('SYSTEM','ADMIN','PENDING_REVIEW'));

CREATE INDEX IF NOT EXISTS products_hsn_idx ON products(hsn_code);
CREATE INDEX IF NOT EXISTS seller_profiles_gst_status_idx ON seller_profiles(gst_status);

-- Admin-maintained HSN/category mapping. Rates are intentionally data-driven so
-- tax changes can be updated without changing Android code.
CREATE TABLE IF NOT EXISTS gst_rate_catalog (
  id BIGSERIAL PRIMARY KEY,
  hsn_prefix TEXT,
  category TEXT,
  gst_rate NUMERIC(5,2) NOT NULL CHECK (gst_rate >= 0 AND gst_rate <= 100),
  active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK (NULLIF(TRIM(hsn_prefix),'') IS NOT NULL OR NULLIF(TRIM(category),'') IS NOT NULL)
);
CREATE INDEX IF NOT EXISTS gst_rate_catalog_lookup_idx
  ON gst_rate_catalog(active, hsn_prefix, category);
