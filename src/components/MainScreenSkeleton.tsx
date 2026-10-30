import React from 'react';

interface MainScreenSkeletonProps {
  className?: string;
}

export const MainScreenSkeleton: React.FC<MainScreenSkeletonProps> = ({ className = '' }) => {
  return (
    <div
      aria-label="Loading content"
      aria-busy="true"
      className={`w-full max-w-[440px] bg-white dark:bg-[#120F26] min-h-screen shadow-2xl flex flex-col relative overflow-hidden select-none animate-in fade-in duration-200 ${className}`}
    >
      {/* 1. Header Skeleton - Royal Blue zone matching AARVO header */}
      <header className="bg-[#2418D8] text-white pt-2.5 pb-3.5 px-4 sticky top-0 z-30 shadow-md">
        <div className="flex items-center justify-between gap-2 mb-2.5">
          {/* Menu icon placeholder */}
          <div className="w-9 h-9 rounded-xl bg-white/20 animate-pulse" />

          {/* Logo & title placeholder */}
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-full bg-white/25 animate-pulse" />
            <div className="w-20 h-5 rounded-md bg-white/25 animate-pulse" />
          </div>

          {/* Right utility buttons (Theme, Bell, Cart) */}
          <div className="flex items-center gap-1.5">
            <div className="w-8 h-8 rounded-xl bg-white/20 animate-pulse" />
            <div className="w-8 h-8 rounded-xl bg-white/20 animate-pulse" />
            <div className="w-8 h-8 rounded-xl bg-white/20 animate-pulse" />
          </div>
        </div>

        {/* Big Pill Search Bar Skeleton */}
        <div className="w-full h-11 sm:h-12 bg-white/95 dark:bg-[#1A1638] rounded-full flex items-center px-4 shadow-sm animate-pulse">
          <div className="w-4 h-4 rounded-full bg-slate-300 dark:bg-slate-700 mr-3" />
          <div className="w-44 h-3 rounded-full bg-slate-200 dark:bg-slate-700" />
          <div className="ml-auto w-6 h-6 rounded-full bg-slate-200 dark:bg-slate-700" />
        </div>

        {/* Horizontal Category Filter Bar Skeleton */}
        <div className="mt-2.5 -mx-4 px-4 flex items-center gap-2 overflow-hidden py-0.5">
          <div className="h-7 w-20 rounded-full bg-white/35 animate-pulse shrink-0" />
          <div className="h-7 w-24 rounded-full bg-white/20 animate-pulse shrink-0" />
          <div className="h-7 w-20 rounded-full bg-white/20 animate-pulse shrink-0" />
          <div className="h-7 w-22 rounded-full bg-white/20 animate-pulse shrink-0" />
          <div className="h-7 w-28 rounded-full bg-white/20 animate-pulse shrink-0" />
        </div>
      </header>

      {/* Main Scrollable Content Skeleton */}
      <main className="flex-1 pb-20 space-y-3.5 overflow-hidden">
        {/* 2. Hero Banner Slider Skeleton */}
        <section className="px-3 pt-3">
          <div className="w-full h-38 sm:h-44 rounded-2xl bg-gradient-to-r from-slate-200 via-slate-100 to-slate-200 dark:from-slate-800 dark:via-slate-700/60 dark:to-slate-800 animate-pulse relative overflow-hidden shadow-sm flex flex-col justify-end p-4">
            <div className="w-20 h-4 bg-white/40 dark:bg-slate-600 rounded-full mb-2" />
            <div className="w-48 h-5 bg-white/50 dark:bg-slate-600 rounded-md mb-1.5" />
            <div className="w-32 h-3.5 bg-white/35 dark:bg-slate-600 rounded-md" />
            {/* Dots indicator skeleton */}
            <div className="flex items-center justify-center gap-1.5 mt-3">
              <div className="w-5 h-1.5 rounded-full bg-white/70 dark:bg-slate-500" />
              <div className="w-1.5 h-1.5 rounded-full bg-white/40 dark:bg-slate-600" />
              <div className="w-1.5 h-1.5 rounded-full bg-white/40 dark:bg-slate-600" />
            </div>
          </div>
        </section>

        {/* 3. 10 Circular Category Icons Skeleton */}
        <section className="px-3 py-1">
          <div className="grid grid-cols-5 gap-y-3.5 gap-x-2">
            {Array.from({ length: 10 }).map((_, idx) => (
              <div key={idx} className="flex flex-col items-center">
                <div className="w-13 h-13 sm:w-14 sm:h-14 rounded-full bg-slate-100 dark:bg-slate-800/80 animate-pulse border border-slate-200/50 dark:border-slate-800 flex items-center justify-center">
                  <div className="w-6 h-6 rounded-full bg-slate-200 dark:bg-slate-700" />
                </div>
                <div className="w-10 h-2 bg-slate-200 dark:bg-slate-700 rounded-full mt-2 animate-pulse" />
              </div>
            ))}
          </div>
        </section>

        {/* 4. Top Deals For You Banner Strip Skeleton */}
        <section className="px-3">
          <div className="h-16 rounded-2xl bg-gradient-to-r from-blue-100 via-indigo-50 to-purple-100 dark:from-slate-800 dark:via-slate-800/80 dark:to-slate-800 p-3 flex items-center justify-between shadow-xs animate-pulse">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-orange-200 dark:bg-orange-900/40" />
              <div className="space-y-1.5">
                <div className="w-28 h-3.5 bg-slate-300 dark:bg-slate-600 rounded" />
                <div className="w-36 h-2.5 bg-slate-200 dark:bg-slate-700 rounded" />
              </div>
            </div>
            <div className="w-6 h-6 rounded-full bg-amber-200 dark:bg-amber-900/40" />
          </div>
        </section>

        {/* 5. Section Header & 4 Product Cards Grid Skeleton */}
        <section className="px-3 space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <div className="w-5 h-5 rounded-full bg-orange-200 dark:bg-orange-900/40 animate-pulse" />
              <div className="w-32 h-4 bg-slate-200 dark:bg-slate-800 rounded animate-pulse" />
            </div>
            <div className="w-16 h-3 bg-slate-200 dark:bg-slate-800 rounded animate-pulse" />
          </div>

          <div className="grid grid-cols-2 gap-3">
            {Array.from({ length: 4 }).map((_, idx) => (
              <div
                key={idx}
                className="rounded-2xl border border-slate-100 dark:border-slate-800/80 p-2.5 bg-white dark:bg-[#181436] shadow-xs flex flex-col justify-between"
              >
                {/* Product Image Skeleton */}
                <div className="w-full aspect-square rounded-xl bg-slate-100 dark:bg-slate-800/90 animate-pulse relative overflow-hidden flex items-center justify-center">
                  <div className="w-10 h-10 rounded-lg bg-slate-200 dark:bg-slate-700/60" />
                  <div className="absolute top-2 left-2 w-12 h-3.5 rounded bg-emerald-100 dark:bg-emerald-950/50" />
                </div>

                {/* Title & Brand Skeleton */}
                <div className="mt-2.5 space-y-1.5">
                  <div className="w-full h-3 bg-slate-200 dark:bg-slate-700 rounded animate-pulse" />
                  <div className="w-2/3 h-2.5 bg-slate-200 dark:bg-slate-700/60 rounded animate-pulse" />
                </div>

                {/* Rating & Price */}
                <div className="mt-2.5 space-y-2">
                  <div className="flex items-center gap-2">
                    <div className="w-14 h-4 bg-slate-200 dark:bg-slate-700 rounded" />
                    <div className="w-10 h-3 bg-slate-100 dark:bg-slate-800 rounded" />
                  </div>
                  <div className="w-20 h-4 bg-slate-300 dark:bg-slate-600 rounded" />
                </div>

                {/* Add to Cart button skeleton */}
                <div className="mt-3 w-full h-8 rounded-xl bg-slate-200 dark:bg-slate-800 animate-pulse" />
              </div>
            ))}
          </div>
        </section>
      </main>

      {/* 6. Bottom Navigation Skeleton */}
      <footer className="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[440px] bg-white dark:bg-[#120F26] border-t border-slate-200/90 dark:border-slate-800 py-2.5 px-3 flex items-center justify-around shadow-2xl z-40">
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
