-- Phone verification foundation for buyer/seller authentication.
-- Delivery provider configuration remains external; OTPs are never stored in plaintext.

ALTER TABLE users ADD COLUMN IF NOT EXISTS phone TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_verified BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_verified_at TIMESTAMPTZ;

UPDATE users u
SET phone = s.phone,
    phone_verified = COALESCE(s.verified, false),
    phone_verified_at = CASE WHEN COALESCE(s.verified, false) THEN COALESCE(u.phone_verified_at, now()) ELSE NULL END
FROM seller_profiles s
WHERE s.seller_id = u.id AND (u.phone IS NULL OR u.phone = '');

CREATE UNIQUE INDEX IF NOT EXISTS users_phone_uidx ON users (phone) WHERE phone IS NOT NULL AND phone <> '';

CREATE TABLE IF NOT EXISTS phone_verification_challenges (
  id UUID PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  phone TEXT NOT NULL,
  code_hash TEXT NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
  consumed_at TIMESTAMPTZ,
  last_sent_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS phone_verification_user_idx
  ON phone_verification_challenges (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS phone_verification_expiry_idx
  ON phone_verification_challenges (expires_at);
