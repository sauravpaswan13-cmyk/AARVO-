CREATE TABLE IF NOT EXISTS third_party_delivery_shipments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  provider TEXT NOT NULL,
  provider_order_id TEXT NOT NULL DEFAULT '',
  provider_shipment_id TEXT NOT NULL,
  awb TEXT NOT NULL DEFAULT '',
  courier_name TEXT NOT NULL DEFAULT '',
  status TEXT NOT NULL DEFAULT 'CREATED',
  tracking_url TEXT NOT NULL DEFAULT '',
  provider_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
ALTER TABLE third_party_delivery_shipments ADD COLUMN IF NOT EXISTS provider_order_id TEXT NOT NULL DEFAULT '';
ALTER TABLE third_party_delivery_shipments ADD COLUMN IF NOT EXISTS awb TEXT NOT NULL DEFAULT '';
ALTER TABLE third_party_delivery_shipments ADD COLUMN IF NOT EXISTS courier_name TEXT NOT NULL DEFAULT '';
CREATE UNIQUE INDEX IF NOT EXISTS third_party_delivery_provider_shipment_uidx
  ON third_party_delivery_shipments(provider,provider_shipment_id);
CREATE INDEX IF NOT EXISTS third_party_delivery_order_idx
  ON third_party_delivery_shipments(order_id,updated_at DESC);
CREATE INDEX IF NOT EXISTS third_party_delivery_awb_idx
  ON third_party_delivery_shipments(provider,awb);
