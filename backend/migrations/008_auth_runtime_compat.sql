-- Keep the runtime auth implementation compatible with the phone verification
-- table created by migrations 006/007. Existing installations may already have
-- the older code_hash/consumed_at column names.
ALTER TABLE phone_verification_challenges
  ADD COLUMN IF NOT EXISTS otp_hash TEXT,
  ADD COLUMN IF NOT EXISTS verified_at TIMESTAMPTZ;

-- Runtime inserts omit the challenge id, so give it a database-generated UUID.
CREATE EXTENSION IF NOT EXISTS pgcrypto;
ALTER TABLE phone_verification_challenges
  ALTER COLUMN id SET DEFAULT gen_random_uuid();

-- Preserve already-issued challenges when possible by mirroring the legacy hash.
UPDATE phone_verification_challenges
SET otp_hash = code_hash
WHERE otp_hash IS NULL AND code_hash IS NOT NULL;

-- Preserve consumed challenges as verified challenges for the current runtime.
UPDATE phone_verification_challenges
SET verified_at = consumed_at
WHERE verified_at IS NULL AND consumed_at IS NOT NULL;

CREATE INDEX IF NOT EXISTS phone_verification_phone_idx
  ON phone_verification_challenges(phone, created_at DESC);
