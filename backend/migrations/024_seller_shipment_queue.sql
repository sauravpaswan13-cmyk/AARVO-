ALTER TABLE third_party_delivery_shipments
  ADD COLUMN IF NOT EXISTS seller_id UUID;

CREATE UNIQUE INDEX IF NOT EXISTS third_party_delivery_order_seller_uidx
  ON third_party_delivery_shipments(order_id,seller_id)
  WHERE seller_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS third_party_delivery_jobs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  seller_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  status TEXT NOT NULL DEFAULT 'PENDING'
    CHECK (status IN ('PENDING','PROCESSING','RETRY','DONE','FAILED')),
  attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
  next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  locked_at TIMESTAMPTZ,
  last_error TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(order_id,seller_id)
);
CREATE INDEX IF NOT EXISTS third_party_delivery_jobs_ready_idx
  ON third_party_delivery_jobs(status,next_attempt_at,created_at);
