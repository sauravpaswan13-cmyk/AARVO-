/**
 * AARVO Shopping - Service Worker Configuration & Registration Module
 * Enables static asset caching, offline fallback, and network resiliency.
 */

export const SW_CACHE_NAME = 'aarvo-static-v1';
export const SW_DYNAMIC_CACHE_NAME = 'aarvo-dynamic-v1';

// Key static assets to precache for offline availability
export const PRECACHE_ASSETS: readonly string[] = [
  '/',
  '/index.html',
  '/src/assets/aarvo/aarvo_logo_transparent.png',
  '/src/assets/aarvo/aarvo_logo_isolated.jpg',
  '/src/assets/aarvo/aarvo_splash_screen.jpg',
  '/src/assets/aarvo/hero_banner_1.jpg',
] as const;

export interface ServiceWorkerOptions {
  swPath?: string;
  scope?: string;
  onSuccess?: (registration: ServiceWorkerRegistration) => void;
  onUpdate?: (registration: ServiceWorkerRegistration) => void;
  onError?: (error: Error) => void;
  onOffline?: () => void;
  onOnline?: () => void;
}

/**
 * Checks if the browser is currently running in offline mode.
 */
export function isAppOffline(): boolean {
  return typeof navigator !== 'undefined' && !navigator.onLine;
}

/**
 * Subscribes to network status changes (online/offline).
 * Returns an cleanup function to remove the listeners.
 */
export function onNetworkStatusChange(callback: (isOnline: boolean) => void): () => void {
  if (typeof window === 'undefined') return () => {};

  const handleOnline = () => callback(true);
  const handleOffline = () => callback(false);

  window.addEventListener('online', handleOnline);
  window.addEventListener('offline', handleOffline);

  return () => {
    window.removeEventListener('online', handleOnline);
    window.removeEventListener('offline', handleOffline);
  };
}

/**
 * Clears old or all AARVO caches from CacheStorage.
 */
export async function clearAppCache(): Promise<boolean> {
  if (typeof window === 'undefined' || !('caches' in window)) return false;

  try {
    const keys = await caches.keys();
    await Promise.all(
      keys
        .filter((key) => key.startsWith('aarvo-'))
        .map((key) => caches.delete(key))
    );
    console.log('[AARVO SW] App caches cleared successfully.');
    return true;
  } catch (error) {
    console.error('[AARVO SW] Error clearing app cache:', error);
    return false;
  }
}

/**
 * Registers the Service Worker to enable offline caching and asset management.
 */
export async function registerServiceWorker(
  options: ServiceWorkerOptions = {}
): Promise<ServiceWorkerRegistration | null> {
  if (typeof window === 'undefined' || !('serviceWorker' in navigator)) {
    console.warn('[AARVO SW] Service Worker is not supported in this browser environment.');
    return null;
  }

  // Setup online/offline listeners
  window.addEventListener('online', () => {
    console.log('%c[AARVO SW]%c App is back online', 'color: #2418D8; font-weight: bold;', 'color: #059669;');
    options.onOnline?.();
  });

  window.addEventListener('offline', () => {
    console.warn('%c[AARVO SW]%c App is running in offline mode. Serving cached resources.', 'color: #2418D8; font-weight: bold;', 'color: #D97706;');
    options.onOffline?.();
  });

  const swUrl = options.swPath || '/sw.js';
  const scope = options.scope || '/';

  const register = async () => {
    try {
      const registration = await navigator.serviceWorker.register(swUrl, { scope });
      console.log(
        `%c[AARVO SW]%c Registered successfully with scope: ${registration.scope}`,
        'color: #2418D8; font-weight: bold;',
        'color: #334155;'
      );

      // Handle registration lifecycle
      registration.onupdatefound = () => {
        const installingWorker = registration.installing;
        if (!installingWorker) return;

        installingWorker.onstatechange = () => {
          if (installingWorker.state === 'installed') {
            if (navigator.serviceWorker.controller) {
              console.log('[AARVO SW] New content is available; will update upon page reload.');
              options.onUpdate?.(registration);
            } else {
              console.log('[AARVO SW] Content is cached for offline use.');
              options.onSuccess?.(registration);
            }
          }
        };
      };

      options.onSuccess?.(registration);
      return registration;
    } catch (error) {
      console.error('[AARVO SW] Registration failed:', error);
      options.onError?.(error instanceof Error ? error : new Error(String(error)));
      return null;
    }
  };

  // If document is already loaded, register immediately; otherwise wait for 'load'
  if (document.readyState === 'complete') {
    return register();
  } else {
    return new Promise((resolve) => {
      window.addEventListener('load', async () => {
        const reg = await register();
        resolve(reg);
      });
    });
  }
}

/**
 * Unregisters any active Service Workers for clean debugging or teardown.
 */
export async function unregisterServiceWorker(): Promise<boolean> {
  if (typeof window === 'undefined' || !('serviceWorker' in navigator)) {
    return false;
  }

  try {
    const registration = await navigator.serviceWorker.getRegistration();
    if (registration) {
      const unregistered = await registration.unregister();
      console.log('[AARVO SW] Service Worker unregistered:', unregistered);
      return unregistered;
    }
    return false;
  } catch (error) {
    console.error('[AARVO SW] Failed to unregister Service Worker:', error);
    return false;
  }
}
