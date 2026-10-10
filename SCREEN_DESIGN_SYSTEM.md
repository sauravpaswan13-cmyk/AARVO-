# AARVO Screen Design System

## Purpose
Keep screen appearance editable without rewriting navigation, login, OTP, or business logic.

## Source of truth
- `app/src/main/java/com/aarvo/AarvoScreenDesign.kt` — shared colors, sizes, spacing, and Splash/Welcome appearance.
- `app/src/main/java/com/aarvo/WelcomeActivity.kt` — Welcome layout and button navigation only.
- `app/src/main/java/com/aarvo/SplashActivity.kt` — Splash layout and transition only.
- `app/src/main/java/com/aarvo/PhoneAuthActivity.kt` — mobile/OTP flow; visual colors and key spacing should come from `AarvoScreenDesign`.

## Safe screen-change workflow
1. Change the requested screen's layout or design tokens only.
2. Keep logo source pixels unchanged; use the canonical `aarvo_top_logo` drawable and `FIT_CENTER`.
3. Do not edit navigation, OTP requests, session storage, or API logic for a visual-only request.
4. Build Android debug and release variants in GitHub Actions.
5. Review build results before sharing an APK. A green build proves compilation, not that the screen was visually verified on a device.
6. For each screen change, state exactly which screen changed and which flows were intentionally left untouched.

## Current scope
Shared design tokens are used by Splash, Welcome, and the mobile authentication screen. This is a controlled starting point, not a drag-and-drop visual editor. The login crash must be investigated separately using a device stack trace/logcat; keeping Welcome underneath Login is only a fallback and does not prove the underlying crash is fixed.
