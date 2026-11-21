import React, { useEffect, useState } from 'react';
import {
  Bike,
  Box,
  Check,
  CheckCircle2,
  ChevronDown,
  ChevronUp,
  Clock,
  Copy,
  CreditCard,
  ExternalLink,
  FileText,
  MapPin,
  Package,
  Phone,
  Receipt,
  RotateCcw,
  ShieldCheck,
  ShoppingCart,
  Truck,
  X,
  XCircle,
} from 'lucide-react';
import { Order, OrderItem } from './OrdersSection';
import { haptics } from '../utils/haptics';

export type ItemTrackingStage =
  | 'Ordered'
  | 'Packed'
  | 'Shipped'
  | 'Out for Delivery'
  | 'Delivered'
  | 'Cancelled';

export interface OrderItemTimelineStep {
  label: string;
  timestamp: string;
  detail: string;
  completed: boolean;
}

export interface OrderShippingAddress {
  recipientName?: string;
  phone?: string;
  line1?: string;
  line2?: string;
  landmark?: string;
  city?: string;
  state?: string;
  postalCode?: string;
  country?: string;
  fullText: string;
}

export interface OrderPriceBreakdown {
  itemsSubtotal: number;
  deliveryFee: number;
  platformFee: number;
  discount: number;
  taxIncluded: number;
  grandTotal: number;
}

export interface TrackedOrderItem extends OrderItem {
  itemStatus: ItemTrackingStage;
  itemStep: 0 | 1 | 2 | 3 | 4; // 0 = Cancelled, 1 = Ordered, 2 = Packed, 3 = Shipped/OFD, 4 = Delivered
  trackingCode: string;
  trackingUrl?: string;
  carrierName: string;
  updatedAt: string;
  etaText: string;
  itemEvents: OrderItemTimelineStep[];
}

export interface TrackedOrder extends Order {
  items: TrackedOrderItem[];
  source: 'api' | 'local';
  rawStatus?: string;
  shippingAddressDetails?: OrderShippingAddress;
  priceBreakdown?: OrderPriceBreakdown;
  paymentStatus?: string;
  gatewayOrderId?: string;
  gatewayPaymentId?: string;
  cancelReason?: string;
}

export interface OrderDetailsModalProps {
  order: TrackedOrder | null;
  isOpen: boolean;
  onClose: () => void;
  onReorder?: (item: OrderItem) => void;
  onReorderAll?: (items: OrderItem[]) => void;
  onShowToast?: (message: string) => void;
  onNavigateToCart?: () => void;
  onTrackRider?: (order: TrackedOrder) => void;
  customerName?: string;
  customerPhone?: string;
  onOpenSupport?: () => void;
}

const ITEM_STAGE_STEPS = ['Ordered', 'Packed', 'Shipped', 'Delivered'] as const;

export function buildCarrierTrackingUrl(carrier: string, trackingCode: string): string {
  const cleanCode = encodeURIComponent((trackingCode || '').trim());
  const lower = (carrier || '').toLowerCase();

  if (lower.includes('shiprocket')) {
    return `https://shiprocket.co/tracking/${cleanCode}`;
  }
  if (lower.includes('bluedart') || lower.includes('blue dart')) {
    return `https://www.bluedart.com/tracking?trackFor=0&trackNo=${cleanCode}`;
  }
  if (lower.includes('delhivery')) {
    return `https://www.delhivery.com/track-v2/package/${cleanCode}`;
  }
  if (lower.includes('xpressbees')) {
    return `https://www.xpressbees.com/shipment/tracking?awbNo=${cleanCode}`;
  }
  if (lower.includes('ekart')) {
    return `https://ekartlogistics.com/track/${cleanCode}`;
  }
  if (lower.includes('dtdc')) {
    return `https://www.dtdc.in/tracking.asp?strCnno=${cleanCode}`;
  }
  return `https://www.aftership.com/track?num=${cleanCode}`;
}

export function parseShippingAddressText(
  addressText: string,
  customerName?: string,
  customerPhone?: string
): OrderShippingAddress {
  const cleaned = (addressText || 'Registered delivery address').trim();
  const pinMatch = cleaned.match(/(\d{6})/);
  const postalCode = pinMatch ? pinMatch[1] : undefined;

  const withoutPin = cleaned.replace(/[-–,]?\s*\d{6}\s*$/, '').trim();
  const parts = withoutPin
    .split(',')
    .map((p) => p.trim())
    .filter(Boolean);

  let city: string | undefined;
  let state: string | undefined;
  let line1 = cleaned;
  let line2: string | undefined;

  if (parts.length >= 3) {
    city = parts[parts.length - 1];
    line1 = parts[0];
    line2 = parts.slice(1, parts.length - 1).join(', ');
  } else if (parts.length === 2) {
    line1 = parts[0];
    city = parts[1];
  }

  return {
    recipientName: customerName,
    phone: customerPhone,
    line1,
    line2,
    city,
    state,
    postalCode,
    country: 'India',
    fullText: cleaned,
  };
}

export function computePriceBreakdown(order: TrackedOrder): OrderPriceBreakdown {
  if (order.priceBreakdown) {
    return order.priceBreakdown;
  }

  const itemsSubtotal = order.items.reduce(
    (sum, item) => sum + Number(item.price || 0) * Number(item.quantity || 1),
    0
  );
  const grandTotal = Number(order.total || itemsSubtotal);
  const delta = grandTotal - itemsSubtotal;
  const deliveryFee = delta > 0 ? delta : 0;
  const discount = delta < 0 ? Math.abs(delta) : 0;
  const taxIncluded = Math.round(grandTotal - grandTotal / 1.18);

  return {
    itemsSubtotal,
    deliveryFee,
    platformFee: 0,
    discount,
    taxIncluded,
    grandTotal,
  };
}

const ModalItemThumb: React.FC<{ src: string; alt: string }> = ({ src, alt }) => {
  const [failed, setFailed] = useState(false);

  if (failed || !src) {
    return (
      <div className="w-14 h-14 rounded-xl bg-orange-50 dark:bg-[#241B17] border border-orange-200/70 dark:border-stone-800 flex items-center justify-center text-[#FF5722] shrink-0">
        <Package className="w-5 h-5" />
      </div>
    );
  }

  return (
    <img
      src={src}
      alt={alt}
      referrerPolicy="no-referrer"
      onError={() => setFailed(true)}
      className="w-14 h-14 rounded-xl object-cover bg-orange-50/40 dark:bg-[#120E0C] border border-orange-100/80 dark:border-stone-800 shrink-0"
    />
  );
};

export const OrderDetailsModal: React.FC<OrderDetailsModalProps> = ({
  order,
  isOpen,
  onClose,
  onReorder,
  onReorderAll,
  onShowToast,
  onNavigateToCart,
  onTrackRider,
  customerName = 'AARVO Customer',
  customerPhone = '',
  onOpenSupport,
}) => {
  const [copiedKey, setCopiedKey] = useState<string | null>(null);
  const [expandedItems, setExpandedItems] = useState<Record<string, boolean>>({});
  const [reorderedAll, setReorderedAll] = useState(false);

  useEffect(() => {
    setExpandedItems({});
    setReorderedAll(false);
    setCopiedKey(null);
  }, [order?.id]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        haptics.light();
        onClose();
      }
    };

    if (isOpen) {
      document.body.style.overflow = 'hidden';
      window.addEventListener('keydown', handleKeyDown);
    }

    return () => {
      document.body.style.overflow = '';
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, onClose]);

  if (!isOpen || !order) return null;

  const notify = (msg: string) => {
    if (onShowToast) onShowToast(msg);
  };

  const copyValue = (key: string, value: string, label: string) => {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(value);
    }
    haptics.saved();
    setCopiedKey(key);
    notify(`${label} copied`);
    setTimeout(() => {
      setCopiedKey((prev) => (prev === key ? null : prev));
    }, 2000);
  };

  const totalUnits = order.items.reduce((sum, item) => sum + item.quantity, 0);
  const breakdown = computePriceBreakdown(order);
  const shipping =
    order.shippingAddressDetails ||
    parseShippingAddressText(order.deliveryAddress, customerName, customerPhone);

  const handleReorderAll = () => {
    if (onReorderAll) {
      onReorderAll(order.items);
    } else if (onReorder) {
      haptics.addToCart();
      order.items.forEach((item) => onReorder(item));
      notify(`Added ${totalUnits} item(s) from ${order.id} to cart`);
    }
    setReorderedAll(true);
    setTimeout(() => setReorderedAll(false), 2400);
  };

  const handleDownloadInvoice = () => {
    haptics.subtle();
    const lines = [
      `AARVO MARKETPLACE — ORDER RECEIPT`,
      `==================================================`,
      `Order ID       : ${order.id}`,
      `Order Date     : ${order.date}`,
      `Status         : ${order.status}`,
      `Payment Method : ${order.paymentMethod}`,
      order.gatewayPaymentId ? `Payment Ref    : ${order.gatewayPaymentId}` : '',
      ``,
      `SHIPPING ADDRESS`,
      `----------------`,
      `Recipient : ${shipping.recipientName || customerName}`,
      `Phone     : ${shipping.phone || customerPhone}`,
      `Address   : ${shipping.fullText}`,
      ``,
      `ITEMS & INDIVIDUAL TRACKING`,
      `---------------------------`,
      ...order.items.map(
        (it, i) =>
          `${i + 1}. ${it.name} (${it.category})\n   Qty: ${it.quantity} x INR ${it.price.toLocaleString('en-IN')} = INR ${(
            it.price * it.quantity
          ).toLocaleString('en-IN')}\n   Status: ${it.itemStatus} | Carrier: ${it.carrierName} | AWB: ${it.trackingCode}\n   Track: ${
            it.trackingUrl || buildCarrierTrackingUrl(it.carrierName, it.trackingCode)
          }`
      ),
      ``,
      `PRICE BREAKDOWN`,
      `---------------`,
      `Items Subtotal : INR ${breakdown.itemsSubtotal.toLocaleString('en-IN')}`,
      `Delivery Fee   : ${breakdown.deliveryFee === 0 ? 'Included' : `INR ${breakdown.deliveryFee.toLocaleString('en-IN')}`}`,
      breakdown.platformFee > 0 ? `Platform Fee   : INR ${breakdown.platformFee.toLocaleString('en-IN')}` : '',
      breakdown.discount > 0 ? `Discount       : -INR ${breakdown.discount.toLocaleString('en-IN')}` : '',
      `GST (Included) : INR ${breakdown.taxIncluded.toLocaleString('en-IN')}`,
      `Grand Total    : INR ${breakdown.grandTotal.toLocaleString('en-IN')}`,
    ].filter(Boolean);

    const blob = new Blob([lines.join('\n')], { type: 'text/plain;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `aarvo-receipt-${order.id}.txt`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
    notify(`Receipt downloaded for ${order.id}`);
  };

  const paymentBadgeText = (() => {
    const ps = (order.paymentStatus || '').toUpperCase();
    if (order.status === 'Cancelled' || ps === 'FAILED' || ps === 'COD_CANCELLED') {
      return ps === 'REFUNDED' ? 'Refunded' : 'Cancelled';
    }
    if (ps === 'COD_PENDING' || order.paymentMethod.toLowerCase().includes('cod')) {
      return 'Pay on Delivery';
    }
    if (ps === 'CREATED') return 'Payment Pending';
    return 'Verified & Paid';
  })();

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="order-details-modal-title"
      className="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-0 sm:p-4"
    >
      {/* Backdrop */}
      <div
        onClick={() => {
          haptics.light();
          onClose();
        }}
        className="fixed inset-0 bg-black/65 backdrop-blur-xs transition-opacity"
      />

      {/* Modal Sheet */}
      <div className="relative w-full max-w-2xl max-h-[92vh] bg-[#FFFDF9] dark:bg-[#14100E] text-slate-900 dark:text-slate-100 rounded-t-3xl sm:rounded-3xl shadow-2xl flex flex-col z-10 overflow-hidden border border-orange-100 dark:border-stone-800">
        {/* Sticky Header */}
        <div className="sticky top-0 z-20 flex items-center justify-between gap-3 px-5 py-4 bg-white/95 dark:bg-[#181311]/95 backdrop-blur-md border-b border-orange-100 dark:border-stone-800">
          <div className="flex items-center gap-3 min-w-0">
            <div className="w-10 h-10 rounded-xl bg-orange-50 dark:bg-[#241B17] border border-orange-200/70 dark:border-stone-700 text-[#FF5722] dark:text-[#FF8A65] flex items-center justify-center shrink-0">
              <Receipt className="w-5 h-5" />
            </div>
            <div className="min-w-0">
              <div className="flex items-center gap-2 flex-wrap">
                <h2
                  id="order-details-modal-title"
                  className="font-mono font-bold text-sm sm:text-base text-slate-900 dark:text-white tabular-nums truncate"
                >
                  Order #{order.id}
                </h2>
                <button
                  type="button"
                  onClick={() => copyValue('order-id', order.id, 'Order ID')}
                  className="inline-flex items-center gap-1 text-[11px] font-semibold text-[#FF5722] dark:text-[#FF8A65] hover:underline cursor-pointer whitespace-nowrap"
                >
                  {copiedKey === 'order-id' ? <Check className="w-3 h-3" /> : <Copy className="w-3 h-3" />}
                  <span>{copiedKey === 'order-id' ? 'Copied' : 'Copy'}</span>
                </button>
              </div>
              <p className="text-xs text-slate-500 dark:text-slate-400 tabular-nums truncate">
                Placed {order.date} &middot; {totalUnits} {totalUnits === 1 ? 'unit' : 'units'} &middot; {order.status}
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={() => {
              haptics.light();
              onClose();
            }}
            aria-label="Close order details"
            className="p-2 rounded-xl bg-orange-50/70 hover:bg-orange-100 dark:bg-[#241B17] dark:hover:bg-[#30241E] text-slate-600 dark:text-slate-300 transition-colors cursor-pointer shrink-0 focus-visible:outline-2 focus-visible:outline-[#FF5722]"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Scrollable Content */}
        <div className="flex-1 overflow-y-auto p-4 sm:p-5 space-y-5">
          {/* 1. Fulfillment Summary & Live Tracking Action */}
          <div className="p-4 rounded-2xl bg-white dark:bg-[#181311] border border-orange-100 dark:border-stone-800 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div className="space-y-1 min-w-0">
              <div className="flex items-center gap-2">
                {order.status === 'Delivered' ? (
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 dark:text-emerald-400 shrink-0" />
                ) : order.status === 'Cancelled' ? (
                  <XCircle className="w-4 h-4 text-rose-600 dark:text-rose-400 shrink-0" />
                ) : order.status === 'Shipped' ? (
                  <Truck className="w-4 h-4 text-[#FF5722] dark:text-[#FF8A65] shrink-0" />
                ) : (
                  <Clock className="w-4 h-4 text-amber-600 dark:text-amber-400 shrink-0" />
                )}
                <span className="font-display font-bold text-sm text-slate-900 dark:text-white">
                  {order.status === 'Delivered'
                    ? 'Delivered'
                    : order.status === 'Cancelled'
                    ? 'Order Cancelled'
                    : order.status === 'Shipped'
                    ? 'In Transit · Out for Delivery'
                    : 'Processing Order'}
                </span>
              </div>
              <p className="text-xs text-slate-600 dark:text-slate-300">
                {order.cancelReason ? `Reason: ${order.cancelReason}` : order.estimatedDelivery}
              </p>
              <p className="text-[11px] font-mono text-slate-500 dark:text-slate-400 tabular-nums">
                Primary Carrier: {order.carrier.replace(/&bull;/g, '·')} &middot; AWB {order.trackingNumber}
              </p>
            </div>

            {order.status !== 'Cancelled' && onTrackRider && (
              <button
                type="button"
                onClick={() => {
                  haptics.selection();
                  onTrackRider(order);
                }}
                className="px-4 py-2 rounded-xl bg-[#FF5722] hover:bg-[#E64A19] text-white text-xs font-semibold flex items-center justify-center gap-1.5 transition-colors cursor-pointer whitespace-nowrap shrink-0"
              >
                <Bike className="w-4 h-4" />
                <span>Live Rider Map</span>
              </button>
            )}
          </div>

          {/* 2. Full Shipping Address */}
          <div className="p-4 rounded-2xl bg-white dark:bg-[#181311] border border-orange-100 dark:border-stone-800 space-y-2.5">
            <div className="flex items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                <MapPin className="w-4 h-4 text-[#FF5722] dark:text-[#FF8A65] shrink-0" />
                <h3 className="font-display font-bold text-sm text-slate-900 dark:text-white">
                  Shipping Address
                </h3>
              </div>

              <button
                type="button"
                onClick={() =>
                  copyValue(
                    'shipping-address',
                    `${shipping.recipientName || customerName}, ${shipping.fullText}${
                      shipping.phone ? `, Phone: ${shipping.phone}` : ''
                    }`,
                    'Shipping address'
                  )
                }
                className="px-2.5 py-1 rounded-lg bg-orange-50/80 hover:bg-orange-100 dark:bg-[#241B17] dark:hover:bg-[#30241E] text-slate-700 dark:text-slate-200 text-xs font-semibold inline-flex items-center gap-1 transition-colors cursor-pointer whitespace-nowrap shrink-0"
              >
                {copiedKey === 'shipping-address' ? <Check className="w-3 h-3 text-emerald-600" /> : <Copy className="w-3 h-3" />}
                <span>{copiedKey === 'shipping-address' ? 'Copied' : 'Copy Address'}</span>
              </button>
            </div>

            <div className="pl-6 space-y-1 text-xs text-slate-600 dark:text-slate-300 leading-relaxed">
              <div className="flex items-center gap-2 flex-wrap">
                <span className="font-semibold text-slate-900 dark:text-white text-sm">
                  {shipping.recipientName || customerName}
                </span>
                {(shipping.phone || customerPhone) && (
                  <>
                    <span aria-hidden="true" className="text-slate-400">&middot;</span>
                    <span className="inline-flex items-center gap-1 font-mono text-slate-600 dark:text-slate-300 tabular-nums">
                      <Phone className="w-3 h-3 text-slate-400" />
                      {shipping.phone || customerPhone}
                    </span>
                  </>
                )}
              </div>

              {shipping.line1 && shipping.line1 !== shipping.fullText ? (
                <div className="space-y-0.5">
                  <p>{shipping.line1}</p>
                  {shipping.line2 && <p>{shipping.line2}</p>}
                  {shipping.landmark && <p className="text-slate-500 dark:text-slate-400">Landmark: {shipping.landmark}</p>}
                  <p className="tabular-nums">
                    {[shipping.city, shipping.state, shipping.postalCode].filter(Boolean).join(', ')}
                    {shipping.country ? ` · ${shipping.country}` : ''}
                  </p>
                </div>
              ) : (
                <p>{shipping.fullText}</p>
              )}
            </div>
          </div>

          {/* 3. Items & Individual Tracking Links */}
          <div className="rounded-2xl bg-white dark:bg-[#181311] border border-orange-100 dark:border-stone-800 overflow-hidden">
            <div className="px-4 py-3 bg-[#FFF8F2] dark:bg-[#1F1713] border-b border-orange-100 dark:border-stone-800 flex items-center justify-between gap-2">
              <div>
                <h3 className="font-display font-bold text-sm text-slate-900 dark:text-white">
                  Items &amp; Individual Tracking ({order.items.length})
                </h3>
                <p className="text-xs text-slate-500 dark:text-slate-400 tabular-nums">
                  {totalUnits} {totalUnits === 1 ? 'item' : 'items'} in this order
                </p>
              </div>

              {(onReorderAll || onReorder) && (
                <button
                  type="button"
                  onClick={handleReorderAll}
                  className="px-3 py-1.5 rounded-xl bg-orange-50 hover:bg-[#FF5722] text-[#FF5722] hover:text-white dark:bg-[#241B17] dark:text-[#FF8A65] dark:hover:bg-[#FF5722] dark:hover:text-white text-xs font-semibold inline-flex items-center gap-1.5 transition-colors cursor-pointer whitespace-nowrap shrink-0"
                >
                  <RotateCcw className="w-3.5 h-3.5" />
                  <span>Buy All Again</span>
                </button>
              )}
            </div>

            <div className="divide-y divide-orange-100/80 dark:divide-stone-800">
              {order.items.map((item, idx) => {
                const trackUrl =
                  item.trackingUrl || buildCarrierTrackingUrl(item.carrierName, item.trackingCode);
                const itemKey = `${order.id}-${item.id}-${idx}`;
                const showItemTimeline = !!expandedItems[itemKey];
                const lineTotal = item.price * item.quantity;

                return (
                  <div key={itemKey} className="p-4 space-y-3">
                    {/* Product Info Row */}
                    <div className="flex items-start gap-3">
                      <ModalItemThumb src={item.image} alt={item.name} />

                      <div className="flex-1 min-w-0 space-y-1">
                        <div className="flex items-start justify-between gap-2">
                          <div className="min-w-0">
                            <p className="text-xs text-slate-500 dark:text-slate-400 tabular-nums truncate">
                              {item.category} &middot; Qty {item.quantity} &times; ₹{item.price.toLocaleString('en-IN')}
                            </p>
                            <h4 className="font-semibold text-sm text-slate-900 dark:text-slate-100 leading-snug">
                              {item.name}
                            </h4>
                          </div>
                          <span className="font-bold text-sm text-slate-900 dark:text-white tabular-nums shrink-0">
                            ₹{lineTotal.toLocaleString('en-IN')}
                          </span>
                        </div>

                        <div className="flex items-center gap-2 text-xs text-slate-600 dark:text-slate-300 flex-wrap">
                          <span
                            className={`font-semibold ${
                              item.itemStatus === 'Delivered'
                                ? 'text-emerald-700 dark:text-emerald-400'
                                : item.itemStatus === 'Cancelled'
                                ? 'text-rose-600 dark:text-rose-400'
                                : 'text-[#FF5722] dark:text-[#FF8A65]'
                            }`}
                          >
                            {item.itemStatus}
                          </span>
                          <span aria-hidden="true" className="text-slate-400">&middot;</span>
                          <span className="text-slate-500 dark:text-slate-400">{item.etaText}</span>
                        </div>
                      </div>
                    </div>

                    {/* Compact 4-stage Indicator */}
                    {item.itemStatus !== 'Cancelled' && (
                      <div className="grid grid-cols-4 gap-1 pt-0.5">
                        {ITEM_STAGE_STEPS.map((stageLabel, sIdx) => {
                          const stepNum = sIdx + 1;
                          const reached = item.itemStep >= stepNum;
                          return (
                            <div key={stageLabel} className="space-y-1">
                              <div
                                className={`h-1.5 rounded-full ${
                                  reached
                                    ? item.itemStep >= 4
                                      ? 'bg-emerald-600 dark:bg-emerald-500'
                                      : 'bg-[#FF5722]'
                                    : 'bg-slate-100 dark:bg-stone-800'
                                }`}
                              />
                              <div className="flex items-center gap-1 min-w-0">
                                {stepNum === 1 && <Box className={`w-3 h-3 shrink-0 ${reached ? 'text-[#FF5722]' : 'text-slate-300 dark:text-stone-600'}`} />}
                                {stepNum === 2 && <Package className={`w-3 h-3 shrink-0 ${reached ? 'text-[#FF5722]' : 'text-slate-300 dark:text-stone-600'}`} />}
                                {stepNum === 3 && <Truck className={`w-3 h-3 shrink-0 ${reached ? 'text-[#FF5722]' : 'text-slate-300 dark:text-stone-600'}`} />}
                                {stepNum === 4 && <CheckCircle2 className={`w-3 h-3 shrink-0 ${reached ? 'text-emerald-600 dark:text-emerald-400' : 'text-slate-300 dark:text-stone-600'}`} />}
                                <span
                                  className={`text-[11px] truncate ${
                                    reached
                                      ? 'font-semibold text-slate-800 dark:text-slate-200'
                                      : 'text-slate-400 dark:text-stone-500'
                                  }`}
                                >
                                  {stageLabel}
                                </span>
                              </div>
                            </div>
                          );
                        })}
                      </div>
                    )}

                    {/* Individual Item Tracking Link Bar */}
                    <div className="p-3 rounded-xl bg-[#FFFDF9] dark:bg-[#120E0C] border border-orange-100/90 dark:border-stone-800 flex flex-col sm:flex-row sm:items-center justify-between gap-2.5">
                      <div className="min-w-0 space-y-0.5">
                        <p className="text-xs font-semibold text-slate-800 dark:text-slate-200 truncate">
                          {item.carrierName}
                        </p>
                        <div className="flex items-center gap-2 text-xs font-mono text-slate-600 dark:text-slate-400 tabular-nums">
                          <span className="truncate">AWB: {item.trackingCode}</span>
                          <button
                            type="button"
                            onClick={() => copyValue(`awb-${itemKey}`, item.trackingCode, 'Tracking code')}
                            className="inline-flex items-center gap-1 text-[#FF5722] dark:text-[#FF8A65] font-sans font-semibold hover:underline cursor-pointer whitespace-nowrap"
                          >
                            {copiedKey === `awb-${itemKey}` ? <Check className="w-3 h-3" /> : <Copy className="w-3 h-3" />}
                            <span>{copiedKey === `awb-${itemKey}` ? 'Copied' : 'Copy'}</span>
                          </button>
                        </div>
                      </div>

                      <div className="flex items-center gap-2 flex-wrap shrink-0">
                        <a
                          href={trackUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="px-3 py-1.5 rounded-lg bg-[#FF5722] hover:bg-[#E64A19] text-white text-xs font-semibold inline-flex items-center gap-1.5 transition-colors whitespace-nowrap shrink-0"
                        >
                          <Truck className="w-3.5 h-3.5" />
                          <span>Track Package</span>
                          <ExternalLink className="w-3 h-3" />
                        </a>

                        <button
                          type="button"
                          onClick={() => {
                            haptics.selection();
                            setExpandedItems((prev) => ({ ...prev, [itemKey]: !prev[itemKey] }));
                          }}
                          className="px-2.5 py-1.5 rounded-lg bg-orange-50/80 hover:bg-orange-100 dark:bg-[#241B17] dark:hover:bg-[#30241E] text-slate-700 dark:text-slate-200 text-xs font-semibold inline-flex items-center gap-1 transition-colors cursor-pointer whitespace-nowrap shrink-0"
                        >
                          <span>{showItemTimeline ? 'Hide Log' : 'Checkpoints'}</span>
                          {showItemTimeline ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
                        </button>

                        {onReorder && (
                          <button
                            type="button"
                            onClick={() => {
                              haptics.addToCart();
                              onReorder(item);
                              notify(`Added ${item.name} to cart`);
                            }}
                            className="px-2.5 py-1.5 rounded-lg bg-white dark:bg-[#181311] border border-orange-200/80 dark:border-stone-700 hover:bg-orange-50 dark:hover:bg-[#241B17] text-slate-700 dark:text-slate-200 text-xs font-semibold inline-flex items-center gap-1 transition-colors cursor-pointer whitespace-nowrap shrink-0"
                          >
                            <RotateCcw className="w-3 h-3" />
                            <span>Buy Again</span>
                          </button>
                        )}
                      </div>
                    </div>

                    {/* Expandable Item Checkpoints */}
                    {showItemTimeline && (
                      <div className="pl-2 space-y-2 pt-1">
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
                                  className={`font-semibold ${
                                    ev.completed
                                      ? 'text-slate-800 dark:text-slate-100'
                                      : 'text-slate-400 dark:text-stone-500'
                                  }`}
                                >
                                  {ev.label}
                                </p>
                                <p className="text-[11px] text-slate-500 dark:text-slate-400">
                                  {ev.detail}
                                </p>
                              </div>
                            </div>
                            <span className="font-mono text-[11px] text-slate-400 dark:text-slate-500 tabular-nums shrink-0">
                              {ev.timestamp}
                            </span>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>

          {/* 4. Itemized Price Breakdown & Payment Information */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 items-stretch">
            {/* Itemized Price Breakdown */}
            <div className="p-4 rounded-2xl bg-white dark:bg-[#181311] border border-orange-100 dark:border-stone-800 space-y-3 flex flex-col justify-between">
              <div className="space-y-3">
                <div className="flex items-center gap-2">
                  <Receipt className="w-4 h-4 text-[#FF5722] dark:text-[#FF8A65] shrink-0" />
                  <h3 className="font-display font-bold text-sm text-slate-900 dark:text-white">
                    Itemized Price Breakdown
                  </h3>
                </div>

                {/* Line Items */}
                <div className="space-y-1.5 pb-2.5 border-b border-orange-100/80 dark:border-stone-800 text-xs">
                  {order.items.map((item, idx) => (
                    <div
                      key={`${item.id}-${idx}`}
                      className="flex items-start justify-between gap-3 text-slate-600 dark:text-slate-300"
                    >
                      <span className="truncate">
                        {item.name} <span className="text-slate-400 tabular-nums">&times; {item.quantity}</span>
                      </span>
                      <span className="font-medium text-slate-900 dark:text-slate-100 tabular-nums shrink-0">
                        ₹{(item.price * item.quantity).toLocaleString('en-IN')}
                      </span>
                    </div>
                  ))}
                </div>

                {/* Fee & Tax Summary */}
                <div className="space-y-1.5 text-xs">
                  <div className="flex items-center justify-between text-slate-600 dark:text-slate-400">
                    <span>Items Subtotal ({totalUnits} {totalUnits === 1 ? 'unit' : 'units'})</span>
                    <span className="tabular-nums text-slate-800 dark:text-slate-200 font-medium">
                      ₹{breakdown.itemsSubtotal.toLocaleString('en-IN')}
                    </span>
                  </div>

                  <div className="flex items-center justify-between text-slate-600 dark:text-slate-400">
                    <span>Delivery &amp; Shipping</span>
                    <span className="tabular-nums font-semibold text-slate-800 dark:text-slate-200">
                      {breakdown.deliveryFee === 0
                        ? 'Included'
                        : `₹${breakdown.deliveryFee.toLocaleString('en-IN')}`}
                    </span>
                  </div>

                  {breakdown.platformFee > 0 && (
                    <div className="flex items-center justify-between text-slate-600 dark:text-slate-400">
                      <span>Platform &amp; Handling Fee</span>
                      <span className="tabular-nums text-slate-800 dark:text-slate-200 font-medium">
                        ₹{breakdown.platformFee.toLocaleString('en-IN')}
                      </span>
                    </div>
                  )}

                  {breakdown.discount > 0 && (
                    <div className="flex items-center justify-between text-emerald-700 dark:text-emerald-400">
                      <span>Discount Applied</span>
                      <span className="tabular-nums font-semibold">
                        −₹{breakdown.discount.toLocaleString('en-IN')}
                      </span>
                    </div>
                  )}

                  <div className="flex items-center justify-between text-slate-500 dark:text-slate-400">
                    <span>Estimated GST (18% Included)</span>
                    <span className="tabular-nums">
                      ₹{breakdown.taxIncluded.toLocaleString('en-IN')}
                    </span>
                  </div>
                </div>
              </div>

              <div className="pt-3 border-t border-orange-100 dark:border-stone-800 flex items-center justify-between">
                <span className="font-display font-bold text-sm text-slate-900 dark:text-white">
                  {order.status === 'Cancelled' ? 'Order Value' : 'Grand Total'}
                </span>
                <span className="font-display font-bold text-base text-[#FF5722] dark:text-[#FF8A65] tabular-nums">
                  ₹{breakdown.grandTotal.toLocaleString('en-IN')}
                </span>
              </div>
            </div>

            {/* Payment & Timeline Info */}
            <div className="p-4 rounded-2xl bg-white dark:bg-[#181311] border border-orange-100 dark:border-stone-800 space-y-4">
              <div className="space-y-2.5">
                <div className="flex items-center gap-2">
                  <CreditCard className="w-4 h-4 text-[#FF5722] dark:text-[#FF8A65] shrink-0" />
                  <h3 className="font-display font-bold text-sm text-slate-900 dark:text-white">
                    Payment Details
                  </h3>
                </div>

                <div className="space-y-1.5 text-xs">
                  <div className="flex items-center justify-between gap-2 text-slate-600 dark:text-slate-400">
                    <span>Method</span>
                    <span className="font-semibold text-slate-900 dark:text-slate-100 text-right">
                      {order.paymentMethod}
                    </span>
                  </div>
                  <div className="flex items-center justify-between gap-2 text-slate-600 dark:text-slate-400">
                    <span>Status</span>
                    <span className="inline-flex items-center gap-1 font-semibold text-emerald-700 dark:text-emerald-400">
                      <ShieldCheck className="w-3.5 h-3.5 shrink-0" />
                      <span>{paymentBadgeText}</span>
                    </span>
                  </div>
                  {order.gatewayPaymentId && (
                    <div className="flex items-center justify-between gap-2 text-slate-600 dark:text-slate-400">
                      <span>Transaction ID</span>
                      <span className="font-mono text-[11px] text-slate-800 dark:text-slate-200 tabular-nums truncate">
                        {order.gatewayPaymentId}
                      </span>
                    </div>
                  )}
                </div>
              </div>

              {/* Order Activity Timeline */}
              {order.timeline && order.timeline.length > 0 && (
                <div className="pt-3 border-t border-orange-100 dark:border-stone-800 space-y-2">
                  <h4 className="text-xs font-semibold text-slate-500 dark:text-slate-400">
                    Order Activity Log
                  </h4>
                  <div className="space-y-2">
                    {order.timeline.map((event, idx) => (
                      <div key={idx} className="flex items-start justify-between gap-2 text-xs">
                        <div className="flex items-start gap-2 min-w-0">
                          <CheckCircle2
                            className={`w-3.5 h-3.5 mt-0.5 shrink-0 ${
                              event.completed
                                ? 'text-emerald-600 dark:text-emerald-400'
                                : 'text-slate-300 dark:text-stone-600'
                            }`}
                          />
                          <div className="min-w-0">
                            <p className="font-semibold text-slate-800 dark:text-slate-200 leading-tight">
                              {event.status}
                            </p>
                            <p className="text-[11px] text-slate-500 dark:text-slate-400 leading-tight mt-0.5">
                              {event.description}
                            </p>
                          </div>
                        </div>
                        <span className="font-mono text-[10px] text-slate-400 dark:text-slate-500 tabular-nums shrink-0">
                          {event.time}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Sticky Footer Actions */}
        <div className="sticky bottom-0 z-20 p-4 bg-white/95 dark:bg-[#181311]/95 backdrop-blur-md border-t border-orange-100 dark:border-stone-800 space-y-2">
          {(onReorderAll || onReorder) && (
            <button
              type="button"
              onClick={handleReorderAll}
              className={`w-full py-2.5 px-4 rounded-xl text-xs font-bold transition-colors flex items-center justify-center gap-2 cursor-pointer ${
                reorderedAll
                  ? 'bg-emerald-600 text-white'
                  : 'bg-[#FF5722] hover:bg-[#E64A19] text-white'
              }`}
            >
              {reorderedAll ? (
                <>
                  <CheckCircle2 className="w-4 h-4" />
                  <span>Added {totalUnits} {totalUnits === 1 ? 'Item' : 'Items'} to Cart</span>
                </>
              ) : (
                <>
                  <RotateCcw className="w-4 h-4" />
                  <ShoppingCart className="w-4 h-4" />
                  <span>
                    Reorder Entire Order ({totalUnits} {totalUnits === 1 ? 'item' : 'items'} · ₹{breakdown.grandTotal.toLocaleString('en-IN')})
                  </span>
                </>
              )}
            </button>
          )}

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={handleDownloadInvoice}
              className="flex-1 py-2 px-3 rounded-xl bg-orange-50/80 hover:bg-orange-100 dark:bg-[#241B17] dark:hover:bg-[#30241E] text-slate-800 dark:text-slate-200 text-xs font-semibold flex items-center justify-center gap-1.5 transition-colors cursor-pointer whitespace-nowrap"
            >
              <FileText className="w-3.5 h-3.5 text-[#FF5722] dark:text-[#FF8A65]" />
              <span>Download Receipt</span>
            </button>

            {onNavigateToCart && (
              <button
                type="button"
                onClick={() => {
                  haptics.light();
                  onClose();
                  onNavigateToCart();
                }}
                className="flex-1 py-2 px-3 rounded-xl bg-orange-50/80 hover:bg-orange-100 dark:bg-[#241B17] dark:hover:bg-[#30241E] text-slate-800 dark:text-slate-200 text-xs font-semibold flex items-center justify-center gap-1.5 transition-colors cursor-pointer whitespace-nowrap"
              >
                <ShoppingCart className="w-3.5 h-3.5 text-[#FF5722] dark:text-[#FF8A65]" />
                <span>View Cart</span>
              </button>
            )}

            <button
              type="button"
              onClick={() => {
                haptics.light();
                if (onOpenSupport) {
                  onClose();
                  onOpenSupport();
                } else {
                  notify(`Support request opened for Order #${order.id}`);
                }
              }}
              className="flex-1 py-2 px-3 rounded-xl bg-orange-50/80 hover:bg-orange-100 dark:bg-[#241B17] dark:hover:bg-[#30241E] text-slate-800 dark:text-slate-200 text-xs font-semibold flex items-center justify-center gap-1.5 transition-colors cursor-pointer whitespace-nowrap"
            >
              <ExternalLink className="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400" />
              <span>Need Help?</span>
            </button>

            <button
              type="button"
              onClick={() => {
                haptics.light();
                onClose();
              }}
              className="py-2 px-4 rounded-xl bg-slate-100 hover:bg-slate-200 dark:bg-stone-800 dark:hover:bg-stone-700 text-slate-700 dark:text-slate-200 text-xs font-semibold transition-colors cursor-pointer whitespace-nowrap shrink-0"
            >
              Close
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default OrderDetailsModal;
