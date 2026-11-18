import React, { useState, useEffect } from 'react';
import {
  Package,
  Truck,
  CheckCircle2,
  Clock,
  ChevronRight,
  ArrowLeft,
  RotateCcw,
  FileText,
  MapPin,
  Search,
  ExternalLink,
  X,
  CreditCard,
  ShieldCheck,
  Copy,
  ShoppingCart,
  Download,
  BarChart2,
  TrendingUp,
  ChevronDown,
  ChevronUp,
  Navigation,
} from 'lucide-react';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  Cell,
  CartesianGrid,
} from 'recharts';
import { haptics } from '../utils/haptics';

export type OrderStatus = 'Processing' | 'Shipped' | 'Delivered' | 'Cancelled';

export interface OrderItem {
  id: string;
  name: string;
  price: number;
  quantity: number;
  image: string;
  category: string;
}

export interface Order {
  id: string;
  date: string;
  total: number;
  items: OrderItem[];
  status: OrderStatus;
  statusStep: 1 | 2 | 3; // 1: Processing, 2: Shipped, 3: Delivered
  estimatedDelivery: string;
  trackingNumber: string;
  carrier: string;
  paymentMethod: string;
  deliveryAddress: string;
  timeline: {
    status: string;
    time: string;
    description: string;
    completed: boolean;
  }[];
}

interface OrdersSectionProps {
  orders: Order[];
  onBack: () => void;
  onReorder: (item: OrderItem) => void;
  onReorderAll?: (items: OrderItem[]) => void;
  onShowToast: (message: string) => void;
  onNavigateToCart?: () => void;
  customerName?: string;
  customerPhone?: string;
  onOpenSupport?: () => void;
}

interface OrderDetailModalProps {
  order: Order | null;
  isOpen: boolean;
  onClose: () => void;
  onReorder: (item: OrderItem) => void;
  onReorderAll?: (items: OrderItem[]) => void;
  onShowToast: (message: string) => void;
  onNavigateToCart?: () => void;
  onTrackRider?: (order: Order) => void;
  customerName?: string;
  customerPhone?: string;
  onOpenSupport?: () => void;
}

export const OrderDetailModal: React.FC<OrderDetailModalProps> = ({
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
  const [copiedTracking, setCopiedTracking] = useState(false);
  const [reorderedSuccess, setReorderedSuccess] = useState(false);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
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

  const handleCopyTracking = () => {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(order.trackingNumber);
      haptics.saved();
      setCopiedTracking(true);
      setTimeout(() => setCopiedTracking(false), 2000);
      onShowToast(`Tracking number ${order.trackingNumber} copied! 📋`);
    }
  };

  const handleDownloadInvoice = () => {
    haptics.subtle();
    onShowToast(`Invoice for ${order.id} downloaded successfully! 📄`);
  };

  const totalItemsCount = order.items.reduce((sum, item) => sum + item.quantity, 0);

  const handleReorderAll = () => {
    if (onReorderAll) {
      onReorderAll(order.items);
    } else {
      haptics.addToCart();
      order.items.forEach((item) => onReorder(item));
      onShowToast(`Reordered! ${totalItemsCount} product${totalItemsCount > 1 ? 's' : ''} added to your cart 🛒`);
    }
    setReorderedSuccess(true);
    setTimeout(() => setReorderedSuccess(false), 2500);
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="order-detail-title"
      className="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-0 sm:p-4"
    >
      {/* Blurred Backdrop */}
      <div
        onClick={() => {
          haptics.light();
          onClose();
        }}
        className="fixed inset-0 bg-black/70 backdrop-blur-xs transition-opacity animate-in fade-in duration-200"
      />

      {/* Modal / Bottom Sheet Content */}
      <div className="relative w-full max-w-[440px] max-h-[92vh] bg-white dark:bg-[#181311] text-slate-900 dark:text-slate-100 rounded-t-3xl sm:rounded-3xl shadow-2xl flex flex-col z-10 overflow-hidden animate-in slide-in-from-bottom-8 duration-300 border border-orange-100/80 dark:border-orange-950/50">
        
        {/* Top Header */}
        <div className="sticky top-0 z-20 flex items-center justify-between px-4 py-3.5 bg-gradient-to-b from-[#FFE5CC]/70 via-[#FFF8F2] to-white dark:from-[#26160C] dark:via-[#1B120E] dark:to-[#181311] backdrop-blur-md border-b border-orange-100/80 dark:border-orange-950/50">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-orange-50 dark:bg-orange-950/50 text-[#FF5722] dark:text-[#FF8A65] flex items-center justify-center shrink-0">
              <Package className="w-4 h-4 stroke-[2.2]" />
            </div>
            <div>
              <div className="flex items-center gap-1.5">
                <h2 id="order-detail-title" className="font-mono font-bold text-xs sm:text-sm text-[#FF5722] dark:text-[#FF8A65]">
                  {order.id}
                </h2>
                <span className="text-[10px] text-slate-400 dark:text-slate-500 font-medium">
                  &bull; {order.date}
                </span>
              </div>
              <p className="text-[10px] text-slate-500 dark:text-slate-400">
                {totalItemsCount} item{totalItemsCount > 1 ? 's' : ''} &bull; Total ₹{order.total.toLocaleString('en-IN')}
              </p>
            </div>
          </div>

          <button
            onClick={() => {
              haptics.light();
              onClose();
            }}
            aria-label="Close order details"
            className="p-1.5 text-slate-400 hover:text-slate-900 dark:hover:text-white rounded-xl hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Scrollable Modal Body */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4 no-scrollbar">
          
          {/* Status Header Strip */}
          <div className="p-3.5 rounded-2xl bg-gradient-to-r from-[#FFF4E8] via-orange-50/70 to-amber-50/60 dark:from-[#26160C] dark:via-[#211A17] dark:to-[#26160C] border border-orange-200/80 dark:border-orange-900/40 flex items-center justify-between">
            <div>
              <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 block mb-0.5">
                Current Status
              </span>
              <div className="flex items-center gap-1.5">
                {order.status === 'Processing' && (
                  <span className="inline-flex items-center gap-1.5 text-xs font-black text-amber-700 dark:text-amber-400">
                    <span className="w-2 h-2 rounded-full bg-amber-500" />
                    Processing &bull; Packing at Warehouse
                  </span>
                )}
                {order.status === 'Shipped' && (
                  <span className="inline-flex items-center gap-1.5 text-xs font-black text-[#FF5722] dark:text-[#FF8A65]">
                    <Truck className="w-4 h-4 text-[#FF5722] dark:text-[#FF8A65]" />
                    Shipped &bull; Out for Delivery
                  </span>
                )}
                {order.status === 'Delivered' && (
                  <span className="inline-flex items-center gap-1.5 text-xs font-black text-emerald-700 dark:text-emerald-400">
                    <CheckCircle2 className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />
                    Delivered &bull; Signed by Customer
                  </span>
                )}
              </div>
            </div>

            <span className="px-2.5 py-1 rounded-full text-[10px] font-extrabold uppercase bg-white dark:bg-[#211A17] text-slate-800 dark:text-slate-200 shadow-xs border border-orange-200/70 dark:border-orange-900/50">
              {order.status}
            </span>
          </div>

          {/* 1. Visual 3-Step Status Tracker */}
          <div className="p-4 bg-[#FFFDF9] dark:bg-[#211A17] rounded-2xl border border-orange-100/80 dark:border-orange-950/50">
            <h3 className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-3 flex items-center justify-between">
              <span>Order Tracking Journey</span>
              <span className="font-mono text-[10px] text-[#FF5722] dark:text-[#FF8A65] lowercase">
                Step {order.statusStep} of 3
              </span>
            </h3>

            <div className="relative flex items-center justify-between px-3 my-2">
              <div className="absolute left-7 right-7 top-3.5 h-1 bg-slate-200 dark:bg-slate-700 -z-0">
                <div
                  className="h-full bg-gradient-to-r from-[#FF5722] to-emerald-500 transition-all duration-500"
                  style={{
                    width:
                      order.statusStep === 1
                        ? '15%'
                        : order.statusStep === 2
                        ? '65%'
                        : '100%',
                  }}
                />
              </div>

              <div className="flex flex-col items-center gap-1.5 z-10">
                <div
                  className={`w-7 h-7 rounded-full flex items-center justify-center font-bold text-xs transition-colors ${
                    order.statusStep >= 1
                      ? 'bg-[#FF5722] text-white shadow-xs'
                      : 'bg-slate-200 text-slate-500'
                  }`}
                >
                  <Package className="w-3.5 h-3.5" />
                </div>
                <span className="text-[10px] font-extrabold text-slate-800 dark:text-slate-200">
                  Processing
                </span>
              </div>

              <div className="flex flex-col items-center gap-1.5 z-10">
                <div
                  className={`w-7 h-7 rounded-full flex items-center justify-center font-bold text-xs transition-colors ${
                    order.statusStep >= 2
                      ? 'bg-[#FF5722] text-white shadow-xs'
                      : 'bg-slate-200 dark:bg-slate-700 text-slate-400'
                  }`}
                >
                  <Truck className="w-3.5 h-3.5" />
                </div>
                <span
                  className={`text-[10px] font-extrabold ${
                    order.statusStep >= 2
                      ? 'text-slate-800 dark:text-slate-200'
                      : 'text-slate-400'
                  }`}
                >
                  Shipped
                </span>
              </div>

              {/* Step 3: Delivered */}
              <div className="flex flex-col items-center gap-1.5 z-10">
                <div
                  className={`w-7 h-7 rounded-full flex items-center justify-center font-bold text-xs transition-colors ${
                    order.statusStep >= 3
                      ? 'bg-emerald-600 text-white shadow-xs'
                      : 'bg-slate-200 dark:bg-slate-700 text-slate-400'
                  }`}
                >
                  <CheckCircle2 className="w-3.5 h-3.5" />
                </div>
                <span
                  className={`text-[10px] font-extrabold ${
                    order.statusStep >= 3
                      ? 'text-emerald-600 dark:text-emerald-400'
                      : 'text-slate-400'
                  }`}
                >
                  Delivered
                </span>
              </div>
            </div>

            {/* Carrier & Tracking Number Strip */}
            <div className="mt-4 p-2.5 rounded-xl bg-white dark:bg-[#181311] border border-orange-100/80 dark:border-orange-950/50 text-xs flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Truck className="w-4 h-4 text-[#FF5722] dark:text-[#FF8A65]" />
                <div>
                  <p className="font-bold text-slate-800 dark:text-slate-200 text-[11px] leading-tight">
                    {order.carrier}
                  </p>
                  <p className="text-[10px] text-slate-500 font-mono mt-0.5">
                    AWB: {order.trackingNumber}
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={handleCopyTracking}
                title="Copy tracking number"
                className="p-1.5 hover:bg-orange-50 dark:hover:bg-[#261D19] rounded-lg text-slate-500 hover:text-slate-800 dark:hover:text-white transition-colors flex items-center gap-1 text-[10px] font-bold"
              >
                <Copy className="w-3 h-3" />
                <span>{copiedTracking ? 'Copied' : 'Copy'}</span>
              </button>
            </div>

            {/* SHIPROCKET / COURIER SHIPMENT TRACKING CARD */}
            <div className="mt-2.5 p-3 rounded-2xl bg-gradient-to-r from-[#FF5722]/10 via-orange-500/10 to-amber-500/10 border border-[#FF5722]/30 dark:border-[#FF8A65]/30 flex items-center justify-between gap-2.5 shadow-xs">
              <div className="flex items-center gap-2.5 min-w-0">
                <div className="w-9 h-9 rounded-xl bg-[#FF5722] text-white flex items-center justify-center shrink-0 shadow-sm">
                  <Truck className="w-5 h-5 text-white" />
                </div>
                <div className="min-w-0">
                  <h4 className="text-xs font-bold text-slate-900 dark:text-white truncate">
                    Courier Shipment Tracking
                  </h4>
                  <p className="text-[10px] text-slate-500 dark:text-slate-400 truncate font-mono">
                    {order.carrier} &bull; {order.trackingNumber}
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => {
                  haptics.selection();
                  if (onTrackRider) {
                    onTrackRider(order);
                  }
                }}
                className="px-3 py-1.5 bg-[#FF5722] hover:bg-[#E64A19] text-white text-[11px] font-bold rounded-xl shadow-xs transition-all flex items-center gap-1 active:scale-95 cursor-pointer shrink-0"
              >
                <Navigation className="w-3.5 h-3.5" />
                <span>Track</span>
              </button>
            </div>

            {/* Expected Delivery Callout */}
            <div className="mt-2.5 p-2 rounded-xl bg-[#FFF4E8] dark:bg-[#26160C] text-[11px] text-[#9A3412] dark:text-orange-200 flex items-center gap-2">
              <Clock className="w-3.5 h-3.5 text-[#FF5722] dark:text-[#FF8A65] shrink-0" />
              <span className="font-medium">{order.estimatedDelivery}</span>
            </div>

            {/* Detailed Tracking Timeline */}
            <div className="mt-3.5 pt-3 border-t border-slate-200/70 dark:border-slate-800/80 space-y-2.5">
              <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 block">
                Activity Logs
              </span>
              <div className="space-y-2">
                {order.timeline.map((event, idx) => (
                  <div key={idx} className="flex items-start gap-2.5 text-xs">
                    <span
                      className={`w-2 h-2 rounded-full mt-1.5 shrink-0 ${
                        event.completed
                          ? 'bg-emerald-500 ring-2 ring-emerald-200 dark:ring-emerald-950'
                          : 'bg-slate-300 dark:bg-slate-600'
                      }`}
                    />
                    <div className="flex-1">
                      <p className="font-bold text-slate-800 dark:text-slate-200 leading-tight">
                        {event.status}
                      </p>
                      <p className="text-[10px] text-slate-500 leading-tight mt-0.5">
                        {event.description}
                      </p>
                    </div>
                    <span className="text-[9px] text-slate-400 font-mono shrink-0">
                      {event.time}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* 2. Full Product Items List */}
          <div className="space-y-2.5">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                  Purchased Items ({order.items.length})
                </h3>
                <span className="text-[10px] text-slate-400 font-medium">
                  {totalItemsCount} total unit{totalItemsCount > 1 ? 's' : ''}
                </span>
              </div>

              <button
                type="button"
                onClick={handleReorderAll}
                className="px-2.5 py-1 bg-[#FF5722]/10 hover:bg-[#FF5722] text-[#FF5722] hover:text-white dark:text-[#FF8A65] dark:hover:text-white text-[10px] font-bold rounded-lg transition-all flex items-center gap-1 active:scale-95 cursor-pointer shadow-xs"
              >
                <RotateCcw className="w-3 h-3" />
                <span>Reorder All ({totalItemsCount})</span>
              </button>
            </div>

            <div className="space-y-2">
              {order.items.map((item) => (
                <div
                  key={item.id}
                  className="p-3 bg-white dark:bg-[#211A17] rounded-2xl border border-orange-100/80 dark:border-orange-950/50 flex items-center justify-between gap-3 shadow-xs"
                >
                  <img
                    src={item.image}
                    alt={item.name}
                    className="w-14 h-14 rounded-xl object-cover bg-orange-50/40 dark:bg-[#181311] border border-orange-100/70 dark:border-orange-950/50 shrink-0"
                  />
                  <div className="flex-1 min-w-0">
                    <span className="text-[9px] font-bold text-[#FF5722] dark:text-[#FF8A65] uppercase tracking-wide block">
                      {item.category}
                    </span>
                    <h4 className="font-bold text-xs text-slate-800 dark:text-slate-100 line-clamp-2 leading-tight">
                      {item.name}
                    </h4>
                    <div className="flex items-center gap-2 mt-1">
                      <span className="text-xs font-extrabold text-slate-900 dark:text-white">
                        ₹{(item.price * item.quantity).toLocaleString('en-IN')}
                      </span>
                      <span className="text-[10px] text-slate-400">
                        (₹{item.price.toLocaleString('en-IN')} &times; {item.quantity})
                      </span>
                    </div>
                  </div>

                  <button
                    type="button"
                    onClick={() => {
                      onReorder(item);
                      onShowToast(`Added ${item.name.slice(0, 18)}... to cart!`);
                    }}
                    className="px-2.5 py-1.5 bg-[#FF5722]/10 hover:bg-[#FF5722] text-[#FF5722] hover:text-white dark:text-[#FF8A65] dark:hover:text-white text-[11px] font-bold rounded-xl transition-all flex items-center gap-1 active:scale-95 shrink-0 cursor-pointer shadow-xs"
                  >
                    <RotateCcw className="w-3 h-3" />
                    <span>Reorder</span>
                  </button>
                </div>
              ))}
            </div>
          </div>

          <div className="p-3.5 bg-white dark:bg-[#211A17] rounded-2xl border border-orange-100/80 dark:border-orange-950/50 space-y-2">
            <div className="flex items-center gap-2 text-slate-900 dark:text-white">
              <MapPin className="w-4 h-4 text-[#FF5722] dark:text-[#FF8A65] shrink-0" />
              <h3 className="font-display font-bold text-xs">Delivery Address</h3>
            </div>
            <div className="pl-6 text-xs text-slate-600 dark:text-slate-300 leading-relaxed">
              <p className="font-bold text-slate-900 dark:text-white mb-0.5">{customerName}</p>
              <p>{order.deliveryAddress}</p>
              <p className="text-[10px] text-slate-400 mt-1 font-mono">Contact: {customerPhone}</p>
            </div>
          </div>

          <div className="p-3.5 bg-white dark:bg-[#211A17] rounded-2xl border border-orange-100/80 dark:border-orange-950/50 space-y-2">
            <div className="flex items-center gap-2 text-slate-900 dark:text-white">
              <CreditCard className="w-4 h-4 text-[#FF5722] dark:text-[#FF8A65] shrink-0" />
              <h3 className="font-display font-bold text-xs">Payment Information</h3>
            </div>
            <div className="pl-6 text-xs text-slate-600 dark:text-slate-300 space-y-1">
              <div className="flex items-center justify-between">
                <span>Method:</span>
                <span className="font-bold text-slate-800 dark:text-slate-100">{order.paymentMethod}</span>
              </div>
              <div className="flex items-center justify-between">
                <span>Payment Status:</span>
                <span className="text-emerald-600 dark:text-emerald-400 font-bold flex items-center gap-1">
                  <ShieldCheck className="w-3 h-3" />
                  Verified &amp; Paid
                </span>
              </div>
            </div>
          </div>

          <div className="p-3.5 bg-[#FFFDF9] dark:bg-[#15100E] rounded-2xl border border-orange-100/80 dark:border-orange-950/50 space-y-1.5 text-xs">
            <h4 className="font-bold uppercase tracking-wider text-[10px] text-slate-400 mb-1">
              Payment Summary
            </h4>
            <div className="flex items-center justify-between text-slate-600 dark:text-slate-400">
              <span>Items Total ({totalItemsCount} items)</span>
              <span>₹{order.total.toLocaleString('en-IN')}</span>
            </div>
            <div className="flex items-center justify-between text-slate-600 dark:text-slate-400">
              <span>Delivery Charges</span>
              <span className="text-emerald-600 dark:text-emerald-400 font-bold">FREE</span>
            </div>
            <div className="flex items-center justify-between text-slate-600 dark:text-slate-400">
              <span>Tax &amp; Handling</span>
              <span>Included</span>
            </div>
            <div className="flex items-center justify-between font-black text-sm text-slate-900 dark:text-white pt-2 border-t border-orange-100 dark:border-orange-950/60">
              <span>Total Paid</span>
              <span className="text-[#FF5722] dark:text-[#FF8A65]">₹{order.total.toLocaleString('en-IN')}</span>
            </div>
          </div>
        </div>

        <div className="sticky bottom-0 z-20 p-3 bg-white/95 dark:bg-[#181311]/95 backdrop-blur-md border-t border-orange-100/80 dark:border-orange-950/50 space-y-2">
          <button
            type="button"
            onClick={handleReorderAll}
            className={`w-full py-3 px-4 rounded-xl text-xs font-black active:scale-[0.98] transition-all shadow-md flex items-center justify-center gap-2 cursor-pointer ${
              reorderedSuccess
                ? 'bg-emerald-600 text-white shadow-emerald-900/20'
                : 'bg-[#FF5722] hover:bg-[#E64A19] text-white shadow-orange-900/25'
            }`}
          >
            {reorderedSuccess ? (
              <>
                <CheckCircle2 className="w-4 h-4 animate-in zoom-in" />
                <span>All {totalItemsCount} Products Added to Cart! 🛒</span>
              </>
            ) : (
              <>
                <RotateCcw className="w-4 h-4 stroke-[2.5]" />
                <ShoppingCart className="w-4 h-4" />
                <span>Reorder Entire Order ({totalItemsCount} items &bull; ₹{order.total.toLocaleString('en-IN')})</span>
              </>
            )}
          </button>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={handleDownloadInvoice}
              className="flex-1 py-2 px-3 bg-orange-50/70 hover:bg-orange-100/80 dark:bg-[#211A17] dark:hover:bg-[#2C221E] text-slate-800 dark:text-slate-200 rounded-xl text-xs font-bold flex items-center justify-center gap-1.5 active:scale-95 transition-all shadow-xs cursor-pointer"
            >
              <FileText className="w-3.5 h-3.5 text-[#FF5722] dark:text-[#FF8A65]" />
              <span>Invoice</span>
            </button>

            <button
              type="button"
              onClick={() => {
                haptics.light();
                if (onOpenSupport) {
                  onClose();
                  onOpenSupport();
                } else {
                  onShowToast(`Support ticket opened for Order ${order.id}`);
                }
              }}
              className="flex-1 py-2 px-3 bg-orange-50/70 hover:bg-orange-100/80 dark:bg-[#211A17] dark:hover:bg-[#2C221E] text-slate-800 dark:text-slate-200 rounded-xl text-xs font-bold flex items-center justify-center gap-1.5 active:scale-95 transition-all shadow-xs cursor-pointer"
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
              className="py-2 px-4 bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 rounded-xl text-xs font-bold active:scale-95 transition-all cursor-pointer"
            >
              Close
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export interface LiveRiderTrackingModalProps {
  order: Order | null;
  isOpen: boolean;
  onClose: () => void;
  onShowToast: (message: string) => void;
}

export const LiveRiderTrackingModal: React.FC<LiveRiderTrackingModalProps> = ({
  order,
  isOpen,
  onClose,
  onShowToast,
}) => {
  const [copiedAwb, setCopiedAwb] = useState(false);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
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

  const awbCode = order.trackingNumber || 'Pending Courier Pickup';
  const shiprocketTrackingUrl =
    order.trackingNumber && !order.trackingNumber.startsWith('PENDING')
      ? `https://shiprocket.co/tracking/${encodeURIComponent(order.trackingNumber)}`
      : null;

  const handleCopyAwb = () => {
    haptics.saved();
    if (navigator.clipboard && order.trackingNumber) {
      navigator.clipboard.writeText(order.trackingNumber).catch(() => {});
    }
    setCopiedAwb(true);
    onShowToast(`Tracking code ${awbCode} copied`);
    setTimeout(() => setCopiedAwb(false), 2000);
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="rider-tracking-title"
      className="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-0 sm:p-4"
    >
      <div
        onClick={() => {
          haptics.light();
          onClose();
        }}
        className="fixed inset-0 bg-black/75 backdrop-blur-xs transition-opacity animate-in fade-in duration-200"
      />

      <div className="relative w-full max-w-[460px] max-h-[92vh] bg-white dark:bg-[#181311] text-slate-900 dark:text-slate-100 rounded-t-3xl sm:rounded-3xl shadow-2xl flex flex-col z-10 overflow-hidden animate-in slide-in-from-bottom-8 duration-300 border border-orange-100/80 dark:border-orange-950/50">
        <div className="sticky top-0 z-20 flex items-center justify-between px-4 py-3 bg-gradient-to-r from-[#FF5722] to-[#E64A19] text-white shadow-sm">
          <div className="flex items-center gap-2.5 min-w-0">
            <div className="w-8 h-8 rounded-xl bg-white/20 flex items-center justify-center shrink-0">
              <Truck className="w-5 h-5 text-white" />
            </div>
            <div className="min-w-0">
              <h2 id="rider-tracking-title" className="font-display font-black text-sm sm:text-base leading-tight truncate">
                Shipment &amp; Courier Tracking
              </h2>
              <p className="text-[10px] text-orange-100 truncate">
                Order <strong className="font-mono">{order.id}</strong> &bull; {order.status}
              </p>
            </div>
          </div>
          <button
            onClick={() => {
              haptics.light();
              onClose();
            }}
            aria-label="Close tracking"
            className="p-1.5 text-white/80 hover:text-white rounded-full hover:bg-white/20 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-4 space-y-3.5 no-scrollbar">
          {/* Real Courier & AWB Summary */}
          <div className="p-3.5 rounded-2xl bg-[#FFFDF9] dark:bg-[#211A17] border border-orange-100/80 dark:border-orange-950/50 space-y-2.5">
            <div className="flex items-center justify-between gap-2">
              <div className="min-w-0">
                <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 block">
                  Courier Partner &amp; AWB
                </span>
                <p className="text-xs font-extrabold text-slate-900 dark:text-white truncate mt-0.5">
                  {order.carrier || 'Shiprocket Partner Courier'}
                </p>
                <p className="text-[11px] font-mono font-bold text-[#FF5722] dark:text-[#FF8A65] mt-0.5 truncate">
                  {awbCode}
                </p>
              </div>
              <button
                type="button"
                onClick={handleCopyAwb}
                className="px-2.5 py-1.5 bg-[#FFF4E8] dark:bg-[#181311] border border-orange-200 dark:border-orange-900/50 rounded-xl text-[11px] font-bold text-[#FF5722] flex items-center gap-1 cursor-pointer shrink-0"
              >
                <Copy className="w-3 h-3" />
                <span>{copiedAwb ? 'Copied' : 'Copy AWB'}</span>
              </button>
            </div>

            <div className="p-2.5 rounded-xl bg-[#FFF4E8] dark:bg-[#26160C] text-[11px] text-[#9A3412] dark:text-orange-200 flex items-center gap-2">
              <Clock className="w-3.5 h-3.5 text-[#FF5722] dark:text-[#FF8A65] shrink-0" />
              <span className="font-semibold">{order.estimatedDelivery}</span>
            </div>

            {shiprocketTrackingUrl && (
              <a
                href={shiprocketTrackingUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="w-full py-2 px-3 rounded-xl bg-[#FF5722] hover:bg-[#E64A19] text-white text-xs font-bold flex items-center justify-center gap-1.5 shadow-xs transition-colors"
              >
                <ExternalLink className="w-3.5 h-3.5" />
                <span>Open Live Shiprocket Tracking</span>
              </a>
            )}
          </div>

          {/* Delivery Destination */}
          <div className="p-3.5 rounded-2xl bg-white dark:bg-[#211A17] border border-orange-100/80 dark:border-orange-950/50 space-y-1">
            <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 flex items-center gap-1">
              <MapPin className="w-3 h-3 text-[#FF5722]" />
              <span>Delivery Destination</span>
            </span>
            <p className="text-xs font-medium text-slate-700 dark:text-slate-200 leading-relaxed">
              {order.deliveryAddress}
            </p>
          </div>

          {/* Real Order Milestones */}
          <div className="p-3.5 rounded-2xl bg-[#FFFDF9] dark:bg-[#211A17] border border-orange-100/80 dark:border-orange-950/50 space-y-2.5">
            <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block">
              Shipment Timeline
            </span>
            <div className="space-y-2.5 text-xs">
              {order.timeline.map((ev, i) => (
                <div key={i} className="flex items-start gap-2.5">
                  <span
                    className={`w-2.5 h-2.5 rounded-full mt-1 shrink-0 ${
                      ev.completed
                        ? 'bg-emerald-500 ring-2 ring-emerald-200 dark:ring-emerald-950'
                        : 'bg-slate-300 dark:bg-slate-600'
                    }`}
                  />
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between gap-2">
                      <p className="font-bold text-slate-800 dark:text-slate-200">{ev.status}</p>
                      <span className="text-[10px] text-slate-400 font-mono shrink-0">{ev.time}</span>
                    </div>
                    <p className="text-[10px] text-slate-500 dark:text-slate-400 mt-0.5">
                      {ev.description}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="p-3 bg-[#FFFDF9] dark:bg-[#181311] border-t border-orange-100/80 dark:border-orange-950/50 flex items-center justify-between">
          <span className="text-[10px] text-slate-500">
            Shiprocket Courier Updates
          </span>
          <button
            type="button"
            onClick={() => {
              haptics.light();
              onClose();
            }}
            className="px-4 py-2 bg-orange-50 hover:bg-orange-100 dark:bg-[#211A17] text-slate-800 dark:text-slate-200 font-bold text-xs rounded-xl active:scale-95 transition-all cursor-pointer"
          >
            Close Tracker
          </button>
        </div>
      </div>
    </div>
  );
};

export const OrdersSection: React.FC<OrdersSectionProps> = ({
  orders,
  onBack,
  onReorder,
  onReorderAll,
  onShowToast,
  onNavigateToCart,
  customerName = 'AARVO Customer',
  customerPhone = '',
  onOpenSupport,
}) => {
  const [filter, setFilter] = useState<'All' | 'Active' | 'Processing' | 'Shipped' | 'Delivered'>('All');
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);
  const [riderTrackOrder, setRiderTrackOrder] = useState<Order | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [isDashboardExpanded, setIsDashboardExpanded] = useState(true);

  // State for tracking expandable list for each order
  const [expandedOrders, setExpandedOrders] = useState<Record<string, boolean>>(() => {
    const initial: Record<string, boolean> = {};
    if (orders.length > 0) {
      initial[orders[0].id] = true;
    }
    return initial;
  });

  const toggleOrderExpanded = (orderId: string, e?: React.MouseEvent) => {
    if (e) e.stopPropagation();
    haptics.selection();
    setExpandedOrders((prev) => ({
      ...prev,
      [orderId]: !prev[orderId],
    }));
  };

  // Status statistics for summary dashboard
  const statusCounts = {
    Processing: orders.filter((o) => o.status === 'Processing').length,
    Shipped: orders.filter((o) => o.status === 'Shipped').length,
    Delivered: orders.filter((o) => o.status === 'Delivered').length,
    Cancelled: orders.filter((o) => o.status === 'Cancelled').length,
  };

  const statusAmounts = {
    Processing: orders.filter((o) => o.status === 'Processing').reduce((sum, o) => sum + o.total, 0),
    Shipped: orders.filter((o) => o.status === 'Shipped').reduce((sum, o) => sum + o.total, 0),
    Delivered: orders.filter((o) => o.status === 'Delivered').reduce((sum, o) => sum + o.total, 0),
    Cancelled: orders.filter((o) => o.status === 'Cancelled').reduce((sum, o) => sum + o.total, 0),
  };

  const totalOrdersCount = orders.length;
  const totalOrdersAmount = orders.reduce((sum, o) => sum + o.total, 0);

  // Bar chart data for Recharts
  const chartData = [
    {
      statusKey: 'Processing' as const,
      name: 'Processing',
      count: statusCounts.Processing,
      amount: statusAmounts.Processing,
      color: '#F59E0B',
      lightBg: 'bg-amber-50 dark:bg-amber-950/30 text-amber-600 dark:text-amber-400 border-amber-200 dark:border-amber-800/40',
    },
    {
      statusKey: 'Shipped' as const,
      name: 'Shipped',
      count: statusCounts.Shipped,
      amount: statusAmounts.Shipped,
      color: '#FF5722',
      lightBg: 'bg-orange-50 dark:bg-orange-950/30 text-[#FF5722] dark:text-[#FF8A65] border-orange-200 dark:border-orange-800/40',
    },
    {
      statusKey: 'Delivered' as const,
      name: 'Delivered',
      count: statusCounts.Delivered,
      amount: statusAmounts.Delivered,
      color: '#10B981',
      lightBg: 'bg-emerald-50 dark:bg-emerald-950/30 text-emerald-600 dark:text-emerald-400 border-emerald-200 dark:border-emerald-800/40',
    },
  ];

  const filteredOrders = orders.filter((order) => {
    const matchesFilter =
      filter === 'All' ||
      (filter === 'Active' && (order.status === 'Processing' || order.status === 'Shipped')) ||
      filter === order.status;

    const matchesSearch =
      searchQuery === '' ||
      order.id.toLowerCase().includes(searchQuery.toLowerCase()) ||
      order.items.some((item) => item.name.toLowerCase().includes(searchQuery.toLowerCase()));

    return matchesFilter && matchesSearch;
  });

  const areAllExpanded =
    filteredOrders.length > 0 && filteredOrders.every((o) => expandedOrders[o.id]);

  const toggleExpandAll = () => {
    haptics.selection();
    if (areAllExpanded) {
      setExpandedOrders({});
    } else {
      const allMap: Record<string, boolean> = {};
      filteredOrders.forEach((o) => {
        allMap[o.id] = true;
      });
      setExpandedOrders(allMap);
    }
  };

  const handleOpenDetailModal = (order: Order) => {
    haptics.selection();
    setSelectedOrder(order);
  };

  const handleDownloadCSV = () => {
    if (filteredOrders.length === 0) {
      haptics.warning();
      onShowToast('No orders found to export in current view.');
      return;
    }

    // CSV Headers
    const headers = [
      'Order ID',
      'Order Date',
      'Status',
      'Status Stage',
      'Amount (INR)',
      'Total Items Qty',
      'Products Summary',
      'Carrier',
      'Tracking AWB',
      'Estimated Delivery',
      'Payment Method',
      'Delivery Address',
    ];

    const escapeCSV = (val: string | number | undefined | null) => {
      if (val === undefined || val === null) return '""';
      const str = String(val).replace(/"/g, '""');
      return `"${str}"`;
    };

    const rows = filteredOrders.map((ord) => {
      const itemsSummary = ord.items
        .map((i) => `${i.name} (Qty: ${i.quantity}, ₹${i.price})`)
        .join('; ');
      const totalQty = ord.items.reduce((sum, i) => sum + i.quantity, 0);

      return [
        ord.id,
        ord.date,
        ord.status,
        `Step ${ord.statusStep} of 3 (${ord.status})`,
        ord.total,
        totalQty,
        itemsSummary,
        ord.carrier,
        ord.trackingNumber,
        ord.estimatedDelivery,
        ord.paymentMethod,
        ord.deliveryAddress,
      ]
        .map(escapeCSV)
        .join(',');
    });

    const csvContent = '\uFEFF' + [headers.map(escapeCSV).join(','), ...rows].join('\r\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    const formattedDate = new Date().toISOString().slice(0, 10);
    link.download = `aarvo_orders_delivery_${filter.toLowerCase()}_${formattedDate}.csv`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);

    haptics.subtle();
    onShowToast(`Exported ${filteredOrders.length} order(s) to CSV! 📊`);
  };

  // Custom tooltip for Recharts Bar Chart
  const CustomBarTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      const item = payload[0].payload;
      const percentage = totalOrdersCount > 0 ? Math.round((item.count / totalOrdersCount) * 100) : 0;
      return (
        <div className="bg-[#181311]/95 dark:bg-[#211A17]/95 backdrop-blur-md text-white px-3 py-2.5 rounded-xl shadow-xl border border-orange-900/30 text-xs space-y-1.5 z-30 pointer-events-none">
          <div className="flex items-center gap-2 font-bold pb-1 border-b border-white/10">
            <span className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: item.color }} />
            <span className="text-white">{item.name}</span>
            <span className="text-[10px] text-slate-400 font-mono ml-auto">({percentage}%)</span>
          </div>
          <div className="text-[11px] text-slate-300 flex items-center justify-between gap-4">
            <span>Order Count:</span>
            <strong className="text-white font-mono">{item.count}</strong>
          </div>
          <div className="text-[11px] text-slate-300 flex items-center justify-between gap-4">
            <span>Total Value:</span>
            <strong className="text-emerald-400 font-mono">₹{item.amount.toLocaleString('en-IN')}</strong>
          </div>
          <p className="text-[9px] text-[#FF8A65] pt-0.5">
            Click bar to filter list
          </p>
        </div>
      );
    }
    return null;
  };

  return (
    <div className="flex-1 flex flex-col bg-[#FFFDF9] dark:bg-[#120E0C] pb-12 animate-in fade-in duration-200">
      {/* Top Header Bar */}
      <div className="bg-gradient-to-b from-[#FFE5CC] via-[#FFF4E8] to-white dark:from-[#26160C] dark:via-[#1B120E] dark:to-[#181311] text-slate-900 dark:text-white px-4 pt-[max(0.75rem,env(safe-area-inset-top))] pb-4 sticky top-0 z-20 border-b border-orange-100/80 dark:border-orange-950/50 shadow-xs">
        <div className="flex items-center justify-between gap-2">
          <div className="flex items-center gap-2.5 min-w-0">
            <button
              onClick={() => {
                haptics.light();
                onBack();
              }}
              className="p-1.5 hover:bg-orange-100/70 dark:hover:bg-[#261D19] rounded-xl transition-colors active:scale-95 cursor-pointer shrink-0 text-slate-800 dark:text-white"
              aria-label="Back to Account"
            >
              <ArrowLeft className="w-5 h-5 stroke-[2.5]" />
            </button>
            <div className="min-w-0">
              <h1 className="font-display font-black text-base sm:text-lg leading-tight truncate text-slate-900 dark:text-white">
                Orders &amp; Delivery
              </h1>
              <p className="text-[11px] text-slate-500 dark:text-slate-400 truncate">
                Track packages &amp; purchase history ({orders.length} total)
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={handleDownloadCSV}
            className="px-2.5 py-1.5 bg-[#FFF4E8] hover:bg-[#FF5722] text-[#FF5722] hover:text-white dark:bg-[#211A17] dark:text-[#FF8A65] dark:hover:bg-[#FF5722] dark:hover:text-white border border-orange-200 dark:border-orange-900/50 rounded-xl text-xs font-bold flex items-center gap-1.5 transition-all active:scale-95 cursor-pointer shadow-xs shrink-0"
            title="Download CSV export"
          >
            <Download className="w-3.5 h-3.5 stroke-[2.2]" />
            <span className="text-[11px] font-extrabold">Download CSV</span>
          </button>
        </div>

        <div className="mt-3 relative">
          <div className="h-9 bg-white dark:bg-[#211A17] rounded-xl flex items-center px-3 border border-orange-200/80 dark:border-orange-900/50 shadow-2xs">
            <Search className="w-3.5 h-3.5 text-slate-400 mr-2 shrink-0" />
            <input
              type="text"
              placeholder="Search by Order ID or product name..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full text-xs text-slate-800 dark:text-white placeholder-slate-400 bg-transparent outline-none"
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery('')}
                className="p-1 text-slate-400 hover:text-slate-700 dark:hover:text-white"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            )}
          </div>
        </div>

        <div className="flex items-center gap-2 mt-3 overflow-x-auto no-scrollbar">
          {(['All', 'Active', 'Processing', 'Shipped', 'Delivered'] as const).map((tab) => {
            const countLabel =
              tab === 'All'
                ? totalOrdersCount
                : tab === 'Active'
                ? statusCounts.Processing + statusCounts.Shipped
                : statusCounts[tab];

            return (
              <button
                key={tab}
                onClick={() => {
                  haptics.selection();
                  setFilter(tab);
                }}
                className={`px-3 py-1 rounded-full text-xs font-bold transition-all active:scale-95 cursor-pointer whitespace-nowrap flex items-center gap-1.5 ${
                  filter === tab
                    ? 'bg-[#FF5722] text-white shadow-sm font-black'
                    : 'bg-white/90 dark:bg-[#211A17] text-slate-700 dark:text-slate-200 hover:bg-orange-50 border border-orange-200/70 dark:border-orange-950/60'
                }`}
              >
                <span>{tab === 'Active' ? 'Active / In Transit' : tab}</span>
                <span
                  className={`text-[10px] px-1.5 py-0.2 rounded-full font-mono ${
                    filter === tab
                      ? 'bg-white/25 text-white'
                      : 'bg-orange-50 dark:bg-orange-950/60 text-[#FF5722] dark:text-[#FF8A65]'
                  }`}
                >
                  {countLabel}
                </span>
              </button>
            );
          })}
        </div>
      </div>

      <div className="p-3 pb-1 space-y-2.5">
        <div className="bg-white dark:bg-[#181311] rounded-2xl border border-orange-100/80 dark:border-orange-950/50 shadow-xs p-3.5 space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-xl bg-[#FF5722]/10 text-[#FF5722] dark:bg-[#FF5722]/15 dark:text-[#FF8A65] flex items-center justify-center shrink-0">
                <BarChart2 className="w-4 h-4" />
              </div>
              <div>
                <div className="flex items-center gap-1.5">
                  <h2 className="font-display font-black text-xs sm:text-sm text-slate-900 dark:text-white">
                    Orders Summary Dashboard
                  </h2>
                  <span className="inline-flex items-center px-1.5 py-0.5 bg-emerald-50 text-emerald-700 dark:bg-emerald-950/60 dark:text-emerald-300 text-[9px] font-extrabold rounded-md border border-emerald-200 dark:border-emerald-800/50">
                    Tracked
                  </span>
                </div>
                <p className="text-[10px] text-slate-500 dark:text-slate-400">
                  {totalOrdersCount} Total Orders &bull; Total Value: ₹{totalOrdersAmount.toLocaleString('en-IN')}
                </p>
              </div>
            </div>

            {/* Collapse / Expand Toggle Button */}
            <button
              type="button"
              onClick={() => {
                haptics.light();
                setIsDashboardExpanded((prev) => !prev);
              }}
              className="p-1.5 text-slate-500 hover:text-slate-800 dark:text-slate-400 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg transition-all text-xs font-bold flex items-center gap-1 active:scale-95 cursor-pointer"
              title={isDashboardExpanded ? 'Collapse chart view' : 'Expand chart view'}
            >
              <span className="text-[11px] font-medium hidden sm:inline">
                {isDashboardExpanded ? 'Hide Chart' : 'Show Chart'}
              </span>
              {isDashboardExpanded ? (
                <ChevronUp className="w-4 h-4" />
              ) : (
                <ChevronDown className="w-4 h-4" />
              )}
            </button>
          </div>

          {/* Status Counts Metric Cards Grid (4 columns) */}
          <div className="grid grid-cols-4 gap-2">
            {/* 1. All Orders Metric */}
            <button
              type="button"
              onClick={() => {
                haptics.selection();
                setFilter('All');
              }}
              className={`p-2.5 rounded-xl border text-left transition-all active:scale-95 cursor-pointer flex flex-col justify-between ${
                filter === 'All'
                  ? 'bg-[#FF5722]/10 dark:bg-[#FF5722]/20 border-[#FF5722] shadow-xs'
                  : 'bg-[#FFFDF9] dark:bg-[#211A17] border-orange-100/80 dark:border-orange-950/50 hover:border-orange-300'
              }`}
            >
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-bold text-slate-500 dark:text-slate-400 uppercase tracking-tight truncate">
                  Total
                </span>
                <Package className="w-3.5 h-3.5 text-[#FF5722] dark:text-[#FF8A65] shrink-0" />
              </div>
              <div className="mt-1">
                <span className="font-display font-black text-sm sm:text-base text-slate-900 dark:text-white">
                  {totalOrdersCount}
                </span>
                <span className="text-[9px] text-slate-400 block truncate font-mono">
                  ₹{totalOrdersAmount.toLocaleString('en-IN')}
                </span>
              </div>
            </button>

            <button
              type="button"
              onClick={() => {
                haptics.selection();
                setFilter(filter === 'Processing' ? 'All' : 'Processing');
              }}
              className={`p-2.5 rounded-xl border text-left transition-all active:scale-95 cursor-pointer flex flex-col justify-between ${
                filter === 'Processing'
                  ? 'bg-amber-500/15 dark:bg-amber-500/25 border-amber-500 shadow-xs'
                  : 'bg-[#FFFDF9] dark:bg-[#211A17] border-orange-100/80 dark:border-orange-950/50 hover:border-amber-300 dark:hover:border-amber-700'
              }`}
            >
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-bold text-amber-600 dark:text-amber-400 uppercase tracking-tight truncate">
                  Processing
                </span>
                <Clock className="w-3.5 h-3.5 text-amber-500 shrink-0" />
              </div>
              <div className="mt-1">
                <span className="font-display font-black text-sm sm:text-base text-slate-900 dark:text-white">
                  {statusCounts.Processing}
                </span>
                <span className="text-[9px] text-slate-400 block truncate font-mono">
                  ₹{statusAmounts.Processing.toLocaleString('en-IN')}
                </span>
              </div>
            </button>

            <button
              type="button"
              onClick={() => {
                haptics.selection();
                setFilter(filter === 'Shipped' ? 'All' : 'Shipped');
              }}
              className={`p-2.5 rounded-xl border text-left transition-all active:scale-95 cursor-pointer flex flex-col justify-between ${
                filter === 'Shipped'
                  ? 'bg-[#FF5722]/15 dark:bg-[#FF5722]/25 border-[#FF5722] shadow-xs'
                  : 'bg-[#FFFDF9] dark:bg-[#211A17] border-orange-100/80 dark:border-orange-950/50 hover:border-orange-300 dark:hover:border-orange-700'
              }`}
            >
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-bold text-[#FF5722] dark:text-[#FF8A65] uppercase tracking-tight truncate">
                  Shipped
                </span>
                <Truck className="w-3.5 h-3.5 text-[#FF5722] dark:text-[#FF8A65] shrink-0" />
              </div>
              <div className="mt-1">
                <span className="font-display font-black text-sm sm:text-base text-slate-900 dark:text-white">
                  {statusCounts.Shipped}
                </span>
                <span className="text-[9px] text-slate-400 block truncate font-mono">
                  ₹{statusAmounts.Shipped.toLocaleString('en-IN')}
                </span>
              </div>
            </button>

            <button
              type="button"
              onClick={() => {
                haptics.selection();
                setFilter(filter === 'Delivered' ? 'All' : 'Delivered');
              }}
              className={`p-2.5 rounded-xl border text-left transition-all active:scale-95 cursor-pointer flex flex-col justify-between ${
                filter === 'Delivered'
                  ? 'bg-emerald-500/15 dark:bg-emerald-500/25 border-emerald-500 shadow-xs'
                  : 'bg-[#FFFDF9] dark:bg-[#211A17] border-orange-100/80 dark:border-orange-950/50 hover:border-emerald-300 dark:hover:border-emerald-700'
              }`}
            >
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-bold text-emerald-600 dark:text-emerald-400 uppercase tracking-tight truncate">
                  Delivered
                </span>
                <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500 shrink-0" />
              </div>
              <div className="mt-1">
                <span className="font-display font-black text-sm sm:text-base text-slate-900 dark:text-white">
                  {statusCounts.Delivered}
                </span>
                <span className="text-[9px] text-slate-400 block truncate font-mono">
                  ₹{statusAmounts.Delivered.toLocaleString('en-IN')}
                </span>
              </div>
            </button>
          </div>

          {isDashboardExpanded && (
            <div className="pt-2 border-t border-orange-100/80 dark:border-orange-950/50 space-y-2">
              <div className="flex items-center justify-between text-xs">
                <div className="flex items-center gap-1.5">
                  <span className="font-bold text-[11px] text-slate-700 dark:text-slate-300">
                    Status Distribution
                  </span>
                  <span className="text-[10px] text-slate-400">
                    (Tap any bar to filter)
                  </span>
                </div>
                {filter !== 'All' && (
                  <button
                    type="button"
                    onClick={() => {
                      haptics.light();
                      setFilter('All');
                    }}
                    className="text-[10px] font-bold text-[#FF5722] dark:text-[#FF8A65] hover:underline cursor-pointer"
                  >
                    Reset Filter (Show All)
                  </button>
                )}
              </div>

              <div className="h-44 w-full bg-[#FFFDF9] dark:bg-[#15100E] rounded-xl p-2 border border-orange-100/80 dark:border-orange-950/50">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart
                    data={chartData}
                    margin={{ top: 12, right: 12, left: -22, bottom: 2 }}
                    onClick={(state: any) => {
                      if (state && state.activePayload && state.activePayload.length) {
                        const clicked = state.activePayload[0].payload.statusKey;
                        haptics.selection();
                        setFilter(filter === clicked ? 'All' : clicked);
                      }
                    }}
                  >
                    <CartesianGrid strokeDasharray="3 3" vertical={false} opacity={0.12} />
                    <XAxis
                      dataKey="name"
                      tickLine={false}
                      axisLine={{ stroke: '#CBD5E1', opacity: 0.2 }}
                      tick={{ fontSize: 11, fontWeight: 700, fill: '#64748B' }}
                    />
                    <YAxis
                      allowDecimals={false}
                      tickLine={false}
                      axisLine={false}
                      tick={{ fontSize: 10, fill: '#94A3B8' }}
                    />
                    <Tooltip content={<CustomBarTooltip />} cursor={{ fill: 'rgba(255, 87, 34, 0.06)', radius: 6 }} />
                    <Bar
                      dataKey="count"
                      radius={[6, 6, 0, 0]}
                      maxBarSize={44}
                      className="cursor-pointer"
                    >
                      {chartData.map((entry, index) => {
                        const isSelected = filter === 'All' || filter === entry.statusKey;
                        return (
                          <Cell
                            key={`cell-${index}`}
                            fill={entry.color}
                            opacity={isSelected ? 1 : 0.35}
                          />
                        );
                      })}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </div>

              <div className="space-y-1.5 pt-1">
                <div className="h-2 w-full bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden flex">
                  {chartData.map((item) => {
                    const pct = totalOrdersCount > 0 ? (item.count / totalOrdersCount) * 100 : 0;
                    if (pct <= 0) return null;
                    return (
                      <div
                        key={item.statusKey}
                        style={{ width: `${pct}%`, backgroundColor: item.color }}
                        className="h-full transition-all duration-300"
                        title={`${item.name}: ${item.count} (${Math.round(pct)}%)`}
                      />
                    );
                  })}
                </div>

                <div className="flex items-center justify-between text-[10px] text-slate-400 font-medium px-0.5">
                  <div className="flex items-center gap-3">
                    <span className="flex items-center gap-1">
                      <span className="w-2 h-2 rounded-full bg-amber-500" />
                      Processing ({statusCounts.Processing})
                    </span>
                    <span className="flex items-center gap-1">
                      <span className="w-2 h-2 rounded-full bg-[#FF5722]" />
                      Shipped ({statusCounts.Shipped})
                    </span>
                    <span className="flex items-center gap-1">
                      <span className="w-2 h-2 rounded-full bg-emerald-500" />
                      Delivered ({statusCounts.Delivered})
                    </span>
                  </div>
                  <span className="font-mono text-slate-500">100% Tracked</span>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      <div className="px-3.5 pt-1.5 pb-1 flex items-center justify-between text-xs text-slate-500 dark:text-slate-400">
        <span className="font-medium text-[11px]">
          Showing <strong className="text-slate-800 dark:text-slate-200">{filteredOrders.length}</strong> of {orders.length} order{orders.length > 1 ? 's' : ''}
          {filter !== 'All' && <span className="text-[#FF5722] dark:text-[#FF8A65] ml-1 font-bold">({filter})</span>}
        </span>
        <div className="flex items-center gap-3">
          {filteredOrders.length > 0 && (
            <button
              type="button"
              onClick={toggleExpandAll}
              className="text-[#FF5722] dark:text-[#FF8A65] hover:underline font-bold text-[11px] flex items-center gap-1 cursor-pointer"
            >
              {areAllExpanded ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
              <span>{areAllExpanded ? 'Collapse All' : 'Expand All'}</span>
            </button>
          )}
          <button
            type="button"
            onClick={handleDownloadCSV}
            className="text-[#FF5722] dark:text-[#FF8A65] hover:underline font-bold text-[11px] flex items-center gap-1 cursor-pointer"
          >
            <Download className="w-3 h-3" />
            <span>Export CSV</span>
          </button>
        </div>
      </div>

      <div className="p-3 space-y-3">
        {filteredOrders.length === 0 ? (
          <div className="text-center py-12 px-4 bg-white dark:bg-[#181311] rounded-2xl border border-dashed border-orange-200 dark:border-orange-950/60">
            <div className="w-12 h-12 rounded-full bg-orange-50 dark:bg-[#211A17] text-[#FF5722] flex items-center justify-center mx-auto mb-3">
              <Package className="w-6 h-6" />
            </div>
            <h3 className="font-display font-bold text-sm text-slate-800 dark:text-slate-200">
              No orders found
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
              {searchQuery ? 'Try matching another order ID or title' : 'You have no orders in this category yet'}
            </p>
          </div>
        ) : (
          filteredOrders.map((order) => {
            const itemCount = order.items.reduce((acc, i) => acc + i.quantity, 0);
            const isExpanded = !!expandedOrders[order.id];

            return (
              <div
                key={order.id}
                className="bg-white dark:bg-[#181311] rounded-2xl border border-orange-100/80 dark:border-orange-950/50 shadow-xs hover:shadow-md transition-all duration-200 overflow-hidden"
              >
                <div
                  onClick={(e) => toggleOrderExpanded(order.id, e)}
                  className="p-3.5 flex items-start justify-between gap-2 cursor-pointer hover:bg-orange-50/40 dark:hover:bg-[#211A17]/60 transition-colors"
                >
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="font-mono font-bold text-xs text-[#FF5722] dark:text-[#FF8A65]">
                        {order.id}
                      </span>
                      <span className="text-[10px] text-slate-400 dark:text-slate-500">
                        &bull; {order.date}
                      </span>
                    </div>

                    <div className="flex items-center gap-1.5 mt-1.5 flex-wrap">
                      {order.status === 'Processing' && (
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-50 dark:bg-amber-950/60 text-amber-700 dark:text-amber-300 border border-amber-200 dark:border-amber-800/60">
                          <span className="w-1.5 h-1.5 rounded-full bg-amber-500" />
                          Processing
                        </span>
                      )}
                      {order.status === 'Shipped' && (
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-orange-50 dark:bg-orange-950/60 text-[#FF5722] dark:text-[#FF8A65] border border-orange-200 dark:border-orange-800/60">
                          <Truck className="w-3 h-3 text-[#FF5722] dark:text-[#FF8A65]" />
                          Shipped &bull; In Transit
                        </span>
                      )}
                      {order.status === 'Delivered' && (
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800/60">
                          <CheckCircle2 className="w-3 h-3 text-emerald-600 dark:text-emerald-400" />
                          Delivered
                        </span>
                      )}
                      <span className="text-[10px] text-slate-400 dark:text-slate-500">
                        ({itemCount} item{itemCount > 1 ? 's' : ''})
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-3 shrink-0">
                    <div className="text-right">
                      <span className="font-display font-black text-sm text-slate-900 dark:text-white block">
                        ₹{order.total.toLocaleString('en-IN')}
                      </span>
                      <span className="text-[10px] text-slate-400">
                        {order.paymentMethod.includes('Cash') ? 'Cash on Delivery' : 'Paid Online'}
                      </span>
                    </div>

                    <button
                      type="button"
                      onClick={(e) => toggleOrderExpanded(order.id, e)}
                      aria-label={isExpanded ? 'Collapse order' : 'Expand order'}
                      className={`w-7 h-7 rounded-xl flex items-center justify-center transition-all cursor-pointer ${
                        isExpanded
                          ? 'bg-[#FF5722] text-white rotate-180'
                          : 'bg-orange-50 dark:bg-[#211A17] text-slate-600 dark:text-slate-300 hover:bg-[#FF5722]/10 hover:text-[#FF5722]'
                      }`}
                    >
                      <ChevronDown className="w-4 h-4 transition-transform" />
                    </button>
                  </div>
                </div>

                {!isExpanded && (
                  <div
                    onClick={(e) => toggleOrderExpanded(order.id, e)}
                    className="px-3.5 pb-3 flex items-center gap-2 pt-2 border-t border-orange-100/70 dark:border-orange-950/40 cursor-pointer group"
                  >
                    <div className="flex -space-x-2 overflow-hidden py-0.5 shrink-0">
                      {order.items.slice(0, 3).map((item, idx) => (
                        <img
                          key={idx}
                          src={item.image}
                          alt={item.name}
                          className="inline-block w-8 h-8 rounded-lg object-cover ring-2 ring-white dark:ring-[#181311] shadow-xs bg-orange-50/40 dark:bg-[#211A17]"
                        />
                      ))}
                      {order.items.length > 3 && (
                        <div className="w-8 h-8 rounded-lg bg-orange-50 dark:bg-[#211A17] ring-2 ring-white dark:ring-[#181311] flex items-center justify-center text-[10px] font-bold text-slate-500">
                          +{order.items.length - 3}
                        </div>
                      )}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-xs text-slate-700 dark:text-slate-300 truncate">
                        {order.items.map((i) => i.name).join(', ')}
                      </p>
                    </div>
                    <span className="text-[11px] font-bold text-[#FF5722] dark:text-[#FF8A65] flex items-center gap-0.5 group-hover:underline shrink-0">
                      Show Items ({order.items.length})
                      <ChevronDown className="w-3.5 h-3.5" />
                    </span>
                  </div>
                )}

                {isExpanded && (
                  <div className="px-3.5 pb-3.5 pt-2 border-t border-orange-100/80 dark:border-orange-950/50 space-y-3 bg-[#FFFDF9] dark:bg-[#15100E] animate-in fade-in duration-200">
                    <div className="flex items-center justify-between">
                      <span className="text-[11px] font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                        Order Items &bull; {order.items.length} Product{order.items.length > 1 ? 's' : ''} ({itemCount} Unit{itemCount > 1 ? 's' : ''})
                      </span>
                      {onReorderAll && (
                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation();
                            onReorderAll(order.items);
                          }}
                          className="text-[10px] font-bold text-[#FF5722] dark:text-[#FF8A65] hover:underline flex items-center gap-1 cursor-pointer"
                        >
                          <RotateCcw className="w-3 h-3" />
                          <span>Reorder All</span>
                        </button>
                      )}
                    </div>

                    <div className="space-y-2">
                      {order.items.map((item) => (
                        <div
                          key={item.id}
                          className="p-3 bg-white dark:bg-[#211A17] rounded-xl border border-orange-100/80 dark:border-orange-950/50 shadow-xs flex items-center gap-3"
                        >
                          <img
                            src={item.image}
                            alt={item.name}
                            className="w-14 h-14 rounded-xl object-cover bg-orange-50/40 dark:bg-[#181311] border border-orange-100/80 dark:border-orange-950/50 shrink-0 shadow-xs"
                          />

                          <div className="flex-1 min-w-0">
                            <span className="text-[9px] font-bold text-[#FF5722] dark:text-[#FF8A65] uppercase tracking-wide block">
                              {item.category}
                            </span>
                            <h4 className="font-bold text-xs text-slate-800 dark:text-slate-100 line-clamp-2 leading-tight">
                              {item.name}
                            </h4>

                            <div className="flex items-center gap-2.5 mt-1.5 flex-wrap">
                              <span className="px-2 py-0.5 rounded-md bg-orange-50 dark:bg-orange-950/50 text-[#FF5722] dark:text-[#FF8A65] text-[10px] font-extrabold border border-orange-200/80 dark:border-orange-800">
                                Qty: {item.quantity}
                              </span>

                              <span className="text-xs font-black text-slate-900 dark:text-white">
                                ₹{(item.price * item.quantity).toLocaleString('en-IN')}
                              </span>

                              {item.quantity > 1 && (
                                <span className="text-[10px] text-slate-400">
                                  (₹{item.price.toLocaleString('en-IN')} each)
                                </span>
                              )}
                            </div>
                          </div>

                          <button
                            type="button"
                            onClick={(e) => {
                              e.stopPropagation();
                              onReorder(item);
                              onShowToast(`Added ${item.name.slice(0, 18)}... to cart!`);
                            }}
                            className="px-2.5 py-1.5 bg-[#FF5722]/10 hover:bg-[#FF5722] text-[#FF5722] hover:text-white dark:text-[#FF8A65] dark:hover:text-white text-[11px] font-bold rounded-xl transition-all flex items-center gap-1 active:scale-95 shrink-0 cursor-pointer shadow-xs"
                            title="Reorder this product"
                          >
                            <RotateCcw className="w-3 h-3" />
                            <span>Reorder</span>
                          </button>
                        </div>
                      ))}
                    </div>

                    <div className="pt-2 border-t border-orange-100/80 dark:border-orange-950/50 flex items-center justify-between gap-2 flex-wrap text-xs">
                      <div className="min-w-0 flex items-center gap-1 text-[11px] text-slate-500 dark:text-slate-400 truncate">
                        <MapPin className="w-3 h-3 text-[#FF5722] dark:text-[#FF8A65] shrink-0" />
                        <span className="truncate">{order.deliveryAddress}</span>
                      </div>

                      <div className="flex items-center gap-2">
                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation();
                            haptics.selection();
                            setRiderTrackOrder(order);
                          }}
                          className="px-3 py-1.5 bg-gradient-to-r from-[#FF5722] to-[#E64A19] text-white hover:opacity-95 text-[11px] font-bold rounded-xl shadow-xs transition-all active:scale-95 cursor-pointer flex items-center gap-1 shrink-0"
                        >
                          <Truck className="w-3.5 h-3.5" />
                          <span>Courier Tracking</span>
                        </button>
                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation();
                            handleOpenDetailModal(order);
                          }}
                          className="px-3 py-1.5 bg-[#211A17] text-white hover:bg-[#2C221E] text-[11px] font-bold rounded-xl shadow-xs transition-all active:scale-95 cursor-pointer flex items-center gap-1"
                        >
                          <Truck className="w-3.5 h-3.5" />
                          <span>Track Delivery</span>
                        </button>
                        <button
                          type="button"
                          onClick={(e) => toggleOrderExpanded(order.id, e)}
                          className="px-2.5 py-1.5 bg-orange-50 hover:bg-orange-100 dark:bg-[#211A17] dark:hover:bg-[#2C221E] text-slate-700 dark:text-slate-300 text-[11px] font-bold rounded-xl transition-all active:scale-95 cursor-pointer flex items-center gap-1"
                        >
                          <ChevronUp className="w-3.5 h-3.5" />
                          <span>Collapse</span>
                        </button>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>

      <OrderDetailModal
        order={selectedOrder}
        isOpen={!!selectedOrder}
        onClose={() => setSelectedOrder(null)}
        onReorder={onReorder}
        onReorderAll={onReorderAll}
        onShowToast={onShowToast}
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
        onShowToast={onShowToast}
      />
    </div>
  );
};
