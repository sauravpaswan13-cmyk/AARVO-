# AARVO Production Launch Checklist

This checklist is the final operational gate for a real-money marketplace release. Code and CI can validate the application, but provider-account activation and production secrets must be completed in the deployment environment.

## 1. Backend

- [ ] Deploy `backend/` from the green `main` commit.
- [ ] Configure `NODE_ENV=production` and `PORT`.
- [ ] Configure `DATABASE_URL` and `DATABASE_SSL=true`.
- [ ] Apply `schema.sql` and migrations `001` through `005` in order.
- [ ] Confirm `/health` returns `{"ok":true}` from the deployed API.
- [ ] Configure `CORS_ORIGIN` to the exact production Android/web origin(s).
- [ ] Set a random production `JWT_SECRET` (32+ bytes recommended).
- [ ] Set `DELIVERY_FEE_PAISE` and `PLATFORM_FEE_BPS` intentionally; never rely on undocumented defaults.

## 2. Payments

- [ ] Activate the production Razorpay account.
- [ ] Put live `RAZORPAY_KEY_ID` and `RAZORPAY_KEY_SECRET` in the hosting provider's secret store.
- [ ] Configure `RAZORPAY_WEBHOOK_SECRET` in the same secret store.
- [ ] Point the Razorpay webhook to the deployed `/v1/payments/webhook` endpoint.
- [ ] Verify webhook signature delivery and idempotent event handling.
- [ ] Perform a controlled live payment and verify that the order is confirmed only after server-side verification.
- [ ] Verify failed, cancelled, expired, and already-processed payment cases.

## 3. Seller operations

- [ ] Complete the marketplace's seller verification/KYC process required by the payment/payout provider.
- [ ] Confirm seller payout accounts are activated before allowing production fulfilment.
- [ ] Verify seller product creation, inventory updates, and seller-owned order visibility.
- [ ] Verify seller shipping/status updates cannot modify another seller's order line.

## 4. Delivery

- [ ] Select and contract the production shipping/delivery provider.
- [ ] Configure its API credentials as hosting secrets.
- [ ] Map provider shipment events to AARVO delivery statuses.
- [ ] Verify shipped, out-for-delivery, delivered, failed-delivery and return/refund flows.

## 5. Legal and customer support

- [ ] Publish Terms of Service.
- [ ] Publish Privacy Policy and data-retention/contact details.
- [ ] Publish Refund/Cancellation/Return Policy.
- [ ] Publish Seller Terms and marketplace fee disclosure.
- [ ] Provide customer support contact details and a dispute/escalation path.
- [ ] Review all policies with the business/legal owner before publishing.

## 6. Android release

- [ ] Generate a production keystore outside the repository.
- [ ] Store signing properties/credentials only in the secure CI secret/environment configuration.
- [ ] Build a signed release AAB and verify the signing certificate.
- [ ] Set the production API URL using `AARVO_API_BASE_URL`.
- [ ] Confirm the release build contains no demo payment fallback.
- [ ] Run the release smoke test on a physical Android device.
- [ ] Upload the signed AAB to the Play Console using the production release process.

## 7. Final smoke test

1. Register buyer.
2. Register seller.
3. Seller creates/publishes a product.
4. Buyer sees the product and adds it to cart.
5. Buyer creates an order.
6. Buyer completes a controlled payment.
7. Server verifies the payment and confirms the order.
8. Seller sees only its own order lines.
9. Seller marks shipment status.
10. Buyer sees tracking updates.
11. Seller marks delivered.
12. Buyer submits one review for an eligible product.
13. Buyer can cancel only when the order state permits cancellation.
14. Dispute/refund handling is exercised in a controlled environment.
15. Audit and payment-event records are present in production logs/database.

## Release rule

AARVO is **not** considered production-live merely because GitHub Actions builds the APK/AAB. The repository's CI proves the checked-in software builds and its automated backend checks pass; live payment, payout, delivery, legal, hosting, secrets, and signed-release gates require real external configuration and must be verified before accepting customer money.
