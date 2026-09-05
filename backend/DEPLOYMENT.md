# AARVO Production Deployment

## Docker Compose

This repository includes `docker-compose.production.yml` for a production-shaped single-host deployment with PostgreSQL and the AARVO API.

1. Copy `.env.production.example` to `.env.production` locally.
2. Replace every placeholder with real values. Keep this file out of Git.
3. Start the stack with `docker compose --env-file .env.production -f docker-compose.production.yml up -d --build`.
4. The API container runs `src/migrate.js` before `src/server.js`, applying `schema.sql` and numbered migrations in order.
5. Verify the API health endpoint at `/health`.

The compose file intentionally requires database, JWT, CORS and Razorpay secrets. No production credential is stored in source control.

## External production requirements

- Put TLS/HTTPS in front of the API and restrict `CORS_ORIGIN` to the real application origins.
- Use a managed PostgreSQL service for a serious launch, or configure encrypted persistent storage and backups for a self-hosted database.
- Configure Razorpay live credentials and the webhook secret in the hosting platform's secret store.
- Configure the Android release API base URL to the HTTPS production API.
- Use a real Android release keystore through secure Gradle/CI secrets; the repository CI intentionally produces an unsigned AAB unless signing properties are supplied.
- Complete seller verification/KYC, payout onboarding, shipping-provider integration, privacy/terms/refund policy publication, monitoring and incident-response setup before accepting real customer traffic.

## Database migration safety

The migration runner executes the schema and numbered migrations inside one PostgreSQL transaction. If any migration fails, the transaction is rolled back and the API process does not start.
