import { Product } from '../components/ProductDetailModal';

export type HomeMerchandisingSectionId =
  | 'still_looking'
  | 'brands_spotlight'
  | 'top_value_deals'
  | 'suggested_for_you'
  | 'more_products';

export type MerchandisingThemeKey =
  | 'periwinkle_frost'
  | 'pearl_rose_studio'
  | 'golden_amber_grid'
  | 'mint_botanical'
  | 'terracotta_sand';

export interface HomeMerchandisingSectionConfig {
  id: HomeMerchandisingSectionId;
  title: string;
  subtitle: string;
  badgeText: string;
  themeKey: MerchandisingThemeKey;
  themeName: string;
  sourceLabel: string;
  sourceDetails: string;
  rankingDetails: string;
  enabled: boolean;
  maxItems: number;
  sortOrder: number;
  pinnedProductIds: string[];
}

export interface StillLookingEntry {
  product: Product;
  reason: 'admin' | 'viewed' | 'wishlist' | 'cart' | 'catalog';
  reasonLabel: string;
}

export interface BrandSpotlightEntry {
  product: Product;
  headline: string;
  caption: string;
  isAdminCurated: boolean;
}

export interface MerchandisingProductPick {
  product: Product;
  badgeLabel?: string;
  isAdminCurated: boolean;
}

export const HOME_MERCHANDISING_CONFIG_KEY = 'aarvo_home_merchandising_v2';
export const RECENTLY_VIEWED_KEY = 'aarvo_recently_viewed_v1';

export const DEFAULT_MERCHANDISING_CONFIG: HomeMerchandisingSectionConfig[] = [
  {
    id: 'still_looking',
    title: 'Still looking for these?',
    subtitle: 'Based on items you viewed, saved & carted',
    badgeText: 'RECENT PICKS',
    themeKey: 'periwinkle_frost',
    themeName: 'Periwinkle Lavender Frost',
    sourceLabel: 'Admin Pins + Browsing History + Wishlist + Cart',
    sourceDetails:
      'Agar Admin ne koi product pin kiya ho to sabse pehle dikhta hai; uske baad User ke Recently Viewed, Wishlist aur Cart items aate hain.',
    rankingDetails:
      'Recently viewed sabse upar rehta hai aur real discount hone par ↓X% badge dikhta hai.',
    enabled: true,
    maxItems: 3,
    sortOrder: 0,
    pinnedProductIds: [],
  },
  {
    id: 'brands_spotlight',
    title: 'Brands in Spotlight',
    subtitle: 'Featured storefronts & top offers',
    badgeText: 'SPOTLIGHT',
    themeKey: 'pearl_rose_studio',
    themeName: 'Pearl Rose Studio',
    sourceLabel: 'Admin Spotlight + Verified Seller Catalog',
    sourceDetails:
      'Admin panel se pinned brands/products ya active verified sellers ke best catalog items dikhaye jate hain (bina kisi fake AD tag ke).',
    rankingDetails:
      'Har seller ka best rated / best discount flagship product 3x2 gallery me dikhta hai.',
    enabled: true,
    maxItems: 6,
    sortOrder: 1,
    pinnedProductIds: [],
  },
  {
    id: 'top_value_deals',
    title: 'Top Value Deals',
    subtitle: 'Handpicked savings & best-value picks',
    badgeText: 'VALUE PICKS',
    themeKey: 'golden_amber_grid',
    themeName: 'Golden Amber Grid',
    sourceLabel: 'Admin Deals + Value Price-Drop Index',
    sourceDetails:
      'Admin dwara manage kiye gaye Top Value deals aur highest real discount % wale in-stock products is Golden Grid zone me aate hain.',
    rankingDetails:
      'Real Discount % aur Verified Rating score se best value items rank hote hain.',
    enabled: true,
    maxItems: 3,
    sortOrder: 2,
    pinnedProductIds: [],
  },
  {
    id: 'suggested_for_you',
    title: 'Suggested For You',
    subtitle: 'Curated picks from your favorite categories',
    badgeText: 'FOR YOU',
    themeKey: 'mint_botanical',
    themeName: 'Mint-Sage Botanical',
    sourceLabel: 'Admin Curated + Category Affinity',
    sourceDetails:
      'Admin curated recommendations ke saath aapki pasand ki categories ke high-rated products match kiye jate hain.',
    rankingDetails:
      'Viewed/saved categories ko priority boost milta hai aur rating chip sirf verified reviews par hi dikhta hai.',
    enabled: true,
    maxItems: 3,
    sortOrder: 3,
    pinnedProductIds: [],
  },
  {
    id: 'more_products',
    title: 'More products for you',
    subtitle: 'Explore verified AARVO marketplace listings',
    badgeText: 'DISCOVER',
    themeKey: 'terracotta_sand',
    themeName: 'Warm Terracotta Sand',
    sourceLabel: 'Admin Priority + Full Published Catalog',
    sourceDetails:
      'Admin pinned featured items sabse upar aate hain, uske baad poora live published catalog (/v1/products) grid me dikhta hai.',
    rankingDetails:
      'In-stock items, real price drops (>=45% par Lowest price) aur sachhe COD/payment details ke saath.',
    enabled: true,
    maxItems: 8,
    sortOrder: 4,
    pinnedProductIds: [],
  },
];

export function loadMerchandisingConfig(): HomeMerchandisingSectionConfig[] {
  if (typeof window === 'undefined') return DEFAULT_MERCHANDISING_CONFIG;
  try {
    const raw = window.localStorage.getItem(HOME_MERCHANDISING_CONFIG_KEY);
    if (!raw) return DEFAULT_MERCHANDISING_CONFIG;
    const parsed = JSON.parse(raw) as Array<Partial<HomeMerchandisingSectionConfig>>;
    if (!Array.isArray(parsed) || parsed.length === 0) return DEFAULT_MERCHANDISING_CONFIG;

    const byId = new Map(DEFAULT_MERCHANDISING_CONFIG.map((s) => [s.id, s]));
    const merged: HomeMerchandisingSectionConfig[] = [];

    parsed.forEach((item) => {
      if (!item.id || !byId.has(item.id as HomeMerchandisingSectionId)) return;
      const base = byId.get(item.id as HomeMerchandisingSectionId)!;
      byId.delete(item.id as HomeMerchandisingSectionId);
      merged.push({
        ...base,
        title: typeof item.title === 'string' && item.title.trim() ? item.title.trim() : base.title,
        subtitle: typeof item.subtitle === 'string' ? item.subtitle : base.subtitle,
        badgeText: typeof item.badgeText === 'string' ? item.badgeText : base.badgeText,
        enabled: typeof item.enabled === 'boolean' ? item.enabled : base.enabled,
        maxItems:
          typeof item.maxItems === 'number' && item.maxItems >= 2 && item.maxItems <= 16
            ? item.maxItems
            : base.maxItems,
        sortOrder: typeof item.sortOrder === 'number' ? item.sortOrder : base.sortOrder,
        pinnedProductIds: Array.isArray(item.pinnedProductIds)
          ? item.pinnedProductIds.map(String)
          : base.pinnedProductIds,
      });
    });

    byId.forEach((remaining) => merged.push(remaining));
    return merged;
  } catch {
    return DEFAULT_MERCHANDISING_CONFIG;
  }
}

export function saveMerchandisingConfig(config: HomeMerchandisingSectionConfig[]): void {
  if (typeof window === 'undefined') return;
  try {
    window.localStorage.setItem(HOME_MERCHANDISING_CONFIG_KEY, JSON.stringify(config));
  } catch {
    // ignore storage limits
  }
}

export function mergeRemoteMerchandisingConfig(
  localConfig: HomeMerchandisingSectionConfig[],
  remoteSections: Array<{
    id: string;
    title?: string;
    subtitle?: string;
    badgeText?: string;
    enabled?: boolean;
    maxItems?: number;
    sortOrder?: number;
    pinnedProductIds?: string[];
  }>
): HomeMerchandisingSectionConfig[] {
  if (!Array.isArray(remoteSections) || remoteSections.length === 0) return localConfig;
  const localById = new Map(localConfig.map((s) => [s.id, s]));
  const baseById = new Map(DEFAULT_MERCHANDISING_CONFIG.map((s) => [s.id, s]));

  const next: HomeMerchandisingSectionConfig[] = [];
  remoteSections
    .slice()
    .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
    .forEach((remote) => {
      const id = remote.id as HomeMerchandisingSectionId;
      const base = localById.get(id) || baseById.get(id);
      if (!base) return;
      baseById.delete(id);
      localById.delete(id);
      next.push({
        ...base,
        title: remote.title?.trim() || base.title,
        subtitle: typeof remote.subtitle === 'string' ? remote.subtitle : base.subtitle,
        badgeText: typeof remote.badgeText === 'string' ? remote.badgeText : base.badgeText,
        enabled: typeof remote.enabled === 'boolean' ? remote.enabled : base.enabled,
        maxItems:
          typeof remote.maxItems === 'number' && remote.maxItems >= 2 && remote.maxItems <= 16
            ? remote.maxItems
            : base.maxItems,
        sortOrder: typeof remote.sortOrder === 'number' ? remote.sortOrder : base.sortOrder,
        pinnedProductIds: Array.isArray(remote.pinnedProductIds)
          ? remote.pinnedProductIds.map(String)
          : base.pinnedProductIds,
      });
    });

  localById.forEach((remaining) => next.push(remaining));
  return next;
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
  limit = 3,
  pinnedIds: string[] = []
): StillLookingEntry[] {
  const byId = new Map(catalog.map((p) => [p.id, p]));
  const picked: StillLookingEntry[] = [];
  const seen = new Set<string>();

  const pushUnique = (
    p: Product | undefined,
    reason: StillLookingEntry['reason'],
    reasonLabel: string
  ) => {
    if (!p || seen.has(p.id) || picked.length >= limit) return;
    seen.add(p.id);
    picked.push({ product: p, reason, reasonLabel });
  };

  pinnedIds.forEach((id) => pushUnique(byId.get(id), 'admin', 'Featured'));
  recentlyViewedIds.forEach((id) => pushUnique(byId.get(id), 'viewed', 'Recently Viewed'));
  wishlistIds.forEach((id) => pushUnique(byId.get(id), 'wishlist', 'Saved in Wishlist'));
  cartProductIds.forEach((id) => pushUnique(byId.get(id), 'cart', 'In Your Cart'));

  if (picked.length < limit) {
    const fallbackPool = [...catalog]
      .filter((p) => p.inStock !== false)
      .sort((a, b) => getRealDiscountPercent(b) - getRealDiscountPercent(a));
    for (const item of fallbackPool) {
      if (picked.length >= limit) break;
      pushUnique(item, 'catalog', 'Top Price Drop');
    }
  }

  return picked.slice(0, limit);
}

export function selectBrandsInSpotlight(
  catalog: Product[],
  limit = 6,
  pinnedIds: string[] = []
): BrandSpotlightEntry[] {
  const byId = new Map(catalog.map((p) => [p.id, p]));
  const chosen: Array<{ product: Product; isAdminCurated: boolean }> = [];
  const seen = new Set<string>();

  pinnedIds.forEach((id) => {
    const p = byId.get(id);
    if (p && !seen.has(p.id) && chosen.length < limit) {
      seen.add(p.id);
      chosen.push({ product: p, isAdminCurated: true });
    }
  });

  const inStock = catalog.filter((p) => p.inStock !== false);
  const bySeller = new Map<string, Product>();

  inStock.forEach((product) => {
    if (seen.has(product.id)) return;
    const sellerKey = (product.seller || 'AARVO Store').trim();
    const existing = bySeller.get(sellerKey);
    if (!existing) {
      bySeller.set(sellerKey, product);
      return;
    }
    const existingScore = getRealDiscountPercent(existing) + existing.rating * 5;
    const nextScore = getRealDiscountPercent(product) + product.rating * 5;
    if (nextScore > existingScore) {
      bySeller.set(sellerKey, product);
    }
  });

  const uniqueSellerPicks = Array.from(bySeller.values()).sort(
    (a, b) => getRealDiscountPercent(b) - getRealDiscountPercent(a)
  );

  for (const p of uniqueSellerPicks) {
    if (chosen.length >= limit) break;
    if (!seen.has(p.id)) {
      seen.add(p.id);
      chosen.push({ product: p, isAdminCurated: false });
    }
  }

  for (const p of inStock) {
    if (chosen.length >= limit) break;
    if (!seen.has(p.id)) {
      seen.add(p.id);
      chosen.push({ product: p, isAdminCurated: false });
    }
  }

  return chosen.slice(0, limit).map(({ product, isAdminCurated }) => {
    const pct = getRealDiscountPercent(product);
    const headline =
      pct >= 45
        ? `Up to ${pct}% Off`
        : product.price <= 999
        ? `Just ₹${product.price.toLocaleString('en-IN')}`
        : 'Shop now';
    const shortCat = product.category.split('/')[0].trim();
    return {
      product,
      headline,
      caption: product.seller ? `${shortCat} · ${product.seller}` : shortCat,
      isAdminCurated,
    };
  });
}

export function selectTopValueDeals(
  catalog: Product[],
  limit = 3,
  pinnedIds: string[] = []
): MerchandisingProductPick[] {
  const byId = new Map(catalog.map((p) => [p.id, p]));
  const picked: MerchandisingProductPick[] = [];
  const seen = new Set<string>();

  pinnedIds.forEach((id) => {
    const p = byId.get(id);
    if (p && !seen.has(p.id) && picked.length < limit) {
      seen.add(p.id);
      picked.push({ product: p, isAdminCurated: true });
    }
  });

  const ranked = [...catalog]
    .filter((p) => p.inStock !== false && !seen.has(p.id))
    .sort((a, b) => {
      const scoreA = getRealDiscountPercent(a) * 1.4 + a.rating * 8 + (a.price <= 1499 ? 15 : 0);
      const scoreB = getRealDiscountPercent(b) * 1.4 + b.rating * 8 + (b.price <= 1499 ? 15 : 0);
      return scoreB - scoreA;
    });

  for (const item of ranked) {
    if (picked.length >= limit) break;
    seen.add(item.id);
    picked.push({ product: item, isAdminCurated: false });
  }

  return picked.slice(0, limit);
}

export function selectSuggestedForYou(
  catalog: Product[],
  recentlyViewedIds: string[],
  wishlistIds: string[],
  cartProductIds: string[],
  limit = 3,
  pinnedIds: string[] = []
): MerchandisingProductPick[] {
  const byId = new Map(catalog.map((p) => [p.id, p]));
  const picked: MerchandisingProductPick[] = [];
  const seen = new Set<string>();

  pinnedIds.forEach((id) => {
    const p = byId.get(id);
    if (p && !seen.has(p.id) && picked.length < limit) {
      seen.add(p.id);
      picked.push({ product: p, isAdminCurated: true });
    }
  });

  const affinityIds = new Set([...recentlyViewedIds, ...wishlistIds, ...cartProductIds]);
  const preferredCategories = new Set<string>();
  catalog.forEach((p) => {
    if (affinityIds.has(p.id)) {
      preferredCategories.add(p.category.toLowerCase());
    }
  });

  const ranked = [...catalog]
    .filter((p) => p.inStock !== false && !seen.has(p.id))
    .sort((a, b) => {
      const matchA = preferredCategories.has(a.category.toLowerCase()) ? 30 : 0;
      const matchB = preferredCategories.has(b.category.toLowerCase()) ? 30 : 0;
      const scoreA = matchA + a.rating * 10 + getRealDiscountPercent(a) * 0.5;
      const scoreB = matchB + b.rating * 10 + getRealDiscountPercent(b) * 0.5;
      return scoreB - scoreA;
    });

  for (const item of ranked) {
    if (picked.length >= limit) break;
    seen.add(item.id);
    picked.push({ product: item, isAdminCurated: false });
  }

  return picked.slice(0, limit);
}

export function selectMoreProducts(
  catalog: Product[],
  limit = 8,
  pinnedIds: string[] = []
): MerchandisingProductPick[] {
  const byId = new Map(catalog.map((p) => [p.id, p]));
  const picked: MerchandisingProductPick[] = [];
  const seen = new Set<string>();

  pinnedIds.forEach((id) => {
    const p = byId.get(id);
    if (p && !seen.has(p.id) && picked.length < limit) {
      seen.add(p.id);
      picked.push({ product: p, isAdminCurated: true });
    }
  });

  const ranked = [...catalog]
    .filter((p) => !seen.has(p.id))
    .sort((a, b) => {
      const stockDiff = Number(b.inStock !== false) - Number(a.inStock !== false);
      if (stockDiff !== 0) return stockDiff;
      return b.reviewsCount - a.reviewsCount || getRealDiscountPercent(b) - getRealDiscountPercent(a);
    });

  for (const item of ranked) {
    if (picked.length >= limit) break;
    seen.add(item.id);
    picked.push({ product: item, isAdminCurated: false });
  }

  return picked.slice(0, limit);
}
