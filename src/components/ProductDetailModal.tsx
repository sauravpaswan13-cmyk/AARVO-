import React, { useState, useEffect, useRef } from 'react';
import {
  X,
  ArrowLeft,
  Search,
  Star,
  ShoppingCart,
  Heart,
  ShieldCheck,
  Truck,
  RotateCcw,
  CheckCircle2,
  Share2,
  Zap,
  ZoomIn,
  ChevronLeft,
  ChevronRight,
  Tag,
  Banknote,
  Check,
  Plus,
  Minus,
  MapPin,
  Sparkles,
} from 'lucide-react';
import { haptics } from '../utils/haptics';
import { Product360ViewerModal } from './Product360ViewerModal';
import { ProductImageZoomModal } from './ProductImageZoomModal';
import { Order } from './OrdersSection';

export interface ReviewItem {
  id: string;
  userName: string;
  rating: number;
  date: string;
  comment: string;
  size?: string;
  color?: string;
  verifiedBuyer?: boolean;
}

export interface Product {
  id: string;
  name: string;
  category: string;
  image: string;
  images?: string[];
  price: number;
  originalPrice: number;
  rating: number;
  reviewsCount: number;
  discount: string;
  inStock: boolean;
  stockQuantity?: number;
  seller: string;
  description?: string;
  highlights?: string[];
  specifications?: Record<string, string>;
  codAvailable?: boolean;
  availableSizes?: string[];
  availableColors?: string[];
  fabric?: string;
  reviewsList?: ReviewItem[];
}

interface ProductDetailModalProps {
  product: Product | null;
  isOpen: boolean;
  onClose: () => void;
  onAddToCart: (product: Product, quantity?: number) => void;
  isWishlisted: boolean;
  onToggleWishlist: (productId: string, productName?: string) => void;
  onBuyNow?: (product: Product, quantity?: number) => void;
  onPlaceOrder?: (order: Order) => void;
  onAddReview?: (productId: string, review: ReviewItem) => void;
  cartBadgeCount?: number;
  onOpenCart?: () => void;
  onOpenOrders?: () => void;
  onOpenImageZoom?: (product: Product) => void;
  savedAddress?: string;
  customerName?: string;
  onUpdateAddress?: (addr: string) => void;
}

export const ProductDetailModal: React.FC<ProductDetailModalProps> = ({
  product,
  isOpen,
  onClose,
  onAddToCart,
  isWishlisted,
  onToggleWishlist,
  onBuyNow,
  onPlaceOrder,
  onAddReview,
  cartBadgeCount = 0,
  onOpenCart,
  onOpenOrders,
  onOpenImageZoom,
  savedAddress,
  customerName = 'Saurav Paswan',
  onUpdateAddress,
}) => {
  // Image slider state
  const [currentImageIndex, setCurrentImageIndex] = useState(0);
  const [selectedQty, setSelectedQty] = useState(1);
  const [copiedShare, setCopiedShare] = useState(false);
  const [is360Open, setIs360Open] = useState(false);
  const [isZoomOpen, setIsZoomOpen] = useState(false);

  // Variant states: Size & Color
  const [selectedSize, setSelectedSize] = useState<string>('');
  const [selectedColor, setSelectedColor] = useState<string>('');

  // Customer Ratings & Reviews state
  const [showReviewForm, setShowReviewForm] = useState(false);
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewerName, setReviewerName] = useState(customerName);
  const [reviewComment, setReviewComment] = useState('');
  const [localReviews, setLocalReviews] = useState<ReviewItem[]>([]);
  const [reviewSuccess, setReviewSuccess] = useState(false);
  const [showAllOffers, setShowAllOffers] = useState(false);

  // Fast Checkout (Cash on Delivery) Drawer states
  const [isCheckoutOpen, setIsCheckoutOpen] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState<'COD' | 'UPI' | 'CARD'>('COD');
  const [deliveryAddress, setDeliveryAddress] = useState(
    savedAddress || 'Home • 24B, Model Town, Phase 2, Patna - 800001'
  );
  const [isEditingAddress, setIsEditingAddress] = useState(false);
  const [pincode, setPincode] = useState(() => {
    const m = savedAddress?.match(/\b\d{6}\b/);
    return m ? m[0] : '800001';
  });
  const [isEditingPincode, setIsEditingPincode] = useState(false);
  const [pincodeInput, setPincodeInput] = useState('');
  const [isPincodeValid, setIsPincodeValid] = useState(true);
  const [orderConfirmed, setOrderConfirmed] = useState<Order | null>(null);

  // Swipe / Drag gesture tracking for image carousel
  const touchStartX = useRef<number>(0);
  const touchDeltaX = useRef<number>(0);
  const isMouseDown = useRef<boolean>(false);
  const mouseStartX = useRef<number>(0);

  // Compute product images list (guarantees at least 4 thumbnails for clean gallery view)
  const productImages: string[] = React.useMemo(() => {
    if (!product) return [];
    if (product.images && product.images.length >= 4) {
      return product.images;
    }
    if (product.images && product.images.length > 0) {
      const imgs = [...product.images];
      while (imgs.length < 4) {
        imgs.push(product.images[imgs.length % product.images.length]);
      }
      return imgs;
    }
    return [product.image, product.image, product.image, product.image];
  }, [product]);

  // Extract real sizes if specified for this product
  const parsedSizes: string[] = React.useMemo(() => {
    if (!product) return [];
    if (product.availableSizes && product.availableSizes.length > 0) {
      return product.availableSizes;
    }
    if (product.description) {
      const match = product.description.match(/\[Sizes:\s*([^\]]+)\]/i);
      if (match && match[1]) {
        return match[1].split(',').map((s) => s.trim()).filter(Boolean);
      }
    }
    return [];
  }, [product]);

  // Extract real colours if specified for this product
  const parsedColors: string[] = React.useMemo(() => {
    if (!product) return [];
    if (product.availableColors && product.availableColors.length > 0) {
      return product.availableColors;
    }
    if (product.description) {
      const match = product.description.match(/\[Colours?:\s*([^\]]+)\]/i);
      if (match && match[1]) {
        return match[1].split(',').map((c) => c.trim()).filter(Boolean);
      }
    }
    return [];
  }, [product]);

  useEffect(() => {
    // Reset state whenever modal opens or product changes
    setCurrentImageIndex(0);
    setSelectedQty(1);
    setCopiedShare(false);
    setIs360Open(false);
    setIsZoomOpen(false);
    setIsCheckoutOpen(false);
    setOrderConfirmed(null);
    setShowReviewForm(false);
    setReviewComment('');
    setReviewSuccess(false);
    setIsEditingPincode(false);

    if (savedAddress) {
      setDeliveryAddress(savedAddress);
      const pinMatch = savedAddress.match(/\b\d{6}\b/);
      if (pinMatch) setPincode(pinMatch[0]);
    }
    if (customerName) {
      setReviewerName(customerName);
    }

    if (product) {
      const s = product.availableSizes && product.availableSizes.length > 0
        ? product.availableSizes
        : (product.description?.match(/\[Sizes:\s*([^\]]+)\]/i)?.[1]?.split(',').map((x) => x.trim()).filter(Boolean) || []);
      setSelectedSize(s.length > 0 ? s[0] : '');

      const c = product.availableColors && product.availableColors.length > 0
        ? product.availableColors
        : (product.description?.match(/\[Colours?:\s*([^\]]+)\]/i)?.[1]?.split(',').map((x) => x.trim()).filter(Boolean) || []);
      setSelectedColor(c.length > 0 ? c[0] : '');

      setLocalReviews(product.reviewsList || []);
    }

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        if (orderConfirmed) {
          setOrderConfirmed(null);
          onClose();
        } else if (isCheckoutOpen) {
          setIsCheckoutOpen(false);
        } else {
          onClose();
        }
      }
      if (e.key === 'ArrowLeft') {
        handlePrevImage();
      }
      if (e.key === 'ArrowRight') {
        handleNextImage();
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
  }, [isOpen, product, onClose, isCheckoutOpen, orderConfirmed]);

  if (!isOpen || !product) return null;

  const totalImages = productImages.length;

  const handleNextImage = () => {
    haptics.selection();
    setCurrentImageIndex((prev) => (prev + 1) % totalImages);
  };

  const handlePrevImage = () => {
    haptics.selection();
    setCurrentImageIndex((prev) => (prev - 1 + totalImages) % totalImages);
  };

  // Touch Swipe Handlers
  const handleTouchStart = (e: React.TouchEvent) => {
    touchStartX.current = e.touches[0].clientX;
    touchDeltaX.current = 0;
  };

  const handleTouchMove = (e: React.TouchEvent) => {
    touchDeltaX.current = e.touches[0].clientX - touchStartX.current;
  };

  const handleTouchEnd = () => {
    if (touchDeltaX.current < -35 && totalImages > 1) {
      handleNextImage();
    } else if (touchDeltaX.current > 35 && totalImages > 1) {
      handlePrevImage();
    }
    touchDeltaX.current = 0;
  };

  // Mouse Drag Handlers for Desktop
  const handleMouseDown = (e: React.MouseEvent) => {
    isMouseDown.current = true;
    mouseStartX.current = e.clientX;
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    if (!isMouseDown.current) return;
    const delta = e.clientX - mouseStartX.current;
    if (Math.abs(delta) > 50 && totalImages > 1) {
      if (delta < 0) handleNextImage();
      else handlePrevImage();
      isMouseDown.current = false;
    }
  };

  const handleMouseUp = () => {
    isMouseDown.current = false;
  };

  const baseReviewsCount = product.reviewsCount || 0;
  const totalReviewsCount = baseReviewsCount + localReviews.length;
  const effectiveRating =
    totalReviewsCount > 0
      ? localReviews.length > 0
        ? (baseReviewsCount * (product.rating || 0) +
            localReviews.reduce((sum, r) => sum + r.rating, 0)) /
          totalReviewsCount
        : product.rating || 0
      : 0;

  const savings = Math.max(0, product.originalPrice - product.price);
  const totalPrice = product.price * selectedQty;
  const totalMRP = product.originalPrice * selectedQty;

  const defaultDescription =
    product.description ||
    `Experience top-tier quality and premium craftsmanship with the ${product.name}. Designed to deliver maximum performance, verified durability, and complete comfort, backed by the AARVO Authentic Guarantee with hassle-free doorstep returns.`;

  const defaultHighlights = product.highlights || [
    '100% Original and Brand-Certified Product',
    'Free Express Doorstep Delivery within 24-48 Hours',
    'Cash On Delivery (COD) Eligible with Zero Extra Fee',
    '7 Days Easy Replacement & Refund Guarantee',
    'GST Invoice and 1 Year Manufacturer Warranty Included',
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

  // Real Cash on Delivery Order Placement
  const handleConfirmOrder = () => {
    haptics.success();
    const orderId = `AARVO-COD-${Math.floor(10000 + Math.random() * 90000)}`;
    const trackingNo = `AARVO-EXP-${Math.floor(100000 + Math.random() * 900000)}`;

    const newOrder: Order = {
      id: orderId,
      date: 'Today, ' + new Date().toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }),
      total: totalPrice,
      status: 'Processing',
      statusStep: 1,
      estimatedDelivery: 'Tomorrow by 9:00 PM',
      trackingNumber: trackingNo,
      carrier: 'AARVO Express Surface • AWB ' + trackingNo.slice(-6),
      paymentMethod: paymentMethod === 'COD' ? 'Cash on Delivery (Pay at doorstep)' : paymentMethod === 'UPI' ? 'UPI' : 'Card',
      deliveryAddress: deliveryAddress,
      items: [
        {
          id: product.id,
          name: product.name,
          price: product.price,
          quantity: selectedQty,
          image: product.image,
          category: product.category,
        },
      ],
      timeline: [
        {
          status: 'Order Placed (Cash on Delivery)',
          time: 'Just now',
          description: `Order confirmed for ₹${totalPrice.toLocaleString('en-IN')}. Please keep exact cash ready upon delivery.`,
          completed: true,
        },
        {
          status: 'Seller Packaging',
          time: 'Preparing',
          description: `${product.seller} is packing your item with verified seal.`,
          completed: false,
        },
        {
          status: 'Out for Delivery',
          time: 'Tomorrow, by 9:00 AM',
          description: 'AARVO delivery agent will arrive at your address with the package.',
          completed: false,
        },
        {
          status: 'Delivered & Cash Collected',
          time: 'Tomorrow, by 9:00 PM',
          description: 'Cash payment will be verified and collected at your doorstep.',
          completed: false,
        },
      ],
    };

    if (onPlaceOrder) {
      onPlaceOrder(newOrder);
    }
    setOrderConfirmed(newOrder);
    setIsCheckoutOpen(false);
  };

  // Real Customer Review submission
  const handleSubmitReview = (e: React.FormEvent) => {
    e.preventDefault();
    if (!product || !reviewComment.trim()) return;
    haptics.success();
    const newRev: ReviewItem = {
      id: `rev-${Date.now()}`,
      userName: reviewerName.trim() || 'Verified Buyer',
      rating: reviewRating,
      date: 'Just now',
      comment: reviewComment.trim(),
      size: selectedSize,
      color: selectedColor,
      verifiedBuyer: true,
    };
    const updated = [newRev, ...localReviews];
    setLocalReviews(updated);
    setReviewComment('');
    setShowReviewForm(false);
    setReviewSuccess(true);
    setTimeout(() => setReviewSuccess(false), 4500);
    if (onAddReview) {
      onAddReview(product.id, newRev);
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
          if (orderConfirmed) {
            setOrderConfirmed(null);
            onClose();
          } else if (isCheckoutOpen) {
            setIsCheckoutOpen(false);
          } else {
            onClose();
          }
        }}
        className="fixed inset-0 bg-black/75 backdrop-blur-xs transition-opacity animate-in fade-in duration-200"
      />

      {/* Modal / Bottom Sheet Container (Full slide-up on mobile, Flipkart-style) */}
      <div className="relative w-full max-w-[460px] h-[95vh] sm:h-auto sm:max-h-[94vh] bg-white dark:bg-[#181311] text-slate-900 dark:text-slate-100 rounded-t-3xl sm:rounded-3xl shadow-2xl flex flex-col z-10 overflow-hidden animate-in slide-in-from-bottom duration-300 border border-orange-100/80 dark:border-orange-950/50">
        
        {/* Mobile Swipe Grab Handle Indicator */}
        <div className="w-12 h-1.5 rounded-full bg-orange-200 dark:bg-orange-900/60 mx-auto my-1.5 sm:hidden shrink-0" />

        {/* ================================================================= */}
        {/* 1. FLIPKART-STYLE TOP APP BAR                                     */}
        {/* ================================================================= */}
        <header className="sticky top-0 z-30 flex items-center justify-between px-3.5 pt-[max(0.75rem,env(safe-area-inset-top))] pb-3 bg-gradient-to-b from-[#FFE5CC]/75 via-[#FFF8F2] to-white dark:from-[#26160C] dark:via-[#1B120E] dark:to-[#181311] backdrop-blur-md border-b border-orange-100/80 dark:border-orange-950/50">
          <div className="flex items-center gap-2.5">
            <button
              type="button"
              onClick={() => {
                haptics.light();
                onClose();
              }}
              aria-label="Back"
              className="p-1 text-slate-800 dark:text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-full transition-colors cursor-pointer"
            >
              <ArrowLeft className="w-5 h-5 stroke-[2.2]" />
            </button>
            <h2 className="text-sm sm:text-base font-bold text-slate-900 dark:text-white">
              Product Details
            </h2>
          </div>

          <div className="flex items-center gap-1.5">
            {/* Search button */}
            <button
              type="button"
              onClick={() => {
                haptics.light();
                onClose();
              }}
              aria-label="Search items"
              className="p-1.5 text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-full transition-colors cursor-pointer"
            >
              <Search className="w-5 h-5" />
            </button>

            {/* Wishlist toggle button */}
            <button
              type="button"
              onClick={() => {
                haptics.light();
                onToggleWishlist(product.id, product.name);
              }}
              aria-label={isWishlisted ? 'Remove from wishlist' : 'Add to wishlist'}
              className={`p-1.5 rounded-full transition-all active:scale-75 cursor-pointer ${
                isWishlisted
                  ? 'bg-rose-50 dark:bg-rose-950/80 text-rose-500'
                  : 'text-slate-700 dark:text-slate-200 hover:text-rose-500 hover:bg-slate-100 dark:hover:bg-slate-800'
              }`}
            >
              <Heart
                className={`w-5 h-5 ${
                  isWishlisted ? 'fill-rose-500 stroke-rose-500' : 'stroke-current fill-none'
                }`}
              />
            </button>

            {/* Cart Button with live Badge Counter */}
            <button
              type="button"
              onClick={() => {
                haptics.selection();
                if (onOpenCart) {
                  onClose();
                  onOpenCart();
                } else {
                  onAddToCart(product);
                }
              }}
              aria-label="Open Cart"
              className="relative p-1.5 text-slate-800 dark:text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-full transition-colors cursor-pointer"
            >
              <ShoppingCart className="w-5 h-5 stroke-[2]" />
              {cartBadgeCount > 0 && (
                <span className="absolute -top-1 -right-1 min-w-[17px] h-4 px-1 rounded-full bg-[#FF5722] text-white text-[9px] font-black flex items-center justify-center shadow-xs">
                  {cartBadgeCount}
                </span>
              )}
            </button>
          </div>
        </header>

        {/* ================================================================= */}
        {/* 2. SCROLLABLE MAIN PRODUCT CONTENT                                */}
        {/* ================================================================= */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4 no-scrollbar">
          {copiedShare && (
            <div className="p-2 text-center text-xs bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 rounded-xl font-semibold border border-emerald-200 dark:border-emerald-800 animate-in fade-in">
              Product link copied to clipboard!
            </div>
          )}

          {/* =============================================================== */}
          {/* MULTI-IMAGE SLIDER WITH TOUCH SWIPE & PAGINATION DOTS           */}
          {/* Shows exactly 5 dots if 5 images, or 3 dots if 3 images!         */}
          {/* =============================================================== */}
          <div className="space-y-2.5">
            <div
              onTouchStart={handleTouchStart}
              onTouchMove={handleTouchMove}
              onTouchEnd={handleTouchEnd}
              onMouseDown={handleMouseDown}
              onMouseMove={handleMouseMove}
              onMouseUp={handleMouseUp}
              onMouseLeave={handleMouseUp}
              className="relative aspect-square w-full rounded-2xl overflow-hidden bg-white dark:bg-[#211A17] shadow-sm border border-orange-100/80 dark:border-orange-950/50 group select-none cursor-grab active:cursor-grabbing"
            >
              {/* Image Slides Track */}
              <div
                className="flex w-full h-full transition-transform duration-300 ease-out"
                style={{ transform: `translateX(-${currentImageIndex * 100}%)` }}
              >
                {productImages.map((imgUrl, idx) => (
                  <div
                    key={idx}
                    onClick={() => {
                      haptics.light();
                      if (onOpenImageZoom) {
                        onOpenImageZoom({ ...product, image: imgUrl });
                      } else {
                        setIsZoomOpen(true);
                      }
                    }}
                    className="w-full h-full shrink-0 flex items-center justify-center p-3 relative cursor-zoom-in"
                  >
                    <img
                      src={imgUrl}
                      alt={`${product.name} view ${idx + 1}`}
                      className="w-full h-full object-contain pointer-events-none"
                      draggable={false}
                    />
                  </div>
                ))}
              </div>

              {/* Discount Badge */}
              {product.originalPrice > product.price && (
                <span className="absolute top-3 left-3 bg-red-600 text-white text-[11px] font-black px-2.5 py-1 rounded-lg shadow-md tracking-wide pointer-events-none z-10">
                  {product.discount}
                </span>
              )}

              {/* Image Counter Badge (e.g. 1/5 or 3/3) */}
              <div className="absolute top-3 right-3 bg-black/60 backdrop-blur-md text-white text-[10px] font-black px-2.5 py-1 rounded-full shadow-md border border-white/20 pointer-events-none z-10 flex items-center gap-1">
                <span>{currentImageIndex + 1}</span>
                <span className="text-white/60">/</span>
                <span>{totalImages}</span>
              </div>

              {/* Tap to Zoom Indicator */}
              <div
                onClick={(e) => {
                  e.stopPropagation();
                  haptics.light();
                  if (onOpenImageZoom) {
                    onOpenImageZoom({ ...product, image: productImages[currentImageIndex] });
                  } else {
                    setIsZoomOpen(true);
                  }
                }}
                className="absolute bottom-3 left-3 bg-black/60 hover:bg-[#FF5722] backdrop-blur-md text-white px-2.5 py-1 rounded-full shadow-md border border-white/20 flex items-center gap-1.5 text-[10px] font-bold transition-colors cursor-pointer z-10"
              >
                <ZoomIn className="w-3 h-3" />
                <span>Tap to Zoom</span>
              </div>

              {/* Left / Right Carousel Navigation Arrows */}
              {totalImages > 1 && (
                <>
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      handlePrevImage();
                    }}
                    aria-label="Previous image"
                    className="absolute left-2 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full bg-white/90 dark:bg-slate-900/90 text-slate-800 dark:text-slate-100 shadow-md border border-slate-200/60 dark:border-slate-700 flex items-center justify-center transition-all opacity-85 hover:opacity-100 active:scale-90 z-10 cursor-pointer"
                  >
                    <ChevronLeft className="w-4 h-4 stroke-[2.5]" />
                  </button>
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleNextImage();
                    }}
                    aria-label="Next image"
                    className="absolute right-2 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full bg-white/90 dark:bg-slate-900/90 text-slate-800 dark:text-slate-100 shadow-md border border-slate-200/60 dark:border-slate-700 flex items-center justify-center transition-all opacity-85 hover:opacity-100 active:scale-90 z-10 cursor-pointer"
                  >
                    <ChevronRight className="w-4 h-4 stroke-[2.5]" />
                  </button>
                </>
              )}
            </div>

            {/* Pagination Dots (Exactly 5 dots for 5 images, or 3 dots for 3 images!) */}
            {totalImages > 1 && (
              <div className="flex items-center justify-center gap-1.5 py-1">
                {productImages.map((_, idx) => {
                  const isActive = idx === currentImageIndex;
                  return (
                    <button
                      key={idx}
                      type="button"
                      aria-label={`Go to slide ${idx + 1}`}
                      onClick={() => {
                        haptics.selection();
                        setCurrentImageIndex(idx);
                      }}
                      className={`transition-all duration-300 rounded-full cursor-pointer ${
                        isActive
                          ? 'w-6 h-2 bg-[#FF5722] shadow-xs'
                          : 'w-2 h-2 bg-slate-300 dark:bg-slate-700 hover:bg-slate-400'
                      }`}
                    />
                  );
                })}
              </div>
            )}

            {/* Thumbnail Strip: 4 Square Cards matching mockup with orange border on selected */}
            <div className="grid grid-cols-4 gap-2.5 py-1">
              {productImages.slice(0, 4).map((thumbUrl, idx) => {
                const isSelected = idx === currentImageIndex;
                return (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => {
                      haptics.selection();
                      setCurrentImageIndex(idx);
                    }}
                    className={`relative aspect-square rounded-xl overflow-hidden border-2 transition-all cursor-pointer bg-slate-50 dark:bg-slate-900 ${
                      isSelected
                        ? 'border-[#FF5722] ring-2 ring-[#FF5722]/30 shadow-xs scale-102'
                        : 'border-slate-200 dark:border-slate-700/80 opacity-70 hover:opacity-100'
                    }`}
                  >
                    <img
                      src={thumbUrl}
                      alt={`Thumbnail ${idx + 1}`}
                      className="w-full h-full object-contain p-1"
                    />
                  </button>
                );
              })}
            </div>
          </div>

          {/* =============================================================== */}
          {/* TITLE & SHARE BUTTON (MATCHING MOCKUP SCREENSHOT)                */}
          {/* =============================================================== */}
          <div>
            <div className="flex items-start justify-between gap-3">
              <h1
                id="product-detail-title"
                className="font-display font-extrabold text-base sm:text-lg text-slate-900 dark:text-white leading-snug flex-1"
              >
                {product.name}
              </h1>
              <button
                type="button"
                onClick={handleShare}
                aria-label="Share product"
                className="p-1.5 text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-full transition-colors shrink-0 cursor-pointer"
                title="Share product link"
              >
                <Share2 className="w-5 h-5 stroke-[2]" />
              </button>
            </div>

            {/* Real Rating pill or honest empty prompt */}
            <div className="flex items-center gap-2 mt-2">
              {totalReviewsCount > 0 ? (
                <>
                  <div className="flex items-center gap-1 bg-emerald-600 text-white text-xs font-black px-2 py-0.5 rounded-md shadow-2xs">
                    <span>{effectiveRating.toFixed(1)}</span>
                    <Star className="w-3 h-3 fill-white" />
                  </div>
                  <span className="text-xs text-slate-500 dark:text-slate-400 font-medium">
                    ({totalReviewsCount.toLocaleString('en-IN')} reviews)
                  </span>
                </>
              ) : (
                <button
                  type="button"
                  onClick={() => {
                    haptics.selection();
                    setShowReviewForm(true);
                  }}
                  className="text-xs font-semibold text-[#FF5722] dark:text-[#FF8A65] hover:underline cursor-pointer"
                >
                  No reviews yet &bull; Be the first to rate
                </button>
              )}
            </div>
          </div>

          {/* =============================================================== */}
          {/* PRICING ZONE                                                    */}
          {/* =============================================================== */}
          <div className="flex items-baseline gap-2.5 pt-1">
            <span className="text-2xl sm:text-3xl font-black text-slate-900 dark:text-white font-display">
              ₹{product.price.toLocaleString('en-IN')}
            </span>
            {product.originalPrice > product.price && (
              <>
                <span className="text-sm sm:text-base text-slate-400 dark:text-slate-500 line-through font-semibold">
                  ₹{product.originalPrice.toLocaleString('en-IN')}
                </span>
                {product.discount && (
                  <span className="text-xs font-bold text-white bg-[#E53935] px-2 py-0.5 rounded shadow-xs tracking-wide">
                    {product.discount}
                  </span>
                )}
              </>
            )}
          </div>

          {/* =============================================================== */}
          {/* 2 TRUST CARDS MATCHING SCREENSHOT EXACTLY (FREE DELIVERY & SECURE PAYMENT) */}
          {/* =============================================================== */}
          <div className="space-y-2.5 pt-1">
            {/* 1. Green Free Delivery Card */}
            <div className="p-3 rounded-2xl bg-[#E8F8EE] dark:bg-emerald-950/40 border border-emerald-200/90 dark:border-emerald-800/80 flex items-center gap-3 shadow-2xs">
              <div className="w-8 h-8 rounded-full bg-emerald-600 text-white flex items-center justify-center shrink-0 shadow-2xs">
                <Check className="w-4 h-4 stroke-[3]" />
              </div>
              <div className="flex-1 min-w-0">
                <h4 className="text-xs sm:text-[13px] font-black text-slate-900 dark:text-white leading-tight">
                  Free Delivery
                </h4>
                <p className="text-[11px] text-slate-600 dark:text-slate-300 font-medium mt-0.5">
                  Delivery by 7–10 Nov
                </p>
              </div>
            </div>

            {/* 2. Blue Secure Payment Card */}
            <div className="p-3 rounded-2xl bg-[#EEF4FF] dark:bg-blue-950/40 border border-blue-200/90 dark:border-blue-800/80 flex items-center gap-3 shadow-2xs">
              <div className="w-8 h-8 rounded-full bg-blue-600 text-white flex items-center justify-center shrink-0 shadow-2xs">
                <ShieldCheck className="w-4 h-4 stroke-[2.5]" />
              </div>
              <div className="flex-1 min-w-0">
                <h4 className="text-xs sm:text-[13px] font-black text-slate-900 dark:text-white leading-tight">
                  Secure Payment
                </h4>
                <p className="text-[11px] text-slate-600 dark:text-slate-300 font-medium mt-0.5">
                  100% Secure &amp; Easy Payments
                </p>
              </div>
            </div>
          </div>

          {/* =============================================================== */}
          {/* FLIPKART "AVAILABLE OFFERS" ACCORDION                            */}
          {/* =============================================================== */}
          <div className="p-3.5 rounded-2xl bg-white dark:bg-[#211A17] border border-orange-100/80 dark:border-orange-950/50 space-y-2.5">
            <h3 className="font-display font-extrabold text-xs uppercase tracking-wider text-slate-900 dark:text-white flex items-center gap-1.5">
              <Tag className="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400" />
              <span>Available offers</span>
            </h3>

            {/* Offer 1 */}
            <div className="flex items-start gap-2 text-xs">
              <Tag className="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400 shrink-0 mt-0.5" />
              <div className="flex-1 leading-snug">
                <span className="font-black text-slate-900 dark:text-slate-100">Bank Offer: </span>
                <span className="text-slate-700 dark:text-slate-300">5% Unlimited Cashback on AARVO Axis Bank Credit Card </span>
                <span className="text-[#FF5722] dark:text-[#FF8A65] font-bold text-[10px]">T&amp;C</span>
              </div>
            </div>

            {/* Offer 2 */}
            <div className="flex items-start gap-2 text-xs">
              <Tag className="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400 shrink-0 mt-0.5" />
              <div className="flex-1 leading-snug">
                <span className="font-black text-slate-900 dark:text-slate-100">Bank Offer: </span>
                <span className="text-slate-700 dark:text-slate-300">10% instant discount on SBI &amp; HDFC Bank cards on orders above ₹1,499 </span>
                <span className="text-[#FF5722] dark:text-[#FF8A65] font-bold text-[10px]">T&amp;C</span>
              </div>
            </div>

            {/* Collapsible extra offers */}
            {showAllOffers && (
              <div className="space-y-2.5 pt-1 animate-in fade-in duration-200">
                <div className="flex items-start gap-2 text-xs">
                  <Tag className="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400 shrink-0 mt-0.5" />
                  <div className="flex-1 leading-snug">
                    <span className="font-black text-slate-900 dark:text-slate-100">Coupon Offer: </span>
                    <span className="text-slate-700 dark:text-slate-300">Use AARVO200 in Cart on orders ₹999+ for flat ₹200 off </span>
                  </div>
                </div>

                <div className="flex items-start gap-2 text-xs">
                  <Tag className="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400 shrink-0 mt-0.5" />
                  <div className="flex-1 leading-snug">
                    <span className="font-black text-slate-900 dark:text-slate-100">Welcome Offer: </span>
                    <span className="text-slate-700 dark:text-slate-300">Use WELCOME100 in Cart on orders ₹499+ for flat ₹100 off </span>
                  </div>
                </div>
              </div>
            )}

            {/* Accordion expand/collapse toggle */}
            <button
              type="button"
              onClick={() => {
                haptics.selection();
                setShowAllOffers(!showAllOffers);
              }}
              className="text-xs font-bold text-[#FF5722] dark:text-[#FF8A65] hover:underline pt-1 flex items-center gap-1 cursor-pointer"
            >
              <span>{showAllOffers ? 'View fewer offers' : '+ 2 more offers'}</span>
            </button>
          </div>

          {/* =============================================================== */}
          {/* FLIPKART & MEESHO STYLE COLOUR & SIZE VARIANT SELECTOR           */}
          {/* (SHOWN ONLY WHEN SELLER HAS CONFIGURED SIZES OR COLOURS)          */}
          {/* =============================================================== */}
          {(parsedColors.length > 0 || parsedSizes.length > 0) && (
            <div className="p-3.5 rounded-2xl bg-white dark:bg-[#211A17] border border-orange-100/80 dark:border-orange-950/50 space-y-3">
              {/* Color Selection - Only if product has colours */}
              {parsedColors.length > 0 && (
                <div>
                  <div className="flex items-center justify-between mb-1.5">
                    <span className="text-xs font-bold text-slate-800 dark:text-slate-200 flex items-center gap-1.5">
                      <span>Colour:</span>
                      <strong className="text-[#FF5722] dark:text-[#FF8A65] font-black">{selectedColor || parsedColors[0]}</strong>
                    </span>
                    <span className="text-[10px] text-slate-400 font-medium">
                      {parsedColors.length} options
                    </span>
                  </div>
                  <div className="flex items-center gap-2 overflow-x-auto no-scrollbar py-1">
                    {parsedColors.map((col) => {
                      const isColSelected = (selectedColor || parsedColors[0]).toLowerCase() === col.toLowerCase();
                      const colorMap: Record<string, string> = {
                        black: '#0F172A',
                        white: '#F8FAFC',
                        'royal blue': '#2563EB',
                        blue: '#2563EB',
                        red: '#DC2626',
                        maroon: '#831843',
                        'emerald green': '#059669',
                        green: '#059669',
                        navy: '#1E3A8A',
                        'navy blue': '#1E3A8A',
                        yellow: '#FBBF24',
                        pink: '#F472B6',
                        grey: '#64748B',
                        beige: '#D97706',
                        orange: '#EA580C',
                        purple: '#7E22CE',
                      };
                      const hex = colorMap[col.toLowerCase()] || '#FF5722';
                      return (
                        <button
                          key={col}
                          type="button"
                          onClick={() => {
                            haptics.selection();
                            setSelectedColor(col);
                          }}
                          className={`flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl border text-xs font-bold transition-all cursor-pointer ${
                            isColSelected
                              ? 'border-[#FF5722] bg-[#FFF4E8] dark:bg-orange-950/40 text-[#FF5722] dark:text-[#FF8A65] ring-2 ring-[#FF5722]/20'
                              : 'border-slate-200 dark:border-slate-700 bg-white dark:bg-[#181311] text-slate-700 dark:text-slate-300'
                          }`}
                        >
                          <span
                            className="w-3.5 h-3.5 rounded-full border border-black/20 shrink-0 shadow-2xs"
                            style={{ backgroundColor: hex }}
                          />
                          <span>{col}</span>
                        </button>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* Size Selection - Only if product has sizes */}
              {parsedSizes.length > 0 && (
                <div className={parsedColors.length > 0 ? "pt-2 border-t border-orange-100/80 dark:border-orange-950/50" : ""}>
                  <div className="flex items-center justify-between mb-1.5">
                    <span className="text-xs font-bold text-slate-800 dark:text-slate-200 flex items-center gap-1.5">
                      <span>Size:</span>
                      <strong className="text-[#FF5722] dark:text-[#FF8A65] font-black">{selectedSize || parsedSizes[0]}</strong>
                    </span>
                    <span className="text-[10px] text-emerald-600 dark:text-emerald-400 font-bold flex items-center gap-1">
                      <Check className="w-3 h-3" />
                      <span>In Stock</span>
                    </span>
                  </div>
                  <div className="flex flex-wrap gap-1.5">
                    {parsedSizes.map((size) => {
                      const isSizeSelected = (selectedSize || parsedSizes[0]).toLowerCase() === size.toLowerCase();
                      return (
                        <button
                          key={size}
                          type="button"
                          onClick={() => {
                            haptics.selection();
                            setSelectedSize(size);
                          }}
                          className={`min-w-[40px] px-3 py-1.5 rounded-xl text-xs font-extrabold transition-all cursor-pointer ${
                            isSizeSelected
                              ? 'bg-[#FF5722] text-white shadow-sm ring-2 ring-[#FF5722]/30 scale-105'
                              : 'bg-[#FFFDF9] dark:bg-[#181311] text-slate-700 dark:text-slate-300 border border-slate-200 dark:border-slate-700 hover:border-[#FF5722]'
                          }`}
                        >
                          {size}
                        </button>
                      );
                    })}
                  </div>
                  {product.fabric && (
                    <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-2 flex items-center gap-1">
                      <Sparkles className="w-3 h-3 text-amber-500" />
                      <span>Fabric: <strong className="text-slate-700 dark:text-slate-300">{product.fabric}</strong></span>
                    </p>
                  )}
                </div>
              )}
            </div>
          )}

          {/* =============================================================== */}
          {/* CASH ON DELIVERY & FREE DELIVERY ASSURANCE                       */}
          {/* =============================================================== */}
          <div className="p-3.5 rounded-2xl bg-white dark:bg-[#211A17] border border-orange-100 dark:border-orange-950/60 space-y-2.5">
            {/* Delivery time */}
            <div className="flex items-center gap-2.5">
              <Truck className="w-4 h-4 text-[#FF5722] shrink-0" />
              <div className="text-xs">
                <span className="font-bold text-slate-900 dark:text-white">
                  Delivery by Tomorrow, 9 PM
                </span>
                <span className="text-emerald-600 dark:text-emerald-400 font-extrabold ml-1.5">
                  FREE
                </span>
              </div>
            </div>

            {/* Inline Pincode check (no window.prompt) */}
            <div className="pt-1.5 border-t border-slate-100 dark:border-white/10 text-xs space-y-2">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-1.5 text-slate-600 dark:text-slate-300">
                  <MapPin className="w-3.5 h-3.5 text-[#FF5722]" />
                  <span>Deliver to: <strong className="text-slate-900 dark:text-white">{pincode}</strong></span>
                </div>
                <button
                  type="button"
                  onClick={() => {
                    haptics.light();
                    setPincodeInput(pincode);
                    setIsEditingPincode(!isEditingPincode);
                  }}
                  className="text-xs font-bold text-[#FF5722] dark:text-[#FF8A65] hover:underline cursor-pointer"
                >
                  {isEditingPincode ? 'Cancel' : 'Change'}
                </button>
              </div>
              {isEditingPincode && (
                <form
                  onSubmit={(e) => {
                    e.preventDefault();
                    const cleaned = pincodeInput.replace(/\D/g, '').slice(0, 6);
                    if (cleaned.length === 6) {
                      setPincode(cleaned);
                      setIsPincodeValid(true);
                      setIsEditingPincode(false);
                      haptics.success();
                    } else {
                      setIsPincodeValid(false);
                      haptics.warning();
                    }
                  }}
                  className="flex items-center gap-2"
                >
                  <input
                    type="text"
                    inputMode="numeric"
                    maxLength={6}
                    value={pincodeInput}
                    onChange={(e) => {
                      setPincodeInput(e.target.value.replace(/\D/g, '').slice(0, 6));
                      setIsPincodeValid(true);
                    }}
                    placeholder="6-digit pincode"
                    className="flex-1 px-2.5 py-1.5 rounded-lg text-xs bg-[#FFF4E8] dark:bg-[#181311] text-slate-900 dark:text-white border border-orange-200 dark:border-orange-900/60 outline-none focus:border-[#FF5722]"
                  />
                  <button
                    type="submit"
                    className="px-3 py-1.5 rounded-lg bg-[#FF5722] hover:bg-[#E64A19] text-white font-bold text-xs cursor-pointer"
                  >
                    Check
                  </button>
                </form>
              )}
              {!isPincodeValid && (
                <p className="text-[10px] font-semibold text-rose-600 dark:text-rose-400">
                  Please enter a valid 6-digit delivery pincode.
                </p>
              )}
            </div>

            {/* Cash on Delivery Banner */}
            {product.codAvailable !== false && (
              <div className="flex items-center gap-2 p-2 rounded-xl bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-900/60">
                <Banknote className="w-4 h-4 text-amber-700 dark:text-amber-400 shrink-0" />
                <div className="flex-1 text-[11px] leading-tight">
                  <span className="font-extrabold text-amber-900 dark:text-amber-300 block">
                    Cash On Delivery Available
                  </span>
                  <span className="text-amber-800/80 dark:text-amber-400/80 text-[10px]">
                    Pay cash at your doorstep with 100% purchase protection.
                  </span>
                </div>
                <Check className="w-4 h-4 text-emerald-600 dark:text-emerald-400 shrink-0" />
              </div>
            )}
          </div>

          {/* =============================================================== */}
          {/* THREE CORE TRUST ASSURANCES                                     */}
          {/* =============================================================== */}
          <div className="grid grid-cols-3 gap-2 text-center">
            <div className="p-2.5 rounded-xl bg-[#FFF4E8]/70 dark:bg-[#211A17] border border-orange-100 dark:border-orange-950/50 flex flex-col items-center">
              <Truck className="w-4 h-4 text-[#FF5722] dark:text-[#FF8A65] mb-1" />
              <span className="text-[10px] font-bold text-slate-800 dark:text-slate-200">
                Free Delivery
              </span>
              <span className="text-[9px] text-slate-500 dark:text-slate-400">Fast doorstep</span>
            </div>

            <div className="p-2.5 rounded-xl bg-[#FFF4E8]/70 dark:bg-[#211A17] border border-orange-100 dark:border-orange-950/50 flex flex-col items-center">
              <RotateCcw className="w-4 h-4 text-[#FF5722] dark:text-[#FF8A65] mb-1" />
              <span className="text-[10px] font-bold text-slate-800 dark:text-slate-200">
                7 Days Return
              </span>
              <span className="text-[9px] text-slate-500 dark:text-slate-400">Easy replacement</span>
            </div>

            <div className="p-2.5 rounded-xl bg-[#FFF4E8]/70 dark:bg-[#211A17] border border-orange-100 dark:border-orange-950/50 flex flex-col items-center">
              <ShieldCheck className="w-4 h-4 text-[#FF5722] dark:text-[#FF8A65] mb-1" />
              <span className="text-[10px] font-bold text-slate-800 dark:text-slate-200">
                Quality Checked
              </span>
              <span className="text-[9px] text-slate-500 dark:text-slate-400">Verified item</span>
            </div>
          </div>

          {/* =============================================================== */}
          {/* =============================================================== */}
          {/* HIGHLIGHTS & PRODUCT DETAILS                                    */}
          {/* =============================================================== */}
          <div className="space-y-2">
            <h3 className="font-display font-bold text-xs uppercase tracking-wider text-slate-500 dark:text-slate-400">
              Product Details &amp; Highlights
            </h3>
            <ul className="space-y-1.5">
              {defaultHighlights.map((hl, idx) => (
                <li
                  key={idx}
                  className="text-xs text-slate-700 dark:text-slate-300 flex items-start gap-2"
                >
                  <span className="w-1.5 h-1.5 rounded-full bg-[#FF5722] dark:bg-[#FF8A65] mt-1.5 shrink-0" />
                  <span>{hl}</span>
                </li>
              ))}
            </ul>
          </div>

          {/* Flipkart-style 2-Column Specifications Table */}
          <div className="p-3.5 rounded-2xl bg-white dark:bg-[#211A17] border border-orange-100 dark:border-orange-950/60 space-y-2">
            <h3 className="font-display font-bold text-xs uppercase tracking-wider text-slate-900 dark:text-white flex items-center justify-between">
              <span>Specifications</span>
              <span className="text-[10px] font-bold text-[#FF5722] dark:text-[#FF8A65]">All details</span>
            </h3>

            <div className="divide-y divide-slate-100 dark:divide-white/10 text-xs">
              <div className="grid grid-cols-2 py-1.5">
                <span className="text-slate-500 dark:text-slate-400">Category</span>
                <span className="font-semibold text-slate-900 dark:text-white">{product.category}</span>
              </div>
              {product.fabric && (
                <div className="grid grid-cols-2 py-1.5">
                  <span className="text-slate-500 dark:text-slate-400">Fabric</span>
                  <span className="font-semibold text-slate-900 dark:text-white">{product.fabric}</span>
                </div>
              )}
              <div className="grid grid-cols-2 py-1.5">
                <span className="text-slate-500 dark:text-slate-400">Country of Origin</span>
                <span className="font-semibold text-slate-900 dark:text-white">India</span>
              </div>
              <div className="grid grid-cols-2 py-1.5">
                <span className="text-slate-500 dark:text-slate-400">Generic Name</span>
                <span className="font-semibold text-slate-900 dark:text-white">{product.name}</span>
              </div>
              <div className="grid grid-cols-2 py-1.5">
                <span className="text-slate-500 dark:text-slate-400">Cash on Delivery</span>
                <span className="font-semibold text-emerald-600 dark:text-emerald-400">
                  {product.codAvailable !== false ? 'Available (COD)' : 'Prepaid Only'}
                </span>
              </div>
              <div className="grid grid-cols-2 py-1.5">
                <span className="text-slate-500 dark:text-slate-400">Seller</span>
                <span className="font-semibold text-slate-900 dark:text-white">{product.seller}</span>
              </div>
            </div>
          </div>

          {/* Full Description */}
          <div className="space-y-1.5 pt-1 border-t border-slate-100 dark:border-white/10">
            <h4 className="font-display font-bold text-xs uppercase tracking-wider text-slate-500 dark:text-slate-400">
              Description
            </h4>
            <p className="text-xs text-slate-700 dark:text-slate-300 leading-relaxed">
              {defaultDescription}
            </p>
          </div>

          {/* Seller Information (Zero fake ratings or genuine badges) */}
          <div className="p-3 rounded-xl bg-[#FFF4E8]/70 dark:bg-[#211A17] border border-orange-100 dark:border-orange-950/60 text-xs flex items-center justify-between">
            <div>
              <span className="text-[10px] text-slate-500 dark:text-slate-400 uppercase font-bold block">
                Sold By
              </span>
              <span className="font-bold text-slate-900 dark:text-white">
                {product.seller}
              </span>
            </div>
            {totalReviewsCount > 0 && (
              <span className="text-[10px] font-extrabold text-white bg-emerald-600 px-1.5 py-0.5 rounded">
                {effectiveRating.toFixed(1)} ★
              </span>
            )}
          </div>

          {/* =============================================================== */}
          {/* REAL CUSTOMER RATINGS & REVIEWS SECTION                          */}
          {/* =============================================================== */}
          <div className="p-3.5 rounded-2xl bg-white dark:bg-[#211A17] border border-orange-100 dark:border-orange-950/60 space-y-3">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="font-display font-black text-xs uppercase tracking-wider text-slate-900 dark:text-white flex items-center gap-1.5">
                  <span>Ratings &amp; Customer Reviews</span>
                </h3>
                <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">
                  {localReviews.length > 0
                    ? `${localReviews.length} verified review${localReviews.length > 1 ? 's' : ''}`
                    : 'Be the first customer to rate this product!'}
                </p>
              </div>

              {/* Rate & Review Button */}
              <button
                type="button"
                onClick={() => {
                  haptics.selection();
                  setShowReviewForm(!showReviewForm);
                }}
                className="px-3 py-1.5 bg-[#FF5722] text-white text-xs font-bold rounded-xl hover:bg-[#E64A19] active:scale-95 transition-all shadow-xs cursor-pointer flex items-center gap-1"
              >
                <Star className="w-3.5 h-3.5 fill-amber-200 text-amber-200" />
                <span>{showReviewForm ? 'Cancel' : 'Rate Product'}</span>
              </button>
            </div>

            {/* Real Ratings Summary (Zero fabricated breakdown percentages) */}
            <div className="p-3 rounded-xl bg-[#FFF4E8]/60 dark:bg-[#181311] border border-orange-100 dark:border-orange-950/50 flex items-center gap-4">
              <div className="text-center shrink-0 pr-3 border-r border-orange-200/70 dark:border-white/10">
                <div className="text-2xl font-black text-slate-900 dark:text-white flex items-center justify-center gap-1">
                  <span>{totalReviewsCount > 0 ? effectiveRating.toFixed(1) : '—'}</span>
                  <Star className={`w-4 h-4 ${totalReviewsCount > 0 ? 'fill-emerald-600 text-emerald-600' : 'text-slate-400'}`} />
                </div>
                <span className="text-[10px] text-slate-500 dark:text-slate-400 block mt-0.5">
                  {totalReviewsCount > 0 ? `${totalReviewsCount.toLocaleString('en-IN')} Ratings` : '0 Ratings'}
                </span>
              </div>

              {localReviews.length > 0 ? (
                <div className="flex-1 space-y-1 text-[10px]">
                  {[5, 4, 3, 2, 1].map((starLevel) => {
                    const count = localReviews.filter((r) => Math.round(r.rating) === starLevel).length;
                    const pct = Math.round((count / localReviews.length) * 100);
                    const barColor =
                      starLevel >= 4
                        ? 'bg-emerald-600'
                        : starLevel === 3
                        ? 'bg-amber-500'
                        : starLevel === 2
                        ? 'bg-orange-500'
                        : 'bg-rose-500';
                    return (
                      <div key={starLevel} className="flex items-center gap-2">
                        <span className="w-4 font-bold text-slate-600 dark:text-slate-400">{starLevel}★</span>
                        <div className="flex-1 h-1.5 bg-slate-200 dark:bg-white/10 rounded-full overflow-hidden">
                          <div className={`h-full ${barColor} rounded-full`} style={{ width: `${pct}%` }} />
                        </div>
                        <span className="w-7 text-right font-medium text-slate-500 dark:text-slate-400">{pct}%</span>
                      </div>
                    );
                  })}
                </div>
              ) : (
                <div className="flex-1 text-[11px] text-slate-600 dark:text-slate-300 leading-snug">
                  {totalReviewsCount > 0
                    ? `Rated ${effectiveRating.toFixed(1)} ★ across ${totalReviewsCount.toLocaleString('en-IN')} verified orders. Write a review to add yours.`
                    : 'No customer ratings yet — tap "Rate Product" to add the first verified rating.'}
                </div>
              )}
            </div>

            {/* Review Success Confirmation Banner */}
            {reviewSuccess && (
              <div className="p-2.5 bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-800 rounded-xl text-xs text-emerald-800 dark:text-emerald-300 flex items-center gap-2 animate-in fade-in">
                <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                <span>Thank you! Your verified customer review has been added.</span>
              </div>
            )}

            {/* Interactive Rating & Review Submission Form */}
            {showReviewForm && (
              <form
                onSubmit={handleSubmitReview}
                className="p-3 bg-slate-50 dark:bg-slate-900/60 rounded-xl border border-slate-200 dark:border-slate-700 space-y-2.5 animate-in fade-in"
              >
                <div className="flex items-center justify-between">
                  <span className="text-xs font-extrabold text-slate-800 dark:text-slate-200">
                    Your Rating:
                  </span>
                  {/* Rating Label */}
                  <span className="text-xs font-bold text-amber-600 dark:text-amber-400">
                    {reviewRating === 5
                      ? '5 ★ Outstanding'
                      : reviewRating === 4
                      ? '4 ★ Very Good'
                      : reviewRating === 3
                      ? '3 ★ Good'
                      : reviewRating === 2
                      ? '2 ★ Fair'
                      : '1 ★ Poor'}
                  </span>
                </div>

                {/* 5 Interactive Clickable Stars */}
                <div className="flex items-center gap-2 py-0.5">
                  {[1, 2, 3, 4, 5].map((starVal) => (
                    <button
                      key={starVal}
                      type="button"
                      onClick={() => {
                        haptics.impact();
                        setReviewRating(starVal);
                      }}
                      className="p-1 hover:scale-125 transition-all cursor-pointer"
                    >
                      <Star
                        className={`w-6 h-6 transition-colors ${
                          starVal <= reviewRating
                            ? 'text-amber-400 fill-amber-400 drop-shadow-xs'
                            : 'text-slate-300 dark:text-slate-600'
                        }`}
                      />
                    </button>
                  ))}
                </div>

                {/* Reviewer Name */}
                <div>
                  <label className="text-[10px] font-bold text-slate-600 dark:text-slate-400 block mb-1">
                    Your Name
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="Enter your name"
                    value={reviewerName}
                    onChange={(e) => setReviewerName(e.target.value)}
                    className="w-full px-2.5 py-1.5 text-xs bg-white dark:bg-[#211A17] text-slate-900 dark:text-white border border-slate-200 dark:border-white/10 rounded-lg outline-none focus:border-[#FF5722]"
                  />
                </div>

                {/* Review Comment */}
                <div>
                  <label className="text-[10px] font-bold text-slate-600 dark:text-slate-400 block mb-1">
                    Your Review &amp; Experience
                  </label>
                  <textarea
                    rows={2}
                    required
                    placeholder="Describe product quality, fitting, fabric or packaging..."
                    value={reviewComment}
                    onChange={(e) => setReviewComment(e.target.value)}
                    className="w-full px-2.5 py-1.5 text-xs bg-white dark:bg-[#211A17] text-slate-900 dark:text-white border border-slate-200 dark:border-white/10 rounded-lg outline-none focus:border-[#FF5722]"
                  />
                </div>

                {/* Submit Review Button */}
                <button
                  type="submit"
                  className="w-full py-2 bg-gradient-to-r from-[#FF7043] to-[#FF5722] text-white font-extrabold text-xs rounded-lg shadow-sm hover:opacity-95 active:scale-95 transition-all cursor-pointer"
                >
                  Submit Customer Review
                </button>
              </form>
            )}

            {/* Real Review Cards List */}
            <div className="space-y-2 pt-1">
              {localReviews.length > 0 ? (
                localReviews.map((rev) => (
                  <div
                    key={rev.id}
                    className="p-2.5 rounded-xl bg-slate-50 dark:bg-slate-900/40 border border-slate-100 dark:border-slate-800 text-xs space-y-1"
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-1.5">
                        <span className="font-bold text-slate-900 dark:text-white">
                          {rev.userName}
                        </span>
                        {rev.verifiedBuyer && (
                          <span className="text-[9px] bg-emerald-100 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 font-bold px-1.5 py-0.2 rounded-full flex items-center gap-0.5">
                            ✓ Verified Buyer
                          </span>
                        )}
                      </div>
                      <span className="text-[10px] text-slate-400">{rev.date}</span>
                    </div>

                    <div className="flex items-center gap-1.5">
                      <div className="flex items-center bg-emerald-600 text-white text-[10px] font-black px-1.5 py-0.2 rounded">
                        <span>{rev.rating}</span>
                        <span className="ml-0.5">★</span>
                      </div>
                      {(rev.size || rev.color) && (
                        <span className="text-[10px] text-slate-400">
                          {rev.size ? `Size: ${rev.size}` : ''}
                          {rev.size && rev.color ? ' • ' : ''}
                          {rev.color ? `Colour: ${rev.color}` : ''}
                        </span>
                      )}
                    </div>

                    <p className="text-slate-700 dark:text-slate-300 text-[11px] leading-relaxed pt-0.5">
                      {rev.comment}
                    </p>
                  </div>
                ))
              ) : (
                <div className="p-3 text-center bg-slate-50 dark:bg-slate-900/30 rounded-xl border border-dashed border-slate-200 dark:border-slate-800 text-xs text-slate-500">
                  <span>No customer reviews written yet. Tap "Rate Product" above to be the first!</span>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* ================================================================= */}
        {/* 3. STICKY BOTTOM ACTIONS BAR                                      */}
        {/* ================================================================= */}
        <div className="sticky bottom-0 z-30 p-3 bg-white/98 dark:bg-[#181311]/98 backdrop-blur-md border-t border-orange-100 dark:border-white/10 flex items-center gap-3 shadow-xl">
          {/* Add to Cart Button (White with orange border) */}
          <button
            type="button"
            onClick={() => {
              haptics.success();
              onAddToCart(product, selectedQty);
            }}
            className="flex-1 py-3 px-3 rounded-xl bg-white dark:bg-[#211A17] hover:bg-orange-50/60 dark:hover:bg-white/5 text-[#FF5722] font-black text-xs sm:text-sm flex items-center justify-center gap-2 border-2 border-[#FF5722] active:scale-95 transition-all shadow-xs cursor-pointer"
          >
            <ShoppingCart className="w-4 h-4 text-[#FF5722] stroke-[2.2]" />
            <span>Add to Cart</span>
          </button>

          {/* Buy Now Button (Solid orange with zap icon) */}
          <button
            type="button"
            onClick={() => {
              haptics.impact();
              setIsCheckoutOpen(true);
            }}
            className="flex-1 py-3 px-3 rounded-xl bg-[#FF5722] hover:bg-[#E64A19] text-white font-black text-xs sm:text-sm flex items-center justify-center gap-1.5 active:scale-95 transition-all shadow-md cursor-pointer"
          >
            <Zap className="w-4 h-4 fill-white text-white" />
            <span>Buy Now</span>
          </button>
        </div>

        {/* ================================================================= */}
        {/* 4. REAL CASH ON DELIVERY (COD) FAST CHECKOUT DRAWER               */}
        {/* ================================================================= */}
        {isCheckoutOpen && (
          <div className="absolute inset-0 z-40 bg-[#FFFDF9] dark:bg-[#181311] flex flex-col animate-in slide-in-from-bottom duration-200 overflow-hidden">
            {/* Checkout Header */}
            <div className="flex items-center justify-between px-4 pb-3 pt-[max(0.75rem,env(safe-area-inset-top))] bg-gradient-to-b from-[#FFE5CC] via-[#FFF4E8] to-white dark:from-[#26160C] dark:via-[#1B120E] dark:to-[#181311] border-b border-orange-100 dark:border-white/10">
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setIsCheckoutOpen(false)}
                  className="p-1.5 hover:bg-black/5 dark:hover:bg-white/10 rounded-full cursor-pointer text-slate-900 dark:text-white"
                >
                  <ArrowLeft className="w-5 h-5" />
                </button>
                <h3 className="font-bold text-sm text-slate-900 dark:text-white">Order Checkout</h3>
              </div>
              <span className="text-[11px] bg-[#FF5722]/15 text-[#FF5722] dark:text-[#FF8A65] px-2.5 py-0.5 rounded-full font-bold">
                100% Safe
              </span>
            </div>

            {/* Checkout Body */}
            <div className="flex-1 overflow-y-auto p-4 space-y-3.5 no-scrollbar">
              {/* Delivery Address */}
              <div className="p-3 rounded-2xl bg-white dark:bg-[#211A17] border border-orange-100 dark:border-white/10">
                <div className="flex items-center justify-between mb-1">
                  <span className="text-[11px] font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider flex items-center gap-1">
                    <MapPin className="w-3.5 h-3.5 text-[#FF5722]" />
                    <span>Deliver to</span>
                  </span>
                  <button
                    type="button"
                    onClick={() => {
                      if (isEditingAddress && onUpdateAddress && deliveryAddress.trim()) {
                        onUpdateAddress(deliveryAddress.trim());
                      }
                      setIsEditingAddress(!isEditingAddress);
                    }}
                    className="text-xs font-bold text-[#FF5722] dark:text-[#FF8A65] hover:underline cursor-pointer"
                  >
                    {isEditingAddress ? 'Save' : 'Change'}
                  </button>
                </div>
                {isEditingAddress ? (
                  <textarea
                    rows={2}
                    value={deliveryAddress}
                    onChange={(e) => setDeliveryAddress(e.target.value)}
                    className="w-full text-xs p-2 rounded-xl border border-orange-200 dark:border-white/15 bg-[#FFF4E8]/50 dark:bg-[#181311] text-slate-900 dark:text-white outline-none focus:border-[#FF5722]"
                  />
                ) : (
                  <p className="text-xs font-bold text-slate-800 dark:text-slate-200">
                    {deliveryAddress}
                  </p>
                )}
              </div>

              {/* Product preview & quantity selector */}
              <div className="p-3 rounded-2xl bg-white dark:bg-[#211A17] border border-orange-100 dark:border-white/10 flex items-center gap-3">
                <img
                  src={product.image}
                  alt={product.name}
                  className="w-14 h-14 rounded-xl object-cover bg-white"
                />
                <div className="flex-1 min-w-0">
                  <h4 className="text-xs font-bold text-slate-900 dark:text-white truncate">
                    {product.name}
                  </h4>
                  <div className="flex items-baseline gap-2 mt-1">
                    <span className="text-sm font-black text-slate-900 dark:text-white">
                      ₹{totalPrice.toLocaleString('en-IN')}
                    </span>
                    <span className="text-[10px] text-emerald-600 font-bold">
                      {product.discount}
                    </span>
                  </div>
                </div>

                {/* Qty controller */}
                <div className="flex items-center gap-2 bg-[#FFF4E8] dark:bg-[#181311] rounded-lg p-1 border border-orange-200/70 dark:border-white/10">
                  <button
                    type="button"
                    onClick={() => {
                      haptics.light();
                      setSelectedQty((prev) => Math.max(1, prev - 1));
                    }}
                    className="w-6 h-6 rounded flex items-center justify-center text-slate-700 dark:text-slate-300 hover:bg-white dark:hover:bg-white/10 cursor-pointer"
                  >
                    <Minus className="w-3 h-3" />
                  </button>
                  <span className="text-xs font-bold text-slate-900 dark:text-white min-w-[14px] text-center">
                    {selectedQty}
                  </span>
                  <button
                    type="button"
                    onClick={() => {
                      haptics.light();
                      setSelectedQty((prev) => prev + 1);
                    }}
                    className="w-6 h-6 rounded flex items-center justify-center text-slate-700 dark:text-slate-300 hover:bg-white dark:hover:bg-white/10 cursor-pointer"
                  >
                    <Plus className="w-3 h-3" />
                  </button>
                </div>
              </div>

              {/* PAYMENT METHOD SELECTION */}
              <div className="space-y-2">
                <span className="text-[11px] font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider block">
                  Select Payment Option
                </span>

                {/* Option 1: Cash On Delivery */}
                <div
                  onClick={() => {
                    haptics.selection();
                    setPaymentMethod('COD');
                  }}
                  className={`p-3 rounded-2xl border-2 transition-all cursor-pointer flex items-center justify-between ${
                    paymentMethod === 'COD'
                      ? 'border-[#FF5722] bg-[#FFF4E8] dark:bg-[#2E1A12] shadow-xs'
                      : 'border-slate-200 dark:border-white/10 bg-white dark:bg-[#211A17]'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-full bg-emerald-600 text-white flex items-center justify-center font-black text-sm">
                      ₹
                    </div>
                    <div>
                      <div className="flex items-center gap-1.5">
                        <span className="text-xs font-black text-slate-900 dark:text-white">
                          Cash on Delivery (COD)
                        </span>
                        <span className="text-[9px] bg-emerald-600 text-white px-1.5 py-0.2 rounded font-black">
                          Popular
                        </span>
                      </div>
                      <p className="text-[10px] text-slate-500 dark:text-slate-400 mt-0.5">
                        Pay cash directly to delivery executive at your doorstep
                      </p>
                    </div>
                  </div>
                  <div
                    className={`w-4 h-4 rounded-full border-2 flex items-center justify-center ${
                      paymentMethod === 'COD'
                        ? 'border-[#FF5722] bg-[#FF5722]'
                        : 'border-slate-300 dark:border-slate-600'
                    }`}
                  >
                    {paymentMethod === 'COD' && <Check className="w-3 h-3 text-white" />}
                  </div>
                </div>

                {/* Option 2: UPI */}
                <div
                  onClick={() => {
                    haptics.selection();
                    setPaymentMethod('UPI');
                  }}
                  className={`p-3 rounded-2xl border-2 transition-all cursor-pointer flex items-center justify-between ${
                    paymentMethod === 'UPI'
                      ? 'border-[#FF5722] bg-[#FFF4E8] dark:bg-[#2E1A12] shadow-xs'
                      : 'border-slate-200 dark:border-white/10 bg-white dark:bg-[#211A17]'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-full bg-[#FF5722] text-white flex items-center justify-center font-black text-xs">
                      UPI
                    </div>
                    <div>
                      <span className="text-xs font-bold text-slate-900 dark:text-white block">
                        UPI (GPay / PhonePe / Paytm)
                      </span>
                      <p className="text-[10px] text-slate-500 dark:text-slate-400">
                        Instant scan &amp; pay
                      </p>
                    </div>
                  </div>
                  <div
                    className={`w-4 h-4 rounded-full border-2 flex items-center justify-center ${
                      paymentMethod === 'UPI'
                        ? 'border-[#FF5722] bg-[#FF5722]'
                        : 'border-slate-300 dark:border-slate-600'
                    }`}
                  >
                    {paymentMethod === 'UPI' && <Check className="w-3 h-3 text-white" />}
                  </div>
                </div>
              </div>

              {/* Price Details */}
              <div className="p-3.5 rounded-2xl bg-white dark:bg-[#211A17] border border-orange-100 dark:border-white/10 space-y-2 text-xs">
                <span className="font-bold text-slate-900 dark:text-white block pb-1 border-b border-slate-100 dark:border-white/10">
                  Price Details ({selectedQty} {selectedQty === 1 ? 'item' : 'items'})
                </span>
                <div className="flex justify-between text-slate-600 dark:text-slate-400">
                  <span>Total MRP</span>
                  <span>₹{totalMRP.toLocaleString('en-IN')}</span>
                </div>
                <div className="flex justify-between text-emerald-600 font-semibold">
                  <span>Product Discount</span>
                  <span>-₹{(totalMRP - totalPrice).toLocaleString('en-IN')}</span>
                </div>
                <div className="flex justify-between text-slate-600 dark:text-slate-400">
                  <span>Delivery Charges</span>
                  <span className="text-emerald-600 font-bold">FREE</span>
                </div>
                <div className="flex justify-between text-slate-600 dark:text-slate-400">
                  <span>COD Handling Fee</span>
                  <span className="text-emerald-600 font-bold">FREE</span>
                </div>
                <div className="flex justify-between font-black text-sm text-slate-900 dark:text-white pt-2 border-t border-slate-100 dark:border-white/10">
                  <span>Total Payable</span>
                  <span className="text-[#FF5722] dark:text-[#FF8A65]">
                    ₹{totalPrice.toLocaleString('en-IN')}
                  </span>
                </div>
              </div>
            </div>

            {/* Confirm COD Order Button */}
            <div className="p-3.5 bg-white dark:bg-[#181311] border-t border-orange-100 dark:border-white/10">
              <button
                type="button"
                onClick={handleConfirmOrder}
                className="w-full py-3.5 rounded-xl bg-[#FF5722] hover:bg-[#E64A19] text-white font-extrabold text-xs flex items-center justify-center gap-2 active:scale-95 transition-all shadow-md cursor-pointer"
              >
                <CheckCircle2 className="w-4 h-4 text-emerald-200" />
                <span>
                  {paymentMethod === 'COD'
                    ? `Confirm Order with Cash on Delivery (₹${totalPrice.toLocaleString('en-IN')})`
                    : `Pay ₹${totalPrice.toLocaleString('en-IN')} & Place Order`}
                </span>
              </button>
            </div>
          </div>
        )}

        {/* ================================================================= */}
        {/* 5. ORDER PLACED SUCCESS CELEBRATION MODAL                         */}
        {/* ================================================================= */}
        {orderConfirmed && (
          <div className="absolute inset-0 z-50 bg-[#FFFDF9] dark:bg-[#181311] flex flex-col items-center justify-center p-6 text-center animate-in zoom-in-95 duration-200">
            <div className="w-16 h-16 rounded-full bg-emerald-100 dark:bg-emerald-950/60 text-emerald-600 flex items-center justify-center mb-3 animate-bounce">
              <CheckCircle2 className="w-10 h-10 stroke-[2.5]" />
            </div>

            <span className="text-[10px] font-black uppercase tracking-wider text-emerald-600 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/80 px-2.5 py-0.5 rounded-full mb-1">
              {orderConfirmed.paymentMethod.includes('Cash') ? 'Cash On Delivery Confirmed' : 'Payment Confirmed'}
            </span>

            <h3 className="font-display font-black text-lg text-slate-900 dark:text-white mb-1">
              Order Placed Successfully!
            </h3>

            <p className="text-xs text-slate-500 dark:text-slate-400 mb-4 max-w-[280px]">
              {orderConfirmed.paymentMethod.includes('Cash')
                ? `Please pay ₹${orderConfirmed.total.toLocaleString('en-IN')} in cash upon delivery tomorrow by 9:00 PM.`
                : 'Thank you for shopping on AARVO. Your item has been sent for dispatch.'}
            </p>

            <div className="w-full max-w-[320px] p-3 rounded-2xl bg-white dark:bg-[#211A17] border border-orange-100 dark:border-white/10 mb-5 text-left text-xs space-y-1.5">
              <div className="flex justify-between">
                <span className="text-slate-500 dark:text-slate-400">Order ID:</span>
                <span className="font-bold text-slate-900 dark:text-white">{orderConfirmed.id}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500 dark:text-slate-400">Amount Due:</span>
                <span className="font-black text-emerald-600">₹{orderConfirmed.total.toLocaleString('en-IN')}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500 dark:text-slate-400">Estimated Delivery:</span>
                <span className="font-semibold text-slate-800 dark:text-slate-200">{orderConfirmed.estimatedDelivery}</span>
              </div>
            </div>

            <div className="w-full max-w-[320px] space-y-2">
              <button
                type="button"
                onClick={() => {
                  haptics.selection();
                  setOrderConfirmed(null);
                  onClose();
                  if (onOpenOrders) {
                    onOpenOrders();
                  }
                }}
                className="w-full py-3 bg-[#FF5722] hover:bg-[#E64A19] text-white font-bold text-xs rounded-xl shadow-md active:scale-95 transition-all cursor-pointer"
              >
                View in Orders &amp; Delivery
              </button>
              <button
                type="button"
                onClick={() => {
                  haptics.light();
                  setOrderConfirmed(null);
                  onClose();
                }}
                className="w-full py-2.5 bg-orange-50 hover:bg-orange-100 dark:bg-[#211A17] dark:hover:bg-white/10 text-slate-800 dark:text-slate-200 font-bold text-xs rounded-xl active:scale-95 transition-all cursor-pointer"
              >
                Continue Shopping
              </button>
            </div>
          </div>
        )}
      </div>

      {/* 360° AARVO Full-Screen Product Viewer Modal */}
      <Product360ViewerModal
        product={product}
        isOpen={is360Open}
        onClose={() => setIs360Open(false)}
        onAddToCart={onAddToCart}
        onBuyNow={onBuyNow}
      />

      {/* Full-Screen Image Zoom Lightbox Modal */}
      <ProductImageZoomModal
        product={product}
        isOpen={isZoomOpen}
        onClose={() => setIsZoomOpen(false)}
        onAddToCart={onAddToCart}
        onBuyNow={onBuyNow}
        onOpen360={() => {
          setIsZoomOpen(false);
          setIs360Open(true);
        }}
      />
    </div>
  );
};
