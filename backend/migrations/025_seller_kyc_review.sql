-- Track explicit manual review of seller KYC documents without storing document images or secrets.
ALTER TABLE seller_profiles ADD COLUMN IF NOT EXISTS kyc_review_status TEXT NOT NULL DEFAULT 'PENDING';
ALTER TABLE seller_profiles ADD COLUMN IF NOT EXISTS kyc_review_notes TEXT;
ALTER TABLE seller_profiles ADD COLUMN IF NOT EXISTS kyc_reviewed_at TIMESTAMPTZ;
ALTER TABLE seller_profiles ADD COLUMN IF NOT EXISTS kyc_reviewed_by TEXT;
ALTER TABLE seller_profiles ADD CONSTRAINT seller_profiles_kyc_review_status_check CHECK (kyc_review_status IN ('PENDING','IN_REVIEW','VERIFIED','REJECTED'));
