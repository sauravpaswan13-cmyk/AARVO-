import React, { useState, useEffect } from 'react';
import {
  X,
  Star,
  ShoppingCart,
  Heart,
  ShieldCheck,
  Truck,
  RotateCcw,
  CheckCircle2,
  Share2,
  Zap,
} from 'lucide-react';
import { haptics } from '../utils/haptics';

export interface Product {
  id: string;
  name: string;
  category: string;
  image: string;
  price: number;
  originalPrice: number;
  rating: number;
  reviewsCount: number;
  discount: string;
  inStock: boolean;
  seller: string;
  description?: string;
  highlights?: string[];
}

interface ProductDetailModalProps {
  product: Product | null;
  isOpen: boolean;
  onClose: () => void;
  onAddToCart: (product: Product) => void;
  isWishlisted: boolean;
  onToggleWishlist: (productId: string, productName?: string) => void;
  onBuyNow?: (product: Product) => void;
}

export const ProductDetailModal: React.FC<ProductDetailModalProps> = ({
  product,
  isOpen,
  onClose,
  onAddToCart,
  isWishlisted,
  onToggleWishlist,
  onBuyNow,
}) => {
  const [selectedQty, setSelectedQty] = useState(1);
  const [copiedShare, setCopiedShare] = useState(false);

  useEffect(() => {
    // Reset quantity whenever new product opens
    setSelectedQty(1);
    setCopiedShare(false);

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
  }, [isOpen, product, onClose]);

  if (!isOpen || !product) return null;

  const savings = Math.max(0, product.originalPrice - product.price);

  const defaultDescription =
    product.description ||
    `Experience top-tier quality and premium craftsmanship with the ${product.name}. Designed to deliver maximum performance, verified durability, and complete comfort, backed by the AARVO Authentic Guarantee with hassle-free doorstep returns.`;

  const defaultHighlights = product.highlights || [
    '100% Original and Brand-Certified Product',
    'Free Express Doorstep Delivery within 48 Hours',
    '7 Days Easy Replacement & Refund Guarantee',
    'GST Invoice and Manufacturer Warranty Included',
  ];

  const handleShare = () => {
    haptics.light();
    if (typeof navigator !== 'undefined' && navigator.share) {
      navigator.share({
        title: product.name,
        text: `Check out ${product.name} on AARVO for ₹${product.price.toLocaleString('en-IN')}!`,
        url: window.location.href,
      }).catch(() => {});
    } else {
      setCopiedShare(true);
      setTimeout(() => setCopiedShare(false), 2000);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="product-detail-title"
      className="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-0 sm:p-4"
    >
      {/* Dimmed Backdrop */}
      <div
        onClick={() => {
          haptics.light();
          onClose();
        }}
        className="fixed inset-0 bg-black/70 backdrop-blur-xs transition-opacity animate-in fade-in duration-200"
      />

      {/* Modal / Bottom Sheet Container */}
      <div className="relative w-full max-w-[440px] max-h-[92vh] bg-white dark:bg-[#14112B] text-slate-900 dark:text-slate-100 rounded-t-3xl sm:rounded-3xl shadow-2xl flex flex-col z-10 overflow-hidden animate-in slide-in-from-bottom-8 duration-300 border border-slate-100 dark:border-slate-800">
        
        {/* Top Floating Action Header */}
        <div className="sticky top-0 z-20 flex items-center justify-between px-4 py-3 bg-white/90 dark:bg-[#14112B]/90 backdrop-blur-md border-b border-slate-100 dark:border-slate-800">
          <div className="flex items-center gap-2">
            <span className="text-[10px] font-black uppercase tracking-wider bg-[#2418D8]/10 text-[#2418D8] dark:text-[#7B73FF] dark:bg-purple-950/60 px-2.5 py-1 rounded-full">
              {product.category}
            </span>
            <span className="text-xs text-slate-400 font-medium">&bull; AARVO Verified</span>
          </div>

          <div className="flex items-center gap-1.5">
            {/* Share button */}
            <button
              onClick={handleShare}
              aria-label="Share product"
              className="p-2 text-slate-500 hover:text-slate-800 dark:hover:text-white rounded-full hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
            >
              <Share2 className="w-4 h-4" />
            </button>

            {/* Wishlist toggle button */}
            <button
              onClick={() => onToggleWishlist(product.id, product.name)}
              aria-label={isWishlisted ? 'Remove from wishlist' : 'Add to wishlist'}
              className={`p-2 rounded-full transition-all active:scale-75 ${
                isWishlisted
                  ? 'bg-rose-50 dark:bg-rose-950/80 text-rose-500'
                  : 'text-slate-500 hover:text-rose-500 hover:bg-slate-100 dark:hover:bg-slate-800'
              }`}
            >
              <Heart
                className={`w-4 h-4 ${
                  isWishlisted ? 'fill-rose-500 stroke-rose-500' : 'stroke-current fill-none'
                }`}
              />
            </button>

            {/* Close button */}
            <button
              onClick={() => {
                haptics.light();
                onClose();
              }}
              aria-label="Close product details"
              className="p-2 text-slate-500 hover:text-slate-900 dark:hover:text-white rounded-full hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors ml-1"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Scrollable Product Details Content */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4 no-scrollbar">
          {copiedShare && (
            <div className="p-2 text-center text-xs bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 rounded-xl font-semibold border border-emerald-200 dark:border-emerald-800 animate-in fade-in">
              Product link copied to clipboard!
            </div>
          )}

          {/* Large High-Res Product Image Showcase */}
          <div className="relative aspect-square w-full rounded-2xl overflow-hidden bg-slate-50 dark:bg-[#1A1638] shadow-inner border border-slate-100 dark:border-slate-800 group">
            <img
              src={product.image}
              alt={product.name}
              className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
            />
            {/* Discount Badge */}
            <span className="absolute top-3 left-3 bg-red-600 text-white text-xs font-black px-2.5 py-1 rounded-lg shadow-md tracking-wide">
              {product.discount}
            </span>

            {/* In-Stock Indicator */}
            <span className="absolute bottom-3 left-3 bg-emerald-600/90 backdrop-blur-xs text-white text-[10px] font-extrabold px-2 py-0.5 rounded-full flex items-center gap-1 shadow-xs">
              <CheckCircle2 className="w-3 h-3" />
              In Stock &bull; Ready to Ship
            </span>
          </div>

          {/* Title & Ratings */}
          <div>
            <h2
              id="product-detail-title"
              className="font-display font-black text-base sm:text-lg text-slate-900 dark:text-white leading-snug"
            >
              {product.name}
            </h2>

            <div className="flex items-center gap-2 mt-2">
              <span className="flex items-center gap-1 bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 text-xs font-extrabold px-2 py-0.5 rounded-md border border-emerald-200 dark:border-emerald-800/60">
                <span>{product.rating}</span>
                <Star className="w-3 h-3 fill-current text-emerald-600 dark:text-emerald-400" />
              </span>
              <span className="text-xs text-slate-500 dark:text-slate-400 font-medium">
                {product.reviewsCount.toLocaleString('en-IN')} Verified Ratings
              </span>
              <span className="text-slate-300 dark:text-slate-700">&bull;</span>
              <span className="text-xs text-[#2418D8] dark:text-[#7B73FF] font-semibold">
                Assured
              </span>
            </div>
          </div>

          {/* Pricing Zone */}
          <div className="p-3.5 rounded-2xl bg-slate-50 dark:bg-[#1A1638] border border-slate-100 dark:border-slate-800/80">
            <div className="flex items-baseline gap-2.5">
              <span className="text-2xl font-black text-slate-900 dark:text-white font-display">
                ₹{product.price.toLocaleString('en-IN')}
              </span>
              <span className="text-sm text-slate-400 dark:text-slate-500 line-through font-semibold">
                ₹{product.originalPrice.toLocaleString('en-IN')}
              </span>
              <span className="text-xs font-bold text-emerald-600 dark:text-emerald-400 bg-emerald-100/60 dark:bg-emerald-950/60 px-2 py-0.5 rounded-md">
                Save ₹{savings.toLocaleString('en-IN')}
              </span>
            </div>
            <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-1">
              Inclusive of all taxes &bull; No cost EMI available from ₹
              {Math.round(product.price / 3).toLocaleString('en-IN')}/month
            </p>
          </div>

          {/* Assurance Badges (Flipkart/Amazon style) */}
          <div className="grid grid-cols-3 gap-2 text-center">
            <div className="p-2.5 rounded-xl bg-purple-50/70 dark:bg-purple-950/30 border border-purple-100 dark:border-purple-900/30 flex flex-col items-center">
              <Truck className="w-4 h-4 text-[#2418D8] dark:text-[#7B73FF] mb-1" />
              <span className="text-[10px] font-bold text-slate-800 dark:text-slate-200">
                Free Delivery
              </span>
              <span className="text-[9px] text-slate-500">In 2 Days</span>
            </div>

            <div className="p-2.5 rounded-xl bg-purple-50/70 dark:bg-purple-950/30 border border-purple-100 dark:border-purple-900/30 flex flex-col items-center">
              <RotateCcw className="w-4 h-4 text-[#2418D8] dark:text-[#7B73FF] mb-1" />
              <span className="text-[10px] font-bold text-slate-800 dark:text-slate-200">
                7 Days Return
              </span>
              <span className="text-[9px] text-slate-500">Doorstep pickup</span>
            </div>

            <div className="p-2.5 rounded-xl bg-purple-50/70 dark:bg-purple-950/30 border border-purple-100 dark:border-purple-900/30 flex flex-col items-center">
              <ShieldCheck className="w-4 h-4 text-[#2418D8] dark:text-[#7B73FF] mb-1" />
              <span className="text-[10px] font-bold text-slate-800 dark:text-slate-200">
                AARVO Assured
              </span>
              <span className="text-[9px] text-slate-500">100% Genuine</span>
            </div>
          </div>

          {/* Full Description */}
          <div className="space-y-2">
            <h3 className="font-display font-bold text-xs uppercase tracking-wider text-slate-500 dark:text-slate-400">
              Product Overview &amp; Details
            </h3>
            <p className="text-xs text-slate-700 dark:text-slate-300 leading-relaxed">
              {defaultDescription}
            </p>
          </div>

          {/* Key Highlights */}
          <div className="space-y-2">
            <h3 className="font-display font-bold text-xs uppercase tracking-wider text-slate-500 dark:text-slate-400">
              Key Highlights
            </h3>
            <ul className="space-y-1.5">
              {defaultHighlights.map((hl, idx) => (
                <li
                  key={idx}
                  className="text-xs text-slate-700 dark:text-slate-300 flex items-start gap-2"
                >
                  <span className="w-1.5 h-1.5 rounded-full bg-[#2418D8] dark:bg-[#7B73FF] mt-1.5 shrink-0" />
                  <span>{hl}</span>
                </li>
              ))}
            </ul>
          </div>

          {/* Verified Seller Information */}
          <div className="p-3 rounded-xl bg-slate-50 dark:bg-[#1A1638] border border-slate-100 dark:border-slate-800 text-xs flex items-center justify-between">
            <div>
              <span className="text-[10px] text-slate-400 uppercase font-bold block">
                Sold By
              </span>
              <span className="font-bold text-slate-900 dark:text-white">
                {product.seller}
              </span>
            </div>
            <span className="text-[10px] font-bold text-emerald-600 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/60 px-2 py-0.5 rounded-md border border-emerald-200 dark:border-emerald-800">
              GST Verified
            </span>
          </div>
        </div>

        {/* Sticky Bottom Actions Bar */}
        <div className="sticky bottom-0 z-20 p-3.5 bg-white/95 dark:bg-[#14112B]/95 backdrop-blur-md border-t border-slate-100 dark:border-slate-800 flex items-center gap-2.5">
          <button
            type="button"
            onClick={() => {
              haptics.success();
              onAddToCart(product);
            }}
            className="flex-1 py-3 px-4 rounded-xl bg-slate-100 hover:bg-slate-200 dark:bg-[#201B42] dark:hover:bg-[#282255] text-slate-900 dark:text-white font-bold text-xs flex items-center justify-center gap-2 active:scale-95 transition-all shadow-xs"
          >
            <ShoppingCart className="w-4 h-4 text-[#2418D8] dark:text-[#7B73FF]" />
            <span>Add to Cart</span>
          </button>

          <button
            type="button"
            onClick={() => {
              haptics.impact();
              if (onBuyNow) {
                onBuyNow(product);
              } else {
                onAddToCart(product);
                onClose();
              }
            }}
            className="flex-1 py-3 px-4 rounded-xl bg-[#2418D8] hover:bg-[#1A10B3] text-white font-extrabold text-xs flex items-center justify-center gap-1.5 active:scale-95 transition-all shadow-md"
          >
            <Zap className="w-4 h-4 fill-current text-yellow-300" />
            <span>Buy Now</span>
          </button>
        </div>
      </div>
    </div>
  );
};
