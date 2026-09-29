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
- `THIRD_PARTY_DELIVERY_ENABLED` and the `THIRD_PARTY_DELIVERY_*` provider settings when a logistics partner is activated.

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
8. Select and contract a production shipping/logistics provider, then configure its API credentials and AARVO delivery webhook.
9. Shipping/logistics provider and tracking webhooks are operational.
10. Refund, return, cancellation and dispute operations are documented and tested.
11. Privacy policy, terms, refund/return policy and customer support are published.
12. Monitoring, logs, backups and alerting are enabled.
13. A real-money test is performed only after the payment and delivery providers' production approval and go-live checklists are complete.

Razorpay distinguishes Test Mode from Live Mode; real customer payments require the live setup and account verification. See the [Razorpay Quickstart](https://razorpay.com/docs/payments/quickstart/?preferred-country=IN).

Until those production services and credentials are configured, the repository is development software and must not be presented as accepting real customer money.

## Third-party delivery architecture

AARVO does not require its own Rider App. The backend now has a provider-neutral delivery adapter and signed webhook endpoint. When `THIRD_PARTY_DELIVERY_ENABLED=true`, an admin can create a shipment through `POST /v1/admin/orders/:id/third-party-delivery`; the external provider assigns and operates the rider, while provider webhooks update AARVO order tracking. Provider-specific credentials stay server-side in Render secrets.
