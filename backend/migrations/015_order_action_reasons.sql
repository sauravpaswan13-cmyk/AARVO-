-- Require and preserve structured buyer reasons for cancellation and returns.
ALTER TABLE orders ADD COLUMN IF NOT EXISTS cancel_reason TEXT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS return_reason TEXT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS return_details TEXT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS return_requested_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS orders_cancel_reason_idx ON orders (cancel_reason) WHERE cancel_reason IS NOT NULL;
CREATE INDEX IF NOT EXISTS orders_return_reason_idx ON orders (return_reason) WHERE return_reason IS NOT NULL;
