import {createRoot} from 'react-dom/client';
import App from './App.tsx';
import './index.css';
import {registerServiceWorker} from './service-worker';

createRoot(document.getElementById('root')!).render(<App />);

// Register Service Worker for static asset caching and offline functionality
registerServiceWorker({
  onSuccess: (registration) => {
    console.log('%c[AARVO SW]%c Service worker registered successfully. Offline support enabled.', 'color: #2418D8; font-weight: bold;', 'color: #059669;');
  },
  onUpdate: () => {
    console.log('%c[AARVO SW]%c New app version is available.', 'color: #2418D8; font-weight: bold;', 'color: #3B82F6;');
  },
  onError: (err) => {
    console.warn('[AARVO SW] Service worker registration notice:', err.message);
  },
});

