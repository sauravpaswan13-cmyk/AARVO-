# AARVO External Release Checklist

This checklist records the external actions that cannot be truthfully marked complete from repository code alone.

## 1. Render production secrets
Set these on the `aarvo-api` Render service without committing them to GitHub:

- `DATABASE_URL`
- `JWT_SECRET`
- `RAZORPAY_KEY_ID`
- `RAZORPAY_KEY_SECRET`
- `RAZORPAY_WEBHOOK_SECRET`
- `MSG91_AUTH_KEY` (or the exact OTP-provider credentials required by the configured provider)
- Any required sender/template identifiers for OTP delivery

After setting them, verify `/health` reports database/auth/payment/webhook readiness and perform a real OTP delivery test.

## 2. GitHub Actions Android signing secrets
The signed AAB workflow expects these repository Actions secrets:

- `AARVO_KEYSTORE_BASE64`
- `AARVO_KEYSTORE_PASSWORD`
- `AARVO_KEY_ALIAS`
- `AARVO_KEY_PASSWORD`

Never commit the `.jks`, passwords, or private signing material. The repository intentionally ignores `*.jks`, `*.keystore`, `*.p12`, and local signing properties.

## 3. Play Console

- Create/register the AARVO app with package `com.aarvo`.
- Use Play App Signing.
- Configure the upload key used by the signed AAB workflow.
- Complete developer/account verification and required app declarations.
- Upload the signed AAB to an internal/closed test track.
- Install the Play-generated build on a physical device and run the production smoke test.
- Complete store listing, data safety, content rating, privacy policy, target audience and app access declarations.

## 4. Real marketplace smoke test

Run this with real configured providers:

1. Guest browse/search/product detail.
2. Register/login and phone OTP verification.
3. Add/update/remove cart items.
4. Save/select delivery address.
5. Create payment order and verify payment.
6. Create order with idempotency protection.
7. Buyer order history and status timeline.
8. Seller incoming order and status progression.
9. Cancellation/return/refund path.
10. Seller settlement/payout reconciliation.
11. Review/dispute flow.
12. Retry/error behavior when payment or OTP provider is unavailable.

## Completion rule
A provider/account item is complete only after a real configured test succeeds. Repository code and CI contracts alone do not count as proof of external completion.
