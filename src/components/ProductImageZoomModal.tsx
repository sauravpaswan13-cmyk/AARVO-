import React, { useState, useRef, useEffect, useCallback } from 'react';
import {
  X,
  ZoomIn,
  ZoomOut,
  Maximize2,
  RotateCcw,
  ShoppingCart,
  Zap,
  ChevronLeft,
  ChevronRight,
  Share2,
  Check,
} from 'lucide-react';
import { Product } from './ProductDetailModal';
import { haptics } from '../utils/haptics';

interface ProductImageZoomModalProps {
  product: Product | null;
  isOpen: boolean;
  onClose: () => void;
  onAddToCart?: (product: Product) => void;
  onBuyNow?: (product: Product) => void;
  onOpen360?: (product: Product) => void;
}

export const ProductImageZoomModal: React.FC<ProductImageZoomModalProps> = ({
  product,
  isOpen,
  onClose,
  onAddToCart,
  onBuyNow,
  onOpen360,
}) => {
  // Gallery angles (main image plus detail views)
  const [activeImageIndex, setActiveImageIndex] = useState(0);
  const [copiedShare, setCopiedShare] = useState(false);

  // Zoom & Pan transformation states
  const [scale, setScale] = useState(1);
  const [position, setPosition] = useState({ x: 0, y: 0 });
  const [isDragging, setIsDragging] = useState(false);

  // Drag-to-dismiss state (when at scale 1)
  const [dismissOffsetY, setDismissOffsetY] = useState(0);
  const [isDismissing, setIsDismissing] = useState(false);

  // References for touch calculation
  const containerRef = useRef<HTMLDivElement>(null);
  const imageRef = useRef<HTMLImageElement>(null);

  const touchStartRef = useRef<{
    time: number;
    x: number;
    y: number;
    initialDistance: number;
    initialScale: number;
    initialPos: { x: number; y: number };
  }>({
    time: 0,
    x: 0,
    y: 0,
    initialDistance: 0,
    initialScale: 1,
    initialPos: { x: 0, y: 0 },
  });

  const lastTapRef = useRef<number>(0);

  // Gallery items for the product
  const galleryImages = product
    ? [
        { label: 'Front View', url: product.image },
        { label: 'Angle View', url: product.image },
        { label: 'Close-Up Detail', url: product.image },
        { label: 'Dimension & Scale', url: product.image },
      ]
    : [];

  // Reset zoom & positions when image changes or modal opens
  const resetTransform = useCallback(() => {
    setScale(1);
    setPosition({ x: 0, y: 0 });
    setDismissOffsetY(0);
    setIsDismissing(false);
  }, []);

  useEffect(() => {
    if (isOpen) {
      resetTransform();
      setActiveImageIndex(0);
      setCopiedShare(false);
      // Prevent body scrolling
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }
    return () => {
      document.body.style.overflow = '';
    };
  }, [isOpen, resetTransform]);

  // Keyboard navigation
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      } else if (e.key === 'ArrowRight') {
        setActiveImageIndex((prev) => (prev + 1) % galleryImages.length);
        resetTransform();
      } else if (e.key === 'ArrowLeft') {
        setActiveImageIndex((prev) => (prev - 1 + galleryImages.length) % galleryImages.length);
        resetTransform();
      } else if (e.key === '+' || e.key === '=') {
        zoomIn();
      } else if (e.key === '-' || e.key === '_') {
        zoomOut();
      } else if (e.key === '0') {
        resetTransform();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, galleryImages.length, onClose, resetTransform]);

  if (!isOpen || !product) return null;

  // Zoom manipulation helpers
  const zoomIn = () => {
    haptics.selection();
    setScale((prev) => Math.min(prev + 0.75, 4));
  };

  const zoomOut = () => {
    haptics.selection();
    setScale((prev) => {
      const next = Math.max(prev - 0.75, 1);
      if (next === 1) {
        setPosition({ x: 0, y: 0 });
      }
      return next;
    });
  };

  const handleDoubleTap = (clientX: number, clientY: number) => {
    haptics.impact();
    if (scale > 1.2) {
      // Reset back to 1x
      resetTransform();
    } else {
      // Zoom in to 2.5x centered toward tap point
      const container = containerRef.current?.getBoundingClientRect();
      if (container) {
        const originX = clientX - container.left - container.width / 2;
        const originY = clientY - container.top - container.height / 2;
        setPosition({
          x: -originX * 1.2,
          y: -originY * 1.2,
        });
      }
      setScale(2.5);
    }
  };

  // Touch Handlers for Pinch-to-zoom and Pan
  const handleTouchStart = (e: React.TouchEvent) => {
    if (e.touches.length === 2) {
      // Two finger pinch start
      const t1 = e.touches[0];
      const t2 = e.touches[1];
      const dist = Math.hypot(t1.clientX - t2.clientX, t1.clientY - t2.clientY);
      touchStartRef.current = {
        time: Date.now(),
        x: (t1.clientX + t2.clientX) / 2,
        y: (t1.clientY + t2.clientY) / 2,
        initialDistance: dist,
        initialScale: scale,
        initialPos: { ...position },
      };
      setIsDragging(true);
    } else if (e.touches.length === 1) {
      const now = Date.now();
      const t = e.touches[0];

      // Double-tap detection
      if (now - lastTapRef.current < 300) {
        handleDoubleTap(t.clientX, t.clientY);
        lastTapRef.current = 0;
        return;
      }
      lastTapRef.current = now;

      touchStartRef.current = {
        time: now,
        x: t.clientX,
        y: t.clientY,
        initialDistance: 0,
        initialScale: scale,
        initialPos: { ...position },
      };
      setIsDragging(true);
    }
  };

  const handleTouchMove = (e: React.TouchEvent) => {
    if (!isDragging) return;

    if (e.touches.length === 2 && touchStartRef.current.initialDistance > 0) {
      // Pinch to Zoom
      e.preventDefault();
      const t1 = e.touches[0];
      const t2 = e.touches[1];
      const dist = Math.hypot(t1.clientX - t2.clientX, t1.clientY - t2.clientY);
      const ratio = dist / touchStartRef.current.initialDistance;
      const newScale = Math.min(Math.max(touchStartRef.current.initialScale * ratio, 0.9), 4.5);
      setScale(newScale);
    } else if (e.touches.length === 1) {
      const t = e.touches[0];
      const deltaX = t.clientX - touchStartRef.current.x;
      const deltaY = t.clientY - touchStartRef.current.y;

      if (scale > 1.05) {
        // Panning when zoomed in
        e.preventDefault();
        const maxPanX = (scale - 1) * 220;
        const maxPanY = (scale - 1) * 280;
        setPosition({
          x: Math.min(Math.max(touchStartRef.current.initialPos.x + deltaX, -maxPanX), maxPanX),
          y: Math.min(Math.max(touchStartRef.current.initialPos.y + deltaY, -maxPanY), maxPanY),
        });
      } else {
        // Swipe down to dismiss gesture (when at 1x)
        if (deltaY > 0) {
          setDismissOffsetY(deltaY);
        }
      }
    }
  };

  const handleTouchEnd = () => {
    setIsDragging(false);

    // If swipe down to dismiss was dragged sufficiently (> 120px)
    if (dismissOffsetY > 120) {
      setIsDismissing(true);
      haptics.light();
      setTimeout(onClose, 180);
      return;
    } else {
      setDismissOffsetY(0);
    }

    // Reset if zoom was pinched smaller than 1x
    if (scale < 1.05) {
      resetTransform();
    }
  };

  // Mouse Wheel Zoom for Desktop
  const handleWheel = (e: React.WheelEvent) => {
    e.preventDefault();
    const zoomDelta = e.deltaY < 0 ? 0.3 : -0.3;
    setScale((prev) => {
      const next = Math.min(Math.max(prev + zoomDelta, 1), 4);
      if (next === 1) {
        setPosition({ x: 0, y: 0 });
      }
      return next;
    });
  };

  // Desktop Mouse Drag
  const handleMouseDown = (e: React.MouseEvent) => {
    if (scale <= 1) return;
    setIsDragging(true);
    touchStartRef.current = {
      time: Date.now(),
      x: e.clientX,
      y: e.clientY,
      initialDistance: 0,
      initialScale: scale,
      initialPos: { ...position },
    };
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    if (!isDragging || scale <= 1) return;
    const deltaX = e.clientX - touchStartRef.current.x;
    const deltaY = e.clientY - touchStartRef.current.y;
    const maxPanX = (scale - 1) * 220;
    const maxPanY = (scale - 1) * 280;
    setPosition({
      x: Math.min(Math.max(touchStartRef.current.initialPos.x + deltaX, -maxPanX), maxPanX),
      y: Math.min(Math.max(touchStartRef.current.initialPos.y + deltaY, -maxPanY), maxPanY),
    });
  };

  const handleMouseUp = () => {
    setIsDragging(false);
  };

  const handleShare = async () => {
    haptics.light();
    try {
      if (navigator.share) {
        await navigator.share({
          title: product.name,
          text: `Check out ${product.name} on AARVO Shopping!`,
          url: window.location.href,
        });
      } else {
        await navigator.clipboard.writeText(window.location.href);
        setCopiedShare(true);
        setTimeout(() => setCopiedShare(false), 2000);
      }
    } catch {
      // ignored
    }
  };

  // Calculate dismiss opacity based on drag down
  const backdropOpacity = Math.max(0.4, 1 - dismissOffsetY / 400);

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="image-zoom-title"
      style={{
        backgroundColor: `rgba(18, 14, 12, ${backdropOpacity})`,
      }}
      className={`fixed inset-0 z-[110] flex flex-col justify-between overflow-hidden select-none transition-opacity duration-200 ${
        isDismissing ? 'opacity-0 scale-95' : 'opacity-100 scale-100'
      }`}
    >
      {/* =================================================================== */}
      {/* TOP FLIPKART-STYLE APP BAR                                          */}
      {/* =================================================================== */}
      <div className="relative z-30 flex items-center justify-between px-3 sm:px-5 pt-[max(0.75rem,env(safe-area-inset-top))] pb-3 bg-gradient-to-b from-[#181311]/95 via-[#181311]/70 to-transparent backdrop-blur-xs">
        <div className="flex items-center gap-2.5 min-w-0">
          <button
            type="button"
            onClick={() => {
              haptics.light();
              onClose();
            }}
            aria-label="Close full-screen image zoom"
            className="w-10 h-10 rounded-full bg-white/10 hover:bg-white/20 active:scale-90 text-white flex items-center justify-center transition-all cursor-pointer backdrop-blur-md border border-white/10"
          >
            <X className="w-5 h-5" />
          </button>
          <div className="min-w-0">
            <h2
              id="image-zoom-title"
              className="text-xs sm:text-sm font-bold text-white truncate max-w-[180px] sm:max-w-md"
            >
              {product.name}
            </h2>
            <div className="flex items-center gap-2 text-[10px] text-slate-400">
              <span className="uppercase font-semibold tracking-wider text-[#FF8A65]">
                {product.category}
              </span>
              <span>&bull;</span>
              <span>{Math.round(scale * 100)}% Zoom</span>
              {scale > 1 && (
                <span className="text-emerald-400 font-bold animate-pulse">&bull; Pan active</span>
              )}
            </div>
          </div>
        </div>

        {/* Action icons (360° Switch, Share, Reset) */}
        <div className="flex items-center gap-2 shrink-0">
          {onOpen360 && (
            <button
              type="button"
              onClick={() => {
                haptics.selection();
                onClose();
                onOpen360(product);
              }}
              className="px-2.5 py-1.5 rounded-full bg-gradient-to-r from-[#FF5722] to-[#E64A19] hover:from-[#F4511E] hover:to-[#D84315] text-white text-xs font-black flex items-center gap-1.5 shadow-md active:scale-95 transition-all cursor-pointer"
              title="Open 360° Interactive View"
            >
              <RotateCcw className="w-3.5 h-3.5 animate-spin-slow" />
              <span className="hidden sm:inline">AARVO</span>
              <span>360°</span>
            </button>
          )}

          <button
            type="button"
            onClick={handleShare}
            aria-label="Share product"
            className="w-9 h-9 rounded-full bg-white/10 hover:bg-white/20 active:scale-90 text-white flex items-center justify-center transition-all cursor-pointer"
            title="Share Product"
          >
            {copiedShare ? (
              <Check className="w-4 h-4 text-emerald-400" />
            ) : (
              <Share2 className="w-4 h-4" />
            )}
          </button>
        </div>
      </div>

      {/* =================================================================== */}
      {/* MAIN INTERACTIVE ZOOM CANVAS (Pinch, Drag, Wheel, Double-Tap)       */}
      {/* =================================================================== */}
      <div
        ref={containerRef}
        onTouchStart={handleTouchStart}
        onTouchMove={handleTouchMove}
        onTouchEnd={handleTouchEnd}
        onWheel={handleWheel}
        onMouseDown={handleMouseDown}
        onMouseMove={handleMouseMove}
        onMouseUp={handleMouseUp}
        onMouseLeave={handleMouseUp}
        style={{
          transform: `translateY(${dismissOffsetY}px)`,
          transition: isDragging ? 'none' : 'transform 0.2s ease-out',
        }}
        className={`relative flex-1 flex items-center justify-center overflow-hidden touch-none ${
          scale > 1 ? 'cursor-grab active:cursor-grabbing' : 'cursor-zoom-in'
        }`}
      >
        {/* Animated Expanding Hero Image */}
        <div
          style={{
            transform: `translate3d(${position.x}px, ${position.y}px, 0px) scale(${scale})`,
            transition: isDragging ? 'none' : 'transform 0.25s cubic-bezier(0.2, 0.8, 0.2, 1)',
          }}
          className="relative max-w-full max-h-full flex items-center justify-center will-change-transform"
        >
          <img
            ref={imageRef}
            src={galleryImages[activeImageIndex]?.url || product.image}
            alt={`${product.name} - ${galleryImages[activeImageIndex]?.label || 'Detail view'}`}
            draggable={false}
            className="max-h-[75vh] max-w-[92vw] sm:max-w-[85vw] object-contain drop-shadow-2xl select-none pointer-events-auto rounded-xl animate-in zoom-in-95 duration-200"
          />
        </div>

        {/* Flipkart-style Quick Angle Navigation Arrows */}
        {galleryImages.length > 1 && scale === 1 && (
          <>
            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation();
                haptics.light();
                setActiveImageIndex((prev) => (prev - 1 + galleryImages.length) % galleryImages.length);
                resetTransform();
              }}
              aria-label="Previous image"
              className="absolute left-3 top-1/2 -translate-y-1/2 w-10 h-10 rounded-full bg-slate-900/70 hover:bg-slate-900 border border-white/10 text-white flex items-center justify-center shadow-lg active:scale-90 transition-all z-20 cursor-pointer"
            >
              <ChevronLeft className="w-5 h-5" />
            </button>
            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation();
                haptics.light();
                setActiveImageIndex((prev) => (prev + 1) % galleryImages.length);
                resetTransform();
              }}
              aria-label="Next image"
              className="absolute right-3 top-1/2 -translate-y-1/2 w-10 h-10 rounded-full bg-slate-900/70 hover:bg-slate-900 border border-white/10 text-white flex items-center justify-center shadow-lg active:scale-90 transition-all z-20 cursor-pointer"
            >
              <ChevronRight className="w-5 h-5" />
            </button>
          </>
        )}

        {/* Pinch / Double-tap Hint Badge */}
        {scale === 1 && dismissOffsetY === 0 && (
          <div className="absolute bottom-4 left-1/2 -translate-x-1/2 pointer-events-none opacity-80 animate-pulse">
            <span className="text-[11px] font-semibold text-slate-300 bg-slate-900/80 backdrop-blur-md px-3 py-1 rounded-full border border-slate-700/60 shadow-lg flex items-center gap-1.5">
              <span>Pinch or double-tap to zoom</span>
              <span className="text-[#FF8A65]">&bull;</span>
              <span>Swipe down to close</span>
            </span>
          </div>
        )}
      </div>

      {/* =================================================================== */}
      {/* FLOATING ZOOM CONTROLS TOOLBAR                                      */}
      {/* =================================================================== */}
      <div className="relative z-30 flex items-center justify-center py-2 px-4">
        <div className="flex items-center gap-1.5 bg-slate-900/90 backdrop-blur-md border border-slate-800 p-1.5 rounded-full shadow-2xl">
          <button
            type="button"
            onClick={zoomOut}
            disabled={scale <= 1}
            aria-label="Zoom Out"
            className="w-8 h-8 rounded-full flex items-center justify-center text-slate-200 hover:text-white hover:bg-white/10 disabled:opacity-30 disabled:pointer-events-none active:scale-90 transition-all cursor-pointer"
            title="Zoom Out (-)"
          >
            <ZoomOut className="w-4 h-4" />
          </button>

          <button
            type="button"
            onClick={resetTransform}
            className="px-2.5 py-1 rounded-full text-xs font-bold text-slate-200 hover:text-white hover:bg-white/10 active:scale-95 transition-all cursor-pointer"
            title="Reset Zoom (Fit to Screen)"
          >
            {Math.round(scale * 100)}%
          </button>

          <button
            type="button"
            onClick={zoomIn}
            disabled={scale >= 4}
            aria-label="Zoom In"
            className="w-8 h-8 rounded-full flex items-center justify-center text-slate-200 hover:text-white hover:bg-white/10 disabled:opacity-30 disabled:pointer-events-none active:scale-90 transition-all cursor-pointer"
            title="Zoom In (+)"
          >
            <ZoomIn className="w-4 h-4" />
          </button>

          <div className="h-4 w-px bg-slate-700 mx-1" />

          <button
            type="button"
            onClick={() => {
              haptics.selection();
              if (scale === 1) {
                setScale(2.5);
              } else {
                resetTransform();
              }
            }}
            aria-label="Toggle Fullscreen Zoom"
            className="w-8 h-8 rounded-full flex items-center justify-center text-slate-200 hover:text-white hover:bg-white/10 active:scale-90 transition-all cursor-pointer"
            title="Toggle Fit / Zoom"
          >
            <Maximize2 className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      {/* =================================================================== */}
      {/* BOTTOM THUMBNAILS & FLIPKART PURCHASE BAR                           */}
      {/* =================================================================== */}
      <div className="relative z-30 bg-gradient-to-t from-slate-950 via-slate-950/90 to-transparent p-3 sm:p-4 border-t border-slate-900/80 space-y-3">
        {/* Gallery Thumbnails Strip */}
        {galleryImages.length > 1 && (
          <div className="flex items-center justify-center gap-2 overflow-x-auto no-scrollbar py-1">
            {galleryImages.map((img, idx) => (
              <button
                key={idx}
                type="button"
                onClick={() => {
                  haptics.selection();
                  setActiveImageIndex(idx);
                  resetTransform();
                }}
                className={`relative w-12 h-12 rounded-xl overflow-hidden border-2 transition-all cursor-pointer shrink-0 ${
                  activeImageIndex === idx
                    ? 'border-[#FF5722] ring-2 ring-[#FF5722]/50 scale-105'
                    : 'border-slate-800 opacity-60 hover:opacity-100'
                }`}
              >
                <img
                  src={img.url}
                  alt={img.label}
                  className="w-full h-full object-cover"
                />
              </button>
            ))}
          </div>
        )}

        {/* Product Price & Fast Action Bar */}
        <div className="flex items-center justify-between gap-3 max-w-lg mx-auto w-full pt-1">
          <div>
            <div className="flex items-baseline gap-2">
              <span className="text-lg font-black text-white">
                ₹{product.price.toLocaleString('en-IN')}
              </span>
              {product.originalPrice > product.price && (
                <span className="text-xs text-slate-400 line-through">
                  ₹{product.originalPrice.toLocaleString('en-IN')}
                </span>
              )}
              <span className="text-[10px] font-bold text-emerald-400">
                {product.discount}
              </span>
            </div>
            <p className="text-[10px] text-slate-400">
              Free Delivery &bull; 7 Days Return
            </p>
          </div>

          <div className="flex items-center gap-2">
            {onAddToCart && (
              <button
                type="button"
                onClick={() => {
                  haptics.success();
                  onAddToCart(product);
                }}
                className="px-3.5 py-2.5 rounded-xl bg-[#211A17] hover:bg-[#2C221E] active:scale-95 text-[#FF8A65] text-xs font-bold flex items-center gap-1.5 transition-all cursor-pointer border border-[#FF5722]/40"
              >
                <ShoppingCart className="w-3.5 h-3.5" />
                <span>Add to Cart</span>
              </button>
            )}

            {onBuyNow && (
              <button
                type="button"
                onClick={() => {
                  haptics.success();
                  onClose();
                  onBuyNow(product);
                }}
                className="px-4 py-2.5 rounded-xl bg-[#FF5722] hover:bg-[#E64A19] active:scale-95 text-white text-xs font-black flex items-center gap-1.5 shadow-lg shadow-orange-600/30 transition-all cursor-pointer"
              >
                <Zap className="w-3.5 h-3.5 fill-current" />
                <span>Buy Now</span>
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
