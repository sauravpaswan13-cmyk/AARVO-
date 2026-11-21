import {createRoot} from 'react-dom/client';
import App from './App.tsx';
import './index.css';
import {registerServiceWorker} from './service-worker';

declare global {
  interface Window {
    gm_authFailure?: () => void;
  }
}

if (typeof window !== 'undefined') {
  window.gm_authFailure = () => {
    window.dispatchEvent(
      new CustomEvent('gmp-quota-exceeded', {
        detail: {
          message:
            'Google Maps API authentication failed or quota was reached. You can still enter your 6-digit PIN or address manually.',
        },
      })
    );
  };

  const originalConsoleError = console.error.bind(console);
  console.error = (...args: unknown[]) => {
    originalConsoleError(...args);
    try {
      const joined = args
        .map((a) => (typeof a === 'string' ? a : a instanceof Error ? a.message : JSON.stringify(a)))
        .join(' ');
      if (
        joined.includes('OverQuotaMapError') ||
        joined.includes('QuotaExceededError') ||
        joined.includes('RESOURCE_EXHAUSTED')
      ) {
        window.dispatchEvent(
          new CustomEvent('gmp-quota-exceeded', {
            detail: {
              message:
                'Google Maps temporary demo quota exceeded. Manual PIN/address entry remains active.',
            },
          })
        );
      }
    } catch {
      // ignore console serialization errors
    }
  };
}

createRoot(document.getElementById('root')!).render(<App />);

// Register Service Worker for static asset caching and offline functionality
registerServiceWorker({
  onSuccess: () => {
    console.log(
      '%c[AARVO SW]%c Service worker registered successfully. Offline support enabled.',
      'color: #FF5722; font-weight: bold;',
      'color: #059669;'
    );
  },
  onUpdate: () => {
    console.log(
      '%c[AARVO SW]%c New app version is available.',
      'color: #FF5722; font-weight: bold;',
      'color: #3B82F6;'
    );
  },
  onError: (err) => {
    console.warn('[AARVO SW] Service worker registration notice:', err.message);
  },
});
