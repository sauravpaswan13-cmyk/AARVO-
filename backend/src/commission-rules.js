// AARVO's marketplace commission is a flat 3% across every product category.
// The commission is deducted from the seller's item proceeds; it is not added
// again as a separate customer checkout surcharge.
const DEFAULT_COMMISSION_BPS = 300;

export function commissionBpsForCategory(_category) {
  return DEFAULT_COMMISSION_BPS;
}

export function commissionPaise(amountPaise, category) {
  const amount = Math.max(0, Number(amountPaise) || 0);
  return Math.floor(amount * commissionBpsForCategory(category) / 10000);
}

export function commissionRatePercent(category) {
  return commissionBpsForCategory(category) / 100;
}

export const COMMISSION_RANGE = Object.freeze({ minPercent: 3, maxPercent: 3 });
