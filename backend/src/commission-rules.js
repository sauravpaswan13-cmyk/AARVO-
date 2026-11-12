// AARVO category-wise commission, selected by the product's unit selling price.
// All rates are basis points (100 bps = 1%). Below ₹1,000 every category is 0%.
const CATEGORY_RATES = Object.freeze({
  MOBILE: [200, 300, 400, 500, 600],
  MOBILES: [200, 300, 400, 500, 600],
  ELECTRONICS: [300, 400, 500, 600, 700],
  'ELECTRONICS & ACCESSORIES': [300, 400, 500, 600, 700],
  FASHION: [400, 500, 600, 700, 800],
  CLOTHING: [400, 500, 600, 700, 800],
  'HOME & KITCHEN': [300, 400, 500, 600, 700],
  HOME: [300, 400, 500, 600, 700],
  KITCHEN: [300, 400, 500, 600, 700],
  BEAUTY: [400, 500, 600, 700, 800],
  'PERSONAL CARE': [400, 500, 600, 700, 800],
  'GENERAL MERCHANDISE': [300, 400, 500, 600, 700],
  GENERAL: [300, 400, 500, 600, 700]
});

const DEFAULT_RATES = [300, 400, 500, 600, 700];
const PRICE_BANDS_PAISE = [100000, 200000, 500000, 1000000, 2500000];

function priceBandIndex(unitPricePaise) {
  const price = Math.max(0, Number(unitPricePaise) || 0);
  if (price < PRICE_BANDS_PAISE[0]) return -1;
  if (price < PRICE_BANDS_PAISE[1]) return 0;
  if (price < PRICE_BANDS_PAISE[2]) return 1;
  if (price < PRICE_BANDS_PAISE[3]) return 2;
  if (price < PRICE_BANDS_PAISE[4]) return 3;
  return 4;
}

export function commissionBpsForCategory(category, unitPricePaise = 0) {
  const band = priceBandIndex(unitPricePaise);
  if (band < 0) return 0;
  const key = String(category || '').trim().toUpperCase();
  const rates = CATEGORY_RATES[key] || DEFAULT_RATES;
  return rates[band];
}

// amountPaise is the full order-line value; unitPricePaise chooses the rate band.
export function commissionPaise(amountPaise, category, unitPricePaise = amountPaise) {
  const amount = Math.max(0, Number(amountPaise) || 0);
  return Math.floor(amount * commissionBpsForCategory(category, unitPricePaise) / 10000);
}

export function commissionRatePercent(category, unitPricePaise = 0) {
  return commissionBpsForCategory(category, unitPricePaise) / 100;
}

export const COMMISSION_RANGE = Object.freeze({ minPercent: 0, maxPercent: 8 });
