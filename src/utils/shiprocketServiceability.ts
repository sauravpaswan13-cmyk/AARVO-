export interface ShiprocketServiceabilityResult {
  serviceable: boolean;
  pickupPincode: string;
  deliveryPincode: string;
  minDays: number | null;
  maxDays: number | null;
  estimatedDays: number | null;
  estimatedDaysText: string;
  etd: string | null;
  courierName: string | null;
  provider: string;
  codAvailable: boolean;
  freightCharge: number | null;
  zoneLabel?: string;
  sizeTier?: string;
  deliveryMode?: string;
  billableWeightKg?: number;
  dispatchNote?: string;
  source: string;
  riderMessage: string;
}

const DEFAULT_API_BASE =
  ((import.meta as unknown as { env?: Record<string, string> }).env?.VITE_API_BASE_URL ||
    'https://aarvo-api.onrender.com').replace(/\/$/, '');

const METRO_PIN_PREFIXES = new Set([
  '110', '122', '201', '302', '400', '411', '500', '560', '600', '700', '800',
]);
const REMOTE_PIN_PREFIXES = new Set(['19', '74', '79']);
const IST_OFFSET_MS = 330 * 60 * 1000;
const IST_MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
const IST_WEEKDAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

const clientCache = new Map<string, { value: ShiprocketServiceabilityResult; expiresAt: number }>();

function getIstDate(offsetDays = 0): Date {
  return new Date(Date.now() + IST_OFFSET_MS + offsetDays * 24 * 60 * 60 * 1000);
}

function formatIstEtaDate(days: number): string {
  const d = getIstDate(Math.max(1, days));
  return `${IST_WEEKDAYS[d.getUTCDay()]}, ${d.getUTCDate()} ${IST_MONTHS[d.getUTCMonth()]} ${d.getUTCFullYear()}`;
}

export function sanitizeBuyerCourierLabel(raw?: string | null, mode?: string): string {
  const txt = String(raw || '').trim();
  if (!txt || /shiprocket/i.test(txt)) {
    if (mode === 'Surface Cargo') return 'Heavy Surface Freight';
    if (mode === 'Air Express') return 'Express Air Courier';
    return 'Standard Courier Partner';
  }
  return txt;
}

export function classifyDeliveryParcel(options?: {
  category?: string;
  sizeClass?: string;
  weightKg?: number;
  lengthCm?: number;
  breadthCm?: number;
  heightCm?: number;
}) {
  const catLower = String(options?.category || '').toLowerCase();
  const sizeLower = String(options?.sizeClass || '').toLowerCase();
  const actualWeight = options?.weightKg && options.weightKg > 0 ? options.weightKg : 0;
  const volWeight =
    options?.lengthCm && options?.breadthCm && options?.heightCm &&
    options.lengthCm > 0 && options.breadthCm > 0 && options.heightCm > 0
      ? (options.lengthCm * options.breadthCm * options.heightCm) / 5000
      : 0;
  const billableWeightKg = Math.max(actualWeight > 0 ? actualWeight : 0.5, volWeight);
  const maxDim = Math.max(options?.lengthCm || 0, options?.breadthCm || 0, options?.heightCm || 0);

  const isHeavyCategory = /(appliance|furniture|refrigerator|washing|television|\btv\b|sofa|mattress|cooler|geyser|almirah|bed|table|chair|wardrobe|dishwasher)/.test(catLower);
  const isMediumCategory = /(home & kitchen|kitchen|electronics|laptop|printer|monitor|audio|mixer|microwave|luggage|sports|fitness)/.test(catLower);

  let sizeTier = 'Standard Parcel';
  let extraTransitDays = 0;
  let extraHandlingDays = 0;
  let deliveryMode = 'Air Express';

  if (billableWeightKg > 5 || maxDim > 60 || sizeLower === 'heavy' || sizeLower === 'bulky' || (isHeavyCategory && actualWeight === 0)) {
    sizeTier = 'Heavy & Bulky';
    extraTransitDays = 2;
    extraHandlingDays = 1;
    deliveryMode = 'Surface Cargo';
  } else if (billableWeightKg > 1.5 || maxDim > 35 || sizeLower === 'medium' || isMediumCategory) {
    sizeTier = 'Medium Parcel';
    extraTransitDays = 1;
    extraHandlingDays = 0;
    deliveryMode = 'Surface / Air';
  }

  const istNow = getIstDate(0);
  const istHour = istNow.getUTCHours();
  const istDay = istNow.getUTCDay();
  const missedCutoff = istHour >= 14;
  let dispatchOffset = (missedCutoff ? 1 : 0) + extraHandlingDays;
  if (istDay === 0 || (missedCutoff && istDay === 6)) dispatchOffset += 1;
  const dispatchNote =
    dispatchOffset === 0
      ? 'Dispatches today (order before 2 PM)'
      : dispatchOffset === 1
      ? 'Dispatches tomorrow'
      : 'Dispatches in 2 business days';

  return {
    billableWeightKg: Number(billableWeightKg.toFixed(2)),
    sizeTier,
    deliveryMode,
    extraTransitDays,
    dispatchOffset,
    dispatchNote,
  };
}

export function computeZoneTransitFallback(
  deliveryPin: string,
  pickupPin = '110001',
  codAllowed = true,
  meta?: {
    category?: string;
    sizeClass?: string;
    weightKg?: number;
    lengthCm?: number;
    breadthCm?: number;
    heightCm?: number;
  }
): ShiprocketServiceabilityResult | null {
  const d = deliveryPin.replace(/\D/g, '').slice(0, 6);
  if (!/^[1-9]\d{5}$/.test(d)) return null;
  const p = /^[1-9]\d{5}$/.test(pickupPin) ? pickupPin : '110001';
  const parcelInfo = classifyDeliveryParcel(meta);
  const isDeliveryMetro = METRO_PIN_PREFIXES.has(d.slice(0, 3));

  let baseMin = 3;
  let baseMax = 4;
  let zoneLabel = 'National Courier Network';
  let modeForRoute = parcelInfo.sizeTier === 'Heavy & Bulky' ? 'Surface Cargo' : parcelInfo.deliveryMode;

  if (p.slice(0, 3) === d.slice(0, 3)) {
    baseMin = 1;
    baseMax = 2;
    zoneLabel = 'Local City Hub';
    modeForRoute = parcelInfo.sizeTier === 'Heavy & Bulky' ? 'Local Heavy Van' : 'Local City Courier';
  } else if (p.slice(0, 2) === d.slice(0, 2)) {
    baseMin = 2;
    baseMax = 3;
    zoneLabel = 'Intra-State Network';
  } else if (REMOTE_PIN_PREFIXES.has(d.slice(0, 2)) || d.startsWith('744')) {
    baseMin = 5;
    baseMax = 7;
    zoneLabel = 'Extended & Hill Zone';
  } else if (p[0] === d[0]) {
    baseMin = 3;
    baseMax = 4;
    zoneLabel = 'Regional Zone';
  } else if (METRO_PIN_PREFIXES.has(p.slice(0, 3)) && isDeliveryMetro) {
    baseMin = 2;
    baseMax = 3;
    zoneLabel = 'Metro-to-Metro';
    modeForRoute = parcelInfo.sizeTier === 'Heavy & Bulky' ? 'Surface Cargo' : 'Air Express';
  }

  let minDays = Math.max(1, baseMin + parcelInfo.dispatchOffset + (parcelInfo.sizeTier === 'Heavy & Bulky' ? 1 : 0));
  let maxDays = Math.max(minDays, baseMax + parcelInfo.dispatchOffset + parcelInfo.extraTransitDays);
  if (!isDeliveryMetro && getIstDate(maxDays).getUTCDay() === 0) {
    maxDays += 1;
    if (minDays === maxDays - 1) minDays = maxDays;
  }

  const etd = formatIstEtaDate(maxDays);
  const etaRangeText = minDays === maxDays ? `${maxDays} day${maxDays > 1 ? 's' : ''}` : `${minDays}-${maxDays} days`;

  return {
    serviceable: true,
    pickupPincode: p,
    deliveryPincode: d,
    minDays,
    maxDays,
    estimatedDays: maxDays,
    estimatedDaysText: etaRangeText,
    etd,
    courierName: sanitizeBuyerCourierLabel('', modeForRoute),
    provider: 'courier',
    codAvailable: codAllowed,
    freightCharge: null,
    zoneLabel,
    sizeTier: parcelInfo.sizeTier,
    deliveryMode: modeForRoute,
    billableWeightKg: parcelInfo.billableWeightKg,
    dispatchNote: parcelInfo.dispatchNote,
    source: 'zone_estimate',
    riderMessage: `Delivery by ${etd} to PIN ${d} (${parcelInfo.sizeTier} • ${modeForRoute})`,
  };
}

export async function checkShiprocketServiceability(
  deliveryPincode: string,
  options?: {
    productId?: string | number;
    cod?: boolean;
    category?: string;
    sizeClass?: string;
    weightKg?: number;
    lengthCm?: number;
    breadthCm?: number;
    heightCm?: number;
    signal?: AbortSignal;
  }
): Promise<ShiprocketServiceabilityResult | null> {
  const cleanPin = deliveryPincode.replace(/\D/g, '').slice(0, 6);
  if (!/^[1-9]\d{5}$/.test(cleanPin)) return null;

  const pid = options?.productId ? String(options.productId).replace(/\D/g, '') : '';
  const cat = String(options?.category || '').trim();
  const w = options?.weightKg && options.weightKg > 0 ? options.weightKg : 0;
  const cacheKey = `${cleanPin}:${pid}:${cat}:${w}:${options?.cod === false ? '0' : '1'}`;
  const cached = clientCache.get(cacheKey);
  if (cached && cached.expiresAt > Date.now()) return cached.value;

  const params = new URLSearchParams({ deliveryPincode: cleanPin });
  if (pid) params.set('productId', pid);
  if (cat) params.set('category', cat);
  if (options?.sizeClass) params.set('sizeClass', options.sizeClass);
  if (w > 0) params.set('weightKg', String(w));
  if (options?.lengthCm && options.lengthCm > 0) params.set('length', String(options.lengthCm));
  if (options?.breadthCm && options.breadthCm > 0) params.set('breadth', String(options.breadthCm));
  if (options?.heightCm && options.heightCm > 0) params.set('height', String(options.heightCm));
  if (options?.cod !== undefined) params.set('cod', options.cod ? '1' : '0');

  try {
    const res = await fetch(`${DEFAULT_API_BASE}/v1/delivery/serviceability?${params.toString()}`, {
      signal: options?.signal,
    });
    if (res.ok) {
      const data = (await res.json()) as ShiprocketServiceabilityResult;
      if (data && typeof data.serviceable === 'boolean') {
        const sanitized: ShiprocketServiceabilityResult = {
          ...data,
          courierName: data.courierName ? sanitizeBuyerCourierLabel(data.courierName, data.deliveryMode) : null,
        };
        clientCache.set(cacheKey, { value: sanitized, expiresAt: Date.now() + 10 * 60 * 1000 });
        return sanitized;
      }
    }
  } catch {
    // Fallback cleanly below if offline or timeout
  }

  const fallback = computeZoneTransitFallback(cleanPin, '110001', options?.cod ?? true, {
    category: cat,
    sizeClass: options?.sizeClass,
    weightKg: options?.weightKg,
    lengthCm: options?.lengthCm,
    breadthCm: options?.breadthCm,
    heightCm: options?.heightCm,
  });
  if (fallback) {
    clientCache.set(cacheKey, { value: fallback, expiresAt: Date.now() + 5 * 60 * 1000 });
  }
  return fallback;
}
