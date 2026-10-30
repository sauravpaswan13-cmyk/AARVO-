import React, { useState, useEffect, useRef } from 'react';
import { SplashScreen } from './components/SplashScreen';
import { LoginScreen } from './components/LoginScreen';
import { MainScreen } from './components/MainScreen';
import { haptics } from './utils/haptics';

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
      'color: #2418D8; font-weight: bold; padding: 2px 4px; border-radius: 4px;',
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
          'color: #2418D8; font-weight: bold; padding: 2px 4px; border-radius: 4px;',
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

  return (
    <div
      data-theme={theme}
      className={`min-h-screen ${theme === 'dark' ? 'dark' : ''} bg-[#F7F5FF] dark:bg-[#0A0718] text-slate-900 dark:text-slate-100 font-sans transition-colors duration-300`}
    >
      {currentScreen === 'splash' && (
        <SplashScreen
          duration={1300}
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
    </div>
  );
}
