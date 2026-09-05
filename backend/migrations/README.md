# AARVO database migrations

Apply these files in numeric order against the same PostgreSQL database:

1. `001_production_integrity.sql`
2. `002_marketplace_operations.sql`
3. `003_reviews_delivery.sql`
4. `004_marketplace_hardening.sql`

All migrations are intended to be idempotent. Do not rename or duplicate numeric migration prefixes; CI contains a regression check for the sequence.

The application must not be pointed at a production database until the migrations have completed successfully and the expected constraints/indexes have been verified.
