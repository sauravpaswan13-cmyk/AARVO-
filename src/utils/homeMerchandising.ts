import { Product } from '../components/ProductDetailModal';

export interface StillLookingConfig {
  enabled: boolean;
  maxItems: number;
  includeWishlist: boolean;
  includeCart: boolean;
  includeCatalogFallback: boolean;
}

export interface StillLookingEntry {
  product: Product;
  reason: 'viewed' | 'wishlist' | 'cart' | 'catalog';
  reasonLabel: string;
}

export const STILL_LOOKING_CONFIG_KEY = 'aarvo_still_looking_config_v1';
export const RECENTLY_VIEWED_KEY = 'aarvo_recently_viewed_v1';

export const DEFAULT_STILL_LOOKING_CONFIG: StillLookingConfig = {
  enabled: true,
  maxItems: 3,
  includeWishlist: true,
  includeCart: true,
  includeCatalogFallback: true,
};

export function loadStillLookingConfig(): StillLookingConfig {
  if (typeof window === 'undefined') return DEFAULT_STILL_LOOKING_CONFIG;
  try {
    const raw = window.localStorage.getItem(STILL_LOOKING_CONFIG_KEY);
    if (!raw) return DEFAULT_STILL_LOOKING_CONFIG;
    const parsed = JSON.parse(raw) as Partial<StillLookingConfig>;
    return {
      enabled:
        typeof parsed.enabled === 'boolean' ? parsed.enabled : DEFAULT_STILL_LOOKING_CONFIG.enabled,
      maxItems:
        typeof parsed.maxItems === 'number' && [3, 4, 6].includes(parsed.maxItems)
          ? parsed.maxItems
          : DEFAULT_STILL_LOOKING_CONFIG.maxItems,
      includeWishlist:
        typeof parsed.includeWishlist === 'boolean'
          ? parsed.includeWishlist
          : DEFAULT_STILL_LOOKING_CONFIG.includeWishlist,
      includeCart:
        typeof parsed.includeCart === 'boolean'
          ? parsed.includeCart
          : DEFAULT_STILL_LOOKING_CONFIG.includeCart,
      includeCatalogFallback:
        typeof parsed.includeCatalogFallback === 'boolean'
          ? parsed.includeCatalogFallback
          : DEFAULT_STILL_LOOKING_CONFIG.includeCatalogFallback,
    };
  } catch {
    return DEFAULT_STILL_LOOKING_CONFIG;
  }
}

export function saveStillLookingConfig(config: StillLookingConfig): void {
  if (typeof window === 'undefined') return;
  try {
    window.localStorage.setItem(STILL_LOOKING_CONFIG_KEY, JSON.stringify(config));
  } catch {
    // ignore storage limits
  }
}

export function loadRecentlyViewedIds(): string[] {
  if (typeof window === 'undefined') return [];
  try {
    const raw = window.localStorage.getItem(RECENTLY_VIEWED_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed.map(String) : [];
  } catch {
    return [];
  }
}

export function saveRecentlyViewedIds(ids: string[]): void {
  if (typeof window === 'undefined') return;
  try {
    window.localStorage.setItem(RECENTLY_VIEWED_KEY, JSON.stringify(ids.slice(0, 20)));
  } catch {
    // ignore storage limits
  }
}

export function getRealDiscountPercent(product: Product): number {
  if (!product.originalPrice || product.originalPrice <= product.price) return 0;
  return Math.max(
    1,
    Math.round(((product.originalPrice - product.price) * 100) / product.originalPrice)
  );
}

export function selectStillLookingEntries(
  catalog: Product[],
  recentlyViewedIds: string[],
  wishlistIds: string[],
  cartProductIds: string[],
  config: StillLookingConfig = DEFAULT_STILL_LOOKING_CONFIG
): StillLookingEntry[] {
  const byId = new Map(catalog.map((p) => [p.id, p]));
  const picked: StillLookingEntry[] = [];
  const seen = new Set<string>();

  const pushUnique = (
    p: Product | undefined,
    reason: StillLookingEntry['reason'],
    reasonLabel: string
  ) => {
    if (!p || seen.has(p.id) || picked.length >= config.maxItems) return;
    seen.add(p.id);
    picked.push({ product: p, reason, reasonLabel });
  };

  // Priority 1: Recently viewed browsing history (most recent first)
  recentlyViewedIds.forEach((id) => pushUnique(byId.get(id), 'viewed', 'Recently Viewed'));

  // Priority 2: Saved in Wishlist
  if (config.includeWishlist) {
    wishlistIds.forEach((id) => pushUnique(byId.get(id), 'wishlist', 'Saved in Wishlist'));
  }

  // Priority 3: Items currently sitting in Cart
  if (config.includeCart) {
    cartProductIds.forEach((id) => pushUnique(byId.get(id), 'cart', 'In Your Cart'));
  }

  // Priority 4: High-discount in-stock catalog fallback if user history is sparse
  if (config.includeCatalogFallback && picked.length < config.maxItems) {
    const fallbackPool = [...catalog]
      .filter((p) => p.inStock !== false)
      .sort((a, b) => getRealDiscountPercent(b) - getRealDiscountPercent(a));
    for (const item of fallbackPool) {
      if (picked.length >= config.maxItems) break;
      pushUnique(item, 'catalog', 'Top Price Drop');
    }
  }

  return picked.slice(0, config.maxItems);
}
