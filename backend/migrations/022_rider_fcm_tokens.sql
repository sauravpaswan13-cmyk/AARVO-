-- FCM device tokens for real-time rider push notifications.
CREATE TABLE IF NOT EXISTS rider_fcm_tokens (
  id BIGSERIAL PRIMARY KEY,
  rider_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(rider_id, token)
);
CREATE INDEX IF NOT EXISTS rider_fcm_tokens_rider_idx ON rider_fcm_tokens(rider_id, updated_at DESC);

ALTER TABLE rider_notifications ADD COLUMN IF NOT EXISTS pushed_at TIMESTAMPTZ;
CREATE INDEX IF NOT EXISTS rider_notifications_push_idx ON rider_notifications(created_at DESC) WHERE pushed_at IS NULL;
