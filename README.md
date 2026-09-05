# AARVO

AARVO is a modern Android shopping app starter built with Kotlin and Jetpack Compose.

## Phase 1 — 10-step foundation

1. **Project foundation** — Android `app` module, Kotlin and Jetpack Compose.
2. **Theme/UI** — Material 3 with light/dark theme support.
3. **Onboarding** — Get Started flow with local completion state.
4. **Demo sign-in** — Name/email validation with local account state.
5. **Product discovery** — Search and category filtering over the sample catalog.
6. **Product experience** — Product cards, ratings, descriptions and a details screen.
7. **Wishlist** — Save/remove products from the wishlist during the session.
8. **Cart** — Add, remove and clear cart items with a live total and cart badge.
9. **Checkout** — Delivery-address validation and Cash on Delivery demo order confirmation.
10. **Delivery pipeline** — GitHub Actions CI uses JDK 17 + Gradle 9.7.1, builds the debug APK and uploads it as `aarvo-debug-apk`.

## Current architecture

- `data/Product.kt` — product model and sample catalog
- `data/ProductRepository.kt` — product filtering/category access
- `cart/CartViewModel.kt` — cart state
- `MainActivity.kt` — app navigation and current UI flows
- `.github/workflows/android.yml` — Android CI and debug APK artifact

## Open in Android Studio

1. Open this repository as an Android project.
2. Use JDK 17.
3. Sync Gradle and let Android Studio install any missing SDK components.
4. Run the `app` configuration on an Android 7.0+ device/emulator.

The project uses Android Gradle Plugin 9.4.0, Gradle 9.7.1 in CI, Kotlin 2.3.21 and compile/target SDK 37.

## Next production phases

1. Replace demo authentication with a secure backend identity service.
2. Add real product images/catalog API.
3. Persist cart and wishlist with a local database.
4. Add full address management.
5. Integrate a production payment gateway.
6. Persist orders and add order tracking.
7. Add seller/admin dashboards and authorization.
8. Connect backend API and database.
9. Expand automated tests, release signing and release CI.
10. Production security, analytics, crash reporting and Play Store release preparation.
