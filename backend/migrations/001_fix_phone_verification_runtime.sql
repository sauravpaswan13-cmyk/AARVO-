-- Keep the phone-verification table compatible with the runtime auth handlers.
-- The base schema uses code_hash/consumed_at; the deployed runtime uses otp_hash/verified_at.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE phone_verification_challenges
  ADD COLUMN IF NOT EXISTS otp_hash TEXT,
  ADD COLUMN IF NOT EXISTS verified_at TIMESTAMPTZ;

ALTER TABLE phone_verification_challenges
  ALTER COLUMN id SET DEFAULT gen_random_uuid();

-- Backfill compatibility data when the legacy column is populated.
UPDATE phone_verification_challenges
SET otp_hash = code_hash
WHERE otp_hash IS NULL AND code_hash IS NOT NULL;

UPDATE phone_verification_challenges
SET verified_at = consumed_at
WHERE verified_at IS NULL AND consumed_at IS NOT NULL;

CREATE INDEX IF NOT EXISTS phone_verification_phone_idx
  ON phone_verification_challenges (phone, created_at DESC);
