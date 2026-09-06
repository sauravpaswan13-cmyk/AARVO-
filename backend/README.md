# AARVO Live Marketplace Backend

AARVO is being built as a two-sided marketplace, not a demo storefront.

## Production responsibilities

- Buyer and seller accounts with role-based access control
- Seller onboarding and KYC/payout readiness before publishing products
- Server-authoritative catalog, pricing and inventory
- Order state machine: pending payment -> paid -> processing -> shipped -> delivered, plus cancellation/refund/dispute paths
- Payment-provider integration with webhook verification and idempotency
- Seller commission and payout ledger
- Delivery address and order tracking
- Reviews only for completed purchases
- Admin moderation and dispute handling
- Audit logging and rate limiting

## Secure one-time admin bootstrap

Public registration intentionally does not allow the `ADMIN` role. The first admin is created only by the explicit bootstrap command in `src/bootstrap-admin.js`.

Set these variables directly in the hosting provider (never commit them and never send the password in chat):

- `ADMIN_EMAIL` — the admin email address
- `ADMIN_PASSWORD` — at least 12 characters; use a unique strong password
- `ADMIN_DISPLAY_NAME` — optional

Run the bootstrap once:

```bash
npm run bootstrap:admin
```

If the hosting provider does not provide Shell/one-off jobs on the current plan, the same one-time command can be temporarily used as the service start command after migrations:

```bash
node src/migrate.js && node src/bootstrap-admin.js && node src/server.js
```

After the deployment successfully creates the admin, immediately restore the normal start command:

```bash
node src/migrate.js && node src/server.js
```

Then remove `ADMIN_EMAIL`, `ADMIN_PASSWORD`, and `ADMIN_DISPLAY_NAME` from the hosting environment. The bootstrap script is idempotent: if an admin already exists, it makes no changes.

## Current checkout flow

1. Android authenticates the buyer and receives a JWT.
2. Android sends product IDs, quantities and delivery address to `POST /v1/orders`.
3. The backend locks the selected product rows, calculates the authoritative total, creates the Razorpay order and reserves stock.
4. Android opens Razorpay Checkout with the server-created gateway order ID.
5. Android returns the payment identifiers to `POST /v1/payments/verify`.
6. The backend verifies the signature using its stored gateway order ID, fetches the payment from Razorpay, checks amount/order/status, and only then marks the order paid and creates the seller sale ledger entries.
7. Razorpay webhooks are accepted at `POST /v1/webhooks/razorpay`, verified against the raw request body, and deduplicated using `x-razorpay-event-id`.
8. If the customer cancels before payment, `POST /v1/orders/:id/cancel` releases the reserved stock.

Razorpay recommends server-side signature verification and webhooks for asynchronous payment state reconciliation. See the [Razorpay payment verification documentation](https://razorpay.com/docs/payments/payment-gateway/web-integration/standard/integration-steps/).

## Production environment

Required variables are documented in `.env.example`. Never commit real values. In production, configure secrets directly in the hosting provider:

- `DATABASE_URL`
- `JWT_SECRET`
- `RAZORPAY_KEY_ID`
- `RAZORPAY_KEY_SECRET`
- `RAZORPAY_WEBHOOK_SECRET`
- `CORS_ORIGIN`
- `DELIVERY_FEE_PAISE`
- `PLATFORM_FEE_BPS`

The Android app only needs the public HTTPS API base URL. Razorpay's [Android integration documentation](https://razorpay.com/docs/payments/magic-checkout/android-integration/) also recommends keeping sensitive API secrets out of the Android app.

## Database setup

1. Create the PostgreSQL database.
2. Apply `schema.sql`.
3. Apply `migrations/001_production_integrity.sql`.
4. Apply `migrations/002_marketplace_operations.sql`.
5. Run migrations before accepting traffic.
6. Keep regular encrypted database backups and test restoration before launch.

The integrity migrations add seller/order/payment lookup indexes, uniqueness protections, payment reservation expiry support, delivery tracking fields, idempotency support and audit-event storage.

## Container deployment

A production Docker image is provided in `Dockerfile`.

```bash
docker build -t aarvo-api ./backend
docker run --rm -p 8080:8080 --env-file ./backend/.env aarvo-api
```

Do not put `.env` into Git. `.dockerignore` excludes local secrets and development artifacts from the image build context.

## API boundary

The Android application uses `MarketplaceApi`. The production implementation must call a hosted HTTPS API; it must never trust a client-supplied price, stock value, payment result, commission, or seller payout.

## Payment rule

Do not store card/UPI credentials in AARVO. Razorpay Checkout handles payment entry. AARVO stores only the identifiers and verified transaction state needed for orders, refunds, reconciliation and seller settlement.

## Launch checklist

Before AARVO is advertised for real purchases:

1. Production API is hosted behind HTTPS.
2. PostgreSQL production database and backups are configured.
3. `schema.sql` and both integrity/operations migrations are applied.
4. JWT secret is random, long and server-only.
5. Razorpay Live keys are configured server-side after account/KYC approval.
6. Razorpay webhook is configured on HTTPS with the same webhook secret.
7. Seller KYC and bank/payout onboarding are operational.
8. Shipping/logistics provider and tracking webhooks are operational.
9. Refund, return, cancellation and dispute operations are documented and tested.
10. Privacy policy, terms, refund/return policy and customer support are published.
11. Monitoring, logs, backups and alerting are enabled.
12. A real-money test is performed only after the provider's production approval and go-live checklist are complete.

Razorpay distinguishes Test Mode from Live Mode; real customer payments require the live setup and account verification. See the [Razorpay Quickstart](https://razorpay.com/docs/payments/quickstart/?preferred-country=IN).

Until those production services and credentials are configured, the repository is development software and must not be presented as accepting real customer money.
