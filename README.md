# AARVO

AARVO is a modern Android shopping app starter built with Kotlin and Jetpack Compose.

## Implemented in the current build

- Android application module: `app`
- Package: `com.aarvo`
- Kotlin + Jetpack Compose + Material 3
- Onboarding screen
- Local demo authentication with name/email validation
- Product search and category filtering
- Product details screen
- Product cards with ratings and descriptions
- In-memory cart with add/remove/clear
- Wishlist toggle
- Checkout dialog with delivery address and cash-on-delivery demo flow
- Order confirmation state
- Home, Cart and Profile tabs
- Light/dark theme support

## Current architecture

- `data/Product.kt` — product model and sample catalog
- `data/ProductRepository.kt` — product filtering/category access
- `cart/CartViewModel.kt` — cart state
- `MainActivity.kt` — app navigation and current UI flows

## Open in Android Studio

1. Open this repository as an Android project.
2. Use JDK 17.
3. Sync Gradle and let Android Studio install any missing SDK components.
4. Run the `app` configuration on an Android 7.0+ device/emulator.

The project uses Android Gradle Plugin 9.4.0, Gradle 9.6, Kotlin 2.3.21 and compile/target SDK 37.

## Next production phases

1. Replace demo authentication with a secure backend identity service.
2. Add real product images/catalog API.
3. Persist cart and wishlist with a local database.
4. Add full address management.
5. Integrate a production payment gateway.
6. Persist orders and add order tracking.
7. Add seller/admin dashboards and authorization.
8. Connect backend API and database.
9. Add automated tests, CI and release signing.
10. Production security, analytics, crash reporting and Play Store release preparation.
