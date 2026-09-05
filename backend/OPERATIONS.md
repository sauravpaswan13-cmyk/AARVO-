# AARVO Production Operations Runbook

## 1. Required services

- PostgreSQL with all migrations applied in order.
- HTTPS API deployment for `backend/src/server.js`.
- Razorpay account with server credentials and webhook secret configured as deployment secrets.
- A production Android build with the API base URL set to the HTTPS API.

## 2. Environment safety

Never commit real credentials, database passwords, JWT secrets, keystores, or webhook secrets. Keep them in the deployment platform's secret store.

Required runtime values are documented in `.env.example`. `JWT_SECRET` must be a high-entropy secret and must be rotated through a controlled deployment process.

## 3. Database rollout

Apply migrations serially:

1. `001_production_integrity.sql`
2. `002_marketplace_operations.sql`
3. `003_reviews_delivery.sql`
4. `004_marketplace_hardening.sql`
5. `005_operational_indexes.sql`

Back up the database before production schema changes. Verify indexes and constraints after each rollout.

## 4. Payment incident handling

A client callback is not proof of payment. The API verifies the stored gateway order, payment signature, gateway payment status and amount before confirming an order.

If a payment is reported by the gateway but the app shows an unconfirmed order:

1. Inspect the order's gateway identifiers.
2. Inspect `payment_events`.
3. Reconcile the gateway status with the order state.
4. Do not manually mark an order paid without an auditable server-side reconciliation procedure.

## 5. Order lifecycle

Orders are created server-side with stock locking and server-calculated totals. Cancellation, delivery tracking, reviews and disputes are separate lifecycle operations and should remain auditable.

Seller operations must remain scoped to seller-owned order lines and products.

## 6. Health and deployment verification

Before accepting traffic:

- Confirm `/health` responds successfully.
- Confirm database connectivity.
- Confirm Razorpay configuration is present without exposing secrets in logs.
- Run `npm test` and `npm run check`.
- Build the backend container.
- Build the Android debug APK and release AAB.

The CI release AAB is intentionally unsigned unless secure signing properties are supplied by the build environment.
