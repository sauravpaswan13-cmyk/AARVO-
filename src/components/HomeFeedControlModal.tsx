import React, { useState } from 'react';
import {
  X,
  ArrowUp,
  ArrowDown,
  RotateCcw,
  Trash2,
  Eye,
  EyeOff,
  SlidersHorizontal,
  Database,
  Sparkles,
  Pin,
  Plus,
  ChevronRight,
} from 'lucide-react';
import { Product } from './ProductDetailModal';
import {
  HomeMerchandisingSectionConfig,
  HomeMerchandisingSectionId,
  MerchandisingThemeKey,
  getRealDiscountPercent,
  selectStillLookingEntries,
  selectBrandsInSpotlight,
  selectTopValueDeals,
  selectSuggestedForYou,
  selectMoreProducts,
} from '../utils/homeMerchandising';
import { haptics } from '../utils/haptics';

interface HomeFeedControlModalProps {
  isOpen: boolean;
  onClose: () => void;
  config: HomeMerchandisingSectionConfig[];
  onChangeConfig: (next: HomeMerchandisingSectionConfig[]) => void;
  catalog: Product[];
  recentlyViewedIds: string[];
  wishlistIds: string[];
  cartProductIds: string[];
  onClearHistory: () => void;
  onResetDefaults: () => void;
  onOpenProduct: (product: Product) => void;
}

const ITEM_COUNT_OPTIONS = [3, 4, 6, 8];

const THEME_SWATCH_MAP: Record<MerchandisingThemeKey, string> = {
  periwinkle_frost: 'from-[#EBF1FF] via-[#F2E9FF] to-[#FFF2EB]',
  pearl_rose_studio: 'from-[#FFFDF9] via-[#FFE4EC] to-[#FFF3E6]',
  golden_amber_grid: 'from-[#FFE082] via-[#FBBF24] to-[#F59E0B]',
  mint_botanical: 'from-[#D1FAE5] via-[#E0F2FE] to-[#FFFDF9]',
  terracotta_sand: 'from-[#FFE4CC] via-[#FFF6EC] to-[#FFFFFF]',
};

export const HomeFeedControlModal: React.FC<HomeFeedControlModalProps> = ({
  isOpen,
  onClose,
  config,
  onChangeConfig,
  catalog,
  recentlyViewedIds,
  wishlistIds,
  cartProductIds,
  onClearHistory,
  onResetDefaults,
  onOpenProduct,
}) => {
  const [expandedPinSection, setExpandedPinSection] =
    useState<HomeMerchandisingSectionId | null>(null);

  if (!isOpen) return null;

  const moveSection = (index: number, direction: -1 | 1) => {
    const targetIdx = index + direction;
    if (targetIdx < 0 || targetIdx >= config.length) return;
    haptics.selection();
    const copy = [...config];
    const [removed] = copy.splice(index, 1);
    copy.splice(targetIdx, 0, removed);
    onChangeConfig(copy.map((s, idx) => ({ ...s, sortOrder: idx })));
  };

  const toggleSection = (id: HomeMerchandisingSectionId) => {
    haptics.selection();
    onChangeConfig(
      config.map((sec) => (sec.id === id ? { ...sec, enabled: !sec.enabled } : sec))
    );
  };

  const setMaxItems = (id: HomeMerchandisingSectionId, maxItems: number) => {
    haptics.selection();
    onChangeConfig(
      config.map((sec) => (sec.id === id ? { ...sec, maxItems } : sec))
    );
  };

  const togglePinProduct = (sectionId: HomeMerchandisingSectionId, productId: string) => {
    haptics.selection();
    onChangeConfig(
      config.map((sec) => {
        if (sec.id !== sectionId) return sec;
        const exists = sec.pinnedProductIds.includes(productId);
        const nextPins = exists
          ? sec.pinnedProductIds.filter((id) => id !== productId)
          : [...sec.pinnedProductIds, productId];
        return { ...sec, pinnedProductIds: nextPins };
      })
    );
  };

  const getPreviewItems = (
    section: HomeMerchandisingSectionConfig
  ): Array<{ product: Product; badge: string; isPinned: boolean }> => {
    switch (section.id) {
      case 'still_looking':
        return selectStillLookingEntries(
          catalog,
          recentlyViewedIds,
          wishlistIds,
          cartProductIds,
          section.maxItems,
          section.pinnedProductIds
        ).map((e) => ({
          product: e.product,
          badge: e.reasonLabel,
          isPinned: e.reason === 'admin',
        }));
      case 'brands_spotlight':
        return selectBrandsInSpotlight(
          catalog,
          section.maxItems,
          section.pinnedProductIds
        ).map((e) => ({
          product: e.product,
          badge: e.isAdminCurated ? 'Admin Pick' : e.headline,
          isPinned: e.isAdminCurated,
        }));
      case 'top_value_deals':
        return selectTopValueDeals(
          catalog,
          section.maxItems,
          section.pinnedProductIds
        ).map((e) => ({
          product: e.product,
          badge: e.isAdminCurated ? 'Admin Pick' : `₹${e.product.price.toLocaleString('en-IN')}`,
          isPinned: e.isAdminCurated,
        }));
      case 'suggested_for_you':
        return selectSuggestedForYou(
          catalog,
          recentlyViewedIds,
          wishlistIds,
          cartProductIds,
          section.maxItems,
          section.pinnedProductIds
        ).map((e) => ({
          product: e.product,
          badge: e.isAdminCurated ? 'Admin Pick' : e.product.category.split('/')[0],
          isPinned: e.isAdminCurated,
        }));
      case 'more_products':
        return selectMoreProducts(
          catalog,
          section.maxItems,
          section.pinnedProductIds
        ).map((e) => ({
          product: e.product,
          badge: e.isAdminCurated ? 'Admin Pick' : 'Catalog',
          isPinned: e.isAdminCurated,
        }));
    }
  };

  const enabledCount = config.filter((s) => s.enabled).length;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4">
      <div
        onClick={onClose}
        className="fixed inset-0 bg-black/60 backdrop-blur-xs"
      />
      <div className="relative w-full max-w-md max-h-[92vh] overflow-hidden bg-[#FFFDF9] dark:bg-[#1E1714] text-slate-900 dark:text-slate-100 border border-orange-200/80 dark:border-stone-800 rounded-3xl shadow-2xl z-10 flex flex-col">
        {/* Header */}
        <div className="px-4 py-3.5 bg-gradient-to-r from-[#FF7043] via-[#FF5722] to-[#E64A19] text-white flex items-center justify-between gap-2 shrink-0">
          <div className="flex items-center gap-2.5 min-w-0">
            <div className="w-9 h-9 rounded-xl bg-white/20 flex items-center justify-center shrink-0">
              <SlidersHorizontal className="w-4.5 h-4.5" />
            </div>
            <div className="min-w-0">
              <h3 className="font-display font-black text-sm sm:text-base leading-tight truncate">
                Admin Merchandising &amp; Feed Controls
              </h3>
              <p className="text-[10px] text-orange-100 truncate">
                {enabledCount}/{config.length} zones active • Distinct backgrounds &amp; live selection
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
              Catalog: <strong className="text-slate-900 dark:text-white">{catalog.length}</strong>
            </span>
            <span className="text-orange-300 dark:text-stone-700">|</span>
            <span className="font-semibold text-slate-700 dark:text-slate-300 whitespace-nowrap">
              Viewed: <strong className="text-[#FF5722]">{recentlyViewedIds.length}</strong>
            </span>
            <span className="text-orange-300 dark:text-stone-700">|</span>
            <span className="font-semibold text-slate-700 dark:text-slate-300 whitespace-nowrap">
              Wishlist: <strong className="text-slate-900 dark:text-white">{wishlistIds.length}</strong>
            </span>
            <span className="text-orange-300 dark:text-stone-700">|</span>
            <span className="font-semibold text-slate-700 dark:text-slate-300 whitespace-nowrap">
              Cart: <strong className="text-slate-900 dark:text-white">{cartProductIds.length}</strong>
            </span>
          </div>
        </div>

        {/* Section Cards */}
        <div className="flex-1 overflow-y-auto p-3.5 space-y-3 no-scrollbar">
          <div className="p-3 rounded-2xl bg-white dark:bg-[#251C18] border border-orange-100 dark:border-stone-800 text-[11px] text-slate-600 dark:text-slate-300 leading-relaxed">
            <p className="font-bold text-slate-900 dark:text-white mb-0.5 flex items-center gap-1.5">
              <Sparkles className="w-3.5 h-3.5 text-[#FF5722]" />
              <span>Admin Managed + Authentic Real-Data Feed</span>
            </p>
            <p>
              Har merchandising zone ka apna alag background design hai. Admin panel ya niche{' '}
              <strong>Pin Products</strong> selector se jo products chune jate hain wo priority me
              dikhte hain — bina kisi fake Free Delivery, AD, ya Razorpay badge ke.
            </p>
          </div>

          {config.map((section, idx) => {
            const previewItems = getPreviewItems(section);
            const isPinningOpen = expandedPinSection === section.id;

            return (
              <div
                key={section.id}
                className={`rounded-2xl border p-3 transition-all ${
                  section.enabled
                    ? 'bg-white dark:bg-[#251C18] border-orange-200/80 dark:border-stone-700 shadow-2xs'
                    : 'bg-stone-50/80 dark:bg-[#191412] border-stone-200/80 dark:border-stone-800/80 opacity-75'
                }`}
              >
                {/* Row 1: Reorder + Title + Theme Swatch + Toggle */}
                <div className="flex items-start justify-between gap-2">
                  <div className="flex items-start gap-2 min-w-0">
                    <div className="flex flex-col gap-0.5 shrink-0 pt-0.5">
                      <button
                        type="button"
                        disabled={idx === 0}
                        onClick={() => moveSection(idx, -1)}
                        aria-label={`Move ${section.title} up`}
                        className="p-1 rounded-lg bg-[#FFF4E8] dark:bg-stone-800 text-slate-700 dark:text-slate-300 disabled:opacity-30 hover:bg-[#FF5722] hover:text-white transition-colors cursor-pointer disabled:cursor-not-allowed"
                      >
                        <ArrowUp className="w-3 h-3" />
                      </button>
                      <button
                        type="button"
                        disabled={idx === config.length - 1}
                        onClick={() => moveSection(idx, 1)}
                        aria-label={`Move ${section.title} down`}
                        className="p-1 rounded-lg bg-[#FFF4E8] dark:bg-stone-800 text-slate-700 dark:text-slate-300 disabled:opacity-30 hover:bg-[#FF5722] hover:text-white transition-colors cursor-pointer disabled:cursor-not-allowed"
                      >
                        <ArrowDown className="w-3 h-3" />
                      </button>
                    </div>
                    <div className="min-w-0">
                      <div className="flex items-center gap-1.5 flex-wrap">
                        <span
                          className={`w-3.5 h-3.5 rounded-md bg-gradient-to-br ${
                            THEME_SWATCH_MAP[section.themeKey]
                          } border border-black/15 shrink-0`}
                          title={section.themeName}
                        />
                        <h4 className="font-display font-black text-xs sm:text-sm text-slate-900 dark:text-white truncate">
                          {section.title}
                        </h4>
                      </div>
                      <p className="text-[10px] text-slate-500 dark:text-slate-400">
                        Theme: {section.themeName}
                      </p>
                    </div>
                  </div>

                  <button
                    type="button"
                    onClick={() => toggleSection(section.id)}
                    className={`shrink-0 px-2.5 py-1 rounded-xl text-[10px] font-extrabold flex items-center gap-1 transition-colors cursor-pointer ${
                      section.enabled
                        ? 'bg-emerald-600 text-white hover:bg-emerald-700'
                        : 'bg-stone-200 dark:bg-stone-800 text-slate-600 dark:text-slate-300'
                    }`}
                  >
                    {section.enabled ? (
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

                {/* Source & Logic */}
                <div className="mt-2 p-2 rounded-xl bg-[#FFF8F1] dark:bg-[#1B1512] border border-orange-100/80 dark:border-stone-800 text-[10px]">
                  <div className="flex items-center gap-1 font-extrabold text-[#E64A19] dark:text-[#FF8A65] mb-0.5">
                    <Database className="w-3 h-3 shrink-0" />
                    <span>Source: {section.sourceLabel}</span>
                  </div>
                  <p className="text-slate-600 dark:text-slate-300 leading-snug">
                    {section.sourceDetails}
                  </p>
                </div>

                {/* Card Count + Pin Products Button */}
                <div className="mt-2.5 flex items-center justify-between gap-2 flex-wrap">
                  <div className="flex items-center gap-1">
                    <span className="text-[10px] font-bold text-slate-600 dark:text-slate-400 mr-1">
                      Cards:
                    </span>
                    {ITEM_COUNT_OPTIONS.map((countOpt) => {
                      const active = section.maxItems === countOpt;
                      return (
                        <button
                          key={countOpt}
                          type="button"
                          onClick={() => setMaxItems(section.id, countOpt)}
                          className={`px-2 py-0.5 rounded-lg text-[10px] font-black transition-colors cursor-pointer ${
                            active
                              ? 'bg-[#FF5722] text-white shadow-2xs'
                              : 'bg-[#FFF4E8] dark:bg-stone-800 text-slate-700 dark:text-slate-300'
                          }`}
                        >
                          {countOpt}
                        </button>
                      );
                    })}
                  </div>

                  <button
                    type="button"
                    onClick={() =>
                      setExpandedPinSection(isPinningOpen ? null : section.id)
                    }
                    className={`px-2.5 py-1 rounded-lg text-[10px] font-extrabold flex items-center gap-1 cursor-pointer transition-colors ${
                      section.pinnedProductIds.length > 0 || isPinningOpen
                        ? 'bg-slate-900 text-white dark:bg-[#FF5722]'
                        : 'bg-[#FFF4E8] dark:bg-stone-800 text-slate-800 dark:text-slate-200'
                    }`}
                  >
                    <Pin className="w-3 h-3" />
                    <span>
                      Admin Pins ({section.pinnedProductIds.length})
                    </span>
                  </button>
                </div>

                {/* Expandable Product Pinning Picker */}
                {isPinningOpen && (
                  <div className="mt-2 p-2.5 rounded-xl bg-slate-50 dark:bg-[#171210] border border-slate-200 dark:border-stone-800 space-y-1.5">
                    <p className="text-[10px] font-bold text-slate-700 dark:text-slate-300">
                      Select products to pin in &ldquo;{section.title}&rdquo;:
                    </p>
                    <div className="max-h-36 overflow-y-auto space-y-1 pr-0.5">
                      {catalog.map((item) => {
                        const isPinned = section.pinnedProductIds.includes(item.id);
                        return (
                          <button
                            key={`${section.id}-pin-${item.id}`}
                            type="button"
                            onClick={() => togglePinProduct(section.id, item.id)}
                            className={`w-full px-2 py-1.5 rounded-lg text-left flex items-center justify-between gap-2 text-[10px] cursor-pointer transition-colors ${
                              isPinned
                                ? 'bg-orange-100/90 dark:bg-orange-950/60 text-[#E64A19] dark:text-orange-300 font-bold border border-orange-300/70 dark:border-orange-800'
                                : 'bg-white dark:bg-[#211A17] text-slate-700 dark:text-slate-300 border border-stone-200/70 dark:border-stone-800'
                            }`}
                          >
                            <div className="flex items-center gap-2 min-w-0">
                              <img
                                src={item.image}
                                alt={item.name}
                                className="w-6 h-6 rounded object-cover shrink-0"
                              />
                              <span className="truncate">{item.name}</span>
                            </div>
                            <span className="shrink-0 flex items-center gap-1 font-extrabold">
                              ₹{item.price.toLocaleString('en-IN')}
                              {isPinned ? (
                                <Pin className="w-3 h-3 fill-current" />
                              ) : (
                                <Plus className="w-3 h-3" />
                              )}
                            </span>
                          </button>
                        );
                      })}
                    </div>
                  </div>
                )}

                {/* Live Selected Output */}
                <div className="mt-2 pt-2 border-t border-orange-100/70 dark:border-stone-800">
                  <div className="flex items-center justify-between text-[10px] mb-1.5">
                    <span className="font-bold text-slate-700 dark:text-slate-300">
                      Live Output ({previewItems.length} shown):
                    </span>
                    <span className="text-slate-400">Tap to open</span>
                  </div>
                  <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar pb-0.5">
                    {previewItems.map(({ product, badge, isPinned }) => {
                      const pct = getRealDiscountPercent(product);
                      return (
                        <button
                          key={`${section.id}-out-${product.id}`}
                          type="button"
                          onClick={() => {
                            onClose();
                            onOpenProduct(product);
                          }}
                          className="shrink-0 flex items-center gap-1.5 px-2 py-1 rounded-xl bg-[#FFF8F1] dark:bg-[#1B1512] border border-orange-100 dark:border-stone-800 hover:border-[#FF5722] text-left cursor-pointer max-w-[185px]"
                        >
                          <img
                            src={product.image}
                            alt={product.name}
                            className="w-6 h-6 rounded-lg object-cover shrink-0 bg-white"
                          />
                          <div className="min-w-0">
                            <p className="text-[10px] font-bold text-slate-800 dark:text-slate-200 truncate">
                              {product.name}
                            </p>
                            <p className="text-[9px] font-extrabold text-[#FF5722] truncate">
                              {isPinned ? '★ Admin • ' : ''}
                              {badge}
                              {pct > 0 ? ` • ↓${pct}%` : ''}
                            </p>
                          </div>
                          <ChevronRight className="w-3 h-3 text-slate-400 shrink-0" />
                        </button>
                      );
                    })}
                  </div>
                </div>
              </div>
            );
          })}
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
