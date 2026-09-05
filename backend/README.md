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

## API boundary

The Android application uses `MarketplaceApi`. The production implementation must call a hosted HTTPS API; it must never trust a client-supplied price, stock value, payment result, commission, or seller payout.

## Payment rule

Do not store card/UPI credentials in AARVO. A payment provider should tokenize/payment-authorize on its secure surface and notify the backend through signed webhooks. The backend creates the order/payment record and performs seller settlement according to verified transaction state.

## Launch requirement

Before AARVO is advertised for real purchases, configure:

1. Production API host and database.
2. Authentication provider and secure token validation.
3. Indian payment gateway / marketplace payout account and webhook secrets.
4. Seller KYC and bank-account onboarding.
5. Shipping/logistics provider and tracking webhooks.
6. Privacy policy, terms, refund/return policy and customer support process.

Until those production credentials and services are configured, the repository is development software and must not be presented as accepting real customer money.
