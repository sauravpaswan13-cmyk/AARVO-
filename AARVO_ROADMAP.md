# AARVO — Super Marketplace Roadmap

> Goal: build AARVO as a production-grade marketplace with a feature set equal to or broader than major Indian marketplaces, while keeping the app stable, premium and secure.

## Product rule

AARVO will not sacrifice reliability for feature count. Every feature must pass build/contract checks and preserve existing guest browsing, OTP authentication, cart and checkout behavior.

## Monetization model

AARVO's planned revenue model is diversified so the platform does not depend on a single fee:

- **Seller commission:** final planning range **3%–12% of eligible order value**, configurable by product category and promotional/admin rules. The rate must stay within this range unless the business model is deliberately revised later.
- **Suggested initial category bands:** Mobiles/Electronics **3%–6%**; Fashion **5%–8%**; Home/Kitchen **6%–9%**; Beauty/Personal Care **7%–10%**; General Products **8%–12%**; high-margin categories **up to 12%**.
- **Seller services fee:** optional fixed/percentage fee for premium marketplace services when those services are actually provided.
- **Sponsored listings / seller advertising:** sellers can pay for promoted placement; ads must be clearly identified.
- **Shipping margin:** where AARVO negotiates a lower logistics cost than the shipping fee charged to the buyer, the transparent difference can contribute to platform revenue. This must not be treated as live until a shipping provider is integrated.
- **Future membership/referral/affiliate revenue:** reserved for later phases and not required for initial launch.

**Illustrative target only:** at 500 orders/day and ₹1,000 average order value, GMV is ₹5,00,000/day. At a 3%–12% commission range, gross commission would be ₹15,000–₹60,000/day or about ₹4,50,000–₹18,00,000 per 30-day month before refunds, payment costs, taxes, promotions, support, shipping subsidies and other operating expenses. This is not a guaranteed profit forecast.

Commission and seller earnings must always be calculated server-side from recorded transactions. Actual rates should be controlled through admin/category commission rules rather than hard-coded in the Android client.

## Phase 0 — Stability Gate

- [x] Guest entry without verification
- [x] OTP login/verification path
- [x] Login/Signup access from inside the app
- [x] Guest cart/browsing preserved
- [x] Purchase-time authentication gate
- [x] Android + backend CI green
- [ ] Signed production release configuration
- [ ] End-to-end production smoke test on a physical device

## Phase 1 — Discovery & Shopping Experience

- [x] Product discovery foundation
- [x] Search
- [x] Categories
- [x] Sorting
- [x] Rating/price/stock filters
- [ ] Search suggestions and recent searches
- [ ] Voice-search-ready architecture
- [ ] Barcode/visual-search-ready architecture
- [ ] Product comparison
- [ ] Recently viewed products
- [ ] Personalized recommendations
- [ ] Trending / best sellers / new arrivals
- [ ] Deals of the day / flash deals
- [ ] Category landing pages with banners

## Phase 2 — Cart, Wishlist & Offers

- [x] Cart quantity controls
- [x] Wishlist foundation
- [ ] Persistent server cart
- [ ] Save for later
- [ ] Coupon validation at checkout
- [ ] Automatic best-offer selection
- [ ] Bank/card/UPI offer rules
- [ ] Gift cards / wallet-ready architecture
- [ ] Bundle offers and buy-more-save-more
- [ ] Price-drop alerts
- [ ] Back-in-stock alerts

## Phase 3 — Checkout, Payments & Orders

- [ ] Server-authoritative checkout totals
- [ ] Address book and delivery instructions
- [ ] Pincode/serviceability check
- [ ] Shipping fee and ETA calculation
- [ ] COD eligibility
- [ ] UPI/Razorpay production integration
- [ ] Payment webhook verification
- [ ] Idempotent order creation
- [ ] Payment reconciliation
- [ ] Order confirmation
- [ ] Order history
- [ ] Live order status timeline
- [ ] Cancellation rules
- [ ] Returns/replacement/refund workflow
- [ ] Invoice/download receipt

## Phase 4 — Seller Marketplace

- [ ] Seller signup and role switching
- [ ] Seller KYC
- [ ] Store/profile management
- [ ] Product listing creation/editing
- [ ] Product variants
- [ ] Inventory management
- [ ] Bulk catalog operations
- [ ] Seller order management
- [ ] Packing/shipping workflow
- [ ] Seller earnings dashboard
- [ ] Commission calculation **(3%–12% category-aware model)**
- [ ] Payout reconciliation
- [ ] Seller ratings and performance
- [ ] Seller support/disputes

## Phase 5 — Trust, Reviews & Support

- [ ] Verified buyer reviews
- [ ] Star ratings
- [ ] Photo/video reviews
- [ ] Review moderation
- [ ] Seller/product report flow
- [ ] Help center
- [ ] Support tickets
- [ ] Order dispute flow
- [ ] Refund/dispute audit trail
- [ ] Fraud/risk signals
- [ ] Device/session security controls

## Phase 6 — Personalization & Premium UX

- [ ] Premium AARVO home experience
- [ ] Smooth navigation and loading states
- [ ] Skeleton states and retry UX
- [ ] Personalized home feed
- [ ] Smart recommendations
- [ ] Wishlist-driven recommendations
- [ ] Recently viewed rail
- [ ] Occasion/festival collections
- [ ] Premium animations/micro-interactions without harming performance
- [ ] Accessibility and responsive layouts

## Phase 7 — Notifications & Delivery

- [ ] Push notification architecture
- [ ] Order/payment notifications
- [ ] Delivery updates
- [ ] Offer/coupon notifications
- [ ] Wishlist price-drop notifications
- [ ] Back-in-stock notifications
- [ ] Notification preferences
- [ ] Shipping provider abstraction
- [ ] Tracking events
- [ ] Delivery ETA updates

## Phase 8 — Admin Control Plane

- [ ] Admin authentication and role permissions
- [ ] Seller approval/KYC review
- [ ] Catalog moderation
- [ ] Category/brand management
- [ ] Offer/coupon management
- [ ] Commission rules **(3%–12% configurable)**
- [ ] Order/payment/refund operations
- [ ] Fraud/risk review
- [ ] Support/dispute management
- [ ] Audit logs
- [ ] Marketplace analytics
- [ ] Operational dashboards

## Phase 9 — Advanced Marketplace Features

- [ ] Multi-seller product comparison
- [ ] Brand stores
- [ ] Seller storefronts
- [ ] Loyalty/rewards architecture
- [ ] Referral program
- [ ] Affiliate-ready tracking
- [ ] Subscription/membership-ready architecture
- [ ] Gift registry / wishlists sharing
- [ ] Scheduled delivery-ready architecture
- [ ] Multi-language support
- [ ] Multi-currency-ready pricing model

## Phase 10 — Production & Scale

- [ ] Production database hardening
- [ ] API rate limiting
- [ ] Secrets management
- [ ] Observability/logging/metrics
- [ ] Crash reporting
- [ ] Automated regression tests
- [ ] Load/performance testing
- [ ] Backup/restore verification
- [ ] Payment reconciliation monitoring
- [ ] Security review
- [ ] Privacy policy / terms / returns policy
- [ ] Signed release AAB
- [ ] Play Store production release

## Definition of Done

A phase is complete only when its feature is implemented in the correct client/backend layer, existing flows still work, CI is green, and the feature has a production-safe failure path. UI-only placeholders do not count as complete marketplace functionality.

## Today's internal-work gate

External provider/account work is intentionally deferred. Internal work for today is limited to repository code, Android/backend contracts, validation, error handling, tests, build integrity and documentation. Do not mark external payment, KYC, bank, shipping, Play Store or real-money smoke-test items complete until the corresponding external integration has actually been configured and verified.

## Priority order

1. Stability + real backend source of truth
2. Checkout + payments + orders
3. Seller operations
4. Delivery + returns + support
5. Offers + notifications
6. Personalization + premium UX
7. Admin + fraud/risk
8. Advanced marketplace features
9. Scale + Play Store production
