import React from 'react';
import {
  X,
  RotateCcw,
  Trash2,
  Eye,
  EyeOff,
  SlidersHorizontal,
  Database,
  Clock,
  Heart,
  ShoppingCart,
  TrendingDown,
  ChevronRight,
} from 'lucide-react';
import { Product } from './ProductDetailModal';
import {
  StillLookingConfig,
  StillLookingEntry,
  getRealDiscountPercent,
} from '../utils/homeMerchandising';
import { haptics } from '../utils/haptics';

interface HomeFeedControlModalProps {
  isOpen: boolean;
  onClose: () => void;
  config: StillLookingConfig;
  onChangeConfig: (next: StillLookingConfig) => void;
  entries: StillLookingEntry[];
  recentlyViewedCount: number;
  wishlistCount: number;
  cartCount: number;
  onClearHistory: () => void;
  onResetDefaults: () => void;
  onOpenProduct: (product: Product) => void;
}

const ITEM_COUNT_OPTIONS = [3, 4, 6];

export const HomeFeedControlModal: React.FC<HomeFeedControlModalProps> = ({
  isOpen,
  onClose,
  config,
  onChangeConfig,
  entries,
  recentlyViewedCount,
  wishlistCount,
  cartCount,
  onClearHistory,
  onResetDefaults,
  onOpenProduct,
}) => {
  if (!isOpen) return null;

  const reasonBadgeStyle = (reason: StillLookingEntry['reason']) => {
    switch (reason) {
      case 'viewed':
        return 'bg-orange-100 text-[#E64A19] dark:bg-orange-950/60 dark:text-orange-300';
      case 'wishlist':
        return 'bg-rose-100 text-rose-700 dark:bg-rose-950/60 dark:text-rose-300';
      case 'cart':
        return 'bg-emerald-100 text-emerald-700 dark:bg-emerald-950/60 dark:text-emerald-300';
      case 'catalog':
        return 'bg-slate-100 text-slate-700 dark:bg-stone-800 dark:text-slate-300';
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4">
      <div
        onClick={onClose}
        className="fixed inset-0 bg-black/60 backdrop-blur-xs"
      />
      <div className="relative w-full max-w-md max-h-[92vh] overflow-hidden bg-[#FFFDF9] dark:bg-[#1E1714] text-slate-900 dark:text-slate-100 border border-orange-200/80 dark:border-stone-800 rounded-3xl shadow-2xl z-10 flex flex-col">
        {/* Sticky Header */}
        <div className="px-4 py-3.5 bg-gradient-to-r from-[#FF7043] via-[#FF5722] to-[#E64A19] text-white flex items-center justify-between gap-2 shrink-0">
          <div className="flex items-center gap-2.5 min-w-0">
            <div className="w-9 h-9 rounded-xl bg-white/20 flex items-center justify-center shrink-0">
              <SlidersHorizontal className="w-4.5 h-4.5" />
            </div>
            <div className="min-w-0">
              <h3 className="font-display font-black text-sm sm:text-base leading-tight truncate">
                &ldquo;Still looking for these?&rdquo; Controls
              </h3>
              <p className="text-[10px] text-orange-100 truncate">
                Kaha se aur kaise products is section me dikhte hain
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close Controls"
            className="p-1.5 rounded-xl bg-white/15 hover:bg-white/25 text-white cursor-pointer shrink-0"
          >
            <X className="w-4.5 h-4.5" />
          </button>
        </div>

        {/* Live Signal Summary Strip */}
        <div className="px-4 py-2.5 bg-[#FFF4E8] dark:bg-[#261B16] border-b border-orange-100 dark:border-stone-800 flex items-center justify-between gap-2 text-[11px] shrink-0">
          <div className="flex items-center gap-3 overflow-x-auto no-scrollbar">
            <span className="font-semibold text-slate-700 dark:text-slate-300 whitespace-nowrap">
              Viewed: <strong className="text-[#FF5722]">{recentlyViewedCount}</strong>
            </span>
            <span className="text-orange-300 dark:text-stone-700">|</span>
            <span className="font-semibold text-slate-700 dark:text-slate-300 whitespace-nowrap">
              Wishlist: <strong className="text-slate-900 dark:text-white">{wishlistCount}</strong>
            </span>
            <span className="text-orange-300 dark:text-stone-700">|</span>
            <span className="font-semibold text-slate-700 dark:text-slate-300 whitespace-nowrap">
              Cart: <strong className="text-slate-900 dark:text-white">{cartCount}</strong>
            </span>
          </div>
          <button
            type="button"
            onClick={() => {
              haptics.selection();
              onChangeConfig({ ...config, enabled: !config.enabled });
            }}
            className={`shrink-0 px-2.5 py-1 rounded-xl text-[10px] font-extrabold flex items-center gap-1 transition-colors cursor-pointer ${
              config.enabled
                ? 'bg-emerald-600 text-white hover:bg-emerald-700'
                : 'bg-stone-200 dark:bg-stone-800 text-slate-600 dark:text-slate-300'
            }`}
          >
            {config.enabled ? (
              <>
                <Eye className="w-3 h-3" />
                <span>Visible</span>
              </>
            ) : (
              <>
                <EyeOff className="w-3 h-3" />
                <span>Hidden</span>
              </>
            )}
          </button>
        </div>

        {/* Scrollable Content */}
        <div className="flex-1 overflow-y-auto p-3.5 space-y-3 no-scrollbar">
          {/* Architecture Explanation: Kaha Se & Kaise */}
          <div className="p-3 rounded-2xl bg-white dark:bg-[#251C18] border border-orange-100 dark:border-stone-800 space-y-2 text-[11px]">
            <div className="flex items-center gap-1.5 font-extrabold text-[#E64A19] dark:text-[#FF8A65]">
              <Database className="w-3.5 h-3.5 shrink-0" />
              <span>Kaha Se Aur Kaise Kaam Karta Hai (Priority Order)</span>
            </div>
            <ol className="space-y-1.5 text-slate-600 dark:text-slate-300">
              <li className="flex items-start gap-2">
                <Clock className="w-3.5 h-3.5 text-[#FF5722] shrink-0 mt-0.5" />
                <span>
                  <strong className="text-slate-900 dark:text-white">1. Recently Viewed:</strong>{' '}
                  Jab bhi aap koi product open karte hain, wo{' '}
                  <code className="px-1 py-0.2 rounded bg-orange-50 dark:bg-stone-800 text-[10px]">
                    localStorage
                  </code>{' '}
                  history me sabse aage save hota hai.
                </span>
              </li>
              <li className="flex items-start gap-2">
                <Heart className="w-3.5 h-3.5 text-rose-500 shrink-0 mt-0.5" />
                <span>
                  <strong className="text-slate-900 dark:text-white">2. Wishlist Items:</strong>{' '}
                  Agar history me kam items hon to saved Wishlist products automatically fill hote hain.
                </span>
              </li>
              <li className="flex items-start gap-2">
                <ShoppingCart className="w-3.5 h-3.5 text-emerald-600 shrink-0 mt-0.5" />
                <span>
                  <strong className="text-slate-900 dark:text-white">3. Cart Items:</strong>{' '}
                  Checkout pending cart items yaad dilane ke liye add kiye jate hain.
                </span>
              </li>
              <li className="flex items-start gap-2">
                <TrendingDown className="w-3.5 h-3.5 text-blue-600 shrink-0 mt-0.5" />
                <span>
                  <strong className="text-slate-900 dark:text-white">4. Price-Drop Fallback:</strong>{' '}
                  Naye user ke liye empty card na dikhe, isliye highest real{' '}
                  <code className="px-1 py-0.2 rounded bg-orange-50 dark:bg-stone-800 text-[10px]">
                    ↓X%
                  </code>{' '}
                  discount wale in-stock items dikhte hain.
                </span>
              </li>
            </ol>
          </div>

          {/* Toggle Sources & Card Count */}
          <div className="p-3 rounded-2xl bg-white dark:bg-[#251C18] border border-orange-100 dark:border-stone-800 space-y-2.5">
            <h4 className="font-display font-black text-xs text-slate-900 dark:text-white">
              Display &amp; Source Controls
            </h4>

            <div className="flex items-center justify-between text-xs">
              <span className="font-semibold text-slate-700 dark:text-slate-300">
                Cards to show on Home
              </span>
              <div className="flex items-center gap-1">
                {ITEM_COUNT_OPTIONS.map((countOpt) => {
                  const active = config.maxItems === countOpt;
                  return (
                    <button
                      key={countOpt}
                      type="button"
                      onClick={() => {
                        haptics.selection();
                        onChangeConfig({ ...config, maxItems: countOpt });
                      }}
                      className={`px-2.5 py-1 rounded-lg text-[11px] font-black transition-colors cursor-pointer ${
                        active
                          ? 'bg-[#FF5722] text-white shadow-2xs'
                          : 'bg-[#FFF4E8] dark:bg-stone-800 text-slate-700 dark:text-slate-300 hover:bg-orange-100'
                      }`}
                    >
                      {countOpt}
                    </button>
                  );
                })}
              </div>
            </div>

            <div className="divide-y divide-orange-100/70 dark:divide-stone-800 text-xs">
              {[
                {
                  key: 'includeWishlist' as const,
                  label: 'Include Wishlist Products',
                  desc: 'Show saved wishlist items in slots',
                },
                {
                  key: 'includeCart' as const,
                  label: 'Include Cart Products',
                  desc: 'Remind user about items left in cart',
                },
                {
                  key: 'includeCatalogFallback' as const,
                  label: 'Top Discount Fallback (New Users)',
                  desc: 'Fill remaining slots with top real price-drops',
                },
              ].map((toggleItem) => {
                const enabled = config[toggleItem.key];
                return (
                  <div
                    key={toggleItem.key}
                    className="py-2 flex items-center justify-between gap-2"
                  >
                    <div className="min-w-0">
                      <span className="font-bold text-slate-800 dark:text-slate-100 block text-[11px]">
                        {toggleItem.label}
                      </span>
                      <span className="text-[10px] text-slate-500 dark:text-slate-400 block">
                        {toggleItem.desc}
                      </span>
                    </div>
                    <button
                      type="button"
                      onClick={() => {
                        haptics.selection();
                        onChangeConfig({ ...config, [toggleItem.key]: !enabled });
                      }}
                      className={`px-2.5 py-1 rounded-lg text-[10px] font-extrabold shrink-0 cursor-pointer transition-colors ${
                        enabled
                          ? 'bg-[#FF5722] text-white'
                          : 'bg-stone-200 dark:bg-stone-800 text-slate-600 dark:text-slate-400'
                      }`}
                    >
                      {enabled ? 'ON' : 'OFF'}
                    </button>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Live Selected Output */}
          <div className="p-3 rounded-2xl bg-white dark:bg-[#251C18] border border-orange-100 dark:border-stone-800 space-y-2">
            <div className="flex items-center justify-between">
              <h4 className="font-display font-black text-xs text-slate-900 dark:text-white">
                Live Products Currently Shown ({entries.length})
              </h4>
              <span className="text-[10px] text-slate-400">Tap to open</span>
            </div>

            {entries.length === 0 ? (
              <p className="text-[11px] text-slate-500 dark:text-slate-400 py-2">
                Koi product nahi mila. Kisi product par tap karein ya fallback ON rakhein.
              </p>
            ) : (
              <div className="space-y-1.5">
                {entries.map((entry, i) => {
                  const pct = getRealDiscountPercent(entry.product);
                  return (
                    <button
                      key={`sl-preview-${entry.product.id}`}
                      type="button"
                      onClick={() => {
                        onClose();
                        onOpenProduct(entry.product);
                      }}
                      className="w-full p-2 rounded-xl bg-[#FFF8F1] dark:bg-[#1B1512] border border-orange-100 dark:border-stone-800 hover:border-[#FF5722] flex items-center justify-between gap-2 text-left cursor-pointer transition-colors"
                    >
                      <div className="flex items-center gap-2 min-w-0">
                        <span className="text-[10px] font-black text-slate-400 w-4 shrink-0">
                          #{i + 1}
                        </span>
                        <img
                          src={entry.product.image}
                          alt={entry.product.name}
                          className="w-9 h-9 rounded-lg object-cover shrink-0 bg-white"
                        />
                        <div className="min-w-0">
                          <p className="text-[11px] font-bold text-slate-900 dark:text-white truncate">
                            {entry.product.name}
                          </p>
                          <div className="flex items-center gap-1.5 mt-0.5">
                            <span
                              className={`text-[9px] font-extrabold px-1.5 py-0.2 rounded ${reasonBadgeStyle(
                                entry.reason
                              )}`}
                            >
                              {entry.reasonLabel}
                            </span>
                            <span className="text-[10px] font-black text-slate-800 dark:text-slate-200">
                              ₹{entry.product.price.toLocaleString('en-IN')}
                            </span>
                            {pct > 0 && (
                              <span className="text-[9px] font-black text-blue-600 dark:text-blue-400">
                                ↓{pct}%
                              </span>
                            )}
                          </div>
                        </div>
                      </div>
                      <ChevronRight className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                    </button>
                  );
                })}
              </div>
            )}
          </div>
        </div>

        {/* Sticky Footer */}
        <div className="px-4 py-3 bg-white dark:bg-[#211A17] border-t border-orange-100 dark:border-stone-800 flex items-center gap-2 shrink-0">
          <button
            type="button"
            onClick={() => {
              haptics.light();
              onClearHistory();
            }}
            className="flex-1 py-2 px-3 rounded-xl bg-[#FFF4E8] hover:bg-orange-100 dark:bg-stone-800 dark:hover:bg-stone-700 text-slate-800 dark:text-slate-200 text-[11px] font-bold flex items-center justify-center gap-1.5 cursor-pointer transition-colors"
          >
            <Trash2 className="w-3.5 h-3.5 text-rose-500" />
            <span>Clear Viewed History</span>
          </button>
          <button
            type="button"
            onClick={() => {
              haptics.medium();
              onResetDefaults();
            }}
            className="flex-1 py-2 px-3 rounded-xl bg-slate-900 hover:bg-slate-800 dark:bg-[#FF5722] dark:hover:bg-[#E64A19] text-white text-[11px] font-bold flex items-center justify-center gap-1.5 cursor-pointer transition-colors"
          >
            <RotateCcw className="w-3.5 h-3.5" />
            <span>Reset Defaults</span>
          </button>
        </div>
      </div>
    </div>
  );
};
