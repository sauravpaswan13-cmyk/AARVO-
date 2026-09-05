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

Razorpay recommends server-side signature verification and using webhooks for asynchronous payment state reconciliation. urlRazorpay payment verification documentationhttps://razorpay.com/docs/payments/payment-gateway/web-integration/standard/integration-steps/

## Required environment variables

```text
DATABASE_URL=postgresql://...
JWT_SECRET=<long-random-secret>
RAZORPAY_KEY_ID=rzp_live_...
RAZORPAY_KEY_SECRET=<server-only-secret>
RAZORPAY_WEBHOOK_SECRET=<webhook-secret>
CORS_ORIGIN=https://your-android-api-host.example
DELIVERY_FEE_PAISE=0
PLATFORM_FEE_BPS=0
PORT=8080
```

Never put `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET`, `DATABASE_URL`, or `JWT_SECRET` in the Android app or source control. Configure them in the hosting provider's secret/environment settings. The Android build only needs the public API base URL.

## API boundary

The Android application uses `MarketplaceApi`. The production implementation must call a hosted HTTPS API; it must never trust a client-supplied price, stock value, payment result, commission, or seller payout.

## Payment rule

Do not store card/UPI credentials in AARVO. Razorpay Checkout handles payment entry. AARVO stores only the identifiers and verified transaction state needed for orders, refunds, reconciliation and seller settlement.

## Launch requirement

Before AARVO is advertised for real purchases, configure:

1. Production API host and database.
2. Authentication and secure token validation.
3. Indian payment gateway / marketplace payout account and webhook secrets.
4. Seller KYC and bank-account onboarding.
5. Shipping/logistics provider and tracking webhooks.
6. Privacy policy, terms, refund/return policy and customer support process.
7. Razorpay webhook URL on HTTPS (port 443 or 80) with the same secret as `RAZORPAY_WEBHOOK_SECRET`.

Until those production credentials and services are configured, the repository is development software and must not be presented as accepting real customer money.
