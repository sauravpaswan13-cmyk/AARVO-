/**
 * AARVO Shopping Service Worker
 * Caches static assets, handles network fallbacks, and enables offline capability.
 */

const CACHE_NAME = 'aarvo-static-v1';
const DYNAMIC_CACHE_NAME = 'aarvo-dynamic-v1';

const PRECACHE_ASSETS = [
  '/',
  '/index.html',
  '/src/assets/aarvo/aarvo_logo_transparent.png',
  '/src/assets/aarvo/aarvo_logo_isolated.jpg',
  '/src/assets/aarvo/aarvo_splash_screen.jpg',
  '/src/assets/aarvo/hero_banner_1.jpg',
];

// Install Event: Precaches core app shell assets
self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then(async (cache) => {
      // Safely cache assets without failing entire install if an individual URL fails
      const cachePromises = PRECACHE_ASSETS.map((url) =>
        fetch(url)
          .then((response) => {
            if (response.ok) {
              return cache.put(url, response);
            }
            return Promise.resolve();
          })
          .catch((err) => {
            console.warn('[SW] Could not precache:', url, err);
            return Promise.resolve();
          })
      );
      await Promise.all(cachePromises);
      return self.skipWaiting();
    })
  );
});

// Activate Event: Cleans up obsolete caches
self.addEventListener('activate', (event) => {
  const currentCaches = [CACHE_NAME, DYNAMIC_CACHE_NAME];
  event.waitUntil(
    caches
      .keys()
      .then((cacheNames) => {
        return Promise.all(
          cacheNames.map((name) => {
            if (!currentCaches.includes(name)) {
              console.log('[SW] Removing old cache version:', name);
              return caches.delete(name);
            }
            return null;
          })
        );
      })
      .then(() => self.clients.claim())
  );
});

// Fetch Event: Handles offline caching strategies
self.addEventListener('fetch', (event) => {
  const { request } = event;

  // Only intercept GET requests from http / https schemes
  if (request.method !== 'GET') return;
  if (!request.url.startsWith('http')) return;

  const url = new URL(request.url);

  // Strategy 1: HTML / Navigation requests -> Network First, fall back to cached index.html
  if (request.mode === 'navigate') {
    event.respondWith(
      fetch(request)
        .then((networkResponse) => {
          if (networkResponse && networkResponse.status === 200) {
            const responseClone = networkResponse.clone();
            caches.open(DYNAMIC_CACHE_NAME).then((cache) => {
              cache.put(request, responseClone);
            });
          }
          return networkResponse;
        })
        .catch(async () => {
          const cachedResponse = await caches.match(request);
          if (cachedResponse) return cachedResponse;

          const shell = await caches.match('/index.html');
          if (shell) return shell;

          return new Response(
            '<!DOCTYPE html><html><head><meta charset="utf-8"><title>AARVO Offline</title></head><body style="font-family:sans-serif;text-align:center;padding:40px;"><h2>You are offline</h2><p>Please check your internet connection to access live features.</p></body></html>',
            { headers: { 'Content-Type': 'text/html' } }
          );
        })
    );
    return;
  }

  // Strategy 2: Static assets (JS, CSS, images, fonts) -> Cache First with Network Revalidation
  const isStaticAsset =
    request.destination === 'style' ||
    request.destination === 'script' ||
    request.destination === 'image' ||
    request.destination === 'font' ||
    url.pathname.endsWith('.js') ||
    url.pathname.endsWith('.css') ||
    url.pathname.endsWith('.png') ||
    url.pathname.endsWith('.jpg') ||
    url.pathname.endsWith('.svg');

  if (isStaticAsset) {
    event.respondWith(
      caches.match(request).then((cachedResponse) => {
        if (cachedResponse) {
          // Revalidate in background to keep cache fresh
          fetch(request)
            .then((networkResponse) => {
              if (networkResponse && networkResponse.status === 200) {
                caches.open(DYNAMIC_CACHE_NAME).then((cache) => {
                  cache.put(request, networkResponse);
                });
              }
            })
            .catch(() => {});
          return cachedResponse;
        }

        // Not in cache: fetch and cache
        return fetch(request)
          .then((networkResponse) => {
            if (networkResponse && networkResponse.status === 200) {
              const responseClone = networkResponse.clone();
              caches.open(DYNAMIC_CACHE_NAME).then((cache) => {
                cache.put(request, responseClone);
              });
            }
            return networkResponse;
          })
          .catch(() => {
            // If image request fails offline, return a placeholder SVG if needed
            if (request.destination === 'image') {
              return new Response(
                '<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100" viewBox="0 0 100 100"><rect width="100" height="100" fill="#f1f5f9"/><text x="50" y="50" dominant-baseline="middle" text-anchor="middle" fill="#94a3b8" font-size="12">Offline</text></svg>',
                { headers: { 'Content-Type': 'image/svg+xml' } }
              );
            }
            return Promise.reject('Resource offline and unavailable');
          });
      })
    );
    return;
  }

  // Strategy 3: Default Network First
  event.respondWith(
    fetch(request)
      .then((networkResponse) => {
        if (networkResponse && networkResponse.status === 200) {
          const responseClone = networkResponse.clone();
          caches.open(DYNAMIC_CACHE_NAME).then((cache) => {
            cache.put(request, responseClone);
          });
        }
        return networkResponse;
      })
      .catch(() => caches.match(request))
  );
});

// Message Listener for skipWaiting / manual commands
self.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'SKIP_WAITING') {
    self.skipWaiting();
  }
});
