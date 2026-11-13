/**
 * AARVO Shopping - Mobile Haptic Feedback Utility
 * Uses window.navigator.vibrate to trigger subtle tactile feedback on mobile devices.
 */

export type HapticFeedbackType =
  | 'light'          // 12ms tap for standard buttons, tabs, links
  | 'medium'         // 22ms tap for toggles, modal triggers, drawers
  | 'selection'      // 10ms crisp tick for category pills, filters
  | 'subtle'         // [8, 18, 10] gentle micro-confirmation for quiet positive actions
  | 'addToCart'      // [10, 28, 16] crisp two-pulse cart drop-in confirmation
  | 'saved'          // [8, 22, 12] soft confirmation for saving preferences, wishlist, coupons, copy
  | 'orderComplete'  // [14, 34, 18, 42, 26] warm celebratory pulse for order placement
  | 'success'        // [15, 40, 20] dual vibration pattern for general success states
  | 'warning'        // [30, 45, 25] pattern for deletion, alert, removal
  | 'impact';        // 28ms solid impact

const HAPTICS_STORAGE_KEY = 'aarvo_haptics_enabled';

const HAPTIC_PATTERNS: Record<HapticFeedbackType, number | number[]> = {
  light: 12,
  medium: 22,
  selection: 10,
  subtle: [8, 18, 10],
  addToCart: [10, 28, 16],
  saved: [8, 22, 12],
  orderComplete: [14, 34, 18, 42, 26],
  success: [15, 40, 20],
  warning: [30, 45, 25],
  impact: 28,
};

let hapticsEnabledCache: boolean | null = null;

/**
 * Checks whether the current environment supports the Web Vibration API.
 */
export function isHapticsSupported(): boolean {
  return (
    typeof window !== 'undefined' &&
    typeof window.navigator !== 'undefined' &&
    typeof window.navigator.vibrate === 'function'
  );
}

/**
 * Returns whether haptic feedback is currently enabled by the user.
 */
export function isHapticsEnabled(): boolean {
  if (hapticsEnabledCache !== null) {
    return hapticsEnabledCache;
  }
  if (typeof window === 'undefined') {
    return true;
  }
  try {
    const stored = window.localStorage.getItem(HAPTICS_STORAGE_KEY);
    hapticsEnabledCache = stored === null ? true : stored !== 'false';
  } catch {
    hapticsEnabledCache = true;
  }
  return hapticsEnabledCache;
}

/**
 * Enables or disables haptic feedback globally and persists the preference safely.
 */
export function setHapticsEnabled(enabled: boolean): void {
  hapticsEnabledCache = enabled;
  if (typeof window === 'undefined') return;
  try {
    window.localStorage.setItem(HAPTICS_STORAGE_KEY, String(enabled));
  } catch {
    // Ignore storage quota / privacy mode errors
  }
}

/**
 * Triggers a safe Vibration API pulse or pattern if supported and enabled.
 */
function dispatchVibration(pattern: number | number[]): boolean {
  if (!isHapticsEnabled() || !isHapticsSupported()) {
    return false;
  }

  try {
    return window.navigator.vibrate(pattern);
  } catch {
    // Graceful fallback for browsers with restricted Vibration API policies
    return false;
  }
}

/**
 * Triggers a subtle haptic vibration on devices supporting the Vibration API.
 * @param type The type of haptic feedback to trigger (default: 'light')
 * @returns boolean indicating whether vibration was successfully dispatched
 */
export function triggerHaptic(type: HapticFeedbackType = 'light'): boolean {
  const pattern = HAPTIC_PATTERNS[type] ?? 12;
  return dispatchVibration(pattern);
}

/**
 * Triggers a custom vibration duration or pattern safely.
 * @param pattern Single duration in ms or alternating vibrate/pause pattern
 */
export function triggerCustomHaptic(pattern: number | number[]): boolean {
  return dispatchVibration(pattern);
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

  /** Featherlight micro-confirmation for quiet positive actions (refresh, download, restock) */
  subtle: () => triggerHaptic('subtle'),

  /** Crisp two-pulse confirmation for adding items to the shopping cart */
  addToCart: () => triggerHaptic('addToCart'),

  /** Soft confirmation for saving preferences, addresses, wishlist items, coupons, or copy */
  saved: () => triggerHaptic('saved'),

  /** Warm celebratory triple-pulse for completed order placement */
  orderComplete: () => triggerHaptic('orderComplete'),

  /** Dual vibration feedback for general successful actions */
  success: () => triggerHaptic('success'),

  /** Distinct alert pattern for removals (e.g. Delete Cart Item, Remove Coupon) */
  warning: () => triggerHaptic('warning'),

  /** Solid impact for primary CTA presses (e.g. Buy Now, Voice Search) */
  impact: () => triggerHaptic('impact'),
};

