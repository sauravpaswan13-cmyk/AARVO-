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
}

interface OrderDetailModalProps {
  order: Order | null;
  isOpen: boolean;
  onClose: () => void;
  onReorder: (item: OrderItem) => void;
  onReorderAll?: (items: OrderItem[]) => void;
  onShowToast: (message: string) => void;
  onNavigateToCart?: () => void;
}

export const OrderDetailModal: React.FC<OrderDetailModalProps> = ({
  order,
  isOpen,
  onClose,
  onReorder,
  onReorderAll,
  onShowToast,
  onNavigateToCart,
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
    haptics.light();
    if (navigator.clipboard) {
      navigator.clipboard.writeText(order.trackingNumber);
      setCopiedTracking(true);
      setTimeout(() => setCopiedTracking(false), 2000);
      onShowToast(`Tracking number ${order.trackingNumber} copied! 📋`);
    }
  };

  const handleDownloadInvoice = () => {
    haptics.success();
    onShowToast(`Invoice for ${order.id} downloaded successfully! 📄`);
  };

  const totalItemsCount = order.items.reduce((sum, item) => sum + item.quantity, 0);

  const handleReorderAll = () => {
    haptics.success();
    if (onReorderAll) {
      onReorderAll(order.items);
    } else {
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
      <div className="relative w-full max-w-[440px] max-h-[92vh] bg-white dark:bg-[#14112B] text-slate-900 dark:text-slate-100 rounded-t-3xl sm:rounded-3xl shadow-2xl flex flex-col z-10 overflow-hidden animate-in slide-in-from-bottom-8 duration-300 border border-slate-100 dark:border-slate-800">
        
        {/* Top Header */}
        <div className="sticky top-0 z-20 flex items-center justify-between px-4 py-3.5 bg-white/95 dark:bg-[#14112B]/95 backdrop-blur-md border-b border-slate-100 dark:border-slate-800">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-blue-50 dark:bg-blue-950/50 text-[#2418D8] dark:text-[#7B73FF] flex items-center justify-center shrink-0">
              <Package className="w-4 h-4 stroke-[2.2]" />
            </div>
            <div>
              <div className="flex items-center gap-1.5">
                <h2 id="order-detail-title" className="font-mono font-bold text-xs sm:text-sm text-[#2418D8] dark:text-[#7B73FF]">
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
          <div className="p-3.5 rounded-2xl bg-gradient-to-r from-blue-50 via-indigo-50/60 to-purple-50 dark:from-purple-950/40 dark:via-blue-950/30 dark:to-purple-950/40 border border-blue-100 dark:border-purple-900/40 flex items-center justify-between">
            <div>
              <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 block mb-0.5">
                Current Status
              </span>
              <div className="flex items-center gap-1.5">
                {order.status === 'Processing' && (
                  <span className="inline-flex items-center gap-1.5 text-xs font-black text-amber-700 dark:text-amber-400">
                    <span className="w-2 h-2 rounded-full bg-amber-500 animate-pulse" />
                    Processing &bull; Packing at Warehouse
                  </span>
                )}
                {order.status === 'Shipped' && (
                  <span className="inline-flex items-center gap-1.5 text-xs font-black text-blue-700 dark:text-blue-400">
                    <Truck className="w-4 h-4 text-blue-600 dark:text-blue-400" />
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

            <span className="px-2.5 py-1 rounded-full text-[10px] font-extrabold uppercase bg-white dark:bg-[#1A1638] text-slate-800 dark:text-slate-200 shadow-xs border border-slate-200 dark:border-slate-700">
              {order.status}
            </span>
          </div>

          {/* 1. Visual 3-Step Status Tracker */}
          <div className="p-4 bg-slate-50 dark:bg-[#1A1638] rounded-2xl border border-slate-100 dark:border-slate-800">
            <h3 className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-3 flex items-center justify-between">
              <span>Order Tracking Journey</span>
              <span className="font-mono text-[10px] text-[#2418D8] dark:text-[#7B73FF] lowercase">
                Step {order.statusStep} of 3
              </span>
            </h3>

            <div className="relative flex items-center justify-between px-3 my-2">
              {/* Connecting Progress Line */}
              <div className="absolute left-7 right-7 top-3.5 h-1 bg-slate-200 dark:bg-slate-700 -z-0">
                <div
                  className="h-full bg-gradient-to-r from-[#2418D8] to-emerald-500 transition-all duration-500"
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

              {/* Step 1: Processing */}
              <div className="flex flex-col items-center gap-1.5 z-10">
                <div
                  className={`w-7 h-7 rounded-full flex items-center justify-center font-bold text-xs transition-colors ${
                    order.statusStep >= 1
                      ? 'bg-[#2418D8] text-white shadow-xs'
                      : 'bg-slate-200 text-slate-500'
                  }`}
                >
                  <Package className="w-3.5 h-3.5" />
                </div>
                <span className="text-[10px] font-extrabold text-slate-800 dark:text-slate-200">
                  Processing
                </span>
              </div>

              {/* Step 2: Shipped */}
              <div className="flex flex-col items-center gap-1.5 z-10">
                <div
                  className={`w-7 h-7 rounded-full flex items-center justify-center font-bold text-xs transition-colors ${
                    order.statusStep >= 2
                      ? 'bg-[#2418D8] text-white shadow-xs'
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
            <div className="mt-4 p-2.5 rounded-xl bg-white dark:bg-[#14112B] border border-slate-200/80 dark:border-slate-800 text-xs flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Truck className="w-4 h-4 text-[#2418D8] dark:text-[#7B73FF]" />
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
                className="p-1.5 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg text-slate-500 hover:text-slate-800 dark:hover:text-white transition-colors flex items-center gap-1 text-[10px] font-bold"
              >
                <Copy className="w-3 h-3" />
                <span>{copiedTracking ? 'Copied' : 'Copy'}</span>
              </button>
            </div>

            {/* Expected Delivery Callout */}
            <div className="mt-2.5 p-2 rounded-xl bg-blue-50/80 dark:bg-purple-950/30 text-[11px] text-blue-900 dark:text-purple-200 flex items-center gap-2">
              <Clock className="w-3.5 h-3.5 text-[#2418D8] dark:text-[#7B73FF] shrink-0" />
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

              {/* Reorder All Shortcut Button */}
              <button
                type="button"
                onClick={handleReorderAll}
                className="px-2.5 py-1 bg-[#2418D8]/10 hover:bg-[#2418D8] text-[#2418D8] hover:text-white dark:text-[#7B73FF] dark:hover:text-white text-[10px] font-bold rounded-lg transition-all flex items-center gap-1 active:scale-95 cursor-pointer shadow-xs"
              >
                <RotateCcw className="w-3 h-3" />
                <span>Reorder All ({totalItemsCount})</span>
              </button>
            </div>

            <div className="space-y-2">
              {order.items.map((item) => (
                <div
                  key={item.id}
                  className="p-3 bg-white dark:bg-[#1A1633] rounded-2xl border border-slate-100 dark:border-slate-800 flex items-center justify-between gap-3 shadow-xs"
                >
                  <img
                    src={item.image}
                    alt={item.name}
                    className="w-14 h-14 rounded-xl object-cover bg-slate-50 dark:bg-slate-900 border border-slate-100 dark:border-slate-800 shrink-0"
                  />
                  <div className="flex-1 min-w-0">
                    <span className="text-[9px] font-bold text-[#2418D8] dark:text-[#7B73FF] uppercase tracking-wide block">
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
                      haptics.success();
                      onReorder(item);
                      onShowToast(`Added ${item.name.slice(0, 18)}... to cart!`);
                    }}
                    className="px-2.5 py-1.5 bg-[#2418D8]/10 hover:bg-[#2418D8] text-[#2418D8] hover:text-white dark:text-[#7B73FF] dark:hover:text-white text-[11px] font-bold rounded-xl transition-all flex items-center gap-1 active:scale-95 shrink-0 cursor-pointer shadow-xs"
                  >
                    <RotateCcw className="w-3 h-3" />
                    <span>Reorder</span>
                  </button>
                </div>
              ))}
            </div>
          </div>

          {/* 3. Delivery Address & Recipient Card */}
          <div className="p-3.5 bg-white dark:bg-[#1A1633] rounded-2xl border border-slate-100 dark:border-slate-800 space-y-2">
            <div className="flex items-center gap-2 text-slate-900 dark:text-white">
              <MapPin className="w-4 h-4 text-[#2418D8] dark:text-[#7B73FF] shrink-0" />
              <h3 className="font-display font-bold text-xs">Delivery Address</h3>
            </div>
            <div className="pl-6 text-xs text-slate-600 dark:text-slate-300 leading-relaxed">
              <p className="font-bold text-slate-900 dark:text-white mb-0.5">Saurav Paswan</p>
              <p>{order.deliveryAddress}</p>
              <p className="text-[10px] text-slate-400 mt-1 font-mono">Contact: +91 9876543210</p>
            </div>
          </div>

          {/* 4. Payment Information Card */}
          <div className="p-3.5 bg-white dark:bg-[#1A1633] rounded-2xl border border-slate-100 dark:border-slate-800 space-y-2">
            <div className="flex items-center gap-2 text-slate-900 dark:text-white">
              <CreditCard className="w-4 h-4 text-[#2418D8] dark:text-[#7B73FF] shrink-0" />
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

          {/* 5. Price Breakdown Summary */}
          <div className="p-3.5 bg-slate-50 dark:bg-[#161230] rounded-2xl border border-slate-100 dark:border-slate-800 space-y-1.5 text-xs">
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
            <div className="flex items-center justify-between font-black text-sm text-slate-900 dark:text-white pt-2 border-t border-slate-200 dark:border-slate-800">
              <span>Total Paid</span>
              <span className="text-[#2418D8] dark:text-[#7B73FF]">₹{order.total.toLocaleString('en-IN')}</span>
            </div>
          </div>
        </div>

        {/* Sticky Action Footer */}
        <div className="sticky bottom-0 z-20 p-3 bg-white/95 dark:bg-[#14112B]/95 backdrop-blur-md border-t border-slate-100 dark:border-slate-800 space-y-2">
          {/* Main Full-Order Reorder Action Button */}
          <button
            type="button"
            onClick={handleReorderAll}
            className={`w-full py-3 px-4 rounded-xl text-xs font-black active:scale-[0.98] transition-all shadow-md flex items-center justify-center gap-2 cursor-pointer ${
              reorderedSuccess
                ? 'bg-emerald-600 text-white shadow-emerald-900/20'
                : 'bg-[#2418D8] hover:bg-[#1A10B3] text-white shadow-blue-900/25'
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
              className="flex-1 py-2 px-3 bg-slate-100 hover:bg-slate-200 dark:bg-[#1A1638] dark:hover:bg-[#221D47] text-slate-800 dark:text-slate-200 rounded-xl text-xs font-bold flex items-center justify-center gap-1.5 active:scale-95 transition-all shadow-xs cursor-pointer"
            >
              <FileText className="w-3.5 h-3.5 text-[#2418D8] dark:text-[#7B73FF]" />
              <span>Invoice</span>
            </button>

            <button
              type="button"
              onClick={() => {
                haptics.light();
                onShowToast(`Support ticket opened for Order ${order.id}`);
              }}
              className="flex-1 py-2 px-3 bg-slate-100 hover:bg-slate-200 dark:bg-[#1A1638] dark:hover:bg-[#221D47] text-slate-800 dark:text-slate-200 rounded-xl text-xs font-bold flex items-center justify-center gap-1.5 active:scale-95 transition-all shadow-xs cursor-pointer"
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

export const OrdersSection: React.FC<OrdersSectionProps> = ({
  orders,
  onBack,
  onReorder,
  onReorderAll,
  onShowToast,
  onNavigateToCart,
}) => {
  const [filter, setFilter] = useState<'All' | 'Active' | 'Processing' | 'Shipped' | 'Delivered'>('All');
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [isDashboardExpanded, setIsDashboardExpanded] = useState(true);

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
      color: '#2418D8',
      lightBg: 'bg-blue-50 dark:bg-blue-950/30 text-[#2418D8] dark:text-[#7B73FF] border-blue-200 dark:border-blue-800/40',
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

  const handleOpenDetailModal = (order: Order) => {
    haptics.selection();
    setSelectedOrder(order);
  };

  const handleDownloadCSV = () => {
    haptics.success();
    if (filteredOrders.length === 0) {
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

    onShowToast(`Exported ${filteredOrders.length} order(s) to CSV! 📊`);
  };

  // Custom tooltip for Recharts Bar Chart
  const CustomBarTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      const item = payload[0].payload;
      const percentage = totalOrdersCount > 0 ? Math.round((item.count / totalOrdersCount) * 100) : 0;
      return (
        <div className="bg-slate-900/95 dark:bg-[#1A1638]/95 backdrop-blur-md text-white px-3 py-2.5 rounded-xl shadow-xl border border-white/10 text-xs space-y-1.5 z-30 pointer-events-none">
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
          <p className="text-[9px] text-purple-300 pt-0.5">
            Click bar to filter list
          </p>
        </div>
      );
    }
    return null;
  };

  return (
    <div className="flex-1 flex flex-col bg-[#F7F5FF] dark:bg-[#0A0718] pb-12 animate-in fade-in duration-200">
      {/* Top Header Bar */}
      <div className="bg-[#2418D8] text-white px-4 pt-3 pb-4 sticky top-0 z-20 shadow-md">
        <div className="flex items-center justify-between gap-2">
          <div className="flex items-center gap-2.5 min-w-0">
            <button
              onClick={() => {
                haptics.light();
                onBack();
              }}
              className="p-1.5 hover:bg-white/15 rounded-xl transition-colors active:scale-95 cursor-pointer shrink-0"
              aria-label="Back to Account"
            >
              <ArrowLeft className="w-5 h-5 stroke-[2.5]" />
            </button>
            <div className="min-w-0">
              <h1 className="font-display font-black text-base sm:text-lg leading-tight truncate">
                Orders &amp; Delivery
              </h1>
              <p className="text-[11px] text-purple-200 truncate">
                Track packages &amp; purchase history ({orders.length} total)
              </p>
            </div>
          </div>

          {/* Download CSV Action Button */}
          <button
            type="button"
            onClick={handleDownloadCSV}
            className="px-2.5 py-1.5 bg-white/20 hover:bg-white text-white hover:text-[#2418D8] border border-white/30 rounded-xl text-xs font-bold flex items-center gap-1.5 transition-all active:scale-95 cursor-pointer shadow-xs shrink-0"
            title="Download CSV export"
          >
            <Download className="w-3.5 h-3.5 stroke-[2.2]" />
            <span className="text-[11px] font-extrabold">Download CSV</span>
          </button>
        </div>

        {/* Search Order bar */}
        <div className="mt-3 relative">
          <div className="h-9 bg-white/15 backdrop-blur-xs rounded-xl flex items-center px-3 border border-white/20">
            <Search className="w-3.5 h-3.5 text-white/70 mr-2 shrink-0" />
            <input
              type="text"
              placeholder="Search by Order ID or product name..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full text-xs text-white placeholder-white/60 bg-transparent outline-none"
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery('')}
                className="p-1 text-white/70 hover:text-white"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            )}
          </div>
        </div>

        {/* Filter Pills with Counts */}
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
                    ? 'bg-white text-[#2418D8] shadow-sm font-black'
                    : 'bg-white/15 text-white/90 hover:bg-white/25 border border-white/15'
                }`}
              >
                <span>{tab === 'Active' ? 'Active / In Transit' : tab}</span>
                <span
                  className={`text-[10px] px-1.5 py-0.2 rounded-full font-mono ${
                    filter === tab
                      ? 'bg-[#2418D8]/10 text-[#2418D8]'
                      : 'bg-white/20 text-white'
                  }`}
                >
                  {countLabel}
                </span>
              </button>
            );
          })}
        </div>
      </div>

      {/* ================================================================= */}
      {/* SUMMARY DASHBOARD WITH STATUS COUNTS & RECHARTS BAR CHART         */}
      {/* ================================================================= */}
      <div className="p-3 pb-1 space-y-2.5">
        <div className="bg-white dark:bg-[#14112B] rounded-2xl border border-slate-100 dark:border-slate-800/80 shadow-xs p-3.5 space-y-3">
          {/* Dashboard Header Bar */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-xl bg-[#2418D8]/10 text-[#2418D8] dark:bg-[#7B73FF]/15 dark:text-[#7B73FF] flex items-center justify-center shrink-0">
                <BarChart2 className="w-4 h-4" />
              </div>
              <div>
                <div className="flex items-center gap-1.5">
                  <h2 className="font-display font-black text-xs sm:text-sm text-slate-900 dark:text-white">
                    Orders Summary Dashboard
                  </h2>
                  <span className="inline-flex items-center px-1.5 py-0.5 bg-emerald-50 text-emerald-700 dark:bg-emerald-950/60 dark:text-emerald-300 text-[9px] font-extrabold rounded-md border border-emerald-200 dark:border-emerald-800/50">
                    Live Status
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
                  ? 'bg-[#2418D8]/10 dark:bg-[#2418D8]/20 border-[#2418D8] shadow-xs'
                  : 'bg-slate-50 dark:bg-[#1A1638] border-slate-100 dark:border-slate-800 hover:border-slate-300'
              }`}
            >
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-bold text-slate-500 dark:text-slate-400 uppercase tracking-tight truncate">
                  Total
                </span>
                <Package className="w-3.5 h-3.5 text-[#2418D8] dark:text-[#7B73FF] shrink-0" />
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

            {/* 2. Processing Metric */}
            <button
              type="button"
              onClick={() => {
                haptics.selection();
                setFilter(filter === 'Processing' ? 'All' : 'Processing');
              }}
              className={`p-2.5 rounded-xl border text-left transition-all active:scale-95 cursor-pointer flex flex-col justify-between ${
                filter === 'Processing'
                  ? 'bg-amber-500/15 dark:bg-amber-500/25 border-amber-500 shadow-xs'
                  : 'bg-slate-50 dark:bg-[#1A1638] border-slate-100 dark:border-slate-800 hover:border-amber-300 dark:hover:border-amber-700'
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

            {/* 3. Shipped Metric */}
            <button
              type="button"
              onClick={() => {
                haptics.selection();
                setFilter(filter === 'Shipped' ? 'All' : 'Shipped');
              }}
              className={`p-2.5 rounded-xl border text-left transition-all active:scale-95 cursor-pointer flex flex-col justify-between ${
                filter === 'Shipped'
                  ? 'bg-blue-500/15 dark:bg-blue-500/25 border-blue-600 shadow-xs'
                  : 'bg-slate-50 dark:bg-[#1A1638] border-slate-100 dark:border-slate-800 hover:border-blue-300 dark:hover:border-blue-700'
              }`}
            >
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-bold text-blue-600 dark:text-blue-400 uppercase tracking-tight truncate">
                  Shipped
                </span>
                <Truck className="w-3.5 h-3.5 text-blue-600 dark:text-blue-400 shrink-0" />
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

            {/* 4. Delivered Metric */}
            <button
              type="button"
              onClick={() => {
                haptics.selection();
                setFilter(filter === 'Delivered' ? 'All' : 'Delivered');
              }}
              className={`p-2.5 rounded-xl border text-left transition-all active:scale-95 cursor-pointer flex flex-col justify-between ${
                filter === 'Delivered'
                  ? 'bg-emerald-500/15 dark:bg-emerald-500/25 border-emerald-500 shadow-xs'
                  : 'bg-slate-50 dark:bg-[#1A1638] border-slate-100 dark:border-slate-800 hover:border-emerald-300 dark:hover:border-emerald-700'
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

          {/* Recharts Bar Chart Section */}
          {isDashboardExpanded && (
            <div className="pt-2 border-t border-slate-100 dark:border-slate-800/80 space-y-2">
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
                    className="text-[10px] font-bold text-[#2418D8] dark:text-[#7B73FF] hover:underline cursor-pointer"
                  >
                    Reset Filter (Show All)
                  </button>
                )}
              </div>

              {/* Bar Chart Container using Recharts */}
              <div className="h-44 w-full bg-slate-50/70 dark:bg-[#100D24] rounded-xl p-2 border border-slate-100 dark:border-slate-800/50">
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
                    <Tooltip content={<CustomBarTooltip />} cursor={{ fill: 'rgba(36, 24, 216, 0.04)', radius: 6 }} />
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

              {/* Proportional Distribution Bar */}
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
                      <span className="w-2 h-2 rounded-full bg-[#2418D8]" />
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

      {/* Sub-toolbar: Order count & Download CSV export link */}
      <div className="px-3.5 pt-1.5 pb-1 flex items-center justify-between text-xs text-slate-500 dark:text-slate-400">
        <span className="font-medium text-[11px]">
          Showing <strong className="text-slate-800 dark:text-slate-200">{filteredOrders.length}</strong> of {orders.length} order{orders.length > 1 ? 's' : ''}
          {filter !== 'All' && <span className="text-[#2418D8] dark:text-[#7B73FF] ml-1 font-bold">({filter})</span>}
        </span>

        <button
          type="button"
          onClick={handleDownloadCSV}
          className="text-[#2418D8] dark:text-[#7B73FF] hover:underline font-bold text-[11px] flex items-center gap-1 cursor-pointer"
        >
          <Download className="w-3 h-3" />
          <span>Export CSV</span>
        </button>
      </div>

      {/* Orders List Container */}
      <div className="p-3 space-y-3">
        {filteredOrders.length === 0 ? (
          <div className="text-center py-12 px-4 bg-white dark:bg-[#14112B] rounded-2xl border border-dashed border-slate-200 dark:border-slate-800">
            <div className="w-12 h-12 rounded-full bg-slate-100 dark:bg-slate-800 text-slate-400 flex items-center justify-center mx-auto mb-3">
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

            return (
              <div
                key={order.id}
                onClick={() => handleOpenDetailModal(order)}
                className="bg-white dark:bg-[#14112B] rounded-2xl border border-slate-100 dark:border-slate-800/80 shadow-xs hover:shadow-md p-3.5 transition-all duration-200 cursor-pointer active:scale-[0.99] group"
              >
                {/* Header row: ID, Date, Total */}
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-mono font-bold text-xs text-[#2418D8] dark:text-[#7B73FF] group-hover:underline">
                        {order.id}
                      </span>
                      <span className="text-[10px] text-slate-400 dark:text-slate-500">
                        &bull; {order.date}
                      </span>
                    </div>

                    {/* Status Badge */}
                    <div className="flex items-center gap-1.5 mt-1.5">
                      {order.status === 'Processing' && (
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-50 dark:bg-amber-950/60 text-amber-700 dark:text-amber-300 border border-amber-200 dark:border-amber-800/60">
                          <span className="w-1.5 h-1.5 rounded-full bg-amber-500 animate-pulse" />
                          Processing
                        </span>
                      )}

                      {order.status === 'Shipped' && (
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-blue-50 dark:bg-blue-950/60 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-800/60">
                          <Truck className="w-3 h-3 text-blue-600 dark:text-blue-400" />
                          Shipped &bull; In Transit
                        </span>
                      )}

                      {order.status === 'Delivered' && (
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800/60">
                          <CheckCircle2 className="w-3 h-3 text-emerald-600 dark:text-emerald-400" />
                          Delivered
                        </span>
                      )}
                    </div>
                  </div>

                  <div className="text-right">
                    <span className="font-display font-black text-sm text-slate-900 dark:text-white block">
                      ₹{order.total.toLocaleString('en-IN')}
                    </span>
                    <span className="text-[10px] text-slate-400">
                      {itemCount} item{itemCount > 1 ? 's' : ''}
                    </span>
                  </div>
                </div>

                {/* Items Preview Strip */}
                <div className="flex items-center gap-2 mt-3 pt-2.5 border-t border-slate-100 dark:border-slate-800">
                  <div className="flex -space-x-2 overflow-hidden py-0.5 shrink-0">
                    {order.items.slice(0, 4).map((item, idx) => (
                      <img
                        key={idx}
                        src={item.image}
                        alt={item.name}
                        className="inline-block w-9 h-9 rounded-xl object-cover ring-2 ring-white dark:ring-slate-900 shadow-xs bg-slate-50 dark:bg-slate-800"
                      />
                    ))}
                    {order.items.length > 4 && (
                      <div className="w-9 h-9 rounded-xl bg-slate-100 dark:bg-slate-800 ring-2 ring-white dark:ring-slate-900 flex items-center justify-center text-[10px] font-bold text-slate-500">
                        +{order.items.length - 4}
                      </div>
                    )}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-xs text-slate-800 dark:text-slate-200 font-medium truncate">
                      {order.items[0].name}
                    </p>
                    <p className="text-[10px] text-slate-400 flex items-center gap-1">
                      <MapPin className="w-2.5 h-2.5 shrink-0 text-slate-400" />
                      <span className="truncate">{order.deliveryAddress}</span>
                    </p>
                  </div>
                  <div className="flex items-center text-xs font-bold text-[#2418D8] dark:text-[#7B73FF] gap-0.5 group-hover:translate-x-0.5 transition-transform shrink-0">
                    <span className="text-[11px]">View</span>
                    <ChevronRight className="w-3.5 h-3.5" />
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Detailed Order Modal */}
      <OrderDetailModal
        order={selectedOrder}
        isOpen={!!selectedOrder}
        onClose={() => setSelectedOrder(null)}
        onReorder={onReorder}
        onReorderAll={onReorderAll}
        onShowToast={onShowToast}
        onNavigateToCart={onNavigateToCart}
      />
    </div>
  );
};
