-- Keep the runtime auth implementation compatible with the phone verification
-- table created by migrations 006/007. Fresh databases use the legacy
-- code_hash/consumed_at columns; older installations may already have the
-- newer otp_hash/verified_at columns. This migration must be safe in both cases.
ALTER TABLE phone_verification_challenges
  ADD COLUMN IF NOT EXISTS otp_hash TEXT,
  ADD COLUMN IF NOT EXISTS verified_at TIMESTAMPTZ;

-- Only touch legacy columns when they actually exist. Referencing a missing
-- column in a static ALTER/UPDATE would abort a fresh-database migration.
DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = current_schema()
      AND table_name = 'phone_verification_challenges'
      AND column_name = 'code_hash'
  ) THEN
    EXECUTE 'ALTER TABLE phone_verification_challenges ALTER COLUMN code_hash DROP NOT NULL';
    EXECUTE 'UPDATE phone_verification_challenges
             SET otp_hash = code_hash
             WHERE otp_hash IS NULL AND code_hash IS NOT NULL';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = current_schema()
      AND table_name = 'phone_verification_challenges'
      AND column_name = 'consumed_at'
  ) THEN
    EXECUTE 'UPDATE phone_verification_challenges
             SET verified_at = consumed_at
             WHERE verified_at IS NULL AND consumed_at IS NOT NULL';
  END IF;
END $$;

-- Runtime inserts omit the challenge id, so give it a database-generated UUID.
CREATE EXTENSION IF NOT EXISTS pgcrypto;
ALTER TABLE phone_verification_challenges
  ALTER COLUMN id SET DEFAULT gen_random_uuid();

CREATE INDEX IF NOT EXISTS phone_verification_phone_idx
  ON phone_verification_challenges(phone, created_at DESC);
