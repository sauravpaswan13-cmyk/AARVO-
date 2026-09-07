-- Compatibility migration for databases created before marketplace seller/media columns existed.
-- Keep legacy rows usable while allowing the current marketplace schema/indexes to deploy.

ALTER TABLE products ADD COLUMN IF NOT EXISTS seller_id TEXT;
ALTER TABLE products ADD COLUMN IF NOT EXISTS image_url TEXT;

ALTER TABLE product_images ADD COLUMN IF NOT EXISTS seller_id TEXT;

-- Recover seller ownership where an unambiguous seller display name match exists.
UPDATE products p
SET seller_id = u.id
FROM users u
WHERE p.seller_id IS NULL
  AND p.seller_name IS NOT NULL
  AND p.seller_name <> ''
  AND lower(trim(u.display_name)) = lower(trim(p.seller_name));

UPDATE product_images pi
SET seller_id = p.seller_id
FROM products p
WHERE pi.seller_id IS NULL
  AND pi.product_id = p.id
  AND p.seller_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS products_seller_idx ON products(seller_id, updated_at DESC);
CREATE INDEX IF NOT EXISTS product_images_seller_idx ON product_images(seller_id, product_id);
