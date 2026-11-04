ALTER TABLE products
  ADD COLUMN IF NOT EXISTS package_length_cm NUMERIC(8,2),
  ADD COLUMN IF NOT EXISTS package_breadth_cm NUMERIC(8,2),
  ADD COLUMN IF NOT EXISTS package_height_cm NUMERIC(8,2),
  ADD COLUMN IF NOT EXISTS package_weight_kg NUMERIC(8,3);

ALTER TABLE products
  DROP CONSTRAINT IF EXISTS products_package_dimensions_positive_check;

ALTER TABLE products
  ADD CONSTRAINT products_package_dimensions_positive_check
  CHECK (
    (package_length_cm IS NULL AND package_breadth_cm IS NULL AND package_height_cm IS NULL AND package_weight_kg IS NULL)
    OR
    (package_length_cm > 0 AND package_breadth_cm > 0 AND package_height_cm > 0 AND package_weight_kg > 0)
  );
