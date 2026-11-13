import React from 'react';

interface MainScreenSkeletonProps {
  className?: string;
}

export const MainScreenSkeleton: React.FC<MainScreenSkeletonProps> = ({ className = '' }) => {
  return (
    <div
      aria-label="Loading content"
      aria-busy="true"
      className={`w-full max-w-[440px] bg-white dark:bg-[#181311] min-h-screen shadow-2xl flex flex-col relative overflow-hidden select-none animate-in fade-in duration-200 ${className}`}
    >
      {/* 1. Header Skeleton - Warm Peach-to-White zone matching AARVO header */}
      <header className="bg-gradient-to-b from-[#FFE5CC] via-[#FFF4E8] to-white dark:from-[#26160C] dark:via-[#1B120E] dark:to-[#181311] pt-[max(0.625rem,env(safe-area-inset-top))] pb-2.5 px-3.5 sticky top-0 z-30 border-b border-orange-100/70 dark:border-orange-950/40">
        <div className="flex items-center gap-2 mb-2.5">
          <div className="w-9 h-9 rounded-full bg-orange-200/60 dark:bg-orange-900/30 animate-pulse shrink-0" />
          <div className="flex-1 h-10 bg-white/95 dark:bg-[#231C18] rounded-full flex items-center px-3 shadow-2xs border border-orange-200/60 dark:border-orange-900/40 animate-pulse">
            <div className="w-4 h-4 rounded-full bg-slate-200 dark:bg-slate-700 mr-2.5" />
            <div className="w-36 h-3 rounded-full bg-slate-200 dark:bg-slate-700" />
          </div>
          <div className="w-9 h-9 rounded-full bg-orange-200/60 dark:bg-orange-900/30 animate-pulse shrink-0" />
        </div>

        {/* Centered AARVO Logo placeholder */}
        <div className="flex items-center justify-center py-1.5 gap-2">
          <div className="w-7 h-7 rounded-lg bg-orange-300/70 dark:bg-orange-900/50 animate-pulse" />
          <div className="w-24 h-6 rounded-md bg-orange-300/70 dark:bg-orange-900/50 animate-pulse" />
        </div>

        {/* Horizontal Category Filter Bar Skeleton */}
        <div className="mt-2 -mx-3.5 px-3.5 flex items-center gap-1.5 overflow-hidden py-0.5">
          <div className="h-6 w-14 rounded-full bg-orange-400/50 animate-pulse shrink-0" />
          <div className="h-6 w-20 rounded-full bg-orange-100 dark:bg-slate-800 animate-pulse shrink-0" />
          <div className="h-6 w-16 rounded-full bg-orange-100 dark:bg-slate-800 animate-pulse shrink-0" />
          <div className="h-6 w-20 rounded-full bg-orange-100 dark:bg-slate-800 animate-pulse shrink-0" />
        </div>
      </header>

      {/* Main Scrollable Content Skeleton */}
      <main className="flex-1 pb-20 space-y-3 overflow-hidden">
        {/* Hero Banner */}
        <section className="px-3.5 pt-2.5">
          <div className="aspect-[16/9] w-full rounded-2xl bg-gradient-to-r from-blue-600/30 via-blue-400/20 to-indigo-500/30 dark:from-slate-800 dark:to-slate-700 animate-pulse p-4 flex flex-col justify-center">
            <div className="w-36 h-5 bg-white/50 dark:bg-slate-600 rounded mb-2" />
            <div className="w-28 h-5 bg-white/50 dark:bg-slate-600 rounded mb-3" />
            <div className="w-24 h-7 bg-amber-300/70 rounded-full" />
          </div>
        </section>

        {/* Shop by Category Skeleton */}
        <section className="px-3.5 py-1">
          <div className="flex items-center justify-between mb-2.5">
            <div className="w-32 h-4 bg-slate-200 dark:bg-slate-800 rounded animate-pulse" />
            <div className="w-12 h-3 bg-orange-200 dark:bg-orange-900/40 rounded animate-pulse" />
          </div>
          <div className="grid grid-cols-5 gap-2">
            {Array.from({ length: 5 }).map((_, idx) => (
              <div key={idx} className="flex flex-col items-center">
                <div className="w-full aspect-square rounded-2xl bg-orange-50 dark:bg-slate-800/80 animate-pulse border border-orange-100/60 dark:border-slate-800" />
                <div className="w-10 h-2 bg-slate-200 dark:bg-slate-700 rounded-full mt-1.5 animate-pulse" />
              </div>
            ))}
          </div>
        </section>

        {/* Promo Strip Skeleton */}
        <section className="px-3.5">
          <div className="aspect-[21/9] rounded-2xl bg-gradient-to-r from-amber-300/50 via-orange-300/40 to-orange-400/50 dark:from-slate-800 dark:to-slate-700 p-4 flex items-center animate-pulse">
            <div className="space-y-2">
              <div className="w-32 h-4 bg-white/60 rounded" />
              <div className="w-20 h-6 bg-slate-900/30 rounded-full" />
            </div>
          </div>
        </section>

        {/* Product Cards Grid Skeleton */}
        <section className="px-3.5 space-y-2.5">
          <div className="flex items-center justify-between">
            <div className="w-28 h-4 bg-slate-200 dark:bg-slate-800 rounded animate-pulse" />
            <div className="w-12 h-3 bg-orange-200 dark:bg-orange-900/40 rounded animate-pulse" />
          </div>

          <div className="grid grid-cols-2 gap-3">
            {Array.from({ length: 4 }).map((_, idx) => (
              <div
                key={idx}
                className="rounded-2xl border border-slate-100 dark:border-slate-800/80 p-2.5 bg-white dark:bg-[#211A17] shadow-2xs flex flex-col justify-between"
              >
                <div className="w-full aspect-square rounded-xl bg-slate-100 dark:bg-slate-800/90 animate-pulse" />
                <div className="mt-2.5 space-y-1.5">
                  <div className="w-full h-3 bg-slate-200 dark:bg-slate-700 rounded animate-pulse" />
                  <div className="w-2/3 h-2.5 bg-slate-200 dark:bg-slate-700/60 rounded animate-pulse" />
                </div>
                <div className="mt-2.5 w-20 h-4 bg-slate-300 dark:bg-slate-600 rounded animate-pulse" />
              </div>
            ))}
          </div>
        </section>
      </main>

      {/* Bottom Navigation Skeleton */}
      <footer className="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[440px] bg-white dark:bg-[#181311] border-t border-slate-200/90 dark:border-slate-800 py-2.5 px-3 flex items-center justify-around shadow-2xl z-40">
        {Array.from({ length: 5 }).map((_, idx) => (
          <div key={idx} className="flex flex-col items-center gap-1">
            <div className="w-5 h-5 rounded-md bg-slate-200 dark:bg-slate-700 animate-pulse" />
            <div className="w-8 h-2 rounded bg-slate-200 dark:bg-slate-700 animate-pulse" />
          </div>
        ))}
      </footer>
    </div>
  );
};
