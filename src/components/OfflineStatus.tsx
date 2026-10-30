import React, { useState, useEffect } from 'react';
import { Wifi, WifiOff, X, RefreshCw } from 'lucide-react';
import { haptics } from '../utils/haptics';

interface OfflineStatusProps {
  className?: string;
}

export const OfflineStatus: React.FC<OfflineStatusProps> = ({ className = '' }) => {
  const [isOnline, setIsOnline] = useState<boolean>(() => {
    return typeof navigator !== 'undefined' ? navigator.onLine : true;
  });
  const [showToast, setShowToast] = useState<boolean>(false);
  const [isDismissed, setIsDismissed] = useState<boolean>(false);
  const [isReconnectedToast, setIsReconnectedToast] = useState<boolean>(false);

  useEffect(() => {
    if (typeof window === 'undefined') return;

    let reconnectTimer: NodeJS.Timeout | null = null;

    const handleOnline = () => {
      setIsOnline(true);
      setIsDismissed(false);
      setIsReconnectedToast(true);
      setShowToast(true);

      // Auto-hide the "Back online" toast after 3 seconds
      if (reconnectTimer) clearTimeout(reconnectTimer);
      reconnectTimer = setTimeout(() => {
        setShowToast(false);
        setIsReconnectedToast(false);
      }, 3000);
    };

    const handleOffline = () => {
      setIsOnline(false);
      setIsDismissed(false);
      setIsReconnectedToast(false);
      setShowToast(true);
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    // Initial check: if already offline upon load, show the offline toast
    if (!navigator.onLine) {
      setShowToast(true);
    }

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
      if (reconnectTimer) clearTimeout(reconnectTimer);
    };
  }, []);

  // When online and not in reconnected flash mode, or if user dismissed it, don't show anything
  if (!showToast || isDismissed) {
    return null;
  }

  const handleRetry = () => {
    haptics.light();
    if (navigator.onLine) {
      setIsOnline(true);
      setIsReconnectedToast(true);
      setTimeout(() => setShowToast(false), 2000);
    } else {
      setShowToast(false);
      setTimeout(() => setShowToast(true), 150);
    }
  };

  return (
    <aside
      aria-label="Network Status"
      aria-live="polite"
      className={`fixed bottom-20 left-1/2 -translate-x-1/2 z-40 max-w-[92%] sm:max-w-md w-max pointer-events-auto animate-in fade-in slide-in-from-bottom-2 duration-300 ${className}`}
    >
      {isReconnectedToast ? (
        // Subtle "Back Online" Toast Pill
        <div className="flex items-center gap-2.5 px-3.5 py-2 rounded-full bg-emerald-900/95 dark:bg-emerald-950/95 text-emerald-100 text-xs font-semibold shadow-xl border border-emerald-500/40 backdrop-blur-md">
          <div className="w-5 h-5 rounded-full bg-emerald-800/80 flex items-center justify-center shrink-0">
            <Wifi className="w-3.5 h-3.5 text-emerald-300 stroke-[2.5]" />
          </div>
          <span className="text-[11px] sm:text-xs">
            Back online &bull; Connection restored
          </span>
          <button
            onClick={() => setShowToast(false)}
            aria-label="Dismiss status toast"
            className="p-1 -mr-1 hover:bg-emerald-800/50 rounded-full transition-colors text-emerald-200"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      ) : (
        // Subtle "Offline Mode" Toast Pill
        <div className="flex items-center gap-2.5 px-3.5 py-2 rounded-full bg-slate-900/95 dark:bg-[#181436]/95 text-slate-100 text-xs font-semibold shadow-2xl border border-amber-500/40 dark:border-amber-400/30 backdrop-blur-md">
          <div className="relative flex items-center justify-center w-5 h-5 rounded-full bg-amber-500/20 shrink-0">
            <span className="absolute w-2 h-2 rounded-full bg-amber-500 animate-ping opacity-75" />
            <WifiOff className="w-3.5 h-3.5 text-amber-400 stroke-[2.5] relative z-10" />
          </div>
          
          <div className="flex flex-col sm:flex-row sm:items-center sm:gap-1.5 leading-tight">
            <span className="text-amber-400 font-bold text-[11px] sm:text-xs">
              Offline Mode
            </span>
            <span className="hidden sm:inline text-slate-400">&bull;</span>
            <span className="text-[10px] sm:text-[11px] text-slate-300">
              Cached items available
            </span>
          </div>

          <div className="flex items-center gap-1 ml-1">
            <button
              onClick={handleRetry}
              aria-label="Retry connection"
              title="Check Connection"
              className="px-2 py-0.5 rounded-full bg-slate-800 hover:bg-slate-700 text-amber-300 hover:text-white text-[10px] font-bold border border-slate-700 transition-colors flex items-center gap-1 active:scale-95"
            >
              <RefreshCw className="w-2.5 h-2.5" />
              <span>Retry</span>
            </button>

            <button
              onClick={() => setIsDismissed(true)}
              aria-label="Dismiss offline notification"
              className="p-1 hover:bg-slate-800 rounded-full text-slate-400 hover:text-white transition-colors"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      )}
    </aside>
  );
};
