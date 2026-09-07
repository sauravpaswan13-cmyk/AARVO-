# AARVO — Real Shopping Marketplace

AARVO is being developed as a **real two-sided marketplace**: customers buy genuine products and independent sellers list, sell, fulfill orders and receive payouts. It is not intended to be a demo storefront.

## Product vision

**Buyer:** discover → compare → product details → cart → secure checkout → payment → delivery tracking → verified review.

**Seller:** onboarding/KYC → store profile → add products → pricing/inventory → receive orders → pack/ship → track earnings → payout.

**Admin:** seller approval → catalog moderation → commission rules → orders/payments → refunds/disputes → fraud/risk → analytics → audit logs.

## Core product promise

- Guest users can browse the catalog without verification.
- Login/Signup with OTP remains available for users who want an account.
- Authentication is required at the appropriate purchase step, while browsing and cart access remain available to guests.
- Every completed feature must preserve existing functionality and pass CI/build validation.

## Current implementation status

- Guest entry + guest browsing path: implemented.
- OTP authentication path: implemented.
- Search, categories, sorting and product filters: implemented foundation.
- Cart quantity controls: implemented.
- Wishlist foundation: present.
- Checkout/address and marketplace backend foundations: present.
- Product images and seller settlement/payout foundations: present.
- Refund/cancellation protection foundations: present.
- Android/backend CI: verified green on the latest repository status.

## Internal completion policy

Before external launch dependencies are connected, internal work is treated as complete only after the relevant client/backend implementation exists, failure paths are handled, and build/contract checks pass. Real payment credentials, seller KYC/bank onboarding, shipping-provider activation and other production-account setup are external launch tasks and are intentionally kept separate.

## Production principles

- The backend is the source of truth for price, inventory, order state and seller earnings.
- Payment success is accepted only from a verified payment-provider response/webhook.
- Seller payouts are calculated from recorded transactions, not from Android-client values.
- Buyer/seller/admin permissions are enforced by the backend, not only hidden in the UI.
- Sensitive payment credentials are never stored in the Android app or AARVO database.
- Every order supports cancellation/refund/dispute paths and auditable state transitions.

## Production milestones

1. Backend + database — users, sellers, products, inventory, orders, payments, payouts and audit events.
2. Real authentication — OTP/email login, secure sessions and buyer/seller/admin roles.
3. Seller onboarding — store creation, KYC, bank/payout onboarding and seller approval.
4. Live catalog — product images, categories, search, filters, variants and server-side inventory.
5. Real cart/checkout — persistent cart, addresses, shipping calculation and server-side totals.
6. Real payments — Indian payment gateway, webhook verification, idempotency, refunds and payment reconciliation.
7. Seller operations — listing management, stock, order acceptance, packing/shipping and earnings.
8. Delivery + trust — tracking, notifications, returns, reviews, ratings and disputes.
9. Admin control plane — moderation, commissions, payouts, fraud/risk controls, support and analytics.
10. Production launch — security hardening, tests, monitoring, crash reporting, release signing, policies and Play Store release.

## Release signing preparation

`app/build.gradle.kts` supports an optional `production` signing configuration. Real keystore files and passwords must remain outside Git and should be supplied through a secure local/CI secret mechanism. If signing properties are not supplied, CI can still build an unsigned release AAB for verification.

## Important launch gate

The Android UI alone cannot make AARVO a real marketplace. Before accepting real customer money, the production backend, payment/payout account, seller verification, shipping integration, security configuration and legal policies must be connected and tested end-to-end. Until that gate is passed, no build should be marketed as a live shopping service.
