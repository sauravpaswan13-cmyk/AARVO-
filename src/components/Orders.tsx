import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  ArrowLeft,
  Bike,
  Box,
  CheckCircle2,
  ChevronDown,
  ChevronUp,
  Clock,
  Download,
  ExternalLink,
  MapPin,
  Package,
  RefreshCw,
  RotateCcw,
  Search,
  Truck,
  XCircle,
  AlertCircle,
} from 'lucide-react';
import {
  LiveRiderTrackingModal,
  Order,
  OrderItem,
  OrderStatus,
} from './OrdersSection';
import {
  OrderDetailsModal,
  ItemTrackingStage,
  OrderItemTimelineStep,
  OrderShippingAddress,
  OrderPriceBreakdown,
  TrackedOrder,
  TrackedOrderItem,
  buildCarrierTrackingUrl,
  parseShippingAddressText,
} from './OrderDetailsModal';
import { haptics } from '../utils/haptics';
import { getStoredAuthToken, resolveApiBaseUrl } from '../utils/authSession';
import prodWhiteSneakersImg from '../assets/images/prod_white_sneakers_1794533652181.jpg';
import catMobilesPhoneImg from '../assets/images/cat_mobiles_phone_1794533648438.jpg';
import catFashionBagImg from '../assets/images/cat_fashion_bag_1794533648322.jpg';
import catElectronicsLaptopImg from '../assets/images/cat_electronics_laptop_1794533650182.jpg';
import catHomeBlenderImg from '../assets/images/cat_home_blender_1794533649563.jpg';
import catBeautySerumImg from '../assets/images/cat_beauty_serum_1794533649077.jpg';

export type {
  ItemTrackingStage,
  OrderItemTimelineStep,
  OrderShippingAddress,
  OrderPriceBreakdown,
  TrackedOrder,
  TrackedOrderItem,
};

export interface OrdersProps {
  orders?: Order[];
  onOrdersLoaded?: (orders: Order[]) => void;
  apiBaseUrl?: string;
  authToken?: string | null;
  onBack?: () => void;
  onReorder?: (item: OrderItem) => void;
  onReorderAll?: (items: OrderItem[]) => void;
  onShowToast?: (message: string) => void;
  onNavigateToCart?: () => void;
  customerName?: string;
  customerPhone?: string;
  onOpenSupport?: () => void;
  embedded?: boolean;
}

const ITEM_STAGE_STEPS = ['Ordered', 'Packed', 'Shipped', 'Delivered'] as const;

function fallbackImageForCategory(category: string): string {
  const lower = (category || '').toLowerCase();
  if (lower.includes('mobile') || lower.includes('phone')) return catMobilesPhoneImg;
  if (lower.includes('fashion') || lower.includes('kurta') || lower.includes('apparel')) return catFashionBagImg;
  if (lower.includes('electron') || lower.includes('audio') || lower.includes('laptop')) return catElectronicsLaptopImg;
  if (lower.includes('home') || lower.includes('kitchen')) return catHomeBlenderImg;
  if (lower.includes('beauty') || lower.includes('care')) return catBeautySerumImg;
  return prodWhiteSneakersImg;
}

function sanitizeImageUrl(url: string | undefined, category: string): string {
  if (!url || url.includes('unsplash.com') || url.includes('placeholder.com') || url.includes('picsum.photos')) {
    return fallbackImageForCategory(category);
  }
  return url;
}

function mapBackendStatusToOrderStatus(rawStatus: string): {
  status: OrderStatus;
  statusStep: 1 | 2 | 3;
  itemStage: ItemTrackingStage;
  itemStep: 0 | 1 | 2 | 3 | 4;
} {
  const s = (rawStatus || '').toUpperCase();
  if (s === 'CANCELLED' || s === 'FAILED') {
    return { status: 'Cancelled', statusStep: 1, itemStage: 'Cancelled', itemStep: 0 };
  }
  if (s === 'DELIVERED') {
    return { status: 'Delivered', statusStep: 3, itemStage: 'Delivered', itemStep: 4 };
  }
  if (s === 'OUT_FOR_DELIVERY') {
    return { status: 'Shipped', statusStep: 2, itemStage: 'Out for Delivery', itemStep: 3 };
  }
  if (s === 'SHIPPED') {
    return { status: 'Shipped', statusStep: 2, itemStage: 'Shipped', itemStep: 3 };
  }
  if (s === 'PACKED') {
    return { status: 'Processing', statusStep: 1, itemStage: 'Packed', itemStep: 2 };
  }
  return { status: 'Processing', statusStep: 1, itemStage: 'Ordered', itemStep: 1 };
}

function mapLegacyOrderStatusToItemTracking(
  order: Order,
  itemIndex: number
): { itemStage: ItemTrackingStage; itemStep: 0 | 1 | 2 | 3 | 4 } {
  if (order.status === 'Cancelled') return { itemStage: 'Cancelled', itemStep: 0 };
  if (order.status === 'Delivered') return { itemStage: 'Delivered', itemStep: 4 };
  if (order.status === 'Shipped') {
    const outForDelivery = order.timeline?.some(
      (t) => t.completed && t.status.toLowerCase().includes('out for delivery')
    );
    return {
      itemStage: outForDelivery ? 'Out for Delivery' : 'Shipped',
      itemStep: 3,
    };
  }
  const hasPacked = order.timeline?.some(
    (t) => t.completed && t.status.toLowerCase().includes('pack')
  );
  if (hasPacked || itemIndex > 0) {
    return { itemStage: 'Packed', itemStep: 2 };
  }
  return { itemStage: 'Ordered', itemStep: 1 };
}

function formatReadableDate(dateValue?: string): string {
  if (!dateValue) return 'Recent order';
  const parsed = new Date(dateValue);
  if (Number.isNaN(parsed.getTime())) return dateValue;
  return parsed.toLocaleDateString('en-IN', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  });
}

function normalizeToTrackedOrder(raw: Order): TrackedOrder {
  const existingTracked = raw as Partial<TrackedOrder>;
  const rawItems = raw.items || [];
  const baseCarrier = (raw.carrier || 'AARVO Express').replace(/&bull;/g, '·');
  const baseTrackCode = raw.trackingNumber || `TRK-${raw.id}`;

  const mappedItems: TrackedOrderItem[] = rawItems.map((item, idx) => {
    const existing = item as Partial<TrackedOrderItem>;
    const mapped = mapLegacyOrderStatusToItemTracking(raw, idx);
    const itemStage = existing.itemStatus || mapped.itemStage;
    const itemStep = existing.itemStep ?? mapped.itemStep;
    const cleanImg = sanitizeImageUrl(item.image, item.category);
    const itemCarrier = existing.carrierName || baseCarrier;
    const itemTrackCode =
      existing.trackingCode ||
      (rawItems.length > 1 ? `${baseTrackCode}-${String(idx + 1).padStart(2, '0')}` : baseTrackCode);
    const itemTrackUrl = existing.trackingUrl || buildCarrierTrackingUrl(itemCarrier, itemTrackCode);

    const itemEvents: OrderItemTimelineStep[] =
      existing.itemEvents && existing.itemEvents.length > 0
        ? existing.itemEvents
        : [
            {
              label: 'Order Confirmed',
              timestamp: raw.timeline?.[0]?.time || raw.date,
              detail: `${item.name} verified for fulfillment`,
              completed: itemStep >= 1,
            },
            {
              label: 'Packed by Seller',
              timestamp: raw.timeline?.[1]?.time || (itemStep >= 2 ? 'Completed' : 'Pending'),
              detail: 'Quality checked & sealed with AWB barcode',
              completed: itemStep >= 2,
            },
            {
              label: 'Shipped via Courier',
              timestamp: raw.timeline?.[2]?.time || (itemStep >= 3 ? 'In transit' : 'Pending'),
              detail: `${itemCarrier} · ${itemTrackCode}`,
              completed: itemStep >= 3,
            },
            {
              label: 'Delivered',
              timestamp:
                itemStep >= 4
                  ? raw.timeline?.[raw.timeline.length - 1]?.time || raw.estimatedDelivery
                  : raw.estimatedDelivery,
              detail: itemStep >= 4 ? 'Handed over to recipient' : 'Doorstep OTP verification on arrival',
              completed: itemStep >= 4,
            },
          ];

    return {
      ...item,
      image: cleanImg,
      itemStatus: itemStage,
      itemStep,
      trackingCode: itemTrackCode,
      trackingUrl: itemTrackUrl,
      carrierName: itemCarrier,
      updatedAt: existing.updatedAt || raw.date,
      etaText: existing.etaText || raw.estimatedDelivery,
      itemEvents,
    };
  });

  const itemsSubtotal = mappedItems.reduce((sum, it) => sum + Number(it.price || 0) * Number(it.quantity || 1), 0);
  const grandTotal = Number(raw.total || itemsSubtotal);

  return {
    ...raw,
    source: existingTracked.source || 'local',
    items: mappedItems,
    shippingAddressDetails:
      existingTracked.shippingAddressDetails || parseShippingAddressText(raw.deliveryAddress),
    priceBreakdown: existingTracked.priceBreakdown || {
      itemsSubtotal,
      deliveryFee: Math.max(0, grandTotal - itemsSubtotal),
      platformFee: 0,
      discount: Math.max(0, itemsSubtotal - grandTotal),
      taxIncluded: Math.round(grandTotal - grandTotal / 1.18),
      grandTotal,
    },
    paymentStatus: existingTracked.paymentStatus,
    gatewayOrderId: existingTracked.gatewayOrderId,
    gatewayPaymentId: existingTracked.gatewayPaymentId,
    cancelReason: existingTracked.cancelReason,
  };
}

interface ItemThumbnailProps {
  src: string;
  alt: string;
}

const ItemThumbnail: React.FC<ItemThumbnailProps> = ({ src, alt }) => {
  const [failed, setFailed] = useState(false);

  if (failed || !src) {
    return (
      <div className="w-14 h-14 rounded-xl bg-orange-50 dark:bg-[#241B17] border border-orange-200/70 dark:border-orange-950/60 flex items-center justify-center text-[#FF5722] shrink-0">
        <Package className="w-6 h-6" />
      </div>
    );
  }

  return (
    <img
      src={src}
      alt={alt}
      referrerPolicy="no-referrer"
      onError={() => setFailed(true)}
      className="w-14 h-14 rounded-xl object-cover bg-orange-50/40 dark:bg-[#181311] border border-orange-100/80 dark:border-orange-950/60 shrink-0"
    />
  );
};

interface ItemStatusBadgeProps {
  stage: ItemTrackingStage;
}

const ItemStatusLabel: React.FC<ItemStatusBadgeProps> = ({ stage }) => {
  if (stage === 'Delivered') {
    return (
      <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-emerald-700 dark:text-emerald-400">
        <CheckCircle2 className="w-3.5 h-3.5 shrink-0" />
        <span>Delivered</span>
      </span>
    );
  }
  if (stage === 'Out for Delivery' || stage === 'Shipped') {
    return (
      <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-[#FF5722] dark:text-[#FF8A65]">
        <Truck className="w-3.5 h-3.5 shrink-0" />
        <span>{stage}</span>
      </span>
    );
  }
  if (stage === 'Cancelled') {
    return (
      <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-rose-600 dark:text-rose-400">
        <XCircle className="w-3.5 h-3.5 shrink-0" />
        <span>Cancelled</span>
      </span>
    );
  }
  return (
    <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-amber-700 dark:text-amber-400">
      <Clock className="w-3.5 h-3.5 shrink-0" />
      <span>{stage === 'Packed' ? 'Packed & Ready' : 'Order Placed'}</span>
    </span>
  );
};

export const Orders: React.FC<OrdersProps> = ({
  orders: initialOrdersProp,
  onOrdersLoaded,
  apiBaseUrl,
  authToken,
  onBack,
  onReorder,
  onReorderAll,
  onShowToast,
  onNavigateToCart,
  customerName = 'AARVO Customer',
  customerPhone = '',
  onOpenSupport,
  embedded = false,
}) => {
  const [trackedOrders, setTrackedOrders] = useState<TrackedOrder[]>(() => {
    if (initialOrdersProp && initialOrdersProp.length > 0) {
      return initialOrdersProp.map(normalizeToTrackedOrder);
    }
    try {
      const saved = localStorage.getItem('aarvo_orders');
      if (saved) {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed)) {
          return parsed.map(normalizeToTrackedOrder);
        }
      }
    } catch {
      // safe fallback
    }
    return [];
  });

  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isRefreshing, setIsRefreshing] = useState<boolean>(false);
  const [fetchNotice, setFetchNotice] = useState<string | null>(null);
  const [fetchError, setFetchError] = useState<string | null>(null);
  const [filter, setFilter] = useState<'All' | 'Active' | 'Processing' | 'Shipped' | 'Delivered' | 'Cancelled'>('All');
  const [searchQuery, setSearchQuery] = useState('');
  const [expandedItemsMap, setExpandedItemsMap] = useState<Record<string, boolean>>({});
  const [selectedOrder, setSelectedOrder] = useState<TrackedOrder | null>(null);
  const [riderTrackOrder, setRiderTrackOrder] = useState<Order | null>(null);

  const notify = useCallback(
    (msg: string) => {
      if (onShowToast) onShowToast(msg);
    },
    [onShowToast]
  );

  const initialOrdersRef = useRef(initialOrdersProp);
  initialOrdersRef.current = initialOrdersProp;

  const onOrdersLoadedRef = useRef(onOrdersLoaded);
  onOrdersLoadedRef.current = onOrdersLoaded;

  const notifyRef = useRef(notify);
  notifyRef.current = notify;

  const resolvedApiBase = useMemo(() => resolveApiBaseUrl(apiBaseUrl), [apiBaseUrl]);

  const fetchOrderHistory = useCallback(
    async (manualRefresh = false) => {
      if (manualRefresh) {
        setIsRefreshing(true);
      } else {
        setIsLoading(true);
      }
      setFetchError(null);
      setFetchNotice(null);

      const token =
        authToken !== undefined && authToken !== null && authToken.trim()
          ? authToken.trim()
          : getStoredAuthToken();

      try {
        const headers: Record<string, string> = {
          Accept: 'application/json',
        };
        if (token) {
          headers.Authorization = `Bearer ${token}`;
        }

        const ordersResponse = await fetch(`${resolvedApiBase}/v1/orders`, { headers });

        if (!ordersResponse.ok) {
          if (ordersResponse.status === 401 || ordersResponse.status === 403 || ordersResponse.status === 404 || ordersResponse.status === 503) {
            // Read persisted orders if available when user is browsing preview without live DB token
            const currentInitialOrders = initialOrdersRef.current;
            const localOrders =
              currentInitialOrders && currentInitialOrders.length > 0
                ? currentInitialOrders.map(normalizeToTrackedOrder)
                : (() => {
                    try {
                      const raw = localStorage.getItem('aarvo_orders');
                      return raw ? (JSON.parse(raw) as Order[]).map(normalizeToTrackedOrder) : [];
                    } catch {
                      return [];
                    }
                  })();

            setTrackedOrders(localOrders);
            if (ordersResponse.status === 401 || ordersResponse.status === 403) {
              setFetchNotice('Showing saved order history · Sign in with an active buyer session for live server sync.');
            } else {
              setFetchNotice('Showing saved order history');
            }
            return;
          }
          throw new Error(`Unable to fetch orders (HTTP ${ordersResponse.status})`);
        }

        const rawList = await ordersResponse.json();
        if (!Array.isArray(rawList)) {
          throw new Error('Unexpected response format from orders endpoint');
        }

        // Optionally fetch catalog to enrich product titles/images/categories
        const productLookup = new Map<number, { name: string; category: string; image: string }>();
        try {
          const prodRes = await fetch(`${resolvedApiBase}/v1/products`, { headers });
          if (prodRes.ok) {
            const prodJson = await prodRes.json();
            const rows = Array.isArray(prodJson) ? prodJson : Array.isArray(prodJson?.products) ? prodJson.products : [];
            rows.forEach((p: Record<string, unknown>) => {
              const id = Number(p.id);
              if (Number.isFinite(id)) {
                productLookup.set(id, {
                  name: String(p.name || `Product #${id}`),
                  category: String(p.category || 'General'),
                  image: String(p.image_url || p.imageUrl || ''),
                });
              }
            });
          }
        } catch {
          // Product enrichment optional
        }

        const enriched = await Promise.all(
          rawList.map(async (row: Record<string, unknown>): Promise<TrackedOrder> => {
            const orderId = String(row.id || '');
            let detailRow: Record<string, unknown> = row;
            let trackingEvents: Array<Record<string, unknown>> = [];

            try {
              const [detailRes, trackRes] = await Promise.all([
                fetch(`${resolvedApiBase}/v1/orders/${encodeURIComponent(orderId)}`, { headers }),
                fetch(`${resolvedApiBase}/v1/orders/${encodeURIComponent(orderId)}/tracking`, { headers }),
              ]);
              if (detailRes.ok) {
                detailRow = await detailRes.json();
              }
              if (trackRes.ok) {
                const trackJson = await trackRes.json();
                if (Array.isArray(trackJson)) {
                  trackingEvents = trackJson;
                }
              }
            } catch {
              // Keep summary row if detail/tracking sub-call fails
            }

            const statusInfo = mapBackendStatusToOrderStatus(String(detailRow.status || row.status || 'CONFIRMED'));
            const trackingJson =
              (detailRow.tracking_json as Record<string, unknown> | null) ||
              (row.tracking_json as Record<string, unknown> | null) ||
              {};
            const addressJson =
              (detailRow.address_json as Record<string, unknown> | null) ||
              (row.address_json as Record<string, unknown> | null) ||
              {};

            const formattedAddress = [
              addressJson.line1,
              addressJson.city,
              addressJson.state,
              addressJson.postalCode || addressJson.postal_code,
            ]
              .filter(Boolean)
              .join(', ');

            const totalRupees = Math.round(Number(detailRow.total_paise || row.total_paise || 0) / 100);
            const trackingCode = String(
              trackingJson.trackingNumber ||
                trackingEvents[trackingEvents.length - 1]?.tracking_code ||
                `ARV-${orderId.slice(0, 8).toUpperCase()}`
            );
            const carrierName = String(
              trackingJson.carrier ||
                trackingEvents[trackingEvents.length - 1]?.carrier ||
                'AARVO Express'
            );

            const createdDate = formatReadableDate(String(detailRow.created_at || row.created_at || ''));
            const timeline: Order['timeline'] =
              trackingEvents.length > 0
                ? trackingEvents.map((ev) => ({
                    status: String(ev.status || 'Update'),
                    time: formatReadableDate(String(ev.created_at || '')),
                    description: String(ev.note || `Updated via ${ev.carrier || carrierName}`),
                    completed: true,
                  }))
                : [
                    {
                      status: 'Order Placed',
                      time: createdDate,
                      description: `Order confirmed (${String(detailRow.payment_method || row.payment_method || 'UPI')})`,
                      completed: true,
                    },
                    {
                      status: statusInfo.itemStage,
                      time: formatReadableDate(String(detailRow.updated_at || row.updated_at || '')),
                      description: String(trackingJson.note || `Status: ${statusInfo.itemStage}`),
                      completed: statusInfo.itemStep >= 2,
                    },
                  ];

            const rawItems = Array.isArray(detailRow.items) ? (detailRow.items as Array<Record<string, unknown>>) : [];
            const mappedItems: TrackedOrderItem[] = rawItems.map((it, idx) => {
              const pid = Number(it.productId || it.product_id || idx + 1);
              const meta = productLookup.get(pid);
              const category = String(it.category || meta?.category || 'AARVO Store');
              const name = String(it.name || meta?.name || `Item #${pid}`);
              const unitPriceRupees = Math.round(Number(it.unitPricePaise || it.unit_price_paise || 0) / 100);
              const itemCarrier = String(it.carrier || carrierName);
              const itemTrackingCode = String(
                it.trackingCode ||
                  it.tracking_code ||
                  (rawItems.length > 1 ? `${trackingCode}-${String(idx + 1).padStart(2, '0')}` : trackingCode)
              );
              const itemTrackingUrl = String(
                it.trackingUrl || it.tracking_url || buildCarrierTrackingUrl(itemCarrier, itemTrackingCode)
              );

              return {
                id: String(pid),
                name,
                category,
                price: unitPriceRupees,
                quantity: Number(it.quantity || 1),
                image: sanitizeImageUrl(String(it.imageUrl || it.image || meta?.image || ''), category),
                itemStatus: statusInfo.itemStage,
                itemStep: statusInfo.itemStep,
                trackingCode: itemTrackingCode,
                trackingUrl: itemTrackingUrl,
                carrierName: itemCarrier,
                updatedAt: formatReadableDate(String(detailRow.updated_at || row.updated_at || '')),
                etaText:
                  statusInfo.status === 'Delivered'
                    ? `Delivered on ${formatReadableDate(String(detailRow.updated_at || ''))}`
                    : statusInfo.status === 'Cancelled'
                    ? 'Order cancelled'
                    : trackingJson.etd
                    ? `Estimated by ${String(trackingJson.etd)}`
                    : trackingJson.estimatedDays
                    ? `Estimated in ${String(trackingJson.estimatedDays)} days`
                    : 'Awaiting courier dispatch',
                itemEvents: timeline.map((ev) => ({
                  label: ev.status,
                  timestamp: ev.time,
                  detail: ev.description,
                  completed: ev.completed,
                })),
              };
            });

            const itemsSubtotalRupees = Math.round(
              Number(
                detailRow.subtotal_paise ||
                  row.subtotal_paise ||
                  mappedItems.reduce((sum, it) => sum + it.price * it.quantity * 100, 0)
              ) / 100
            );
            const deliveryFeeRupees = Math.round(Number(detailRow.delivery_fee_paise || row.delivery_fee_paise || 0) / 100);
            const platformFeeRupees = Math.round(Number(detailRow.platform_fee_paise || row.platform_fee_paise || 0) / 100);

            const shippingAddressDetails: OrderShippingAddress = {
              recipientName: String(addressJson.recipientName || addressJson.name || customerName),
              phone: String(addressJson.phone || customerPhone),
              line1: addressJson.line1 ? String(addressJson.line1) : undefined,
              line2: addressJson.line2 ? String(addressJson.line2) : undefined,
              landmark: addressJson.landmark ? String(addressJson.landmark) : undefined,
              city: addressJson.city ? String(addressJson.city) : undefined,
              state: addressJson.state ? String(addressJson.state) : undefined,
              postalCode:
                addressJson.postalCode || addressJson.postal_code
                  ? String(addressJson.postalCode || addressJson.postal_code)
                  : undefined,
              country: String(addressJson.country || 'India'),
              fullText: formattedAddress || 'Registered delivery address',
            };

            return {
              id: orderId,
              date: createdDate,
              total: totalRupees,
              status: statusInfo.status,
              statusStep: statusInfo.statusStep,
              estimatedDelivery:
                statusInfo.status === 'Delivered'
                  ? `Delivered (${formatReadableDate(String(detailRow.updated_at || ''))})`
                  : statusInfo.status === 'Cancelled'
                  ? 'Cancelled'
                  : trackingJson.etd
                  ? `Estimated delivery by ${String(trackingJson.etd)}`
                  : trackingJson.estimatedDays
                  ? `Estimated delivery in ${String(trackingJson.estimatedDays)} days`
                  : 'Updating after courier pickup',
              trackingNumber: trackingCode,
              carrier: carrierName,
              paymentMethod: String(detailRow.payment_method || row.payment_method || 'Online'),
              deliveryAddress: formattedAddress || 'Registered delivery address',
              items: mappedItems,
              timeline,
              source: 'api',
              rawStatus: String(detailRow.status || row.status || ''),
              shippingAddressDetails,
              priceBreakdown: {
                itemsSubtotal: itemsSubtotalRupees,
                deliveryFee: deliveryFeeRupees,
                platformFee: platformFeeRupees,
                discount: Math.max(0, itemsSubtotalRupees + deliveryFeeRupees + platformFeeRupees - totalRupees),
                taxIncluded: Math.round(totalRupees - totalRupees / 1.18),
                grandTotal: totalRupees,
              },
              paymentStatus: String(detailRow.payment_status || row.payment_status || ''),
              gatewayOrderId: detailRow.gateway_order_id ? String(detailRow.gateway_order_id) : undefined,
              gatewayPaymentId: detailRow.gateway_payment_id ? String(detailRow.gateway_payment_id) : undefined,
              cancelReason: detailRow.cancel_reason ? String(detailRow.cancel_reason) : undefined,
            };
          })
        );

        setTrackedOrders(enriched);
        if (onOrdersLoadedRef.current) onOrdersLoadedRef.current(enriched);
        if (manualRefresh) {
          haptics.subtle();
          notifyRef.current('Order history refreshed');
        }
      } catch (err) {
        const message = err instanceof Error ? err.message : 'Unable to fetch order history.';
        setFetchError(message);
        const currentInitialOrders = initialOrdersRef.current;
        if (currentInitialOrders && currentInitialOrders.length > 0) {
          setTrackedOrders(currentInitialOrders.map(normalizeToTrackedOrder));
        }
      } finally {
        setIsLoading(false);
        setIsRefreshing(false);
      }
    },
    [authToken, resolvedApiBase]
  );

  useEffect(() => {
    fetchOrderHistory(false);
  }, [fetchOrderHistory]);

  // Keep synced if parent prop updates locally (e.g. a newly placed order)
  useEffect(() => {
    if (!initialOrdersProp || initialOrdersProp.length === 0) return;
    setTrackedOrders((prev) => {
      const existingApiIds = new Set(prev.filter((o) => o.source === 'api').map((o) => o.id));
      const localUpdates = initialOrdersProp
        .filter((o) => !existingApiIds.has(o.id))
        .map(normalizeToTrackedOrder);
      const apiOnly = prev.filter((o) => o.source === 'api');
      return [...localUpdates, ...apiOnly];
    });
  }, [initialOrdersProp]);

  const filteredOrders = useMemo(() => {
    return trackedOrders.filter((order) => {
      const matchesFilter =
        filter === 'All'
          ? true
          : filter === 'Active'
          ? order.status === 'Processing' || order.status === 'Shipped'
          : order.status === filter;

      if (!matchesFilter) return false;

      const q = searchQuery.trim().toLowerCase();
      if (!q) return true;
      return (
        order.id.toLowerCase().includes(q) ||
        order.trackingNumber.toLowerCase().includes(q) ||
        order.items.some(
          (it) =>
            it.name.toLowerCase().includes(q) ||
            it.category.toLowerCase().includes(q) ||
            it.trackingCode.toLowerCase().includes(q)
        )
      );
    });
  }, [filter, searchQuery, trackedOrders]);

  const totalSpent = useMemo(
    () => trackedOrders.reduce((acc, o) => acc + (o.status === 'Cancelled' ? 0 : o.total), 0),
    [trackedOrders]
  );
  const activeCount = useMemo(
    () => trackedOrders.filter((o) => o.status === 'Processing' || o.status === 'Shipped').length,
    [trackedOrders]
  );

  const toggleItemDetails = (key: string) => {
    haptics.selection();
    setExpandedItemsMap((prev) => ({ ...prev, [key]: !prev[key] }));
  };

  const handleExportCsv = () => {
    if (filteredOrders.length === 0) {
      notify('No orders available to export.');
      return;
    }
    const headers = ['Order ID', 'Date', 'Order Status', 'Item Name', 'Item Status', 'Qty', 'Line Total (INR)', 'Tracking Code'];
    const lines = [headers.join(',')];
    filteredOrders.forEach((order) => {
      order.items.forEach((item) => {
        lines.push(
          [
            order.id,
            `"${order.date.replace(/"/g, '""')}"`,
            order.status,
            `"${item.name.replace(/"/g, '""')}"`,
            item.itemStatus,
            String(item.quantity),
            String(item.price * item.quantity),
            item.trackingCode,
          ].join(',')
        );
      });
    });
    const blob = new Blob([lines.join('\n')], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `aarvo-orders-${new Date().toISOString().slice(0, 10)}.csv`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    haptics.saved();
    notify(`Exported ${filteredOrders.length} order(s)`);
  };

  return (
    <section
      aria-label="Past Orders and Item Status Tracking"
      className={
        embedded
          ? 'flex flex-col pb-4'
          : 'flex-1 flex flex-col bg-[#FFFDF9] dark:bg-[#120E0C] min-h-screen pb-24'
      }
    >
      {/* Top Sticky Header (or compact inline toolbar when embedded inside Settings) */}
      <header
        className={
          embedded
            ? 'bg-white dark:bg-[#211A17] border border-orange-100 dark:border-stone-800 rounded-2xl p-3.5 mb-3 shadow-2xs'
            : 'sticky top-0 z-30 bg-[#FFFDF9]/95 dark:bg-[#181311]/95 backdrop-blur-md border-b border-orange-100 dark:border-stone-800 px-3.5 py-3'
        }
      >
        <div className="flex items-center justify-between gap-2 mb-2.5">
          <div className="flex items-center gap-2 min-w-0">
            {!embedded && onBack && (
              <button
                type="button"
                onClick={() => {
                  haptics.light();
                  onBack();
                }}
                aria-label="Go back"
                className="p-2 rounded-xl bg-orange-50/80 hover:bg-orange-100 dark:bg-[#241B17] dark:hover:bg-[#30241E] text-slate-800 dark:text-slate-100 transition-colors cursor-pointer shrink-0"
              >
                <ArrowLeft className="w-4 h-4" />
              </button>
            )}
            <div className="min-w-0">
              <h1 className="font-display font-bold text-base sm:text-lg text-slate-900 dark:text-white truncate">
                {embedded ? 'Order History' : 'My Orders'}
              </h1>
              <p className="text-xs text-slate-500 dark:text-slate-400 tabular-nums truncate">
                {trackedOrders.length} {trackedOrders.length === 1 ? 'order' : 'orders'} &middot; {activeCount} active &middot; ₹{totalSpent.toLocaleString('en-IN')} spent
              </p>
            </div>
          </div>

          <div className="flex items-center gap-1.5 shrink-0">
            <button
              type="button"
              onClick={() => fetchOrderHistory(true)}
              disabled={isRefreshing}
              aria-label="Refresh order history"
              className="min-h-[36px] px-3 py-1.5 rounded-xl bg-orange-50 hover:bg-orange-100 dark:bg-[#241B17] dark:hover:bg-[#30241E] text-[#FF5722] dark:text-[#FF8A65] text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer whitespace-nowrap shrink-0"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin' : ''}`} />
              <span>{isRefreshing ? 'Syncing' : 'Refresh'}</span>
            </button>

            <button
              type="button"
              onClick={handleExportCsv}
              className="min-h-[36px] px-3 py-1.5 rounded-xl bg-white dark:bg-[#211A17] border border-orange-200/80 dark:border-stone-700 hover:bg-orange-50 dark:hover:bg-[#2A201C] text-slate-700 dark:text-slate-200 text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer whitespace-nowrap shrink-0"
            >
              <Download className="w-3.5 h-3.5 text-[#FF5722] dark:text-[#FF8A65]" />
              <span>CSV</span>
            </button>
          </div>
        </div>

        {/* Search Input */}
        <div className="relative mb-2.5">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search by order ID, product, or tracking number..."
            className="w-full pl-9 pr-3 py-2 rounded-xl bg-white dark:bg-[#120E0C] border border-orange-200/80 dark:border-stone-700 text-xs text-slate-900 dark:text-slate-100 placeholder:text-slate-400 focus:outline-hidden focus:border-[#FF5722]"
          />
        </div>

        {/* Filter Segmented Bar */}
        <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar pb-0.5">
          {(['All', 'Active', 'Processing', 'Shipped', 'Delivered', 'Cancelled'] as const).map((tab) => {
            const active = filter === tab;
            return (
              <button
                key={tab}
                type="button"
                onClick={() => {
                  haptics.selection();
                  setFilter(tab);
                }}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap shrink-0 transition-colors cursor-pointer ${
                  active
                    ? 'bg-[#FF5722] text-white shadow-xs'
                    : 'bg-orange-50/70 dark:bg-[#211A17] text-slate-700 dark:text-slate-300 hover:bg-orange-100 dark:hover:bg-[#2C221E]'
                }`}
              >
                {tab}
              </button>
            );
          })}
        </div>
      </header>

      {/* Main Content */}
      <div className={embedded ? 'space-y-4' : 'p-3.5 space-y-4'}>
        {fetchError && (
          <div
            role="alert"
            className="p-3 rounded-xl bg-rose-50 dark:bg-rose-950/30 border border-rose-200 dark:border-rose-900/60 flex items-center justify-between gap-2 text-xs text-rose-800 dark:text-rose-200"
          >
            <div className="flex items-center gap-2 min-w-0">
              <AlertCircle className="w-4 h-4 text-rose-600 dark:text-rose-400 shrink-0" />
              <span className="truncate">{fetchError}</span>
            </div>
            <button
              type="button"
              onClick={() => fetchOrderHistory(true)}
              className="px-3 py-1 rounded-lg bg-rose-600 text-white font-semibold text-xs whitespace-nowrap shrink-0 cursor-pointer"
            >
              Retry
            </button>
          </div>
        )}

        {fetchNotice && !fetchError && (
          <div className="px-3 py-2 rounded-xl bg-orange-50/70 dark:bg-[#211A17] border border-orange-200/70 dark:border-stone-800 text-xs text-slate-600 dark:text-slate-300 flex items-center justify-between gap-2">
            <span className="truncate">{fetchNotice}</span>
            <button
              type="button"
              onClick={() => fetchOrderHistory(true)}
              className="text-[#FF5722] dark:text-[#FF8A65] font-semibold whitespace-nowrap shrink-0 hover:underline cursor-pointer"
            >
              Retry Sync
            </button>
          </div>
        )}

        {/* Loading Skeleton State */}
        {isLoading ? (
          <div className="space-y-3" aria-busy="true" aria-label="Loading orders">
            {[1, 2, 3].map((n) => (
              <div
                key={n}
                className="p-4 rounded-2xl bg-white dark:bg-[#181311] border border-orange-100/80 dark:border-stone-800 space-y-3"
              >
                <div className="flex justify-between">
                  <div className="h-4 w-32 rounded bg-orange-100/70 dark:bg-stone-800" />
                  <div className="h-4 w-20 rounded bg-orange-100/70 dark:bg-stone-800" />
                </div>
                <div className="flex gap-3">
                  <div className="w-14 h-14 rounded-xl bg-orange-100/70 dark:bg-stone-800 shrink-0" />
                  <div className="flex-1 space-y-2">
                    <div className="h-3.5 w-3/4 rounded bg-orange-100/70 dark:bg-stone-800" />
                    <div className="h-3 w-1/2 rounded bg-orange-100/60 dark:bg-stone-800" />
                    <div className="h-2 w-full rounded bg-orange-100/50 dark:bg-stone-800" />
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : filteredOrders.length === 0 ? (
          <div className="text-center py-12 px-5 bg-white dark:bg-[#181311] rounded-2xl border border-orange-100 dark:border-stone-800 space-y-2.5">
            <div className="w-12 h-12 rounded-2xl bg-orange-50 dark:bg-[#241B17] text-[#FF5722] dark:text-[#FF8A65] flex items-center justify-center mx-auto">
              <Package className="w-6 h-6" />
            </div>
            <h2 className="font-display font-bold text-sm text-slate-900 dark:text-white">
              {searchQuery ? 'No matching orders found' : 'No past orders in this filter'}
            </h2>
            <p className="text-xs text-slate-500 dark:text-slate-400 max-w-xs mx-auto">
              {searchQuery
                ? 'Try clearing your search filter or checking another order status tab.'
                : 'Completed and active purchases will appear here with per-item delivery tracking.'}
            </p>
            {(searchQuery || filter !== 'All') && (
              <button
                type="button"
                onClick={() => {
                  setSearchQuery('');
                  setFilter('All');
                }}
                className="mt-1 px-4 py-2 rounded-xl bg-[#FF5722] text-white text-xs font-semibold cursor-pointer"
              >
                Show All Orders
              </button>
            )}
          </div>
        ) : (
          <div className="space-y-4">
            {filteredOrders.map((order) => {
              const totalQty = order.items.reduce((sum, it) => sum + it.quantity, 0);

              return (
                <article
                  key={order.id}
                  onClick={() => {
                    haptics.selection();
                    setSelectedOrder(order);
                  }}
                  className="bg-white dark:bg-[#181311] rounded-2xl border border-orange-100 dark:border-stone-800 hover:border-[#FF5722]/50 dark:hover:border-[#FF8A65]/50 shadow-xs transition-colors overflow-hidden cursor-pointer"
                >
                  {/* Order Summary Header */}
                  <div className="px-4 py-3 bg-[#FFF8F2] dark:bg-[#1F1713] border-b border-orange-100/80 dark:border-stone-800 flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <div className="flex items-center gap-2 text-xs">
                        <span className="font-mono font-bold text-[#FF5722] dark:text-[#FF8A65] tabular-nums">
                          {order.id}
                        </span>
                        <span aria-hidden="true" className="text-slate-400">&middot;</span>
                        <span className="text-slate-600 dark:text-slate-300 tabular-nums">
                          {order.date}
                        </span>
                      </div>
                      <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5 truncate">
                        {totalQty} {totalQty === 1 ? 'item' : 'items'} &middot; {order.paymentMethod} &middot; {order.estimatedDelivery}
                      </p>
                    </div>

                    <div className="text-right shrink-0">
                      <span className="font-display font-bold text-sm text-slate-900 dark:text-white tabular-nums block">
                        ₹{order.total.toLocaleString('en-IN')}
                      </span>
                      <span className="text-[11px] font-medium text-slate-500 dark:text-slate-400">
                        {order.status}
                      </span>
                    </div>
                  </div>

                  {/* Per-Item List with Individual Status Tracking */}
                  <div className="divide-y divide-orange-100/70 dark:divide-stone-800/80">
                    {order.items.map((item, idx) => {
                      const itemKey = `${order.id}-${item.id}-${idx}`;
                      const showItemLog = !!expandedItemsMap[itemKey];
                      const activeStepIndex = item.itemStep; // 0..4

                      return (
                        <div key={itemKey} className="p-4 space-y-3">
                          {/* Item Row */}
                          <div className="flex items-start gap-3">
                            <ItemThumbnail src={item.image} alt={item.name} />

                            <div className="flex-1 min-w-0 space-y-1">
                              <div className="flex items-start justify-between gap-2">
                                <div className="min-w-0">
                                  <p className="text-xs text-slate-500 dark:text-slate-400 truncate">
                                    {item.category} &middot; Qty {item.quantity} &middot; ₹{item.price.toLocaleString('en-IN')}
                                  </p>
                                  <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100 leading-snug line-clamp-2">
                                    {item.name}
                                  </h3>
                                </div>
                                <span className="font-bold text-sm text-slate-900 dark:text-white tabular-nums shrink-0">
                                  ₹{(item.price * item.quantity).toLocaleString('en-IN')}
                                </span>
                              </div>

                              <div className="flex items-center justify-between gap-2 pt-0.5 flex-wrap">
                                <ItemStatusLabel stage={item.itemStatus} />
                                <span className="font-mono text-[11px] text-slate-500 dark:text-slate-400 tabular-nums">
                                  AWB: {item.trackingCode}
                                </span>
                              </div>
                            </div>
                          </div>

                          {/* Per-Item 4-Step Progress Tracker */}
                          {item.itemStatus === 'Cancelled' ? (
                            <div className="px-3 py-2 rounded-xl bg-rose-50/70 dark:bg-rose-950/25 border border-rose-200/70 dark:border-rose-900/40 text-xs text-rose-700 dark:text-rose-300 flex items-center justify-between">
                              <span>Item cancelled &middot; Refund/cancellation processed</span>
                              <span className="font-mono text-[11px] tabular-nums">{item.updatedAt}</span>
                            </div>
                          ) : (
                            <div className="pt-1">
                              <div className="relative h-1.5 rounded-full bg-slate-100 dark:bg-stone-800 overflow-hidden mb-2">
                                <div
                                  className={`h-full rounded-full transition-all duration-200 ${
                                    activeStepIndex >= 4
                                      ? 'bg-emerald-600 dark:bg-emerald-500'
                                      : 'bg-[#FF5722]'
                                  }`}
                                  style={{
                                    width:
                                      activeStepIndex <= 1
                                        ? '20%'
                                        : activeStepIndex === 2
                                        ? '50%'
                                        : activeStepIndex === 3
                                        ? '78%'
                                        : '100%',
                                  }}
                                />
                              </div>

                              <div className="grid grid-cols-4 gap-1">
                                {ITEM_STAGE_STEPS.map((label, sIdx) => {
                                  const stepNum = sIdx + 1;
                                  const reached = activeStepIndex >= stepNum;
                                  const isCurrent = activeStepIndex === stepNum;

                                  return (
                                    <div key={label} className="flex items-center gap-1 min-w-0">
                                      {stepNum === 1 && (
                                        <Box
                                          className={`w-3 h-3 shrink-0 ${
                                            reached ? 'text-[#FF5722] dark:text-[#FF8A65]' : 'text-slate-300 dark:text-stone-600'
                                          }`}
                                        />
                                      )}
                                      {stepNum === 2 && (
                                        <Package
                                          className={`w-3 h-3 shrink-0 ${
                                            reached ? 'text-[#FF5722] dark:text-[#FF8A65]' : 'text-slate-300 dark:text-stone-600'
                                          }`}
                                        />
                                      )}
                                      {stepNum === 3 && (
                                        <Truck
                                          className={`w-3 h-3 shrink-0 ${
                                            reached ? 'text-[#FF5722] dark:text-[#FF8A65]' : 'text-slate-300 dark:text-stone-600'
                                          }`}
                                        />
                                      )}
                                      {stepNum === 4 && (
                                        <CheckCircle2
                                          className={`w-3 h-3 shrink-0 ${
                                            reached ? 'text-emerald-600 dark:text-emerald-400' : 'text-slate-300 dark:text-stone-600'
                                          }`}
                                        />
                                      )}
                                      <span
                                        className={`text-[11px] truncate ${
                                          isCurrent
                                            ? 'font-bold text-slate-900 dark:text-white'
                                            : reached
                                            ? 'font-medium text-slate-700 dark:text-slate-300'
                                            : 'text-slate-400 dark:text-stone-500'
                                        }`}
                                      >
                                        {stepNum === 3 && item.itemStatus === 'Out for Delivery' ? 'Out for Del.' : label}
                                      </span>
                                    </div>
                                  );
                                })}
                              </div>
                            </div>
                          )}

                          {/* Per-Item Action Row */}
                          <div className="flex items-center justify-between gap-2 pt-1 flex-wrap">
                            <button
                              type="button"
                              onClick={(e) => {
                                e.stopPropagation();
                                toggleItemDetails(itemKey);
                              }}
                              className="text-xs font-semibold text-[#FF5722] dark:text-[#FF8A65] hover:underline flex items-center gap-1 cursor-pointer whitespace-nowrap"
                            >
                              <span>{showItemLog ? 'Hide Item Timeline' : 'Item Tracking Timeline'}</span>
                              {showItemLog ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
                            </button>

                            {onReorder && (
                              <button
                                type="button"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  haptics.addToCart();
                                  onReorder(item);
                                  notify(`Added ${item.name.slice(0, 20)}... to cart`);
                                }}
                                className="px-3 py-1.5 rounded-lg bg-orange-50 hover:bg-[#FF5722] text-[#FF5722] hover:text-white dark:bg-[#241B17] dark:text-[#FF8A65] dark:hover:bg-[#FF5722] dark:hover:text-white text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer whitespace-nowrap"
                              >
                                <RotateCcw className="w-3 h-3" />
                                <span>Buy Again</span>
                              </button>
                            )}
                          </div>

                          {/* Expandable Per-Item Timeline */}
                          {showItemLog && (
                            <div className="pt-2.5 border-t border-orange-100/70 dark:border-stone-800 space-y-2">
                              <p className="text-[11px] text-slate-500 dark:text-slate-400">
                                Carrier: <strong className="text-slate-700 dark:text-slate-200">{item.carrierName}</strong> &middot; {item.etaText}
                              </p>
                              <div className="space-y-2">
                                {item.itemEvents.map((ev, eIdx) => (
                                  <div key={eIdx} className="flex items-start justify-between gap-3 text-xs">
                                    <div className="flex items-start gap-2 min-w-0">
                                      <CheckCircle2
                                        className={`w-3.5 h-3.5 mt-0.5 shrink-0 ${
                                          ev.completed
                                            ? 'text-emerald-600 dark:text-emerald-400'
                                            : 'text-slate-300 dark:text-stone-600'
                                        }`}
                                      />
                                      <div className="min-w-0">
                                        <p
                                          className={`font-semibold leading-tight ${
                                            ev.completed
                                              ? 'text-slate-800 dark:text-slate-100'
                                              : 'text-slate-400 dark:text-stone-500'
                                          }`}
                                        >
                                          {ev.label}
                                        </p>
                                        <p className="text-[11px] text-slate-500 dark:text-slate-400 leading-tight mt-0.5">
                                          {ev.detail}
                                        </p>
                                      </div>
                                    </div>
                                    <span className="font-mono text-[10px] text-slate-400 dark:text-slate-500 tabular-nums shrink-0">
                                      {ev.timestamp}
                                    </span>
                                  </div>
                                ))}
                              </div>
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>

                  {/* Order Footer Bar */}
                  <div className="px-4 py-2.5 bg-[#FFFDF9] dark:bg-[#14100E] border-t border-orange-100/80 dark:border-stone-800 flex items-center justify-between gap-2 flex-wrap">
                    <div className="flex items-center gap-1.5 text-xs text-slate-500 dark:text-slate-400 min-w-0 truncate">
                      <MapPin className="w-3.5 h-3.5 text-[#FF5722] dark:text-[#FF8A65] shrink-0" />
                      <span className="truncate">{order.deliveryAddress}</span>
                    </div>

                    <div className="flex items-center gap-2 shrink-0">
                      {order.status !== 'Cancelled' && (
                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation();
                            haptics.selection();
                            setRiderTrackOrder(order);
                          }}
                          className="px-3 py-1.5 rounded-xl bg-[#FF5722] hover:bg-[#E64A19] text-white text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer whitespace-nowrap"
                        >
                          <Bike className="w-3.5 h-3.5" />
                          <span>Track Rider</span>
                        </button>
                      )}
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          haptics.selection();
                          setSelectedOrder(order);
                        }}
                        className="px-3 py-1.5 rounded-xl bg-orange-50 hover:bg-orange-100 dark:bg-[#241B17] dark:hover:bg-[#30241E] text-slate-800 dark:text-slate-200 text-xs font-semibold flex items-center gap-1 transition-colors cursor-pointer whitespace-nowrap"
                      >
                        <span>Order Details</span>
                        <ExternalLink className="w-3 h-3" />
                      </button>
                    </div>
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </div>

      {/* Detailed Order & Live Rider Modals */}
      <OrderDetailsModal
        order={selectedOrder}
        isOpen={!!selectedOrder}
        onClose={() => setSelectedOrder(null)}
        onReorder={(it) => {
          if (onReorder) onReorder(it);
        }}
        onReorderAll={onReorderAll}
        onShowToast={notify}
        onNavigateToCart={onNavigateToCart}
        onTrackRider={(ord) => setRiderTrackOrder(ord)}
        customerName={customerName}
        customerPhone={customerPhone}
        onOpenSupport={onOpenSupport}
      />

      <LiveRiderTrackingModal
        order={riderTrackOrder}
        isOpen={!!riderTrackOrder}
        onClose={() => setRiderTrackOrder(null)}
        onShowToast={notify}
      />
    </section>
  );
};

export default Orders;
