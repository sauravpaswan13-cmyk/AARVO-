DO $$
BEGIN
 IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname='users_role_check') THEN ALTER TABLE users DROP CONSTRAINT users_role_check; END IF;
END $$;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('BUYER','SELLER','ADMIN','RIDER'));
CREATE TABLE IF NOT EXISTS rider_profiles (rider_id TEXT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE, phone TEXT NOT NULL, active BOOLEAN NOT NULL DEFAULT true, current_city TEXT NOT NULL DEFAULT '', created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now());
CREATE TABLE IF NOT EXISTS delivery_assignments (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE, rider_id TEXT NOT NULL REFERENCES users(id) ON DELETE RESTRICT, status TEXT NOT NULL DEFAULT 'ASSIGNED', pickup_note TEXT NOT NULL DEFAULT '', delivery_note TEXT NOT NULL DEFAULT '', assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(), accepted_at TIMESTAMPTZ, picked_up_at TIMESTAMPTZ, delivered_at TIMESTAMPTZ, updated_at TIMESTAMPTZ NOT NULL DEFAULT now());
CREATE UNIQUE INDEX IF NOT EXISTS delivery_assignments_order_uidx ON delivery_assignments(order_id) WHERE status IN ('ASSIGNED','ACCEPTED','PICKED_UP','OUT_FOR_DELIVERY');
CREATE INDEX IF NOT EXISTS delivery_assignments_rider_idx ON delivery_assignments(rider_id, updated_at DESC);
CREATE TABLE IF NOT EXISTS rider_notifications (id BIGSERIAL PRIMARY KEY, rider_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE, order_id UUID REFERENCES orders(id) ON DELETE CASCADE, title TEXT NOT NULL, body TEXT NOT NULL, read_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT now());
CREATE INDEX IF NOT EXISTS rider_notifications_idx ON rider_notifications(rider_id, created_at DESC);