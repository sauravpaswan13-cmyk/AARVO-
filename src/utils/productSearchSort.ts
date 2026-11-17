export type ProductSortOption =
  | 'relevance'
  | 'price-asc'
  | 'price-desc'
  | 'rating-desc'
  | 'discount-desc';

export const PRODUCT_SORT_OPTIONS: ReadonlyArray<{ id: ProductSortOption; label: string }> = [
  { id: 'relevance', label: 'Relevance' },
  { id: 'price-asc', label: 'Price: Low to High' },
  { id: 'price-desc', label: 'Price: High to Low' },
  { id: 'rating-desc', label: 'Top Rated' },
  { id: 'discount-desc', label: 'Discount' },
];

export interface SearchableProductLike {
  id: number | string;
  name?: string;
  category?: string;
  seller?: string;
  description?: string;
  price?: number;
  originalPrice?: number;
  rating?: number;
  reviews?: number;
}

export function tokenizeSearchQuery(query: string): string[] {
  const cleaned = (query || '').trim().toLowerCase();
  if (!cleaned) return [];
  const rawTokens = cleaned.split(/\s+/).filter(Boolean);
  return Array.from(new Set(rawTokens));
}

export function countKeywordOccurrences(text: string, keyword: string): number {
  if (!text || !keyword) return 0;
  let count = 0;
  let index = 0;
  while (index <= text.length - keyword.length) {
    const foundAt = text.indexOf(keyword, index);
    if (foundAt === -1) break;
    count += 1;
    index = foundAt + keyword.length;
  }
  return count;
}

function hasWholeWordMatch(text: string, keyword: string): boolean {
  if (!text || !keyword) return false;
  const escaped = keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  return new RegExp(`(^|[^a-z0-9])${escaped}([^a-z0-9]|$)`, 'i').test(text);
}

export function computeProductRelevanceScore(
  product: SearchableProductLike,
  query: string
): number {
  const fullQuery = (query || '').trim().toLowerCase();
  const tokens = tokenizeSearchQuery(fullQuery);
  if (tokens.length === 0) return 0;

  const name = (product.name || '').toLowerCase();
  const category = (product.category || '').toLowerCase();
  const seller = (product.seller || '').toLowerCase();
  const description = (product.description || '').toLowerCase();

  let score = 0;

  // Phrase and prefix signals
  if (name === fullQuery) score += 60;
  else if (name.startsWith(fullQuery)) score += 30;
  else if (name.includes(fullQuery)) score += 20;

  if (category.includes(fullQuery)) score += 12;
  if (description.includes(fullQuery)) score += 8;

  // Keyword frequency & coverage scoring
  let distinctTokenHits = 0;
  for (const token of tokens) {
    const nameFreq = countKeywordOccurrences(name, token);
    const catFreq = countKeywordOccurrences(category, token);
    const sellerFreq = countKeywordOccurrences(seller, token);
    const descFreq = Math.min(countKeywordOccurrences(description, token), 12);

    if (nameFreq > 0 || catFreq > 0 || sellerFreq > 0 || descFreq > 0) {
      distinctTokenHits += 1;
    }

    score += nameFreq * 10;
    score += catFreq * 6;
    score += sellerFreq * 4;
    score += descFreq * 3;

    if (hasWholeWordMatch(name, token)) score += 5;
    if (hasWholeWordMatch(category, token)) score += 3;
  }

  // Strongly prefer products that cover all (or more) distinct query keywords
  score += distinctTokenHits * 15;
  if (distinctTokenHits === tokens.length) {
    score += 20;
  }

  // Subtle quality tie-breaker
  score += Number(product.rating || 0) * 0.5;

  return score;
}

export function matchAndScoreProducts<T extends SearchableProductLike>(
  products: T[],
  query: string
): T[] {
  const tokens = tokenizeSearchQuery(query);
  if (tokens.length === 0) return products;

  const matchesAll = products.filter((p) => {
    const hay = `${p.name || ''} ${p.category || ''} ${p.seller || ''} ${p.description || ''}`.toLowerCase();
    return tokens.every((t) => hay.includes(t));
  });

  if (matchesAll.length > 0 || tokens.length === 1) {
    return matchesAll;
  }

  // Multi-word partial fallback so relevance ranking surfaces closest keyword matches
  return products.filter((p) => {
    const hay = `${p.name || ''} ${p.category || ''} ${p.seller || ''} ${p.description || ''}`.toLowerCase();
    return tokens.some((t) => hay.includes(t));
  });
}

export function sortProducts<T extends SearchableProductLike>(
  products: T[],
  sortBy: ProductSortOption,
  searchQuery: string
): T[] {
  const list = [...products];
  const trimmedQuery = (searchQuery || '').trim();

  if (sortBy === 'price-asc') {
    return list.sort((a, b) => Number(a.price || 0) - Number(b.price || 0));
  }
  if (sortBy === 'price-desc') {
    return list.sort((a, b) => Number(b.price || 0) - Number(a.price || 0));
  }
  if (sortBy === 'rating-desc') {
    return list.sort((a, b) => {
      const rDiff = Number(b.rating || 0) - Number(a.rating || 0);
      if (rDiff !== 0) return rDiff;
      return Number(b.reviews || 0) - Number(a.reviews || 0);
    });
  }
  if (sortBy === 'discount-desc') {
    const getDiscountPct = (p: SearchableProductLike) => {
      const price = Number(p.price || 0);
      const orig = Number(p.originalPrice || price);
      if (orig <= 0 || orig <= price) return 0;
      return ((orig - price) / orig) * 100;
    };
    return list.sort((a, b) => getDiscountPct(b) - getDiscountPct(a));
  }

  // 'relevance' sort: keyword frequency matching when a query is present
  if (!trimmedQuery) {
    return list;
  }

  const scored = list.map((item, idx) => ({
    item,
    idx,
    score: computeProductRelevanceScore(item, trimmedQuery),
  }));

  scored.sort((a, b) => {
    if (b.score !== a.score) return b.score - a.score;
    const ratingDiff = Number(b.item.rating || 0) - Number(a.item.rating || 0);
    if (ratingDiff !== 0) return ratingDiff;
    const reviewsDiff = Number(b.item.reviews || 0) - Number(a.item.reviews || 0);
    if (reviewsDiff !== 0) return reviewsDiff;
    return a.idx - b.idx;
  });

  return scored.map((entry) => entry.item);
}
