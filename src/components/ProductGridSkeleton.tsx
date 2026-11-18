import React from 'react';

interface ProductCardSkeletonProps {
  variant?: 'detailed' | 'compact';
  className?: string;
}

export const ProductCardSkeleton: React.FC<ProductCardSkeletonProps> = ({
  variant = 'compact',
  className = '',
}) => {
  const isDetailed = variant === 'detailed';

  return (
    <div
      aria-hidden="true"
      className={`bg-white dark:bg-[#211A17] rounded-2xl border border-orange-100/80 dark:border-stone-800/80 shadow-2xs flex flex-col justify-between select-none ${
        isDetailed ? 'p-2.5' : 'p-2'
      } ${className}`}
    >
      {/* Product Image Placeholder */}
      <div className="relative aspect-square w-full rounded-xl overflow-hidden bg-orange-50/80 dark:bg-stone-900 mb-2">
        <div className="w-full h-full bg-gradient-to-br from-orange-100/60 via-orange-50/40 to-amber-100/50 dark:from-stone-800 dark:via-stone-900 dark:to-stone-800 animate-pulse" />

        {/* Discount badge placeholder */}
        <span className="absolute top-1.5 left-1.5 w-11 h-4 rounded-md bg-emerald-200/70 dark:bg-stone-700/80 animate-pulse" />

        {/* Wishlist circle placeholder */}
        <span className="absolute top-1.5 right-1.5 w-6 h-6 rounded-full bg-white/85 dark:bg-stone-800/90 animate-pulse" />

        {/* Rating badge placeholder */}
        <span className="absolute bottom-1.5 left-1.5 w-10 h-4 rounded bg-white/85 dark:bg-stone-800/90 animate-pulse" />
      </div>

      {/* Product Title & Price Placeholders */}
      <div className="space-y-1.5">
        <div className="h-3 w-full rounded-md bg-slate-200/90 dark:bg-stone-700/80 animate-pulse" />
        <div className="h-3 w-3/4 rounded-md bg-slate-200/70 dark:bg-stone-700/60 animate-pulse" />

        <div className="flex items-center gap-1.5 pt-0.5">
          <div className="h-4 w-14 rounded-md bg-slate-300/85 dark:bg-stone-600/85 animate-pulse" />
          <div className="h-2.5 w-10 rounded-md bg-slate-200/75 dark:bg-stone-700/65 animate-pulse" />
        </div>
      </div>

      {/* Card Footer */}
      {isDetailed ? (
        <div className="flex items-center justify-between pt-2 mt-2 border-t border-orange-100/70 dark:border-stone-800/80">
          <div className="h-3 w-16 rounded bg-emerald-100/80 dark:bg-emerald-950/60 animate-pulse" />
          <div className="w-7 h-7 rounded-lg bg-orange-100/80 dark:bg-stone-800 animate-pulse" />
        </div>
      ) : (
        <div className="mt-2.5 w-full h-7 rounded-xl bg-orange-100/80 dark:bg-stone-800 animate-pulse" />
      )}
    </div>
  );
};

interface ProductGridSkeletonProps {
  count?: number;
  columns?: 2 | 3;
  variant?: 'detailed' | 'compact';
  gapClass?: string;
  label?: string;
  className?: string;
}

export const ProductGridSkeleton: React.FC<ProductGridSkeletonProps> = ({
  count = 4,
  columns = 2,
  variant = 'compact',
  gapClass = 'gap-3',
  label = 'Loading products',
  className = '',
}) => {
  const gridColsClass = columns === 3 ? 'grid-cols-3' : 'grid-cols-2';

  return (
    <div
      role="status"
      aria-busy="true"
      aria-label={label}
      className={`grid ${gridColsClass} ${gapClass} ${className}`}
    >
      <span className="sr-only">{label}</span>
      {Array.from({ length: count }).map((_, idx) => (
        <ProductCardSkeleton key={idx} variant={variant} />
      ))}
    </div>
  );
};
