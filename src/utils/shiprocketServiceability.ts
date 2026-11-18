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
  source: 'shiprocket_live' | 'shiprocket_zone_estimate';
  riderMessage: string;
}

const DEFAULT_API_BASE =
  ((import.meta as unknown as { env?: Record<string, string> }).env?.VITE_API_BASE_URL ||
    'https://aarvo-api.onrender.com').replace(/\/$/, '');

const METRO_PIN_PREFIXES = new Set([
  '110', '122', '201', '302', '400', '411', '500', '560', '600', '700', '800',
]);
const REMOTE_PIN_PREFIXES = new Set(['19', '74', '79']);

const clientCache = new Map<string, { value: ShiprocketServiceabilityResult; expiresAt: number }>();

function formatEtaDate(days: number): string {
  const target = new Date(Date.now() + Math.max(1, days) * 24 * 60 * 60 * 1000);
  return target.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
}

export function computeZoneTransitFallback(
  deliveryPin: string,
  pickupPin = '800001',
  codAllowed = true
): ShiprocketServiceabilityResult | null {
  const d = deliveryPin.replace(/\D/g, '').slice(0, 6);
  if (!/^[1-9]\d{5}$/.test(d)) return null;
  const p = /^[1-9]\d{5}$/.test(pickupPin) ? pickupPin : '800001';

  let minDays = 3;
  let maxDays = 5;
  let zoneLabel = 'National Surface / Air';

  if (p.slice(0, 3) === d.slice(0, 3)) {
    minDays = 1;
    maxDays = 2;
    zoneLabel = 'Local City Hub';
  } else if (p.slice(0, 2) === d.slice(0, 2)) {
    minDays = 2;
    maxDays = 3;
    zoneLabel = 'Intra-State Network';
  } else if (REMOTE_PIN_PREFIXES.has(d.slice(0, 2)) || d.startsWith('744')) {
    minDays = 5;
    maxDays = 7;
    zoneLabel = 'Extended Zone';
  } else if (p[0] === d[0]) {
    minDays = 3;
    maxDays = 4;
    zoneLabel = 'Regional Zone';
  } else if (METRO_PIN_PREFIXES.has(p.slice(0, 3)) && METRO_PIN_PREFIXES.has(d.slice(0, 3))) {
    minDays = 3;
    maxDays = 4;
    zoneLabel = 'Metro Air Express';
  }

  return {
    serviceable: true,
    pickupPincode: p,
    deliveryPincode: d,
    minDays,
    maxDays,
    estimatedDays: maxDays,
    estimatedDaysText: `${minDays}-${maxDays} days`,
    etd: formatEtaDate(maxDays),
    courierName: 'Shiprocket Partner Courier',
    provider: 'shiprocket',
    codAvailable: codAllowed,
    freightCharge: null,
    zoneLabel,
    source: 'shiprocket_zone_estimate',
    riderMessage: `Estimated ${minDays}-${maxDays} days delivery to PIN ${d} (${zoneLabel})`,
  };
}

export async function checkShiprocketServiceability(
  deliveryPincode: string,
  options?: { productId?: string | number; cod?: boolean; signal?: AbortSignal }
): Promise<ShiprocketServiceabilityResult | null> {
  const cleanPin = deliveryPincode.replace(/\D/g, '').slice(0, 6);
  if (!/^[1-9]\d{5}$/.test(cleanPin)) return null;

  const pid = options?.productId ? String(options.productId).replace(/\D/g, '') : '';
  const cacheKey = `${cleanPin}:${pid}:${options?.cod === false ? '0' : '1'}`;
  const cached = clientCache.get(cacheKey);
  if (cached && cached.expiresAt > Date.now()) return cached.value;

  const params = new URLSearchParams({ deliveryPincode: cleanPin });
  if (pid) params.set('productId', pid);
  if (options?.cod !== undefined) params.set('cod', options.cod ? '1' : '0');

  try {
    const res = await fetch(`${DEFAULT_API_BASE}/v1/delivery/serviceability?${params.toString()}`, {
      signal: options?.signal,
    });
    if (res.ok) {
      const data = (await res.json()) as ShiprocketServiceabilityResult;
      if (data && typeof data.serviceable === 'boolean') {
        clientCache.set(cacheKey, { value: data, expiresAt: Date.now() + 10 * 60 * 1000 });
        return data;
      }
    }
  } catch {
    // Fallback cleanly below if offline or timeout
  }

  const fallback = computeZoneTransitFallback(cleanPin, '800001', options?.cod ?? true);
  if (fallback) {
    clientCache.set(cacheKey, { value: fallback, expiresAt: Date.now() + 5 * 60 * 1000 });
  }
  return fallback;
}
