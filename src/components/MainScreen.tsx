import React, { useState, useEffect } from 'react';
import {
  Menu,
  Bell,
  ShoppingCart,
  Search,
  Mic,
  ChevronLeft,
  ChevronRight,
  Smartphone,
  Shirt,
  Laptop,
  Armchair,
  Tv,
  Sparkles,
  Footprints,
  Baby,
  ShoppingBasket,
  LayoutGrid,
  Percent,
  ArrowRight,
  Home,
  Tag,
  User,
  ShieldCheck,
  Store,
  CheckCircle2,
  X,
  Star,
  Plus,
  Minus,
  Trash2,
  Clock,
  Package,
  MapPin,
  Flame,
  Sun,
  Moon,
  Heart,
  Truck,
  AlertTriangle,
} from 'lucide-react';
import { OfflineStatus } from './OfflineStatus';
import { ScrollToTop } from './ScrollToTop';
import { MainScreenSkeleton } from './MainScreenSkeleton';
import { ProductDetailModal, Product } from './ProductDetailModal';
import { OrdersSection, Order, OrderItem } from './OrdersSection';
import { haptics } from '../utils/haptics';

interface MainScreenProps {
  onLogout?: () => void;
  theme?: 'light' | 'dark';
  onToggleTheme?: () => void;
  initialLoading?: boolean;
}

export const MainScreen: React.FC<MainScreenProps> = ({
  onLogout,
  theme = 'light',
  onToggleTheme,
  initialLoading = true,
}) => {
  const [isLoading, setIsLoading] = useState<boolean>(initialLoading);

  useEffect(() => {
    if (!initialLoading) {
      setIsLoading(false);
      return;
    }
    const timer = setTimeout(() => {
      setIsLoading(false);
    }, 600);
    return () => clearTimeout(timer);
  }, [initialLoading]);

  // Navigation tabs: 'home' | 'categories' | 'offers' | 'cart' | 'account'
  const [activeTab, setActiveTab] = useState<'home' | 'categories' | 'offers' | 'cart' | 'account'>('home');
  const [isOrdersViewOpen, setIsOrdersViewOpen] = useState(false);

  const switchTab = (tab: 'home' | 'categories' | 'offers' | 'cart' | 'account') => {
    haptics.selection();
    setIsOrdersViewOpen(false);
    setActiveTab(tab);
  };
  const [selectedCategory, setSelectedCategory] = useState<string>('All');
  const [currentSlide, setCurrentSlide] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [isVoiceModalOpen, setIsVoiceModalOpen] = useState(false);
  const [isSellerModalOpen, setIsSellerModalOpen] = useState(false);
  const [sellerModalTab, setSellerModalTab] = useState<'overview' | 'register'>('overview');
  const [sellerRegistered, setSellerRegistered] = useState(false);
  const [sellerForm, setSellerForm] = useState({ business: '', category: 'Fashion', phone: '' });
  const [isNotificationModalOpen, setIsNotificationModalOpen] = useState(false);
  const [orderSuccessModal, setOrderSuccessModal] = useState<string | null>(null);
  const [couponApplied, setCouponApplied] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);

  // Orders purchase history state with realistic data
  const [orders, setOrders] = useState<Order[]>(() => {
    try {
      const saved = localStorage.getItem('aarvo_orders');
      if (saved) return JSON.parse(saved);
    } catch {
      // safe fallback
    }
    return [
      {
        id: 'ARV-984320',
        date: 'Today, Oct 30, 2026',
        total: 3198,
        status: 'Processing',
        statusStep: 1,
        estimatedDelivery: 'Preparing dispatch, arrives Nov 02',
        trackingNumber: 'SHIPROCKET-992014',
        carrier: 'Shiprocket Surface &bull; AWB 992014',
        paymentMethod: 'UPI (Paytm)',
        deliveryAddress: 'Flat 402, Green Glen Heights, Outer Ring Road, Bangalore - 560103',
        items: [
          {
            id: 'p4',
            name: 'Puma Men Running Shoes (Black, UK 9)',
            price: 1599,
            quantity: 2,
            image: 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=500&auto=format&fit=crop&q=80',
            category: 'Footwear',
          },
        ],
        timeline: [
          { status: 'Order Placed & Verified', time: '09:30 AM', description: 'Order confirmed with Instant UPI Payment', completed: true },
          { status: 'Seller Packaging', time: '11:00 AM', description: 'Seller is packing items at warehouse', completed: true },
          { status: 'Courier Pickup Scheduled', time: 'Expected Today', description: 'Courier partner scheduled to pick up package', completed: false },
          { status: 'Out for Delivery', time: 'Nov 02', description: 'Final doorstep delivery with OTP', completed: false },
        ],
      },
      {
        id: 'ARV-924180',
        date: 'Oct 27, 2026',
        total: 19999,
        status: 'Shipped',
        statusStep: 2,
        estimatedDelivery: 'Arriving Tomorrow, by 8 PM',
        trackingNumber: 'BLUEDART-89124018',
        carrier: 'BlueDart Express &bull; AWB 89124018',
        paymentMethod: 'UPI (Google Pay)',
        deliveryAddress: 'Flat 402, Green Glen Heights, Outer Ring Road, Bangalore - 560103',
        items: [
          {
            id: 'p1',
            name: 'OnePlus Nord CE4 5G (8GB RAM, 128GB Storage)',
            price: 19999,
            quantity: 1,
            image: 'https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=500&auto=format&fit=crop&q=80',
            category: 'Mobiles',
          },
        ],
        timeline: [
          { status: 'Order Placed & Verified', time: '10:15 AM', description: 'Order confirmed with Instant UPI Payment', completed: true },
          { status: 'Packed & Dispatched', time: '02:30 PM', description: 'Package inspected and handed to courier', completed: true },
          { status: 'In Transit', time: '05:45 PM', description: 'Shipment arrived at Bangalore Sorting Hub', completed: true },
          { status: 'Out for Delivery', time: 'Tomorrow', description: 'Courier partner will deliver with OTP confirmation', completed: false },
        ],
      },
      {
        id: 'ARV-814205',
        date: 'Oct 24, 2026',
        total: 5289,
        status: 'Delivered',
        statusStep: 3,
        estimatedDelivery: 'Delivered on Oct 25, 2026',
        trackingNumber: 'DELHIVERY-7740192',
        carrier: 'Delhivery Surface &bull; AWB 7740192',
        paymentMethod: 'HDFC Credit Card (Ending in 4092)',
        deliveryAddress: 'Flat 402, Green Glen Heights, Outer Ring Road, Bangalore - 560103',
        items: [
          {
            id: 'p2',
            name: 'Pure Cotton Embroidered Anarkali Kurta Set',
            price: 1299,
            quantity: 1,
            image: 'https://images.unsplash.com/photo-1610030469983-98e550d6193c?w=500&auto=format&fit=crop&q=80',
            category: 'Fashion',
          },
          {
            id: 'p3',
            name: 'Sony WH-CH520 Wireless Bluetooth Headphones',
            price: 3990,
            quantity: 1,
            image: 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500&auto=format&fit=crop&q=80',
            category: 'Electronics',
          },
        ],
        timeline: [
          { status: 'Order Confirmed', time: 'Oct 24, 09:00 AM', description: 'Payment verified via HDFC Card', completed: true },
          { status: 'Dispatched from Hub', time: 'Oct 24, 01:20 PM', description: 'Picked up by Delhivery Logistics', completed: true },
          { status: 'Delivered Successfully', time: 'Oct 25, 03:45 PM', description: 'Package handed over to resident', completed: true },
        ],
      },
    ];
  });

  // Sync orders with localStorage
  useEffect(() => {
    try {
      localStorage.setItem('aarvo_orders', JSON.stringify(orders));
    } catch {
      // safe catch
    }
  }, [orders]);

  // Local state array to store wishlisted product IDs
  const [wishlist, setWishlist] = useState<string[]>(() => {
    try {
      const saved = localStorage.getItem('aarvo_wishlist');
      return saved ? JSON.parse(saved) : ['p2'];
    } catch {
      return ['p2'];
    }
  });

  // Keep localStorage updated with wishlist
  useEffect(() => {
    try {
      localStorage.setItem('aarvo_wishlist', JSON.stringify(wishlist));
    } catch {
      // Storage safe catch
    }
  }, [wishlist]);

  const toggleWishlist = (productId: string, productName?: string) => {
    setWishlist((prev) => {
      const isWishlisted = prev.includes(productId);
      if (isWishlisted) {
        haptics.warning();
        showToast('Removed from Wishlist');
        return prev.filter((id) => id !== productId);
      } else {
        haptics.success();
        showToast(productName ? `Added ${productName.slice(0, 18)}... to Wishlist ❤️` : 'Added to Wishlist ❤️');
        return [...prev, productId];
      }
    });
  };

  // Countdown timer for Offers
  const [timeLeft, setTimeLeft] = useState({ hours: 4, minutes: 28, seconds: 45 });

  useEffect(() => {
    const timer = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev.seconds > 0) return { ...prev, seconds: prev.seconds - 1 };
        if (prev.minutes > 0) return { ...prev, minutes: 59, seconds: 59 };
        if (prev.hours > 0) return { hours: prev.hours - 1, minutes: 59, seconds: 59 };
        return { hours: 4, minutes: 30, seconds: 0 };
      });
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 2500);
  };

  // Sample real catalog products with stock inventory data
  const [allProducts, setAllProducts] = useState<Product[]>([
    {
      id: 'p1',
      name: 'OnePlus Nord CE4 5G (8GB RAM, 128GB Storage)',
      category: 'Mobiles',
      image: 'https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=500&auto=format&fit=crop&q=80',
      price: 19999,
      originalPrice: 24999,
      rating: 4.5,
      reviewsCount: 1420,
      discount: '20% OFF',
      inStock: true,
      stockQuantity: 14,
      seller: 'TechPro Retail India',
    },
    {
      id: 'p2',
      name: 'Pure Cotton Embroidered Anarkali Kurta Set',
      category: 'Fashion',
      image: 'https://images.unsplash.com/photo-1610030469983-98e550d6193c?w=500&auto=format&fit=crop&q=80',
      price: 1299,
      originalPrice: 3499,
      rating: 4.6,
      reviewsCount: 890,
      discount: '62% OFF',
      inStock: true,
      stockQuantity: 3,
      seller: 'Aarvo Trends Fashion',
    },
    {
      id: 'p3',
      name: 'Sony WH-CH520 Wireless Bluetooth Headphones',
      category: 'Electronics',
      image: 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500&auto=format&fit=crop&q=80',
      price: 3990,
      originalPrice: 4990,
      rating: 4.4,
      reviewsCount: 3120,
      discount: '20% OFF',
      inStock: true,
      stockQuantity: 18,
      seller: 'Sony Authorized Store',
    },
    {
      id: 'p4',
      name: 'Ergonomic High Back Mesh Office & Study Chair',
      category: 'Home & Living',
      image: 'https://images.unsplash.com/photo-1580481077195-c22ae2804b7f?w=500&auto=format&fit=crop&q=80',
      price: 4499,
      originalPrice: 8999,
      rating: 4.3,
      reviewsCount: 450,
      discount: '50% OFF',
      inStock: true,
      stockQuantity: 2,
      seller: 'Urban Home Comfort',
    },
    {
      id: 'p5',
      name: 'Philips Digital Air Fryer with Rapid Air Tech',
      category: 'Appliances',
      image: 'https://images.unsplash.com/photo-1585659722983-3a675dabf23d?w=500&auto=format&fit=crop&q=80',
      price: 6499,
      originalPrice: 11995,
      rating: 4.7,
      reviewsCount: 2240,
      discount: '45% OFF',
      inStock: true,
      stockQuantity: 9,
      seller: 'KitchenWorld Express',
    },
    {
      id: 'p6',
      name: 'Organic Vitamin C Face Serum with Hyaluronic Acid',
      category: 'Beauty & Health',
      image: 'https://images.unsplash.com/photo-1620916566398-39f1143ab7be?w=500&auto=format&fit=crop&q=80',
      price: 499,
      originalPrice: 999,
      rating: 4.5,
      reviewsCount: 1820,
      discount: '50% OFF',
      inStock: true,
      stockQuantity: 4,
      seller: 'GlowSkin Organics',
    },
    {
      id: 'p7',
      name: 'Men Ultra-Light Breathable Running Shoes',
      category: 'Sports',
      image: 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=500&auto=format&fit=crop&q=80',
      price: 1599,
      originalPrice: 3999,
      rating: 4.4,
      reviewsCount: 950,
      discount: '60% OFF',
      inStock: true,
      stockQuantity: 15,
      seller: 'Apex Sports Hub',
    },
    {
      id: 'p8',
      name: '120-Piece Mega Magnetic Building Blocks Set',
      category: 'Toys & Kids',
      image: 'https://images.unsplash.com/photo-1587654780291-39c9404d746b?w=500&auto=format&fit=crop&q=80',
      price: 999,
      originalPrice: 1999,
      rating: 4.8,
      reviewsCount: 670,
      discount: '50% OFF',
      inStock: true,
      stockQuantity: 1,
      seller: 'KidsJoy Funland',
    },
    {
      id: 'p9',
      name: 'California Jumbo Almonds & Cashews Combo (1kg)',
      category: 'Grocery',
      image: 'https://images.unsplash.com/photo-1508061252224-237ff54ed5a3?w=500&auto=format&fit=crop&q=80',
      price: 899,
      originalPrice: 1450,
      rating: 4.6,
      reviewsCount: 1380,
      discount: '38% OFF',
      inStock: true,
      stockQuantity: 24,
      seller: 'NutriHarvest Daily',
    },
  ]);

  const restockProduct = (id: string, amount: number = 10) => {
    setAllProducts((prev) =>
      prev.map((item) =>
        item.id === id ? { ...item, stockQuantity: (item.stockQuantity ?? 0) + amount } : item
      )
    );
    haptics.success();
    showToast(`Restocked +${amount} units successfully!`);
  };

  // Department filter list for horizontal scrollable bar below search bar
  const departmentFilters = [
    { id: 'All', label: 'All' },
    { id: 'Electronics', label: 'Electronics' },
    { id: 'Fashion', label: 'Fashion' },
    { id: 'Mobiles', label: 'Mobiles' },
    { id: 'Home', label: 'Home & Living' },
    { id: 'Grocery', label: 'Grocery' },
    { id: 'Beauty', label: 'Beauty & Care' },
    { id: 'Appliances', label: 'Appliances' },
    { id: 'Footwear', label: 'Footwear' },
    { id: 'Toys', label: 'Toys & Baby' },
  ];

  // Cart State with initial items
  const [cartItems, setCartItems] = useState<{ product: Product; quantity: number }[]>([
    {
      product: allProducts[0],
      quantity: 1,
    },
    {
      product: allProducts[1],
      quantity: 1,
    },
  ]);

  const addToCart = (product: Product) => {
    haptics.success();
    setCartItems((prev) => {
      const existing = prev.find((item) => item.product.id === product.id);
      if (existing) {
        return prev.map((item) =>
          item.product.id === product.id ? { ...item, quantity: item.quantity + 1 } : item
        );
      }
      return [...prev, { product, quantity: 1 }];
    });
    showToast(`Added ${product.name.slice(0, 24)}... to Cart!`);
  };

  const updateCartQuantity = (id: string, delta: number) => {
    haptics.light();
    setCartItems((prev) =>
      prev
        .map((item) => {
          if (item.product.id === id) {
            const newQty = item.quantity + delta;
            return newQty > 0 ? { ...item, quantity: newQty } : null;
          }
          return item;
        })
        .filter(Boolean) as { product: Product; quantity: number }[]
    );
  };

  const removeFromCart = (id: string) => {
    haptics.warning();
    setCartItems((prev) => prev.filter((item) => item.product.id !== id));
    showToast('Item removed from Cart');
  };

  // Cart total calculations
  const totalMRP = cartItems.reduce((acc, item) => acc + item.product.originalPrice * item.quantity, 0);
  const totalSellingPrice = cartItems.reduce((acc, item) => acc + item.product.price * item.quantity, 0);
  const totalDiscount = totalMRP - totalSellingPrice;
  const couponDiscount = couponApplied ? Math.min(200, Math.round(totalSellingPrice * 0.1)) : 0;
  const platformFee = cartItems.length > 0 ? 3 : 0;
  const finalPayable = Math.max(0, totalSellingPrice - couponDiscount + platformFee);

  // Hero Banner Slides
  const slides = [
    {
      id: 1,
      badge: 'Exclusive',
      isGraphicBanner: true,
      image: '/src/assets/aarvo/hero_banner_1.jpg',
    },
    {
      id: 2,
      badge: 'Seller Hub',
      isGraphicBanner: false,
      titlePrimary: 'Verified Sellers',
      titleSecondary: 'Direct & Genuine',
      subtitle: 'Doorstep Delivery • 100% Quality Invoices',
      ctaText: 'Explore Drops',
      bgGradient: 'from-[#1E0E62] via-[#2D1282] to-[#4514B8]',
    },
    {
      id: 3,
      badge: 'Mega Deals',
      isGraphicBanner: false,
      titlePrimary: 'Smart Gadgets',
      titleSecondary: 'Up to 60% OFF',
      subtitle: 'Smartphones, Audio & Wearables with 1 Yr Warranty',
      ctaText: 'Shop Electronics',
      bgGradient: 'from-[#0A1A44] via-[#122A70] to-[#2515DB]',
    },
  ];

  // Auto-play hero slider
  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentSlide((prev) => (prev + 1) % slides.length);
    }, 5000);
    return () => clearInterval(timer);
  }, [slides.length]);

  // 10 Categories matching the screenshot layout exactly
  const categories = [
    { name: 'Mobiles', icon: Smartphone, color: 'bg-[#DBEAFE] text-[#2563EB] dark:bg-blue-950/40 dark:text-blue-400' },
    { name: 'Fashion', icon: Shirt, color: 'bg-[#FFEDD5] text-[#EA580C] dark:bg-orange-950/40 dark:text-orange-400' },
    { name: 'Electronics', icon: Laptop, color: 'bg-[#E0E7FF] text-[#4F46E5] dark:bg-indigo-950/40 dark:text-indigo-400' },
    { name: 'Home &...', icon: Armchair, color: 'bg-[#FEF3C7] text-[#D97706] dark:bg-amber-950/40 dark:text-amber-400' },
    { name: 'Appliances', icon: Tv, color: 'bg-[#E0F2FE] text-[#0284C7] dark:bg-sky-950/40 dark:text-sky-400' },
    { name: 'Beauty &...', icon: Sparkles, color: 'bg-[#FCE7F3] text-[#E11D48] dark:bg-rose-950/40 dark:text-rose-400' },
    { name: 'Sports', icon: Footprints, color: 'bg-[#CCFBF1] text-[#0D9488] dark:bg-teal-950/40 dark:text-teal-400' },
    { name: 'Toys & Kids', icon: Baby, color: 'bg-[#FEF9C3] text-[#CA8A04] dark:bg-yellow-950/40 dark:text-yellow-400' },
    { name: 'Grocery', icon: ShoppingBasket, color: 'bg-[#DCFCE7] text-[#16A34A] dark:bg-emerald-950/40 dark:text-emerald-400' },
    { name: 'More', icon: LayoutGrid, color: 'bg-[#F3E8FF] text-[#9333EA] dark:bg-purple-950/40 dark:text-purple-400' },
  ];

  // Filtered products for search or category selection
  const filteredProducts = allProducts.filter((p) => {
    const matchesSearch =
      searchQuery === '' ||
      p.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.category.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesCategory =
      selectedCategory === 'All' || p.category.toLowerCase().startsWith(selectedCategory.toLowerCase().slice(0, 4));
    return matchesSearch && matchesCategory;
  });

  if (isLoading) {
    return (
      <div className="min-h-screen bg-[#F7F5FF] dark:bg-[#0A0718] text-slate-900 dark:text-slate-100 flex justify-center pb-16 select-none font-sans transition-colors duration-300">
        <MainScreenSkeleton />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#F7F5FF] dark:bg-[#0A0718] text-slate-900 dark:text-slate-100 flex justify-center pb-16 select-none font-sans transition-colors duration-300 animate-in fade-in duration-300">
      <div className="w-full max-w-[440px] bg-white dark:bg-[#120F26] min-h-screen shadow-2xl flex flex-col relative overflow-x-hidden transition-colors duration-300">

        {isOrdersViewOpen ? (
          <OrdersSection
            orders={orders}
            onBack={() => setIsOrdersViewOpen(false)}
            onReorder={(item) => {
              const matchedProduct = allProducts.find((p) => p.id === item.id) || {
                id: item.id,
                name: item.name,
                category: item.category,
                image: item.image,
                price: item.price,
                originalPrice: Math.round(item.price * 1.25),
                rating: 4.5,
                reviewsCount: 150,
                discount: '20% OFF',
                inStock: true,
                seller: 'AARVO Assured',
              };
              addToCart(matchedProduct);
              showToast(`Added ${item.name.slice(0, 20)}... to Cart!`);
            }}
            onReorderAll={(items) => {
              haptics.success();
              setCartItems((prev) => {
                const nextCart = [...prev];
                items.forEach((orderItem) => {
                  const matchedProduct: Product = allProducts.find((p) => p.id === orderItem.id) || {
                    id: orderItem.id,
                    name: orderItem.name,
                    category: orderItem.category,
                    image: orderItem.image,
                    price: orderItem.price,
                    originalPrice: Math.round(orderItem.price * 1.25),
                    rating: 4.5,
                    reviewsCount: 150,
                    discount: '20% OFF',
                    inStock: true,
                    seller: 'AARVO Assured',
                  };
                  const existingIndex = nextCart.findIndex((ci) => ci.product.id === orderItem.id);
                  if (existingIndex > -1) {
                    nextCart[existingIndex] = {
                      ...nextCart[existingIndex],
                      quantity: nextCart[existingIndex].quantity + (orderItem.quantity || 1),
                    };
                  } else {
                    nextCart.push({
                      product: matchedProduct,
                      quantity: orderItem.quantity || 1,
                    });
                  }
                });
                return nextCart;
              });
              const count = items.reduce((acc, it) => acc + (it.quantity || 1), 0);
              showToast(`Reordered! ${count} item${count > 1 ? 's' : ''} added to your Cart 🛒`);
            }}
            onNavigateToCart={() => {
              setIsOrdersViewOpen(false);
              switchTab('cart');
            }}
            onShowToast={showToast}
          />
        ) : (
          <>
            {/* ================================================================= */}
            {/* 1. TOP HEADER: Solid Royal Blue Zone with AARVO Logo & Search Bar */}
            {/* ================================================================= */}
            <header className="bg-[#2418D8] text-white pt-2.5 pb-3.5 px-4 sticky top-0 z-30 shadow-md">
          {/* Top Bar: Menu, Centered AARVO Branding, Theme Toggle, Notification & Cart */}
          <div className="flex items-center justify-between gap-2 mb-2.5">
            {/* Left: Hamburger Menu */}
            <button
              onClick={() => {
                haptics.light();
                setIsSidebarOpen(true);
              }}
              className="p-1.5 hover:bg-white/15 rounded-xl transition-colors active:scale-95"
              aria-label="Open menu"
            >
              <Menu className="w-6 h-6 text-white stroke-[2.2]" />
            </button>

            {/* Center: Official 3D Ribbon Logo + AARVO Wordmark */}
            <div
              onClick={() => switchTab('home')}
              className="flex items-center gap-2 cursor-pointer active:scale-95 transition-transform"
            >
              <div className="w-8 h-8 flex items-center justify-center shrink-0">
                <img
                  src="/src/assets/aarvo/aarvo_logo_transparent.png"
                  alt="AARVO Logo"
                  className="w-full h-full object-contain filter drop-shadow-[0_2px_8px_rgba(255,180,0,0.5)]"
                />
              </div>
              <span className="text-2xl font-black tracking-wider text-white font-display leading-none select-none">
                AARVO
              </span>
            </div>

            {/* Right: Theme Toggle, Notification Bell & Shopping Cart */}
            <div className="flex items-center gap-0.5 sm:gap-1">
              {/* Global Theme Toggle Button */}
              {onToggleTheme && (
                <button
                  onClick={onToggleTheme}
                  className="relative p-2 hover:bg-white/15 rounded-xl transition-all active:scale-90"
                  title={theme === 'dark' ? 'Switch to Light Theme' : 'Switch to Dark Theme'}
                  aria-label="Toggle light or dark theme"
                >
                  {theme === 'dark' ? (
                    <Sun className="w-5 h-5 text-amber-300 stroke-[2.2] transition-transform duration-300 rotate-0 hover:rotate-45" />
                  ) : (
                    <Moon className="w-5 h-5 text-white stroke-[2.2] transition-transform duration-300 -rotate-12 hover:rotate-0" />
                  )}
                </button>
              )}

              <button
                onClick={() => setIsNotificationModalOpen(true)}
                className="relative p-2 hover:bg-white/15 rounded-xl transition-colors active:scale-95"
                aria-label="Notifications"
              >
                <Bell className="w-5 h-5 text-white stroke-[2.2]" />
              </button>

              <button
                onClick={() => setActiveTab('cart')}
                className="relative p-2 hover:bg-white/15 rounded-xl transition-colors active:scale-95"
                aria-label="Cart"
              >
                <ShoppingCart className="w-5 h-5 text-white stroke-[2.2]" />
                {cartItems.length > 0 && (
                  <span className="absolute top-1 right-1 px-1 min-w-[17px] h-4 bg-red-500 text-white text-[10px] font-extrabold rounded-full flex items-center justify-center border-2 border-[#2418D8]">
                    {cartItems.reduce((acc, item) => acc + item.quantity, 0)}
                  </span>
                )}
              </button>
            </div>
          </div>

          {/* Search Bar - Big, prominent pill search bar on Blue Screen */}
          <div className="relative w-full">
            <div className="h-11 sm:h-12 bg-white dark:bg-[#1A1638] rounded-full flex items-center px-3.5 shadow-sm overflow-hidden border border-transparent dark:border-purple-900/30">
              <Search className="w-4 h-4 sm:w-5 sm:h-5 text-slate-400 dark:text-slate-500 shrink-0 pointer-events-none mr-2.5" />
              <input
                type="text"
                placeholder="Search for products, brands and more..."
                value={searchQuery}
                onChange={(e) => {
                  setSearchQuery(e.target.value);
                  if (activeTab !== 'categories' && activeTab !== 'home') {
                    setActiveTab('home');
                  }
                }}
                className="w-full text-xs sm:text-[13px] text-slate-800 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 outline-none bg-transparent font-medium"
              />
              {searchQuery && (
                <button
                  onClick={() => {
                    haptics.light();
                    setSearchQuery('');
                  }}
                  className="p-1 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 mr-1"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              )}
              <button
                onClick={() => {
                  haptics.medium();
                  setIsVoiceModalOpen(true);
                }}
                aria-label="Voice search"
                className="p-1.5 text-slate-500 dark:text-slate-400 hover:text-[#2418D8] dark:hover:text-white transition-colors rounded-full active:scale-90"
              >
                <Mic className="w-4 h-4 sm:w-5 sm:h-5" />
              </button>
            </div>
          </div>

          {/* Horizontal Scrollable Category Filter Bar */}
          <div
            className="mt-2.5 -mx-4 px-4 overflow-x-auto no-scrollbar scroll-smooth flex items-center gap-2 py-0.5"
            role="tablist"
            aria-label="Filter products by department"
          >
            {departmentFilters.map((dept) => {
              const isSelected = selectedCategory === dept.id;
              return (
                <button
                  key={dept.id}
                  type="button"
                  role="tab"
                  aria-selected={isSelected}
                  onClick={() => {
                    haptics.selection();
                    setSelectedCategory(dept.id);
                  }}
                  className={`shrink-0 flex items-center gap-1.5 px-3.5 py-1.5 rounded-full text-xs font-bold transition-all duration-200 active:scale-95 cursor-pointer select-none ${
                    isSelected
                      ? 'bg-white text-[#2418D8] shadow-sm font-black'
                      : 'bg-white/15 hover:bg-white/25 text-white/95 border border-white/20'
                  }`}
                >
                  <span className="whitespace-nowrap">{dept.label}</span>
                </button>
              );
            })}
          </div>
        </header>

        {/* Toast Notification */}
        {toastMessage && (
          <div className="fixed top-28 left-1/2 -translate-x-1/2 z-50 bg-slate-900/95 dark:bg-purple-950/95 text-white px-4 py-2 rounded-full text-xs font-semibold shadow-xl backdrop-blur-xs flex items-center gap-2 animate-bounce border border-white/10">
            <CheckCircle2 className="w-4 h-4 text-emerald-400" />
            <span>{toastMessage}</span>
          </div>
        )}

        {/* ================================================================= */}
        {/* TAB 1: HOME VIEW                                                  */}
        {/* ================================================================= */}
        {activeTab === 'home' && (
          <div className="flex-1 flex flex-col pb-6">
            {/* 2. HERO LIVE SLIDER CAROUSEL */}
            <section className="p-3">
              <div className="relative aspect-[16/9] w-full rounded-2xl overflow-hidden shadow-lg bg-[#2418D8] group">
                {slides[currentSlide].isGraphicBanner ? (
                  <div className="relative w-full h-full">
                    <img
                      src={slides[currentSlide].image}
                      alt="AARVO Big Sale Banner"
                      className="w-full h-full object-cover object-center"
                    />
                    <div className="absolute top-3 right-3 z-10">
                      <span className="flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-red-600 text-white text-[10px] font-bold shadow-md">
                        <span className="w-1.5 h-1.5 rounded-full bg-white animate-ping" />
                        <span>&bull; {slides[currentSlide].badge}</span>
                      </span>
                    </div>
                  </div>
                ) : (
                  <div
                    className={`w-full h-full bg-gradient-to-r ${slides[currentSlide].bgGradient} p-5 flex flex-col justify-between text-white relative`}
                  >
                    <div className="flex items-center justify-between">
                      <span className="text-[10px] font-extrabold uppercase tracking-wider text-amber-300">
                        AARVO Verified
                      </span>
                      <span className="flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-red-600 text-white text-[10px] font-bold shadow-xs">
                        <span className="w-1.5 h-1.5 rounded-full bg-white animate-ping" />
                        <span>&bull; {slides[currentSlide].badge}</span>
                      </span>
                    </div>

                    <div className="my-auto max-w-[70%]">
                      <h2 className="text-xl sm:text-2xl font-black font-display tracking-tight text-white leading-tight">
                        {slides[currentSlide].titlePrimary}
                      </h2>
                      <h3 className="text-xl sm:text-2xl font-black font-display tracking-tight text-[#FFD700] leading-tight">
                        {slides[currentSlide].titleSecondary}
                      </h3>
                      <p className="text-[10px] text-purple-100 font-medium mt-1 line-clamp-1">
                        {slides[currentSlide].subtitle}
                      </p>

                      <button
                        onClick={() => {
                          if (slides[currentSlide].id === 2) setIsSellerModalOpen(true);
                          else setActiveTab('categories');
                        }}
                        className="mt-3 px-4 py-1.5 bg-[#FFD700] hover:bg-[#FACC15] text-[#171329] font-black text-xs rounded-full shadow-md flex items-center gap-1.5 transition-transform active:scale-95"
                      >
                        <span>{slides[currentSlide].ctaText}</span>
                        <ArrowRight className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                )}

                {/* Dots indicator at the bottom center matching screenshot */}
                <div className="absolute bottom-2.5 left-0 right-0 flex items-center justify-center gap-1.5 z-10">
                  {slides.map((_, idx) => (
                    <button
                      key={idx}
                      onClick={() => setCurrentSlide(idx)}
                      aria-label={`Go to slide ${idx + 1}`}
                      className={`h-1.5 rounded-full transition-all duration-300 ${
                        currentSlide === idx ? 'w-5 bg-[#FFD700]' : 'w-1.5 bg-white/60'
                      }`}
                    />
                  ))}
                </div>

                <button
                  onClick={() => setCurrentSlide((prev) => (prev - 1 + slides.length) % slides.length)}
                  className="absolute left-2 top-1/2 -translate-y-1/2 w-7 h-7 rounded-full bg-black/40 hover:bg-black/60 text-white flex items-center justify-center backdrop-blur-xs transition-opacity z-10"
                  aria-label="Previous slide"
                >
                  <ChevronLeft className="w-4 h-4" />
                </button>
                <button
                  onClick={() => setCurrentSlide((prev) => (prev + 1) % slides.length)}
                  className="absolute right-2 top-1/2 -translate-y-1/2 w-7 h-7 rounded-full bg-black/40 hover:bg-black/60 text-white flex items-center justify-center backdrop-blur-xs transition-opacity z-10"
                  aria-label="Next slide"
                >
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>
            </section>

            {/* 3. CATEGORIES GRID (10 Circles exactly matching screenshot) */}
            <section className="px-3 py-2">
              <div className="grid grid-cols-5 gap-y-3.5 gap-x-2">
                {categories.map((cat, idx) => {
                  const IconComp = cat.icon;
                  return (
                    <button
                      key={idx}
                      onClick={() => {
                        haptics.selection();
                        if (cat.name === 'More') {
                          switchTab('categories');
                        } else {
                          setSelectedCategory(cat.name);
                          switchTab('categories');
                        }
                      }}
                      className="flex flex-col items-center group transition-transform active:scale-95"
                    >
                      <div
                        className={`w-13 h-13 sm:w-14 sm:h-14 rounded-full ${cat.color} flex items-center justify-center shadow-xs border border-white/80 dark:border-slate-800 group-hover:scale-105 transition-transform`}
                      >
                        <IconComp className="w-6 h-6 stroke-[2]" />
                      </div>
                      <span className="text-[10px] font-semibold text-slate-700 dark:text-slate-300 mt-1.5 text-center leading-tight line-clamp-1">
                        {cat.name}
                      </span>
                    </button>
                  );
                })}
              </div>
            </section>

            {/* 4. TOP DEALS FOR YOU BANNER STRIP (Matching screenshot) */}
            <section className="px-3 py-2">
              <div
                onClick={() => setActiveTab('offers')}
                className="cursor-pointer rounded-2xl bg-gradient-to-r from-[#2418D8] via-[#3B19E4] to-[#6015E0] p-3 text-white flex items-center justify-between shadow-md active:scale-[0.99] transition-transform"
              >
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-[#FFA126] to-[#FF7A00] flex items-center justify-center shadow-xs shrink-0">
                    <Percent className="w-5 h-5 text-white font-black stroke-[2.5]" />
                  </div>
                  <div>
                    <h3 className="font-display font-extrabold text-sm sm:text-base leading-tight">
                      Top Deals For You
                    </h3>
                    <p className="text-[10px] text-purple-200 mt-0.5">
                      Best Offers &bull; Lowest Prices &bull; Limited Time
                    </p>
                  </div>
                </div>

                <div className="w-8 h-8 rounded-full bg-[#FFD700] hover:bg-yellow-300 text-slate-900 flex items-center justify-center shadow-xs shrink-0">
                  <ArrowRight className="w-4 h-4 font-bold stroke-[2.5]" />
                </div>
              </div>
            </section>

            {/* 6. POPULAR / DEPARTMENT FILTERED PRODUCTS GRID */}
            <section className="px-3 py-2">
              <div className="flex items-center justify-between mb-2.5">
                <div>
                  <h3 className="font-display font-extrabold text-sm text-slate-900 dark:text-white">
                    {selectedCategory === 'All' ? 'Recommended For You' : `${selectedCategory} Department`}
                  </h3>
                  {selectedCategory !== 'All' && (
                    <p className="text-[10px] text-slate-500 dark:text-slate-400">
                      Showing {filteredProducts.length} items
                    </p>
                  )}
                </div>

                {selectedCategory !== 'All' ? (
                  <button
                    onClick={() => {
                      haptics.light();
                      setSelectedCategory('All');
                    }}
                    className="text-xs font-bold text-[#2418D8] dark:text-[#7B73FF] flex items-center gap-1 hover:underline active:scale-95 transition-transform"
                  >
                    <span>Clear Filter</span>
                    <X className="w-3.5 h-3.5" />
                  </button>
                ) : (
                  <button
                    onClick={() => switchTab('categories')}
                    className="text-xs font-bold text-[#2418D8] dark:text-[#7B73FF] flex items-center gap-0.5 hover:underline"
                  >
                    <span>View All</span>
                    <ArrowRight className="w-3 h-3" />
                  </button>
                )}
              </div>

              <div className="grid grid-cols-2 gap-3">
                {(selectedCategory === 'All' ? allProducts.slice(0, 4) : filteredProducts).map((product) => (
                  <div
                    key={product.id}
                    onClick={() => {
                      haptics.light();
                      setSelectedProduct(product);
                    }}
                    className="bg-white dark:bg-[#1A1633] rounded-2xl p-2.5 border border-slate-100 dark:border-slate-800/80 shadow-xs flex flex-col justify-between hover:shadow-md transition-all group cursor-pointer"
                  >
                    <div className="relative aspect-square w-full rounded-xl overflow-hidden bg-slate-50 dark:bg-slate-900 mb-2">
                      <img
                        src={product.image}
                        alt={product.name}
                        className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                      />
                      <span className="absolute top-1.5 left-1.5 bg-emerald-600 text-white text-[9px] font-black px-1.5 py-0.5 rounded-md shadow-xs">
                        {product.discount}
                      </span>

                      {/* Heart-shaped Wishlist Toggle Button */}
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          toggleWishlist(product.id, product.name);
                        }}
                        aria-label={wishlist.includes(product.id) ? 'Remove from Wishlist' : 'Add to Wishlist'}
                        title={wishlist.includes(product.id) ? 'Remove from Wishlist' : 'Add to Wishlist'}
                        className={`absolute top-1.5 right-1.5 w-7 h-7 rounded-full flex items-center justify-center transition-all duration-200 active:scale-75 shadow-xs z-10 cursor-pointer ${
                          wishlist.includes(product.id)
                            ? 'bg-rose-50 dark:bg-rose-950/90 text-rose-500 scale-105'
                            : 'bg-white/85 dark:bg-slate-900/85 backdrop-blur-xs text-slate-400 dark:text-slate-300 hover:text-rose-500 hover:bg-white dark:hover:bg-slate-800'
                        }`}
                      >
                        <Heart
                          className={`w-3.5 h-3.5 transition-colors ${
                            wishlist.includes(product.id)
                              ? 'fill-rose-500 stroke-rose-500'
                              : 'stroke-current fill-none'
                          }`}
                        />
                      </button>
                    </div>

                    <div>
                      <div className="flex items-center gap-1 mb-1">
                        <span className="flex items-center gap-0.5 bg-emerald-50 dark:bg-emerald-950/40 text-emerald-700 dark:text-emerald-400 text-[10px] font-bold px-1.5 py-0.5 rounded">
                          <span>{product.rating}</span>
                          <Star className="w-2.5 h-2.5 fill-current text-emerald-600 dark:text-emerald-400" />
                        </span>
                        <span className="text-[10px] text-slate-400 dark:text-slate-500">
                          ({product.reviewsCount})
                        </span>
                      </div>

                      <h4 className="text-xs font-bold text-slate-800 dark:text-slate-100 line-clamp-2 leading-tight">
                        {product.name}
                      </h4>

                      <div className="flex items-baseline gap-1.5 mt-1.5">
                        <span className="text-sm font-black text-slate-900 dark:text-white">
                          ₹{product.price.toLocaleString('en-IN')}
                        </span>
                        <span className="text-[10px] text-slate-400 dark:text-slate-500 line-through">
                          ₹{product.originalPrice.toLocaleString('en-IN')}
                        </span>
                      </div>
                    </div>

                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        addToCart(product);
                      }}
                      className="mt-2.5 w-full py-1.5 bg-[#2418D8] hover:bg-[#1A10B3] text-white text-xs font-bold rounded-xl flex items-center justify-center gap-1.5 active:scale-95 transition-all shadow-xs"
                    >
                      <ShoppingCart className="w-3.5 h-3.5" />
                      <span>Add to Cart</span>
                    </button>
                  </div>
                ))}
              </div>
            </section>
          </div>
        )}

        {/* ================================================================= */}
        {/* TAB 2: CATEGORIES VIEW                                            */}
        {/* ================================================================= */}
        {activeTab === 'categories' && (
          <div className="flex-1 flex flex-col p-3 pb-6">
            <div className="flex items-center justify-between mb-3">
              <div>
                <h2 className="font-display font-black text-lg text-slate-900 dark:text-white">
                  Explore Categories
                </h2>
                <p className="text-xs text-slate-500 dark:text-slate-400">
                  Browse verified collections with direct doorstep delivery
                </p>
              </div>
            </div>

            {/* Category horizontal selector pills */}
            <div className="flex items-center gap-2 overflow-x-auto pb-2 scrollbar-none mb-3">
              <button
                onClick={() => setSelectedCategory('All')}
                className={`px-3 py-1.5 rounded-full text-xs font-bold shrink-0 transition-colors ${
                  selectedCategory === 'All'
                    ? 'bg-[#2418D8] text-white shadow-xs'
                    : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-700'
                }`}
              >
                All Products
              </button>
              {categories.map((cat, idx) => (
                <button
                  key={idx}
                  onClick={() => setSelectedCategory(cat.name)}
                  className={`px-3 py-1.5 rounded-full text-xs font-bold shrink-0 transition-colors ${
                    selectedCategory === cat.name
                      ? 'bg-[#2418D8] text-white shadow-xs'
                      : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-700'
                  }`}
                >
                  {cat.name}
                </button>
              ))}
            </div>

            {/* Products Listing in Selected Category */}
            <div className="grid grid-cols-2 gap-3">
              {filteredProducts.map((product) => (
                <div
                  key={product.id}
                  onClick={() => {
                    haptics.light();
                    setSelectedProduct(product);
                  }}
                  className="bg-white dark:bg-[#1A1633] rounded-2xl p-2.5 border border-slate-100 dark:border-slate-800/80 shadow-xs flex flex-col justify-between hover:shadow-md transition-all cursor-pointer group"
                >
                  <div className="relative aspect-square w-full rounded-xl overflow-hidden bg-slate-50 dark:bg-slate-900 mb-2">
                    <img
                      src={product.image}
                      alt={product.name}
                      className="w-full h-full object-cover"
                    />
                    <span className="absolute top-1.5 left-1.5 bg-emerald-600 text-white text-[9px] font-black px-1.5 py-0.5 rounded-md shadow-xs">
                      {product.discount}
                    </span>

                    {/* Heart-shaped Wishlist Toggle Button */}
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        toggleWishlist(product.id, product.name);
                      }}
                      aria-label={wishlist.includes(product.id) ? 'Remove from Wishlist' : 'Add to Wishlist'}
                      title={wishlist.includes(product.id) ? 'Remove from Wishlist' : 'Add to Wishlist'}
                      className={`absolute top-1.5 right-1.5 w-7 h-7 rounded-full flex items-center justify-center transition-all duration-200 active:scale-75 shadow-xs z-10 cursor-pointer ${
                        wishlist.includes(product.id)
                          ? 'bg-rose-50 dark:bg-rose-950/90 text-rose-500 scale-105'
                          : 'bg-white/85 dark:bg-slate-900/85 backdrop-blur-xs text-slate-400 dark:text-slate-300 hover:text-rose-500 hover:bg-white dark:hover:bg-slate-800'
                      }`}
                    >
                      <Heart
                        className={`w-3.5 h-3.5 transition-colors ${
                          wishlist.includes(product.id)
                            ? 'fill-rose-500 stroke-rose-500'
                            : 'stroke-current fill-none'
                        }`}
                      />
                    </button>
                  </div>

                  <div>
                    <span className="text-[10px] font-bold text-[#2418D8] dark:text-[#7B73FF] uppercase tracking-wider block mb-0.5">
                      {product.category}
                    </span>
                    <h4 className="text-xs font-bold text-slate-800 dark:text-slate-100 line-clamp-2 leading-tight">
                      {product.name}
                    </h4>

                    <div className="flex items-center gap-1 my-1">
                      <span className="flex items-center gap-0.5 bg-emerald-50 dark:bg-emerald-950/40 text-emerald-700 dark:text-emerald-400 text-[10px] font-bold px-1.5 py-0.5 rounded">
                        <span>{product.rating}</span>
                        <Star className="w-2.5 h-2.5 fill-current text-emerald-600 dark:text-emerald-400" />
                      </span>
                      <span className="text-[10px] text-slate-400 dark:text-slate-500">
                        ({product.reviewsCount})
                      </span>
                    </div>

                    <div className="flex items-baseline gap-1.5">
                      <span className="text-sm font-black text-slate-900 dark:text-white">
                        ₹{product.price.toLocaleString('en-IN')}
                      </span>
                      <span className="text-[10px] text-slate-400 dark:text-slate-500 line-through">
                        ₹{product.originalPrice.toLocaleString('en-IN')}
                      </span>
                    </div>
                  </div>

                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      addToCart(product);
                    }}
                    className="mt-2.5 w-full py-1.5 bg-[#2418D8] hover:bg-[#1A10B3] text-white text-xs font-bold rounded-xl flex items-center justify-center gap-1.5 active:scale-95 transition-all shadow-xs"
                  >
                    <ShoppingCart className="w-3.5 h-3.5" />
                    <span>Add to Cart</span>
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* ================================================================= */}
        {/* TAB 3: OFFERS VIEW                                                */}
        {/* ================================================================= */}
        {activeTab === 'offers' && (
          <div className="flex-1 flex flex-col p-3 pb-6 space-y-3.5">
            {/* Live Lightning Deal Header with countdown */}
            <div className="rounded-2xl bg-gradient-to-r from-red-600 via-rose-600 to-amber-600 p-4 text-white shadow-md">
              <div className="flex items-center justify-between mb-2">
                <span className="flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-white/20 text-white text-[10px] font-black uppercase tracking-wider backdrop-blur-xs">
                  <Flame className="w-3.5 h-3.5 text-yellow-300 fill-yellow-300 animate-pulse" />
                  Flash Lightning Sale
                </span>
                <div className="flex items-center gap-1 text-xs font-mono font-bold bg-black/30 px-2 py-0.5 rounded-lg">
                  <Clock className="w-3.5 h-3.5 text-yellow-300" />
                  <span>
                    {String(timeLeft.hours).padStart(2, '0')}:{String(timeLeft.minutes).padStart(2, '0')}:
                    {String(timeLeft.seconds).padStart(2, '0')}
                  </span>
                </div>
              </div>

              <h3 className="font-display font-black text-xl leading-tight">
                Up to 75% OFF Clearance
              </h3>
              <p className="text-xs text-rose-100 mt-1">
                Extra 10% instant discount with HDFC, ICICI &amp; UPI
              </p>
            </div>

            {/* Coupons Section */}
            <div className="bg-white dark:bg-[#1A1633] rounded-2xl p-3 border border-slate-100 dark:border-slate-800/80 shadow-xs">
              <h4 className="font-display font-bold text-xs text-slate-800 dark:text-slate-200 mb-2 flex items-center gap-1.5">
                <Tag className="w-4 h-4 text-[#2418D8] dark:text-[#7B73FF]" />
                <span>Available Coupons</span>
              </h4>

              <div className="p-2.5 rounded-xl bg-purple-50 dark:bg-purple-950/40 border border-purple-200 dark:border-purple-800/50 flex items-center justify-between">
                <div>
                  <span className="font-mono font-black text-xs text-[#2418D8] dark:text-purple-300 tracking-wider bg-white dark:bg-[#130F29] px-2 py-0.5 rounded border border-purple-200 dark:border-purple-800">
                    AARVO200
                  </span>
                  <p className="text-[10px] text-slate-600 dark:text-slate-300 mt-1">
                    Save flat ₹200 on orders above ₹999
                  </p>
                </div>
                <button
                  onClick={() => {
                    setCouponApplied(true);
                    showToast('Coupon AARVO200 Applied!');
                  }}
                  className="px-3 py-1 bg-[#2418D8] text-white text-[11px] font-bold rounded-lg shadow-xs hover:bg-[#1A10B3] active:scale-95 transition-all"
                >
                  Apply
                </button>
              </div>
            </div>

            {/* Best Steal Deals Products */}
            <div>
              <h4 className="font-display font-bold text-sm text-slate-900 dark:text-white mb-2.5">
                Today&apos;s Super Steals
              </h4>
              <div className="grid grid-cols-2 gap-3">
                {allProducts.slice(1, 5).map((product) => (
                  <div
                    key={product.id}
                    onClick={() => {
                      haptics.light();
                      setSelectedProduct(product);
                    }}
                    className="bg-white dark:bg-[#1A1633] rounded-2xl p-2.5 border border-slate-100 dark:border-slate-800/80 shadow-xs flex flex-col justify-between hover:shadow-md transition-all cursor-pointer group"
                  >
                    <div className="relative aspect-square w-full rounded-xl overflow-hidden bg-slate-50 dark:bg-slate-900 mb-2">
                      <img src={product.image} alt={product.name} className="w-full h-full object-cover" />
                      <span className="absolute top-1.5 left-1.5 bg-red-600 text-white text-[9px] font-black px-1.5 py-0.5 rounded-md shadow-xs">
                        {product.discount}
                      </span>

                      {/* Heart-shaped Wishlist Toggle Button */}
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          toggleWishlist(product.id, product.name);
                        }}
                        aria-label={wishlist.includes(product.id) ? 'Remove from Wishlist' : 'Add to Wishlist'}
                        title={wishlist.includes(product.id) ? 'Remove from Wishlist' : 'Add to Wishlist'}
                        className={`absolute top-1.5 right-1.5 w-7 h-7 rounded-full flex items-center justify-center transition-all duration-200 active:scale-75 shadow-xs z-10 cursor-pointer ${
                          wishlist.includes(product.id)
                            ? 'bg-rose-50 dark:bg-rose-950/90 text-rose-500 scale-105'
                            : 'bg-white/85 dark:bg-slate-900/85 backdrop-blur-xs text-slate-400 dark:text-slate-300 hover:text-rose-500 hover:bg-white dark:hover:bg-slate-800'
                        }`}
                      >
                        <Heart
                          className={`w-3.5 h-3.5 transition-colors ${
                            wishlist.includes(product.id)
                              ? 'fill-rose-500 stroke-rose-500'
                              : 'stroke-current fill-none'
                          }`}
                        />
                      </button>
                    </div>

                    <h4 className="text-xs font-bold text-slate-800 dark:text-slate-100 line-clamp-2 leading-tight">
                      {product.name}
                    </h4>

                    <div className="flex items-baseline gap-1.5 my-1.5">
                      <span className="text-sm font-black text-slate-900 dark:text-white">
                        ₹{product.price.toLocaleString('en-IN')}
                      </span>
                      <span className="text-[10px] text-slate-400 dark:text-slate-500 line-through">
                        ₹{product.originalPrice.toLocaleString('en-IN')}
                      </span>
                    </div>

                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        addToCart(product);
                      }}
                      className="w-full py-1.5 bg-[#2418D8] hover:bg-[#1A10B3] text-white text-xs font-bold rounded-xl flex items-center justify-center gap-1.5 active:scale-95 transition-all shadow-xs"
                    >
                      <ShoppingCart className="w-3.5 h-3.5" />
                      <span>Claim Deal</span>
                    </button>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* ================================================================= */}
        {/* TAB 4: CART VIEW                                                  */}
        {/* ================================================================= */}
        {activeTab === 'cart' && (
          <div className="flex-1 flex flex-col p-3 pb-6">
            <div className="flex items-center justify-between mb-3">
              <div>
                <h2 className="font-display font-black text-lg text-slate-900 dark:text-white">
                  Shopping Cart ({cartItems.length})
                </h2>
                <p className="text-xs text-slate-500 dark:text-slate-400">
                  All items are directly verified for quality and doorstep delivery
                </p>
              </div>
            </div>

            {cartItems.length === 0 ? (
              <div className="flex-1 flex flex-col items-center justify-center py-16 text-center">
                <div className="w-20 h-20 bg-purple-50 dark:bg-purple-950/40 text-[#2418D8] dark:text-[#7B73FF] rounded-full flex items-center justify-center mb-4">
                  <ShoppingCart className="w-10 h-10 stroke-[1.5]" />
                </div>
                <h3 className="font-display font-bold text-base text-slate-800 dark:text-slate-200">
                  Your cart is empty!
                </h3>
                <p className="text-xs text-slate-500 dark:text-slate-400 max-w-xs mt-1 mb-5">
                  Explore millions of quality products across categories and save with exclusive drops.
                </p>
                <button
                  onClick={() => setActiveTab('home')}
                  className="px-6 py-2.5 bg-[#2418D8] text-white font-bold text-xs rounded-xl shadow-md hover:bg-[#1B11AB] active:scale-95 transition-all"
                >
                  Explore Products
                </button>
              </div>
            ) : (
              <div className="space-y-3.5">
                {/* Delivery Address Bar */}
                <div className="p-3 bg-purple-50/70 dark:bg-purple-950/30 border border-purple-100 dark:border-purple-900/50 rounded-2xl flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <MapPin className="w-4 h-4 text-[#2418D8] dark:text-[#7B73FF] shrink-0" />
                    <div>
                      <span className="text-[11px] font-bold text-slate-800 dark:text-slate-200">
                        Deliver to Saurav Paswan - 110001
                      </span>
                      <p className="text-[10px] text-slate-500 dark:text-slate-400">
                        Standard Delivery in 2-3 business days
                      </p>
                    </div>
                  </div>
                  <button className="text-[11px] font-bold text-[#2418D8] dark:text-[#7B73FF] hover:underline">
                    Change
                  </button>
                </div>

                {/* Cart Items List */}
                <div className="space-y-2.5">
                  {cartItems.map((item) => (
                    <div
                      key={item.product.id}
                      className="bg-white dark:bg-[#1A1633] rounded-2xl p-3 border border-slate-100 dark:border-slate-800/80 shadow-xs flex gap-3"
                    >
                      <div className="w-20 h-20 rounded-xl overflow-hidden bg-slate-50 dark:bg-slate-900 shrink-0">
                        <img
                          src={item.product.image}
                          alt={item.product.name}
                          className="w-full h-full object-cover"
                        />
                      </div>

                      <div className="flex-1 flex flex-col justify-between">
                        <div>
                          <div className="flex items-start justify-between gap-1">
                            <h4 className="text-xs font-bold text-slate-800 dark:text-slate-100 line-clamp-1">
                              {item.product.name}
                            </h4>
                            <button
                              onClick={() => removeFromCart(item.product.id)}
                              className="text-slate-400 hover:text-rose-500 p-0.5"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                            </button>
                          </div>
                          <span className="text-[10px] text-slate-500 dark:text-slate-400 block">
                            Seller: {item.product.seller}
                          </span>

                          <div className="flex items-baseline gap-1.5 mt-1">
                            <span className="text-sm font-black text-slate-900 dark:text-white">
                              ₹{(item.product.price * item.quantity).toLocaleString('en-IN')}
                            </span>
                            <span className="text-[10px] text-slate-400 dark:text-slate-500 line-through">
                              ₹{(item.product.originalPrice * item.quantity).toLocaleString('en-IN')}
                            </span>
                            <span className="text-[10px] font-bold text-emerald-600 dark:text-emerald-400">
                              {item.product.discount}
                            </span>
                          </div>
                        </div>

                        {/* Quantity Stepper */}
                        <div className="flex items-center gap-2 mt-2">
                          <button
                            onClick={() => updateCartQuantity(item.product.id, -1)}
                            className="w-6 h-6 rounded-lg bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 flex items-center justify-center font-bold"
                          >
                            <Minus className="w-3 h-3" />
                          </button>
                          <span className="text-xs font-extrabold w-4 text-center dark:text-white">
                            {item.quantity}
                          </span>
                          <button
                            onClick={() => updateCartQuantity(item.product.id, 1)}
                            className="w-6 h-6 rounded-lg bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 flex items-center justify-center font-bold"
                          >
                            <Plus className="w-3 h-3" />
                          </button>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>

                {/* Price Details Breakdown */}
                <div className="bg-white dark:bg-[#1A1633] rounded-2xl p-3.5 border border-slate-100 dark:border-slate-800/80 shadow-xs space-y-2">
                  <h4 className="font-display font-bold text-xs text-slate-900 dark:text-white border-b border-slate-100 dark:border-slate-800 pb-2">
                    PRICE DETAILS ({cartItems.length} Items)
                  </h4>

                  <div className="flex items-center justify-between text-xs text-slate-600 dark:text-slate-400">
                    <span>Total MRP</span>
                    <span>₹{totalMRP.toLocaleString('en-IN')}</span>
                  </div>

                  <div className="flex items-center justify-between text-xs text-emerald-600 dark:text-emerald-400">
                    <span>Discount on MRP</span>
                    <span>-₹{totalDiscount.toLocaleString('en-IN')}</span>
                  </div>

                  {couponApplied && (
                    <div className="flex items-center justify-between text-xs text-emerald-600 dark:text-emerald-400">
                      <span>Coupon Discount (AARVO200)</span>
                      <span>-₹{couponDiscount.toLocaleString('en-IN')}</span>
                    </div>
                  )}

                  <div className="flex items-center justify-between text-xs text-slate-600 dark:text-slate-400">
                    <span>Delivery Charges</span>
                    <span className="text-emerald-600 dark:text-emerald-400 font-bold">FREE</span>
                  </div>

                  <div className="flex items-center justify-between text-xs text-slate-600 dark:text-slate-400">
                    <span>Platform Fee</span>
                    <span>₹{platformFee}</span>
                  </div>

                  <div className="flex items-center justify-between text-sm font-black text-slate-900 dark:text-white border-t border-slate-100 dark:border-slate-800 pt-2">
                    <span>Total Amount</span>
                    <span>₹{finalPayable.toLocaleString('en-IN')}</span>
                  </div>

                  <p className="text-[10px] text-emerald-600 dark:text-emerald-400 font-semibold pt-1">
                    🎉 You will save ₹{(totalDiscount + couponDiscount).toLocaleString('en-IN')} on this order!
                  </p>
                </div>

                {/* Place Order CTA Button */}
                <button
                  onClick={() => {
                    haptics.impact();
                    const orderId = `ARV-${Math.floor(100000 + Math.random() * 900000)}`;
                    const newOrder: Order = {
                      id: orderId,
                      date: 'Today, Just now',
                      total: finalPayable,
                      status: 'Processing',
                      statusStep: 1,
                      estimatedDelivery: 'Arriving in 2-3 Business Days',
                      trackingNumber: `AARVO-EXP-${Math.floor(1000000 + Math.random() * 9000000)}`,
                      carrier: 'AARVO Express FastTrack',
                      paymentMethod: 'UPI / Card Payment',
                      deliveryAddress: 'Flat 402, Green Glen Heights, Outer Ring Road, Bangalore - 560103',
                      items: cartItems.map((ci) => ({
                        id: ci.product.id,
                        name: ci.product.name,
                        price: ci.product.price,
                        quantity: ci.quantity,
                        image: ci.product.image,
                        category: ci.product.category,
                      })),
                      timeline: [
                        { status: 'Order Verified', time: 'Just now', description: 'Order received and verified by AARVO system', completed: true },
                        { status: 'Processing at Warehouse', time: 'In progress', description: 'Seller preparing package for courier handover', completed: false },
                        { status: 'Shipped', time: 'Pending', description: 'Will dispatch to local distribution center', completed: false },
                        { status: 'Delivered', time: 'Expected in 2-3 days', description: 'Doorstep delivery with OTP verification', completed: false },
                      ],
                    };
                    setOrders((prev) => [newOrder, ...prev]);
                    setOrderSuccessModal(orderId);
                    setCartItems([]);
                  }}
                  className="w-full py-3.5 bg-gradient-to-r from-[#2418D8] to-[#4514B8] text-white font-black text-sm rounded-2xl shadow-lg hover:opacity-95 active:scale-[0.98] transition-all flex items-center justify-center gap-2 cursor-pointer"
                >
                  <Package className="w-4 h-4" />
                  <span>Place Order &bull; ₹{finalPayable.toLocaleString('en-IN')}</span>
                </button>
              </div>
            )}
          </div>
        )}

        {/* ================================================================= */}
        {/* TAB 5: ACCOUNT VIEW                                               */}
        {/* ================================================================= */}
        {activeTab === 'account' && (
          <div className="flex-1 flex flex-col p-4 pb-6 space-y-4">
            {/* User Profile Card */}
            <div className="rounded-2xl bg-gradient-to-r from-[#2418D8] to-[#4B16D8] p-4 text-white shadow-md flex items-center gap-3">
              <div className="w-14 h-14 rounded-full bg-white/20 border-2 border-white flex items-center justify-center font-display font-black text-xl">
                SP
              </div>
              <div>
                <div className="flex items-center gap-1.5">
                  <h3 className="font-display font-black text-base">Saurav Paswan</h3>
                  <span className="bg-amber-400 text-slate-900 text-[9px] font-black px-1.5 py-0.5 rounded">
                    PRO
                  </span>
                </div>
                <p className="text-xs text-purple-200">+91 9876543210</p>
                <p className="text-[10px] text-emerald-300 font-semibold mt-0.5">
                  ✓ Verified Aarvo Shopper
                </p>
              </div>
            </div>

            {/* Quick Actions */}
            <div className="grid grid-cols-2 gap-2.5">
              <div className="bg-white dark:bg-[#1A1633] p-3 rounded-2xl border border-slate-100 dark:border-slate-800 shadow-xs">
                <span className="text-[10px] font-bold text-slate-400 dark:text-slate-500 uppercase">
                  SuperCoins
                </span>
                <p className="text-base font-black text-[#2418D8] dark:text-[#7B73FF] mt-0.5">🪙 240 Coins</p>
                <span className="text-[10px] text-emerald-600 dark:text-emerald-400 font-semibold">Worth ₹240</span>
              </div>
              <div className="bg-white dark:bg-[#1A1633] p-3 rounded-2xl border border-slate-100 dark:border-slate-800 shadow-xs">
                <span className="text-[10px] font-bold text-slate-400 dark:text-slate-500 uppercase">
                  Saved Wishlist
                </span>
                <p className="text-base font-black text-slate-800 dark:text-slate-100 mt-0.5">❤️ 5 Items</p>
                <span className="text-[10px] text-[#2418D8] dark:text-[#7B73FF] font-semibold">View Saved</span>
              </div>
            </div>

            {/* Theme Toggle Card in Account */}
            {onToggleTheme && (
              <div className="bg-white dark:bg-[#1A1633] rounded-2xl p-3.5 border border-slate-100 dark:border-slate-800 shadow-xs flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-xl bg-purple-50 dark:bg-purple-950/50 text-[#2418D8] dark:text-[#7B73FF] flex items-center justify-center">
                    {theme === 'dark' ? <Moon className="w-4 h-4" /> : <Sun className="w-4 h-4" />}
                  </div>
                  <div>
                    <h4 className="text-xs font-bold text-slate-900 dark:text-white">App Appearance</h4>
                    <p className="text-[10px] text-slate-500 dark:text-slate-400">
                      Currently using {theme === 'dark' ? 'Dark' : 'Light'} theme
                    </p>
                  </div>
                </div>
                <button
                  onClick={onToggleTheme}
                  className="px-3 py-1.5 bg-[#2418D8] text-white text-[11px] font-bold rounded-xl shadow-xs active:scale-95 transition-transform"
                >
                  {theme === 'dark' ? 'Switch Light' : 'Switch Dark'}
                </button>
              </div>
            )}

            {/* Account Settings List */}
            <div className="bg-white dark:bg-[#1A1633] rounded-2xl border border-slate-100 dark:border-slate-800 shadow-xs divide-y divide-slate-100 dark:divide-slate-800 text-xs font-semibold text-slate-700 dark:text-slate-200">
              <button
                onClick={() => {
                  haptics.selection();
                  setIsOrdersViewOpen(true);
                }}
                className="w-full px-4 py-3 text-left hover:bg-slate-50 dark:hover:bg-slate-800/40 flex items-center justify-between transition-colors cursor-pointer"
              >
                <div className="flex items-center gap-3">
                  <Package className="w-4 h-4 text-[#2418D8] dark:text-[#7B73FF]" />
                  <span>Orders &amp; Delivery Tracking</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="text-[10px] bg-blue-50 dark:bg-blue-950/60 text-[#2418D8] dark:text-[#7B73FF] font-bold px-2 py-0.5 rounded-full border border-blue-200 dark:border-blue-900/50">
                    {orders.filter((o) => o.status === 'Processing' || o.status === 'Shipped').length > 0
                      ? `${orders.filter((o) => o.status === 'Processing' || o.status === 'Shipped').length} Active`
                      : `${orders.length} Orders`}
                  </span>
                  <ChevronRight className="w-4 h-4 text-slate-400" />
                </div>
              </button>

              <button
                onClick={() => showToast(`You have ${wishlist.length} item(s) in your Wishlist ❤️`)}
                className="w-full px-4 py-3 text-left hover:bg-slate-50 dark:hover:bg-slate-800/40 flex items-center justify-between transition-colors"
              >
                <div className="flex items-center gap-3">
                  <Heart className="w-4 h-4 text-rose-500 fill-rose-500" />
                  <span>My Wishlist &amp; Saved Items</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="text-[10px] bg-rose-100 dark:bg-rose-950/60 text-rose-700 dark:text-rose-300 font-bold px-2 py-0.5 rounded-full">
                    {wishlist.length} items
                  </span>
                  <ChevronRight className="w-4 h-4 text-slate-400" />
                </div>
              </button>

              <button
                onClick={() => showToast('Saved address: New Delhi, 110001')}
                className="w-full px-4 py-3 text-left hover:bg-slate-50 dark:hover:bg-slate-800/40 flex items-center justify-between transition-colors"
              >
                <div className="flex items-center gap-3">
                  <MapPin className="w-4 h-4 text-[#2418D8] dark:text-[#7B73FF]" />
                  <span>Saved Delivery Addresses</span>
                </div>
                <ChevronRight className="w-4 h-4 text-slate-400" />
              </button>

              <button
                onClick={() => {
                  setSellerModalTab('overview');
                  setIsSellerModalOpen(true);
                }}
                className="w-full px-4 py-3 text-left hover:bg-slate-50 dark:hover:bg-slate-800/40 flex items-center justify-between transition-colors cursor-pointer"
              >
                <div className="flex items-center gap-3">
                  <Store className="w-4 h-4 text-amber-600 dark:text-amber-400" />
                  <div className="flex items-center gap-1.5">
                    <span>Seller Overview &amp; Hub</span>
                    {allProducts.filter((p) => (p.stockQuantity ?? 10) < 5).length > 0 && (
                      <span className="text-[9px] bg-red-100 dark:bg-red-950/60 text-red-600 dark:text-red-400 font-extrabold px-1.5 py-0.5 rounded-full">
                        {allProducts.filter((p) => (p.stockQuantity ?? 10) < 5).length} Low Stock
                      </span>
                    )}
                  </div>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="text-[10px] bg-amber-100 dark:bg-amber-950/60 text-amber-800 dark:text-amber-300 px-2 py-0.5 rounded-full font-bold">
                    Dashboard
                  </span>
                  <ChevronRight className="w-4 h-4 text-slate-400" />
                </div>
              </button>
            </div>

            {/* Logout button */}
            <button
              onClick={() => {
                if (onLogout) onLogout();
              }}
              className="w-full py-3 bg-rose-50 dark:bg-rose-950/30 hover:bg-rose-100 dark:hover:bg-rose-950/60 text-rose-600 dark:text-rose-400 font-bold text-xs rounded-2xl transition-colors text-center"
            >
              Sign Out / Switch Account
            </button>
          </div>
        )}
      </>
    )}

        {/* ================================================================= */}
        {/* 7. BOTTOM NAVIGATION BAR                                          */}
        {/* ================================================================= */}
        <nav className="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[440px] bg-white dark:bg-[#120F26] border-t border-slate-200/90 dark:border-slate-800 py-1.5 px-2 flex items-center justify-around shadow-2xl z-40 transition-colors duration-300">
          {/* 1. Home */}
          <button
            onClick={() => switchTab('home')}
            className={`flex flex-col items-center gap-0.5 text-[10px] font-bold transition-colors py-1 ${
              activeTab === 'home'
                ? 'text-[#2418D8] dark:text-[#7B73FF]'
                : 'text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-slate-200'
            }`}
          >
            <Home className="w-5 h-5 stroke-[2.2]" />
            <span className="whitespace-nowrap">Home</span>
          </button>

          {/* 2. Categories */}
          <button
            onClick={() => switchTab('categories')}
            className={`flex flex-col items-center gap-0.5 text-[10px] font-bold transition-colors py-1 ${
              activeTab === 'categories'
                ? 'text-[#2418D8] dark:text-[#7B73FF]'
                : 'text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-slate-200'
            }`}
          >
            <LayoutGrid className="w-5 h-5 stroke-[2.2]" />
            <span className="whitespace-nowrap">Categories</span>
          </button>

          {/* 3. Offers */}
          <button
            onClick={() => switchTab('offers')}
            className={`flex flex-col items-center gap-0.5 text-[10px] font-bold transition-colors py-1 ${
              activeTab === 'offers'
                ? 'text-[#2418D8] dark:text-[#7B73FF]'
                : 'text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-slate-200'
            }`}
          >
            <Tag className="w-5 h-5 stroke-[2.2]" />
            <span className="whitespace-nowrap">Offers</span>
          </button>

          {/* 4. Cart with live badge */}
          <button
            onClick={() => switchTab('cart')}
            className={`relative flex flex-col items-center gap-0.5 text-[10px] font-bold transition-colors py-1 ${
              activeTab === 'cart'
                ? 'text-[#2418D8] dark:text-[#7B73FF]'
                : 'text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-slate-200'
            }`}
          >
            <ShoppingCart className="w-5 h-5 stroke-[2.2]" />
            <span className="whitespace-nowrap">Cart</span>
            {cartItems.length > 0 && (
              <span className="absolute -top-1 right-1.5 px-1 min-w-[16px] h-4 bg-red-500 text-white text-[9px] font-extrabold rounded-full flex items-center justify-center">
                {cartItems.reduce((acc, item) => acc + item.quantity, 0)}
              </span>
            )}
          </button>

          {/* 5. Account */}
          <button
            onClick={() => switchTab('account')}
            className={`flex flex-col items-center gap-0.5 text-[10px] font-bold transition-colors py-1 ${
              activeTab === 'account'
                ? 'text-[#2418D8] dark:text-[#7B73FF]'
                : 'text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-slate-200'
            }`}
          >
            <User className="w-5 h-5 stroke-[2.2]" />
            <span className="whitespace-nowrap">Account</span>
          </button>
        </nav>

        {/* Subtle Non-Intrusive Offline Status Toast */}
        <OfflineStatus />

        {/* Floating ScrollToTop Action Button (>300px) */}
        <ScrollToTop threshold={300} />

        {/* ================================================================= */}
        {/* 8. SIDEBAR DRAWER (Hamburger Menu)                                */}
        {/* ================================================================= */}
        {isSidebarOpen && (
          <div className="fixed inset-0 z-50 flex">
            <div
              onClick={() => setIsSidebarOpen(false)}
              className="fixed inset-0 bg-black/60 backdrop-blur-xs transition-opacity"
            />

            <div className="relative w-72 max-w-[80%] bg-white dark:bg-[#14112B] text-slate-800 dark:text-slate-100 h-full shadow-2xl flex flex-col justify-between p-5 z-10 transition-colors">
              <div>
                <div className="flex items-center justify-between pb-4 border-b border-slate-100 dark:border-slate-800">
                  <div className="flex items-center gap-2">
                    <img
                      src="/src/assets/aarvo/aarvo_logo_transparent.png"
                      alt="AARVO Logo"
                      className="w-7 h-7 object-contain"
                    />
                    <span className="text-xl font-black font-display text-[#2418D8] dark:text-white">
                      AARVO
                    </span>
                  </div>
                  <button
                    onClick={() => setIsSidebarOpen(false)}
                    className="p-1 text-slate-400 hover:text-slate-800 dark:hover:text-white rounded-lg"
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>

                {/* Theme Switcher inside Drawer */}
                {onToggleTheme && (
                  <div className="my-3 p-3 rounded-xl bg-purple-50/70 dark:bg-purple-950/30 border border-purple-100 dark:border-purple-900/40 flex items-center justify-between">
                    <div className="flex items-center gap-2.5">
                      {theme === 'dark' ? (
                        <Moon className="w-4 h-4 text-purple-400" />
                      ) : (
                        <Sun className="w-4 h-4 text-amber-500" />
                      )}
                      <div>
                        <p className="text-xs font-bold text-slate-800 dark:text-slate-200">
                          {theme === 'dark' ? 'Dark Mode' : 'Light Mode'}
                        </p>
                        <p className="text-[10px] text-slate-500 dark:text-slate-400">
                          Click to toggle theme
                        </p>
                      </div>
                    </div>
                    <button
                      onClick={onToggleTheme}
                      className="px-2.5 py-1 text-[11px] font-bold rounded-lg bg-[#2418D8] text-white shadow-xs active:scale-95 transition-transform"
                    >
                      Toggle
                    </button>
                  </div>
                )}

                <div className="py-2 space-y-1 text-xs font-semibold text-slate-700 dark:text-slate-200">
                  <button
                    onClick={() => {
                      setIsSidebarOpen(false);
                      setActiveTab('home');
                    }}
                    className="w-full text-left py-2.5 px-3 rounded-xl hover:bg-purple-50 dark:hover:bg-purple-950/40 hover:text-[#2418D8] dark:hover:text-[#7B73FF] transition-colors flex items-center gap-3"
                  >
                    <Home className="w-4 h-4 text-[#2418D8] dark:text-[#7B73FF]" />
                    <span>Home Marketplace</span>
                  </button>

                  <button
                    onClick={() => {
                      setIsSidebarOpen(false);
                      setActiveTab('categories');
                    }}
                    className="w-full text-left py-2.5 px-3 rounded-xl hover:bg-purple-50 dark:hover:bg-purple-950/40 hover:text-[#2418D8] dark:hover:text-[#7B73FF] transition-colors flex items-center gap-3"
                  >
                    <LayoutGrid className="w-4 h-4 text-[#2418D8] dark:text-[#7B73FF]" />
                    <span>All Categories</span>
                  </button>

                  <button
                    onClick={() => {
                      setIsSidebarOpen(false);
                      setActiveTab('offers');
                    }}
                    className="w-full text-left py-2.5 px-3 rounded-xl hover:bg-purple-50 dark:hover:bg-purple-950/40 hover:text-[#2418D8] dark:hover:text-[#7B73FF] transition-colors flex items-center gap-3"
                  >
                    <Tag className="w-4 h-4 text-[#2418D8] dark:text-[#7B73FF]" />
                    <span>Deals &amp; Offers</span>
                  </button>

                  <button
                    onClick={() => {
                      haptics.selection();
                      setIsSidebarOpen(false);
                      setIsOrdersViewOpen(true);
                    }}
                    className="w-full text-left py-2.5 px-3 rounded-xl hover:bg-blue-50 dark:hover:bg-blue-950/40 hover:text-[#2418D8] dark:hover:text-[#7B73FF] transition-colors flex items-center justify-between cursor-pointer"
                  >
                    <div className="flex items-center gap-3">
                      <Package className="w-4 h-4 text-[#2418D8] dark:text-[#7B73FF]" />
                      <span>Orders &amp; Delivery</span>
                    </div>
                    <span className="text-[10px] font-bold bg-blue-100 dark:bg-blue-950 text-[#2418D8] dark:text-[#7B73FF] px-2 py-0.5 rounded-full">
                      {orders.length}
                    </span>
                  </button>

                  <button
                    onClick={() => {
                      setIsSidebarOpen(false);
                      showToast(`You have ${wishlist.length} item(s) in your Wishlist ❤️`);
                    }}
                    className="w-full text-left py-2.5 px-3 rounded-xl hover:bg-rose-50 dark:hover:bg-rose-950/40 hover:text-rose-600 dark:hover:text-rose-400 transition-colors flex items-center justify-between"
                  >
                    <div className="flex items-center gap-3">
                      <Heart className="w-4 h-4 text-rose-500 fill-rose-500" />
                      <span>My Wishlist</span>
                    </div>
                    <span className="text-[10px] font-bold bg-rose-100 dark:bg-rose-950 text-rose-600 dark:text-rose-300 px-2 py-0.5 rounded-full">
                      {wishlist.length}
                    </span>
                  </button>

                  <button
                    onClick={() => {
                      setIsSidebarOpen(false);
                      setIsSellerModalOpen(true);
                    }}
                    className="w-full text-left py-2.5 px-3 rounded-xl hover:bg-amber-50 dark:hover:bg-amber-950/40 hover:text-amber-800 dark:hover:text-amber-300 transition-colors flex items-center gap-3"
                  >
                    <Store className="w-4 h-4 text-amber-600 dark:text-amber-400" />
                    <span>Seller Onboarding Hub</span>
                  </button>
                </div>
              </div>

              <div className="pt-4 border-t border-slate-100 dark:border-slate-800">
                <button
                  onClick={() => {
                    setIsSidebarOpen(false);
                    if (onLogout) onLogout();
                  }}
                  className="w-full py-2.5 px-3 rounded-xl bg-slate-100 dark:bg-slate-800 hover:bg-rose-50 dark:hover:bg-rose-950/40 hover:text-rose-600 dark:hover:text-rose-400 text-slate-700 dark:text-slate-300 font-bold text-xs transition-colors text-center"
                >
                  Logout / Switch Account
                </button>
                <p className="text-[10px] text-slate-400 dark:text-slate-500 text-center mt-2">
                  AARVO v2.2 &bull; Shop Smart, Live Better
                </p>
              </div>
            </div>
          </div>
        )}

        {/* ================================================================= */}
        {/* 9. VOICE SEARCH MODAL (Interactive)                               */}
        {/* ================================================================= */}
        {isVoiceModalOpen && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <div
              onClick={() => setIsVoiceModalOpen(false)}
              className="fixed inset-0 bg-black/60 backdrop-blur-xs"
            />
            <div className="relative w-full max-w-xs bg-white dark:bg-[#181436] text-slate-900 dark:text-slate-100 border border-slate-100 dark:border-slate-800 rounded-3xl p-6 shadow-2xl z-10 text-center flex flex-col items-center">
              <button
                onClick={() => setIsVoiceModalOpen(false)}
                className="absolute top-4 right-4 p-1 text-slate-400 hover:text-slate-800 dark:hover:text-white rounded-lg"
              >
                <X className="w-5 h-5" />
              </button>

              <div className="w-16 h-16 rounded-full bg-blue-50 dark:bg-blue-950/40 text-[#2418D8] dark:text-[#7B73FF] flex items-center justify-center mb-4 relative">
                <div className="absolute inset-0 rounded-full border-2 border-[#2418D8] animate-ping opacity-30" />
                <Mic className="w-8 h-8 text-[#2418D8] dark:text-[#7B73FF]" />
              </div>

              <h3 className="font-display font-black text-base text-slate-900 dark:text-white mb-1">
                Listening...
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400 mb-4">
                Say product name, brand or category
              </p>

              <div className="flex gap-2">
                <button
                  onClick={() => {
                    setSearchQuery('Smartphones');
                    setIsVoiceModalOpen(false);
                    setActiveTab('categories');
                  }}
                  className="px-3 py-1.5 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 rounded-lg text-xs font-semibold text-slate-700 dark:text-slate-300"
                >
                  &ldquo;Smartphones&rdquo;
                </button>
                <button
                  onClick={() => {
                    setSearchQuery('Kurti');
                    setIsVoiceModalOpen(false);
                    setActiveTab('categories');
                  }}
                  className="px-3 py-1.5 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 rounded-lg text-xs font-semibold text-slate-700 dark:text-slate-300"
                >
                  &ldquo;Fashion&rdquo;
                </button>
              </div>
            </div>
          </div>
        )}

        {/* ================================================================= */}
        {/* 10. NOTIFICATIONS MODAL                                            */}
        {/* ================================================================= */}
        {isNotificationModalOpen && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <div
              onClick={() => setIsNotificationModalOpen(false)}
              className="fixed inset-0 bg-black/60 backdrop-blur-xs"
            />
            <div className="relative w-full max-w-sm bg-white dark:bg-[#181436] text-slate-900 dark:text-slate-100 border border-slate-100 dark:border-slate-800 rounded-3xl p-5 shadow-2xl z-10">
              <div className="flex items-center justify-between pb-3 border-b border-slate-100 dark:border-slate-800 mb-3">
                <div className="flex items-center gap-2">
                  <Bell className="w-5 h-5 text-[#2418D8] dark:text-[#7B73FF]" />
                  <h3 className="font-display font-black text-sm text-slate-900 dark:text-white">
                    Notifications
                  </h3>
                </div>
                <button
                  onClick={() => setIsNotificationModalOpen(false)}
                  className="p-1 text-slate-400 hover:text-slate-800 dark:hover:text-white rounded-lg"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              <div className="space-y-2.5">
                <div className="p-3 bg-blue-50/60 dark:bg-blue-950/40 rounded-xl border border-blue-100 dark:border-blue-900/50">
                  <span className="text-[10px] font-bold text-[#2418D8] dark:text-[#7B73FF]">
                    MEGA SALE IS LIVE
                  </span>
                  <p className="text-xs font-semibold text-slate-800 dark:text-slate-200 mt-0.5">
                    Exclusive discounts on electronics &amp; fashion up to 70% off.
                  </p>
                  <span className="text-[9px] text-slate-400 dark:text-slate-500 mt-1 block">10 mins ago</span>
                </div>
                <div className="p-3 bg-slate-50 dark:bg-slate-800/40 rounded-xl border border-slate-100 dark:border-slate-800">
                  <span className="text-[10px] font-bold text-emerald-600 dark:text-emerald-400">
                    DELIVERY UPDATE
                  </span>
                  <p className="text-xs font-semibold text-slate-800 dark:text-slate-200 mt-0.5">
                    Your shipment #ARV-88219 is out for delivery.
                  </p>
                  <span className="text-[9px] text-slate-400 dark:text-slate-500 mt-1 block">2 hours ago</span>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* ================================================================= */}
        {/* 11. ORDER SUCCESS CELEBRATION MODAL                               */}
        {/* ================================================================= */}
        {orderSuccessModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <div
              onClick={() => setOrderSuccessModal(null)}
              className="fixed inset-0 bg-black/60 backdrop-blur-xs"
            />
            <div className="relative w-full max-w-sm bg-white dark:bg-[#181436] text-slate-900 dark:text-slate-100 border border-slate-100 dark:border-slate-800 rounded-3xl p-6 shadow-2xl z-10 text-center">
              <div className="w-16 h-16 rounded-full bg-emerald-100 dark:bg-emerald-950/50 text-emerald-600 dark:text-emerald-400 flex items-center justify-center mx-auto mb-3">
                <CheckCircle2 className="w-10 h-10 stroke-[2.5]" />
              </div>
              <h3 className="font-display font-black text-lg text-slate-900 dark:text-white">
                Order Placed Successfully!
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
                Order ID: <strong className="text-slate-900 dark:text-white font-mono">{orderSuccessModal}</strong>
              </p>
              <p className="text-xs text-slate-600 dark:text-slate-300 mt-2 bg-slate-50 dark:bg-slate-800/60 p-2.5 rounded-xl border border-slate-100 dark:border-slate-800">
                Your order is confirmed and verified. We will send tracking updates to +91 9876543210.
              </p>

              <div className="flex flex-col gap-2 mt-4">
                <button
                  onClick={() => {
                    haptics.selection();
                    setOrderSuccessModal(null);
                    setIsOrdersViewOpen(true);
                  }}
                  className="w-full py-3 bg-[#2418D8] text-white font-bold text-xs rounded-xl shadow-md hover:bg-[#1A10B3] active:scale-95 transition-all flex items-center justify-center gap-2 cursor-pointer"
                >
                  <Truck className="w-4 h-4" />
                  <span>Track Order in My Orders</span>
                </button>

                <button
                  onClick={() => {
                    setOrderSuccessModal(null);
                    setActiveTab('home');
                  }}
                  className="w-full py-2.5 bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 text-slate-700 dark:text-slate-300 font-bold text-xs rounded-xl active:scale-95 transition-all cursor-pointer"
                >
                  Continue Shopping
                </button>
              </div>
            </div>
          </div>
        )}

        {/* ================================================================= */}
        {/* 12. SELLER OVERVIEW & ONBOARDING MODAL                            */}
        {/* ================================================================= */}
        {isSellerModalOpen && (() => {
          const lowStockProducts = allProducts.filter((p) => (p.stockQuantity ?? 10) < 5);
          const lowStockCount = lowStockProducts.length;

          return (
            <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
              <div
                onClick={() => setIsSellerModalOpen(false)}
                className="fixed inset-0 bg-black/60 backdrop-blur-xs"
              />
              <div className="relative w-full max-w-md max-h-[92vh] overflow-y-auto bg-white dark:bg-[#181436] text-slate-900 dark:text-slate-100 border border-slate-100 dark:border-slate-800 rounded-3xl p-5 shadow-2xl z-10">
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center gap-2">
                    <div className="w-8 h-8 rounded-xl bg-amber-50 dark:bg-amber-950/40 text-amber-600 dark:text-amber-400 flex items-center justify-center">
                      <Store className="w-5 h-5" />
                    </div>
                    <div>
                      <h3 className="font-display font-black text-base text-slate-900 dark:text-white leading-tight">
                        AARVO Seller Hub
                      </h3>
                      <p className="text-[10px] text-slate-500 dark:text-slate-400">
                        Seller Overview &amp; Inventory Management
                      </p>
                    </div>
                  </div>
                  <button
                    onClick={() => setIsSellerModalOpen(false)}
                    className="p-1 text-slate-400 hover:text-slate-800 dark:hover:text-white rounded-lg cursor-pointer"
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>

                {/* Tabs: Seller Overview & Merchant Registration */}
                <div className="flex bg-slate-100 dark:bg-slate-800/80 p-1 rounded-xl mb-3.5">
                  <button
                    type="button"
                    onClick={() => setSellerModalTab('overview')}
                    className={`flex-1 py-1.5 text-xs font-bold rounded-lg transition-all cursor-pointer ${
                      sellerModalTab === 'overview'
                        ? 'bg-white dark:bg-[#2418D8] text-slate-900 dark:text-white shadow-xs'
                        : 'text-slate-500 hover:text-slate-900 dark:text-slate-400'
                    }`}
                  >
                    Seller Overview
                  </button>
                  <button
                    type="button"
                    onClick={() => setSellerModalTab('register')}
                    className={`flex-1 py-1.5 text-xs font-bold rounded-lg transition-all cursor-pointer ${
                      sellerModalTab === 'register'
                        ? 'bg-white dark:bg-[#2418D8] text-slate-900 dark:text-white shadow-xs'
                        : 'text-slate-500 hover:text-slate-900 dark:text-slate-400'
                    }`}
                  >
                    Merchant Onboarding
                  </button>
                </div>

                {sellerModalTab === 'overview' ? (
                  <div className="space-y-3.5">
                    {/* Top Stat Cards in Seller Overview */}
                    <div className="grid grid-cols-3 gap-2">
                      <div className="bg-slate-50 dark:bg-slate-800/50 p-2.5 rounded-2xl border border-slate-200/80 dark:border-slate-700/60 text-center">
                        <span className="text-[10px] font-bold text-slate-500 dark:text-slate-400 block">Catalog</span>
                        <span className="text-xl font-black text-slate-900 dark:text-white">{allProducts.length}</span>
                        <span className="text-[9px] text-slate-400 block">Products</span>
                      </div>

                      <div className="bg-slate-50 dark:bg-slate-800/50 p-2.5 rounded-2xl border border-slate-200/80 dark:border-slate-700/60 text-center">
                        <span className="text-[10px] font-bold text-slate-500 dark:text-slate-400 block">Orders</span>
                        <span className="text-xl font-black text-slate-900 dark:text-white">{orders.length}</span>
                        <span className="text-[9px] text-emerald-500 font-semibold block">Fulfilment</span>
                      </div>

                      {/* LOW STOCK ALERT CARD */}
                      <div
                        className={`p-2.5 rounded-2xl border transition-all text-center ${
                          lowStockCount > 0
                            ? 'bg-amber-50/90 dark:bg-amber-950/40 border-amber-300 dark:border-amber-700/80 ring-2 ring-amber-400/30'
                            : 'bg-emerald-50/90 dark:bg-emerald-950/40 border-emerald-300 dark:border-emerald-800'
                        }`}
                      >
                        <div className="flex items-center justify-center gap-1 text-[10px] font-bold text-amber-700 dark:text-amber-300">
                          <AlertTriangle className="w-3 h-3 text-amber-600 dark:text-amber-400 shrink-0" />
                          <span>Low Stock</span>
                        </div>
                        <span
                          className={`text-xl font-black block mt-0.5 ${
                            lowStockCount > 0
                              ? 'text-red-600 dark:text-red-400'
                              : 'text-emerald-600 dark:text-emerald-400'
                          }`}
                        >
                          {lowStockCount}
                        </span>
                        <span className="text-[9px] font-bold text-amber-600 dark:text-amber-400 block truncate">
                          &lt; 5 units left
                        </span>
                      </div>
                    </div>

                    {/* Low Stock Alert Banner */}
                    {lowStockCount > 0 ? (
                      <div className="p-3 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900/50 rounded-2xl flex items-start gap-2.5">
                        <AlertTriangle className="w-4 h-4 text-red-600 dark:text-red-400 shrink-0 mt-0.5" />
                        <div>
                          <h4 className="text-xs font-bold text-red-800 dark:text-red-300">
                            Low Stock Alert: {lowStockCount} Product{lowStockCount > 1 ? 's' : ''} Need Attention
                          </h4>
                          <p className="text-[11px] text-red-700/80 dark:text-red-400 mt-0.5 leading-tight">
                            Products with less than 5 units remaining in the catalog are at risk of stockout. Restock below to keep listings active.
                          </p>
                        </div>
                      </div>
                    ) : (
                      <div className="p-3 bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-900/50 rounded-2xl flex items-center gap-2">
                        <CheckCircle2 className="w-4 h-4 text-emerald-600 dark:text-emerald-400 shrink-0" />
                        <span className="text-xs font-bold text-emerald-800 dark:text-emerald-300">
                          Healthy Inventory: All products have 5 or more units remaining.
                        </span>
                      </div>
                    )}

                    {/* Catalog Low Stock Products List */}
                    <div className="space-y-2">
                      <div className="flex items-center justify-between text-xs font-bold text-slate-700 dark:text-slate-300">
                        <span>Items with &lt; 5 units remaining</span>
                        <span className="text-[10px] bg-red-100 dark:bg-red-950/60 text-red-600 dark:text-red-400 font-extrabold px-1.5 py-0.5 rounded-full">
                          {lowStockCount} Alert{lowStockCount !== 1 ? 's' : ''}
                        </span>
                      </div>

                      <div className="space-y-1.5 max-h-48 overflow-y-auto pr-0.5">
                        {lowStockProducts.map((product) => (
                          <div
                            key={product.id}
                            className="flex items-center justify-between gap-2 p-2 rounded-xl bg-amber-50/50 dark:bg-amber-950/20 border border-amber-200/70 dark:border-amber-900/40 text-xs"
                          >
                            <div className="flex items-center gap-2 min-w-0">
                              <img
                                src={product.image}
                                alt={product.name}
                                className="w-9 h-9 rounded-lg object-cover shrink-0 border border-amber-200/50"
                              />
                              <div className="min-w-0">
                                <p className="font-bold text-slate-900 dark:text-white truncate text-[11px]">
                                  {product.name}
                                </p>
                                <div className="flex items-center gap-1.5 text-[10px]">
                                  <span className="text-slate-500 dark:text-slate-400">{product.category}</span>
                                  <span className="text-slate-300">•</span>
                                  <span className="font-extrabold text-red-600 dark:text-red-400">
                                    {product.stockQuantity} remaining
                                  </span>
                                </div>
                              </div>
                            </div>
                            <button
                              type="button"
                              onClick={() => restockProduct(product.id, 10)}
                              className="px-2.5 py-1 bg-[#2418D8] hover:bg-[#1A10B3] text-white text-[10px] font-bold rounded-lg shadow-xs active:scale-95 transition-all shrink-0 cursor-pointer"
                            >
                              +10 Restock
                            </button>
                          </div>
                        ))}

                        {lowStockCount === 0 && (
                          <div className="p-3 text-center text-xs text-slate-500 dark:text-slate-400 bg-slate-50 dark:bg-slate-800/40 rounded-xl">
                            All products are well-stocked. No replenishment needed!
                          </div>
                        )}
                      </div>
                    </div>

                    {/* Link to Dedicated Web Seller Central */}
                    <div className="pt-2 border-t border-slate-100 dark:border-slate-800/80">
                      <a
                        href="/seller-panel/index.html"
                        className="w-full py-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 dark:bg-purple-700 dark:hover:bg-purple-600 text-white font-bold text-xs shadow-md flex items-center justify-center gap-2 transition-all active:scale-95 cursor-pointer"
                      >
                        <Store className="w-3.5 h-3.5" />
                        <span>Open Web Seller Central Dashboard</span>
                      </a>
                    </div>
                  </div>
                ) : !sellerRegistered ? (
                  <form
                    onSubmit={(e) => {
                      e.preventDefault();
                      setSellerRegistered(true);
                    }}
                    className="space-y-3"
                  >
                    <p className="text-xs text-slate-600 dark:text-slate-400">
                      Register your merchant business to start selling products on AARVO with 0% platform commission.
                    </p>

                    <div>
                      <label className="text-[11px] font-bold text-slate-700 dark:text-slate-300 block mb-1">
                        Business / Store Name
                      </label>
                      <input
                        type="text"
                        required
                        placeholder="e.g. Royal Apparel Ltd."
                        value={sellerForm.business}
                        onChange={(e) => setSellerForm({ ...sellerForm, business: e.target.value })}
                        className="w-full px-3 py-2 text-xs bg-[#F7F5FF] dark:bg-[#120F26] text-slate-900 dark:text-white border border-purple-100 dark:border-slate-700 rounded-xl outline-none focus:border-[#2418D8]"
                      />
                    </div>

                    <div>
                      <label className="text-[11px] font-bold text-slate-700 dark:text-slate-300 block mb-1">
                        Primary Category
                      </label>
                      <select
                        value={sellerForm.category}
                        onChange={(e) => setSellerForm({ ...sellerForm, category: e.target.value })}
                        className="w-full px-3 py-2 text-xs bg-[#F7F5FF] dark:bg-[#120F26] text-slate-900 dark:text-white border border-purple-100 dark:border-slate-700 rounded-xl outline-none focus:border-[#2418D8]"
                      >
                        <option value="Fashion">Fashion &amp; Apparel</option>
                        <option value="Mobiles">Mobiles &amp; Gadgets</option>
                        <option value="Electronics">Electronics</option>
                        <option value="Home">Home &amp; Living</option>
                        <option value="Grocery">Grocery &amp; Staples</option>
                      </select>
                    </div>

                    <div>
                      <label className="text-[11px] font-bold text-slate-700 dark:text-slate-300 block mb-1">
                        Mobile Number
                      </label>
                      <input
                        type="tel"
                        maxLength={10}
                        required
                        placeholder="10-digit mobile number"
                        value={sellerForm.phone}
                        onChange={(e) => setSellerForm({ ...sellerForm, phone: e.target.value })}
                        className="w-full px-3 py-2 text-xs bg-[#F7F5FF] dark:bg-[#120F26] text-slate-900 dark:text-white border border-purple-100 dark:border-slate-700 rounded-xl outline-none focus:border-[#2418D8] font-mono"
                      />
                    </div>

                    <button
                      type="submit"
                      className="w-full py-2.5 rounded-xl bg-[#2418D8] hover:bg-[#1A10B3] text-white font-bold text-xs shadow-md mt-2 transition-all active:scale-95"
                    >
                      Submit Seller Application
                    </button>
                  </form>
                ) : (
                  <div className="text-center py-4 space-y-2">
                    <CheckCircle2 className="w-12 h-12 text-emerald-500 mx-auto" />
                    <h4 className="font-bold text-slate-900 dark:text-white text-sm">
                      Application Received!
                    </h4>
                    <p className="text-xs text-slate-600 dark:text-slate-300">
                      Welcome <strong className="text-[#2418D8] dark:text-[#7B73FF]">{sellerForm.business || 'Merchant'}</strong>! Our verification team will review your GST &amp; inventory within 24 hours.
                    </p>
                    <button
                      onClick={() => {
                        setSellerRegistered(false);
                        setIsSellerModalOpen(false);
                      }}
                      className="mt-3 px-4 py-1.5 rounded-xl bg-slate-900 dark:bg-purple-600 text-white text-xs font-bold"
                    >
                      Close
                    </button>
                  </div>
                )}
              </div>
            </div>
          );
        })()}

        {/* ================================================================= */}
        {/* 13. PRODUCT DETAIL MODAL (Larger images, description, buy/cart)   */}
        {/* ================================================================= */}
        <ProductDetailModal
          product={selectedProduct}
          isOpen={!!selectedProduct}
          onClose={() => setSelectedProduct(null)}
          onAddToCart={(prod) => addToCart(prod)}
          isWishlisted={selectedProduct ? wishlist.includes(selectedProduct.id) : false}
          onToggleWishlist={toggleWishlist}
          onBuyNow={(prod) => {
            addToCart(prod);
            setSelectedProduct(null);
            switchTab('cart');
          }}
        />

      </div>
    </div>
  );
};
