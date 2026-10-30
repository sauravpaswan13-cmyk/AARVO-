/**
 * AARVO Shopping - Mobile Haptic Feedback Utility
 * Uses window.navigator.vibrate to trigger subtle tactile feedback on mobile devices.
 */

export type HapticFeedbackType =
  | 'light'       // 8-12ms tap for standard buttons, tabs, links
  | 'medium'      // 20ms tap for toggles, modal triggers, drawers
  | 'selection'   // 12ms crisp tick for category pills, filters
  | 'success'     // [15, 35, 20] vibration pattern for order completion, add to cart
  | 'warning'     // [25, 40, 25] pattern for deletion, alert, removal
  | 'impact';     // 25ms solid impact

const HAPTIC_PATTERNS: Record<HapticFeedbackType, number | number[]> = {
  light: 12,
  medium: 22,
  selection: 10,
  success: [15, 40, 20],
  warning: [30, 45, 25],
  impact: 28,
};

/**
 * Triggers a subtle haptic vibration on devices supporting the Vibration API.
 * @param type The type of haptic feedback to trigger (default: 'light')
 * @returns boolean indicating whether vibration was successfully dispatched
 */
export function triggerHaptic(type: HapticFeedbackType = 'light'): boolean {
  if (
    typeof window === 'undefined' ||
    typeof window.navigator === 'undefined' ||
    typeof window.navigator.vibrate !== 'function'
  ) {
    return false;
  }

  try {
    const pattern = HAPTIC_PATTERNS[type] ?? 12;
    return window.navigator.vibrate(pattern);
  } catch {
    // Graceful fallback for browsers with restricted Vibration API policies
    return false;
  }
}

/**
 * Convenience methods for common interaction feedbacks throughout the app.
 */
export const haptics = {
  /** Subtle 12ms feedback for standard button clicks and card taps */
  light: () => triggerHaptic('light'),

  /** 22ms feedback for toggle switches (e.g. Light/Dark mode) and drawer menus */
  medium: () => triggerHaptic('medium'),

  /** 10ms feedback for tab switches, filter changes, category selections */
  selection: () => triggerHaptic('selection'),

  /** Dual vibration feedback for successful actions (e.g. Add to Cart, Apply Coupon) */
  success: () => triggerHaptic('success'),

  /** Distinct alert pattern for removals (e.g. Delete Cart Item) */
  warning: () => triggerHaptic('warning'),

  /** Solid impact for primary CTA presses (e.g. Checkout, Voice Search) */
  impact: () => triggerHaptic('impact'),
};
