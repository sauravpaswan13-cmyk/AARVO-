const CATEGORY_COMMISSION_BPS = Object.freeze({
  MOBILE: 300,
  MOBILES: 300,
  ELECTRONICS: 500,
  'ELECTRONICS & ACCESSORIES': 500,
  FASHION: 600,
  CLOTHING: 600,
  'HOME & KITCHEN': 700,
  HOME: 700,
  KITCHEN: 700,
  BEAUTY: 800,
  'PERSONAL CARE': 800,
  'GENERAL MERCHANDISE': 1000,
  GENERAL: 1000
});

const DEFAULT_COMMISSION_BPS = 1200;

export function commissionBpsForCategory(category) {
  const key = String(category || '').trim().toUpperCase();
  return CATEGORY_COMMISSION_BPS[key] ?? DEFAULT_COMMISSION_BPS;
}

export function commissionPaise(amountPaise, category) {
  const amount = Math.max(0, Number(amountPaise) || 0);
  return Math.floor(amount * commissionBpsForCategory(category) / 10000);
}

export function commissionRatePercent(category) {
  return commissionBpsForCategory(category) / 100;
}

export const COMMISSION_RANGE = Object.freeze({ minPercent: 3, maxPercent: 12 });
