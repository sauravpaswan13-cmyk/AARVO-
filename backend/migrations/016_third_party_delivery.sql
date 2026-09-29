CREATE TABLE IF NOT EXISTS third_party_delivery_shipments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  provider TEXT NOT NULL,
  provider_shipment_id TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'CREATED',
  tracking_url TEXT NOT NULL DEFAULT '',
  provider_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS third_party_delivery_provider_shipment_uidx
  ON third_party_delivery_shipments(provider,provider_shipment_id);
CREATE INDEX IF NOT EXISTS third_party_delivery_order_idx
  ON third_party_delivery_shipments(order_id,updated_at DESC);
