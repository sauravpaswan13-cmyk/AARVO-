import React, { Component, useState, useEffect, useRef } from 'react';
import { SplashScreen } from './components/SplashScreen';
import { LoginScreen } from './components/LoginScreen';
import { MainScreen } from './components/MainScreen';
import { haptics } from './utils/haptics';
import { WifiOff, Wifi, X, AlertTriangle } from 'lucide-react';
import { isAppOffline, onNetworkStatusChange, onServiceWorkerOfflineMessage } from './service-worker';

interface ErrorBoundaryState {
  hasError: boolean;
}

class AarvoErrorBoundary extends Component<{ children: React.ReactNode }, ErrorBoundaryState> {
  state: ErrorBoundaryState = { hasError: false };

  static getDerivedStateFromError(): ErrorBoundaryState {
    return { hasError: true };
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen flex items-center justify-center bg-[#FFFDF9] dark:bg-[#120E0C] px-6 text-center">
          <div className="max-w-sm w-full bg-white dark:bg-[#181311] border border-[#FFDFC4] dark:border-[#332620] rounded-2xl p-6 shadow-sm">
            <div className="w-12 h-12 mx-auto rounded-full bg-[#FFF4E8] dark:bg-[#2B1A12] text-[#FF5722] flex items-center justify-center text-xl font-black mb-3">
              !
            </div>
            <h2 className="text-lg font-bold text-[#1F1612] dark:text-[#FAF3EE] mb-1">Something went wrong</h2>
            <p className="text-xs text-[#6B5B53] dark:text-[#CBBDB4] mb-4">
              Tap below to restore AARVO safely without losing your cart or wishlist.
            </p>
            <button
              type="button"
              onClick={() => {
                this.setState({ hasError: false });
              }}
              className="w-full py-2.5 rounded-xl bg-[#FF5722] hover:bg-[#E64A19] text-white text-sm font-bold transition-colors"
            >
              Restore AARVO
            </button>
          </div>
        </div>
      );
    }
    return this.props.children;
  }
}

export default function App() {
  // Navigation Flow State: Default to 'main' so user immediately sees the MainScreen
  const [currentScreen, setCurrentScreen] = useState<'splash' | 'login' | 'main'>('main');

  // Simple Mock Analytics Tracker: Tracks screen transitions and logs to console
  const previousScreenRef = useRef<string | null>(null);

  useEffect(() => {
    const prev = previousScreenRef.current;
    const timestamp = new Date().toLocaleTimeString();

    // Console logging for engagement analytics tracking
    console.log(
      `%c[AARVO Analytics]%c [${timestamp}] Screen View: %c${currentScreen.toUpperCase()}%c${
        prev ? ` (Transitioned from: ${prev})` : ' (Initial Screen)'
      }`,
      'color: #FF5722; font-weight: bold; padding: 2px 4px; border-radius: 4px;',
      'color: #64748B;',
      'color: #059669; font-weight: bold;',
      'color: #64748B;'
    );

    previousScreenRef.current = currentScreen;
  }, [currentScreen]);

  // Performance Observer: Measure and log Time to Interactive (TTI) & startup metrics
  const performanceLoggedRef = useRef<boolean>(false);

  useEffect(() => {
    if (typeof window === 'undefined' || !('performance' in window) || performanceLoggedRef.current) {
      return;
    }

    performanceLoggedRef.current = true;

    let fcpTime: number | null = null;
    let longTasksCount = 0;
    let totalBlockingTime = 0;

    // 1. Observe First Contentful Paint (FCP)
    let paintObserver: PerformanceObserver | null = null;
    if (typeof PerformanceObserver !== 'undefined' && PerformanceObserver.supportedEntryTypes?.includes('paint')) {
      try {
        paintObserver = new PerformanceObserver((list) => {
          for (const entry of list.getEntries()) {
            if (entry.name === 'first-contentful-paint') {
              fcpTime = Math.round(entry.startTime);
            }
          }
        });
        paintObserver.observe({ type: 'paint', buffered: true });
      } catch {
        // Fallback for browsers that do not support buffered paint observer
      }
    }

    // 2. Observe Long Tasks (>50ms) to detect main-thread blockage
    let longTaskObserver: PerformanceObserver | null = null;
    if (typeof PerformanceObserver !== 'undefined' && PerformanceObserver.supportedEntryTypes?.includes('longtask')) {
      try {
        longTaskObserver = new PerformanceObserver((list) => {
          for (const entry of list.getEntries()) {
            longTasksCount++;
            totalBlockingTime += Math.max(0, entry.duration - 50);
          }
        });
        longTaskObserver.observe({ entryTypes: ['longtask'] });
      } catch {
        // Longtask observer optional fallback
      }
    }

    // 3. Compute and log Time to Interactive (TTI) once main thread stabilizes
    const computeTTI = () => {
      const scheduleIdle = window.requestIdleCallback || ((cb: () => void) => setTimeout(cb, 100));

      scheduleIdle(() => {
        const ttiTime = Math.round(performance.now());
        const navEntry = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming | undefined;
        const domInteractive = navEntry ? Math.round(navEntry.domInteractive) : null;
        const domContentLoaded = navEntry ? Math.round(navEntry.domContentLoadedEventEnd) : null;

        const rating = ttiTime <= 1500 ? 'FAST (Excellent)' : ttiTime <= 2800 ? 'GOOD (Acceptable)' : 'NEEDS OPTIMIZATION';
        const ratingColor = ttiTime <= 1500 ? '#059669' : ttiTime <= 2800 ? '#D97706' : '#DC2626';

        console.groupCollapsed(
          `%c[AARVO Performance]%c Startup TTI: %c${ttiTime} ms%c (${rating})`,
          'color: #FF5722; font-weight: bold; padding: 2px 4px; border-radius: 4px;',
          'color: #475569;',
          `color: ${ratingColor}; font-weight: bold;`,
          'color: #64748B;'
        );
        console.log(`⏱️ Time to Interactive (TTI): %c${ttiTime} ms`, 'font-weight: bold;');
        if (fcpTime !== null) {
          console.log(`🎨 First Contentful Paint (FCP): %c${fcpTime} ms`, 'color: #2563EB;');
        }
        if (domInteractive !== null) {
          console.log(`📄 DOM Interactive: %c${domInteractive} ms`, 'color: #0891B2;');
        }
        if (domContentLoaded !== null) {
          console.log(`⚡ DOM Content Loaded: %c${domContentLoaded} ms`, 'color: #0891B2;');
        }
        console.log(`🧱 Long Tasks Detected (>50ms): %c${longTasksCount}`, 'color: #7C3AED;');
        if (totalBlockingTime > 0) {
          console.log(`⏳ Estimated Total Blocking Time: %c${Math.round(totalBlockingTime)} ms`, 'color: #DC2626;');
        }
        console.log(`📊 Rating: %c${rating}`, `color: ${ratingColor}; font-weight: bold;`);
        console.groupEnd();

        // Disconnect observers after measurement
        if (paintObserver) paintObserver.disconnect();
        if (longTaskObserver) longTaskObserver.disconnect();
      });
    };

    if (document.readyState === 'complete') {
      computeTTI();
    } else {
      window.addEventListener('load', computeTTI, { once: true });
    }

    return () => {
      if (paintObserver) paintObserver.disconnect();
      if (longTaskObserver) longTaskObserver.disconnect();
    };
  }, []);

  // Global Theme State: 'light' | 'dark' with localStorage persistence
  const [theme, setTheme] = useState<'light' | 'dark'>(() => {
    try {
      const saved = localStorage.getItem('aarvo_theme');
      if (saved === 'dark' || saved === 'light') return saved;
      return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches
        ? 'dark'
        : 'light';
    } catch {
      return 'light';
    }
  });

  // Sync theme with DOM and localStorage
  useEffect(() => {
    try {
      localStorage.setItem('aarvo_theme', theme);
    } catch {
      // Ignore storage errors in restricted contexts
    }

    if (theme === 'dark') {
      document.documentElement.classList.add('dark');
      document.documentElement.setAttribute('data-theme', 'dark');
    } else {
      document.documentElement.classList.remove('dark');
      document.documentElement.setAttribute('data-theme', 'light');
    }
  }, [theme]);

  const toggleTheme = () => {
    haptics.medium();
    setTheme((prev) => (prev === 'light' ? 'dark' : 'light'));
  };

  // Network Status Listener leveraging Service Worker offline caching capabilities
  const [isOnline, setIsOnline] = useState<boolean>(() => !isAppOffline());
  const [offlineToastDismissed, setOfflineToastDismissed] = useState(false);
  const [showBackOnlineToast, setShowBackOnlineToast] = useState(false);
  const [swReady, setSwReady] = useState(false);
  const wasOfflineRef = useRef<boolean>(isAppOffline());

  useEffect(() => {
    if (typeof window === 'undefined') return;

    let backOnlineTimer: ReturnType<typeof setTimeout> | null = null;

    if ('serviceWorker' in navigator) {
      navigator.serviceWorker.ready
        .then(() => setSwReady(true))
        .catch(() => {});
    }

    const unsubscribe = onNetworkStatusChange((online) => {
      if (!online) {
        wasOfflineRef.current = true;
        setIsOnline(false);
        setOfflineToastDismissed(false);
        setShowBackOnlineToast(false);
        if (backOnlineTimer) {
          clearTimeout(backOnlineTimer);
          backOnlineTimer = null;
        }
      } else {
        setIsOnline(true);
        setOfflineToastDismissed(false);
        if (wasOfflineRef.current) {
          wasOfflineRef.current = false;
          setShowBackOnlineToast(true);
          if (backOnlineTimer) clearTimeout(backOnlineTimer);
          backOnlineTimer = setTimeout(() => {
            setShowBackOnlineToast(false);
          }, 3200);
        }
      }
    });

    const unsubscribeSw = onServiceWorkerOfflineMessage(() => {
      wasOfflineRef.current = true;
      setIsOnline(false);
      setOfflineToastDismissed(false);
    });

    return () => {
      unsubscribe();
      unsubscribeSw();
      if (backOnlineTimer) clearTimeout(backOnlineTimer);
    };
  }, []);

  const [gmpQuotaMessage, setGmpQuotaMessage] = useState<string | null>(null);

  useEffect(() => {
    if (typeof window === 'undefined') return;
    const handleQuota = (ev: Event) => {
      const detail = (ev as CustomEvent<{ message?: string }>).detail;
      setGmpQuotaMessage(
        detail?.message || 'Google Maps API quota exceeded or unavailable.'
      );
    };
    window.addEventListener('gmp-quota-exceeded', handleQuota);
    return () => window.removeEventListener('gmp-quota-exceeded', handleQuota);
  }, []);

  return (
    <AarvoErrorBoundary>
      <div
        data-theme={theme}
        className={`min-h-screen ${theme === 'dark' ? 'dark' : ''} bg-[#FFFDF9] dark:bg-[#120E0C] text-slate-900 dark:text-slate-100 font-sans transition-colors duration-300`}
      >
        {gmpQuotaMessage && (
          <div
            role="status"
            aria-live="polite"
            className="sticky top-0 z-50 w-full bg-amber-50 dark:bg-amber-950/95 border-b border-amber-300 dark:border-amber-800 px-3.5 py-2 flex items-center justify-between gap-2 text-xs text-amber-950 dark:text-amber-100 shadow-xs"
          >
            <div className="flex items-center gap-2 min-w-0">
              <AlertTriangle className="w-4 h-4 text-amber-600 dark:text-amber-400 shrink-0" />
              <p className="truncate sm:whitespace-normal leading-snug">
                <span className="font-bold">{gmpQuotaMessage}</span>{' '}
                <a
                  href="https://developers.google.com/maps/ai/ai-studio?utm_campaign=gmp_mcp_codeassist_v1_aistudio#quota_exceeded_errors"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="underline font-semibold text-amber-900 dark:text-amber-200 hover:text-amber-700"
                >
                  maps developer site
                </a>
              </p>
            </div>
            <button
              type="button"
              onClick={() => setGmpQuotaMessage(null)}
              aria-label="Dismiss map notice"
              className="p-1 rounded hover:bg-amber-200/60 dark:hover:bg-amber-900/60 shrink-0 cursor-pointer"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          </div>
        )}
        {currentScreen === 'splash' && (
          <SplashScreen
            duration={2000}
            onFinish={() => setCurrentScreen('login')}
          />
        )}

        {currentScreen === 'login' && (
          <LoginScreen
            onSuccess={() => setCurrentScreen('main')}
          />
        )}

        {currentScreen === 'main' && (
          <MainScreen
            theme={theme}
            onToggleTheme={toggleTheme}
            onLogout={() => setCurrentScreen('login')}
          />
        )}

        {!isOnline && !offlineToastDismissed && (
          <div
            role="status"
            aria-live="polite"
            className="fixed bottom-20 sm:bottom-5 left-1/2 -translate-x-1/2 sm:left-auto sm:right-5 sm:translate-x-0 z-[100] max-w-[92vw] sm:max-w-sm flex items-center gap-2.5 px-3.5 py-2.5 rounded-xl bg-slate-900/95 dark:bg-[#211A17]/95 text-white border border-amber-500/40 shadow-lg backdrop-blur-md animate-in fade-in slide-in-from-bottom-2 duration-200"
          >
            <WifiOff className="w-4 h-4 text-amber-400 shrink-0" />
            <div className="min-w-0 flex-1">
              <p className="text-xs font-bold leading-tight">You&apos;re offline</p>
              <p className="text-[11px] text-slate-300 leading-tight truncate">
                {swReady
                  ? 'Browsing cached pages — live actions resume when online.'
                  : 'Waiting for internet connection to sync live data.'}
              </p>
            </div>
            <button
              type="button"
              onClick={() => setOfflineToastDismissed(true)}
              aria-label="Dismiss offline notice"
              className="p-1 text-slate-400 hover:text-white rounded-md transition-colors cursor-pointer shrink-0"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          </div>
        )}

        {isOnline && showBackOnlineToast && (
          <div
            role="status"
            aria-live="polite"
            className="fixed bottom-20 sm:bottom-5 left-1/2 -translate-x-1/2 sm:left-auto sm:right-5 sm:translate-x-0 z-[100] flex items-center gap-2 px-3.5 py-2 rounded-xl bg-emerald-700 text-white shadow-lg border border-emerald-500/40 animate-in fade-in slide-in-from-bottom-2 duration-200"
          >
            <Wifi className="w-4 h-4 text-emerald-200 shrink-0" />
            <span className="text-xs font-bold">Back online — Live catalog &amp; delivery synced</span>
          </div>
        )}
      </div>
    </AarvoErrorBoundary>
  );
}
