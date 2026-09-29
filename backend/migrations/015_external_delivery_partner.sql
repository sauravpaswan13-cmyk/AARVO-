CREATE TABLE IF NOT EXISTS delivery_partner_dispatches (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
 provider TEXT NOT NULL,
 status TEXT NOT NULL DEFAULT 'ASSIGNED',
 provider_delivery_id TEXT,
 tracking_url TEXT,
 rider_name TEXT,
 rider_phone TEXT,
 last_response JSONB NOT NULL DEFAULT '{}'::jsonb,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS delivery_partner_dispatches_order_idx ON delivery_partner_dispatches(order_id,created_at DESC);
CREATE INDEX IF NOT EXISTS delivery_partner_dispatches_status_idx ON delivery_partner_dispatches(status,updated_at DESC);
CREATE TABLE IF NOT EXISTS delivery_partner_events (
 id BIGSERIAL PRIMARY KEY,
 order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
 status TEXT NOT NULL,
 payload JSONB NOT NULL DEFAULT '{}'::jsonb,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS delivery_partner_events_order_idx ON delivery_partner_events(order_id,created_at DESC);
