import React, { useState, useEffect, useRef, useCallback, useMemo } from 'react';
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
  RotateCcw,
  Wifi,
  WifiOff,
  Banknote,
  Download,
  Upload,
  Copy,
  HelpCircle,
  MessageSquare,
  Edit2,
  SlidersHorizontal,
  ChevronDown,
  LocateFixed,
} from 'lucide-react';
import { ScrollToTop } from './ScrollToTop';
import { MainScreenSkeleton } from './MainScreenSkeleton';
import { ProductGridSkeleton } from './ProductGridSkeleton';
import { ToastContainer, ToastItem, ToastVariant } from './ToastContainer';
import { ProductDetailModal, Product } from './ProductDetailModal';
import { Product360ViewerModal } from './Product360ViewerModal';
import { ProductImageZoomModal } from './ProductImageZoomModal';
import { Order, OrderItem } from './OrdersSection';
import { Orders } from './Orders';
import { AarvoAnimatedLogo } from './AarvoAnimatedLogo';
import { haptics } from '../utils/haptics';
import catFashionBagImg from '../assets/images/cat_fashion_bag_1794533648322.jpg';
import catMobilesPhoneImg from '../assets/images/cat_mobiles_phone_1794533648438.jpg';
import catElectronicsLaptopImg from '../assets/images/cat_electronics_laptop_1794533650182.jpg';
import catHomeBlenderImg from '../assets/images/cat_home_blender_1794533649563.jpg';
import catBeautySerumImg from '../assets/images/cat_beauty_serum_1794533649077.jpg';
import heroBigSavingsWomanImg from '../assets/images/hero_big_savings_woman_1794533651427.jpg';
import promoGadgetsTrioImg from '../assets/images/promo_gadgets_trio_1794533651323.jpg';
import prodWhiteSneakersImg from '../assets/images/prod_white_sneakers_1794533652181.jpg';
import prodNavyBackpackImg from '../assets/images/prod_navy_backpack_1794533651856.jpg';
import {
  PRODUCT_SORT_OPTIONS,
  ProductSortOption,
  matchAndScoreProducts,
  sortProducts,
} from '../utils/productSearchSort';
import {
  checkShiprocketServiceability,
  ShiprocketServiceabilityResult,
} from '../utils/shiprocketServiceability';

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
  const [sortBy, setSortBy] = useState<ProductSortOption>('relevance');
  const [currentSlide, setCurrentSlide] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [isVoiceModalOpen, setIsVoiceModalOpen] = useState(false);
  const [isVoiceListening, setIsVoiceListening] = useState(false);
  const [voiceTranscript, setVoiceTranscript] = useState('');
  const [voiceError, setVoiceError] = useState<string | null>(null);
  const recognitionRef = useRef<any>(null);

  const startVoiceSearch = useCallback(() => {
    setVoiceError(null);
    setVoiceTranscript('');
    const SpeechRecognition =
      (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;

    if (!SpeechRecognition) {
      setVoiceError('Voice recognition is not supported in this browser. Please type to search.');
      return;
    }

    try {
      if (recognitionRef.current) {
        recognitionRef.current.abort();
      }

      const recognition = new SpeechRecognition();
      recognition.lang = 'en-IN';
      recognition.continuous = false;
      recognition.interimResults = true;
      recognition.maxAlternatives = 1;

      recognition.onstart = () => {
        setIsVoiceListening(true);
        setVoiceError(null);
      };

      recognition.onresult = (event: any) => {
        const transcript = Array.from(event.results)
          .map((result: any) => result[0].transcript)
          .join('');
        setVoiceTranscript(transcript);

        if (event.results[0] && event.results[0].isFinal) {
          const finalClean = transcript.trim();
          if (finalClean) {
            setSearchQuery(finalClean);
            haptics.success();
            setTimeout(() => {
              setIsVoiceModalOpen(false);
              setIsVoiceListening(false);
            }, 750);
          }
        }
      };

      recognition.onerror = (event: any) => {
        if (event.error === 'not-allowed') {
          setVoiceError('Microphone permission denied. Please allow microphone access in your browser settings.');
        } else if (event.error !== 'no-speech') {
          setVoiceError(`Voice recognition: ${event.error || 'Could not understand audio'}`);
        }
        setIsVoiceListening(false);
      };

      recognition.onend = () => {
        setIsVoiceListening(false);
      };

      recognitionRef.current = recognition;
      recognition.start();
    } catch (_err) {
      setVoiceError('Microphone access could not be initialized.');
      setIsVoiceListening(false);
    }
  }, []);

  const stopVoiceSearch = useCallback(() => {
    if (recognitionRef.current) {
      recognitionRef.current.stop();
    }
    setIsVoiceListening(false);
  }, []);

  useEffect(() => {
    if (isVoiceModalOpen) {
      startVoiceSearch();
    } else {
      if (recognitionRef.current) {
        recognitionRef.current.abort();
      }
      setIsVoiceListening(false);
      setVoiceTranscript('');
      setVoiceError(null);
    }
  }, [isVoiceModalOpen, startVoiceSearch]);
  const [isSellerModalOpen, setIsSellerModalOpen] = useState(false);
  const [sellerModalTab, setSellerModalTab] = useState<'overview' | 'add-product' | 'register'>('overview');
  const [sellerRegistered, setSellerRegistered] = useState(false);
  const [sellerForm, setSellerForm] = useState({ business: '', category: 'Fashion', phone: '' });

  // Luxury Add Product Form States
  const [newProductName, setNewProductName] = useState('');
  const [newProductCategory, setNewProductCategory] = useState('Fashion');
  const [customCategory, setCustomCategory] = useState('');
  const [newProductPrice, setNewProductPrice] = useState('');
  const [newProductMRP, setNewProductMRP] = useState('');
  const [newProductStock, setNewProductStock] = useState('25');
  const [newProductDescription, setNewProductDescription] = useState('');
  const [newProductImage, setNewProductImage] = useState('https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=500&auto=format&fit=crop&q=80');
  const [selectedSizes, setSelectedSizes] = useState<string[]>([]);
  const [customSizeText, setCustomSizeText] = useState('');
  const [selectedColors, setSelectedColors] = useState<string[]>([]);
  const [customColorText, setCustomColorText] = useState('');
  const [selectedFabric, setSelectedFabric] = useState('');
  const [codAllowed, setCodAllowed] = useState(true);
  const [selectedReturnPolicy, setSelectedReturnPolicy] = useState('7 Days Return');
  const [isNotificationModalOpen, setIsNotificationModalOpen] = useState(false);
  const [orderSuccessModal, setOrderSuccessModal] = useState<string | null>(null);
  const [appliedCouponCode, setAppliedCouponCode] = useState<'AARVO200' | 'WELCOME100' | null>(null);
  const [couponInputCode, setCouponInputCode] = useState('');
  const [toasts, setToasts] = useState<ToastItem[]>([]);
  const toastTimersRef = useRef<Record<string, ReturnType<typeof setTimeout>>>({});
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);
  const [product360Modal, setProduct360Modal] = useState<Product | null>(null);
  const [zoomImageProduct, setZoomImageProduct] = useState<Product | null>(null);
  const bulkCsvInputRef = useRef<HTMLInputElement | null>(null);

  // Price Range Filter state (₹)
  const [minPriceFilter, setMinPriceFilter] = useState<number | null>(null);
  const [maxPriceFilter, setMaxPriceFilter] = useState<number | null>(null);
  const [isPriceFilterModalOpen, setIsPriceFilterModalOpen] = useState<boolean>(false);
  const [priceMinDraft, setPriceMinDraft] = useState<string>('');
  const [priceMaxDraft, setPriceMaxDraft] = useState<string>('');

  // Interactive Account Modals & Persistent User State
  const [userProfile, setUserProfile] = useState<{ name: string; phone: string }>(() => {
    try {
      const saved = localStorage.getItem('aarvo_profile');
      if (saved) return JSON.parse(saved);
    } catch {
      // safe fallback
    }
    return { name: 'AARVO Shopper', phone: '' };
  });

  useEffect(() => {
    try {
      localStorage.setItem('aarvo_profile', JSON.stringify(userProfile));
    } catch {
      // safe catch
    }
  }, [userProfile]);

  interface SavedAddress {
    id: string;
    label: string;
    fullName: string;
    phone: string;
    addressLine: string;
    city: string;
    pincode: string;
  }

  const [savedAddresses, setSavedAddresses] = useState<SavedAddress[]>(() => {
    try {
      const saved = localStorage.getItem('aarvo_addresses');
      if (saved) {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed) && parsed.length > 0) return parsed;
      }
    } catch {
      // safe fallback
    }
    return [
      {
        id: 'addr-1',
        label: 'Home',
        fullName: 'AARVO Shopper',
        phone: '',
        addressLine: 'Select or add your doorstep address',
        city: 'Patna',
        pincode: '800001',
      },
    ];
  });

  const [activeAddressId, setActiveAddressId] = useState<string>(() => {
    try {
      return localStorage.getItem('aarvo_active_address_id') || 'addr-1';
    } catch {
      return 'addr-1';
    }
  });

  useEffect(() => {
    try {
      localStorage.setItem('aarvo_addresses', JSON.stringify(savedAddresses));
      localStorage.setItem('aarvo_active_address_id', activeAddressId);
    } catch {
      // safe catch
    }
  }, [savedAddresses, activeAddressId]);

  const activeAddressObj =
    savedAddresses.find((a) => a.id === activeAddressId) || savedAddresses[0];
  const activeAddressString = activeAddressObj
    ? `${activeAddressObj.fullName}, ${activeAddressObj.addressLine}, ${activeAddressObj.city} - ${activeAddressObj.pincode}`
    : 'Patna - 800001';

  const [isEditProfileOpen, setIsEditProfileOpen] = useState(false);
  const [profileDraftName, setProfileDraftName] = useState('');
  const [profileDraftPhone, setProfileDraftPhone] = useState('');
  const [isAddressModalOpen, setIsAddressModalOpen] = useState(false);
  const [quickPincodeInput, setQuickPincodeInput] = useState('');
  const [pincodeStatusMessage, setPincodeStatusMessage] = useState<string | null>(null);
  const [isDetectingLocation, setIsDetectingLocation] = useState(false);
  const [editingAddressId, setEditingAddressId] = useState<string | null>(null);
  const [addressDraft, setAddressDraft] = useState({
    label: 'Home',
    fullName: '',
    phone: '',
    addressLine: '',
    city: '',
    pincode: '',
  });
  const [isWishlistModalOpen, setIsWishlistModalOpen] = useState(false);
  const [isCouponsModalOpen, setIsCouponsModalOpen] = useState(false);
  const [isHelpModalOpen, setIsHelpModalOpen] = useState(false);
  const [isPolicyModalOpen, setIsPolicyModalOpen] = useState(false);
  const [isDeleteAccountOpen, setIsDeleteAccountOpen] = useState(false);
  const [deleteConfirmText, setDeleteConfirmText] = useState('');
  const [isDeletingAccount, setIsDeletingAccount] = useState(false);
  const [deleteAccountError, setDeleteAccountError] = useState<string | null>(null);
  const [cartServiceability, setCartServiceability] = useState<ShiprocketServiceabilityResult | null>(null);
  const [isCheckingCartEta, setIsCheckingCartEta] = useState(false);
  const [ticketSubject, setTicketSubject] = useState('Order & Delivery Inquiry');
  const [ticketMessage, setTicketMessage] = useState('');
  const [supportTickets, setSupportTickets] = useState<
    { id: string; subject: string; message: string; status: string; createdAt: string }[]
  >(() => {
    try {
      const saved = localStorage.getItem('aarvo_support_tickets');
      if (saved) return JSON.parse(saved);
    } catch {
      // safe fallback
    }
    return [];
  });

  useEffect(() => {
    try {
      localStorage.setItem('aarvo_support_tickets', JSON.stringify(supportTickets));
    } catch {
      // safe catch
    }
  }, [supportTickets]);

  const downloadSellerCsvTemplate = () => {
    const headers = ['Name*', 'Category*', 'Price_INR*', 'Stock*', 'Sizes', 'Colours', 'COD', 'Fabric', 'Description', 'Length_cm', 'Breadth_cm', 'Height_cm', 'Weight_kg'];
    const samples = [
      ['Pure Cotton Slim Fit Kurta', 'Fashion', '899', '40', 'M, L, XL, XXL', 'Royal Blue, White, Black', 'Yes', 'Pure Cotton', 'Authentic breathable cotton ethnic daily wear kurta.', '25', '20', '4', '0.35'],
      ['Lightweight Running Trainers', 'Footwear', '1899', '25', 'UK 7, UK 8, UK 9, UK 10', 'Black, Navy Blue', 'Yes', 'Standard Material', 'High cushioning athletic footwear for sports and training.', '32', '22', '12', '0.75'],
      ['AARVO True Wireless Earbuds', 'Electronics', '1499', '50', '', 'Black, White', 'Yes', '', 'Ultra low latency ENC earbuds with deep bass and type-C fast charging.', '10', '10', '4', '0.18']
    ];
    const escCsv = (v: any) => {
      const s = String(v ?? '');
      return s.includes(',') || s.includes('"') || s.includes('\n') ? `"${s.replace(/"/g, '""')}"` : s;
    };
    const csv = [headers.map(escCsv).join(','), ...samples.map((r) => r.map(escCsv).join(','))].join('\r\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'aarvo_products_bulk_template.csv';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    haptics.subtle();
    showToast('⬇ CSV template downloaded!');
  };

  const handleBulkCsvUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    e.target.value = '';

    const reader = new FileReader();
    reader.onload = (event) => {
      try {
        const text = String(event.target?.result || '');
        const lines = text.split(/\r?\n/).filter((l) => l.trim().length > 0);
        if (lines.length < 2) {
          haptics.warning();
          showToast('CSV is empty or only has headers.');
          return;
        }

        const parseLine = (lineStr: string) => {
          const res: string[] = [];
          let cur = '';
          let inQ = false;
          for (let i = 0; i < lineStr.length; i++) {
            const c = lineStr[i];
            if (c === '"') {
              if (inQ && lineStr[i + 1] === '"') {
                cur += '"';
                i++;
              } else {
                inQ = !inQ;
              }
            } else if (c === ',' && !inQ) {
              res.push(cur.trim());
              cur = '';
            } else {
              cur += c;
            }
          }
          res.push(cur.trim());
          return res;
        };

        const dataRows = lines.slice(1);
        const importedProducts: Product[] = [];

        dataRows.forEach((rowStr, idx) => {
          const cols = parseLine(rowStr);
          if (cols.length < 3 || !cols[0]) return;

          const name = cols[0];
          const category = cols[1] || 'Fashion';
          const price = parseFloat(cols[2]);
          const stock = parseInt(cols[3], 10) || 15;
          const sizesStr = cols[4] || '';
          const colorsStr = cols[5] || '';
          const codStr = (cols[6] || 'Yes').toLowerCase();
          const fabricStr = cols[7] || '';
          const rawDesc = cols[8] || `${name} - verified authentic item.`;

          if (!name || isNaN(price) || price <= 0) return;

          const sizesList = sizesStr ? sizesStr.split(',').map((s) => s.trim()).filter(Boolean) : undefined;
          const colorsList = colorsStr ? colorsStr.split(',').map((c) => c.trim()).filter(Boolean) : undefined;
          const codAvailable = !codStr.includes('no');

          const newProd: Product = {
            id: `prod-bulk-${Date.now()}-${idx}`,
            name,
            category,
            price: Math.round(price),
            originalPrice: Math.round(price * 1.5),
            discount: '33% OFF',
            rating: 0.0,
            reviewsCount: 0,
            reviewsList: [],
            inStock: stock > 0,
            stockQuantity: stock,
            image: 'https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=500&auto=format&fit=crop&q=80',
            images: [
              'https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=500&auto=format&fit=crop&q=80',
              'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=500&auto=format&fit=crop&q=80',
            ],
            seller: sellerForm.business.trim() || 'AARVO Luxury Certified Merchant',
            description: rawDesc,
            availableSizes: sizesList && sizesList.length > 0 ? sizesList : undefined,
            availableColors: colorsList && colorsList.length > 0 ? colorsList : undefined,
            fabric: fabricStr || undefined,
            codAvailable,
            highlights: [
              `Category: ${category}`,
              sizesList && sizesList.length > 0 ? `Sizes: ${sizesList.join(', ')}` : '',
              colorsList && colorsList.length > 0 ? `Colours: ${colorsList.join(', ')}` : '',
              fabricStr ? `Fabric: ${fabricStr}` : '',
              codAvailable ? 'Cash on Delivery (COD) Eligible' : 'Prepaid Only',
            ].filter(Boolean),
          };
          importedProducts.push(newProd);
        });

        if (importedProducts.length > 0) {
          setAllProducts((prev) => [...importedProducts, ...prev]);
          haptics.saved();
          showToast(`✓ Bulk imported ${importedProducts.length} product(s) with variants!`);
        } else {
          haptics.warning();
          showToast('No valid product rows found in CSV.');
        }
      } catch (err: any) {
        haptics.warning();
        showToast(`CSV error: ${err.message}`);
      }
    };
    reader.readAsText(file);
  };

  // Orders purchase history state (no pre-seeded fake sample orders)
  const [orders, setOrders] = useState<Order[]>(() => {
    try {
      const saved = localStorage.getItem('aarvo_orders');
      if (saved) {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed)) return parsed;
      }
    } catch {
      // safe fallback
    }
    return [];
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
      return saved ? JSON.parse(saved) : [];
    } catch {
      return [];
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
        haptics.saved();
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

  const dismissToast = useCallback((id: string) => {
    if (toastTimersRef.current[id]) {
      clearTimeout(toastTimersRef.current[id]);
      delete toastTimersRef.current[id];
    }
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const showToast = useCallback(
    (
      msg: string,
      variantOrOptions?:
        | ToastVariant
        | {
            variant?: ToastVariant;
            title?: string;
            durationMs?: number;
            actionLabel?: string;
            onAction?: () => void;
          }
    ) => {
      const opts =
        typeof variantOrOptions === 'string'
          ? { variant: variantOrOptions }
          : variantOrOptions || {};
      const lower = msg.toLowerCase();
      const inferredVariant: ToastVariant =
        opts.variant ||
        (lower.includes('error') || lower.includes('failed') || lower.includes('invalid')
          ? 'error'
          : lower.includes('offline') || lower.includes('removed') || lower.includes('cleared')
          ? 'warning'
          : lower.includes('restocked') || lower.includes('detect') || lower.includes('search')
          ? 'info'
          : 'success');
      const id = `toast-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`;
      const durationMs = opts.durationMs ?? 2800;

      setToasts((prev) => [
        ...prev.slice(-2),
        {
          id,
          message: msg,
          title: opts.title,
          variant: inferredVariant,
          actionLabel: opts.actionLabel,
          onAction: opts.onAction,
        },
      ]);

      toastTimersRef.current[id] = setTimeout(() => {
        dismissToast(id);
      }, durationMs);
    },
    [dismissToast]
  );

  useEffect(() => {
    return () => {
      Object.values(toastTimersRef.current).forEach(clearTimeout);
      toastTimersRef.current = {};
    };
  }, []);

  // Payment Method Selection & Management in Cart
  const [paymentMethodSelection, setPaymentMethodSelection] = useState<'COD' | 'UPI'>('COD');
  const [selectedUpiApp, setSelectedUpiApp] = useState<'Google Pay' | 'PhonePe' | 'Paytm' | 'Custom UPI'>('Google Pay');
  const [customUpiId, setCustomUpiId] = useState<string>('');
  const [showAddPaymentModal, setShowAddPaymentModal] = useState<boolean>(false);
  const [newCardNumber, setNewCardNumber] = useState<string>('');
  const [newCardHolder, setNewCardHolder] = useState<string>('');
  const [newCardExpiry, setNewCardExpiry] = useState<string>('');

  // Floating Network Status state using navigator.onLine and event listeners
  const [isOnline, setIsOnline] = useState<boolean>(() => {
    return typeof navigator !== 'undefined' ? navigator.onLine : true;
  });
  const [showOfflineNotice, setShowOfflineNotice] = useState<boolean>(() => {
    return typeof navigator !== 'undefined' ? !navigator.onLine : false;
  });
  const [reconnectNotice, setReconnectNotice] = useState<boolean>(false);
  const [isOfflineDismissed, setIsOfflineDismissed] = useState<boolean>(false);

  useEffect(() => {
    if (typeof window === 'undefined') return;

    let timer: NodeJS.Timeout | null = null;

    const handleOnline = () => {
      setIsOnline(true);
      setShowOfflineNotice(false);
      setIsOfflineDismissed(false);
      setReconnectNotice(true);
      haptics.subtle();

      if (timer) clearTimeout(timer);
      timer = setTimeout(() => {
        setReconnectNotice(false);
      }, 3500);
    };

    const handleOffline = () => {
      setIsOnline(false);
      setReconnectNotice(false);
      setIsOfflineDismissed(false);
      setShowOfflineNotice(true);
      haptics.warning();
    };

    // Initial check using navigator.onLine
    if (typeof navigator !== 'undefined' && !navigator.onLine) {
      setIsOnline(false);
      setShowOfflineNotice(true);
    }

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
      if (timer) clearTimeout(timer);
    };
  }, []);

  const retryConnection = () => {
    if (typeof navigator !== 'undefined' && navigator.onLine) {
      haptics.subtle();
      setIsOnline(true);
      setShowOfflineNotice(false);
      setReconnectNotice(true);
      setTimeout(() => setReconnectNotice(false), 3000);
      showToast('Internet connection restored!');
    } else {
      haptics.warning();
      setShowOfflineNotice(false);
      setTimeout(() => setShowOfflineNotice(true), 150);
      showToast('Still offline. Please check your network.');
    }
  };

  // Sample real catalog products with stock inventory data & multi-image sets
  const [allProducts, setAllProducts] = useState<Product[]>([
    {
      id: 'p_sneakers',
      name: "Men's Sneakers Stylish Sports Shoes",
      category: 'Fashion',
      image: prodWhiteSneakersImg,
      images: [
        prodWhiteSneakersImg,
        'https://images.unsplash.com/photo-1549298916-b41d501d3772?w=800&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1525966222134-fcfa99b8ae77?w=800&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1560769629-975ec94e6a86?w=800&auto=format&fit=crop&q=80',
      ],
      price: 999,
      originalPrice: 1999,
      rating: 4.2,
      reviewsCount: 12458,
      discount: '50% OFF',
      inStock: true,
      stockQuantity: 40,
      seller: 'Aarvo Luxury Footwear',
      availableSizes: ['6', '7', '8', '9', '10'],
      availableColors: ['White Navy', 'Pure White', 'Black Red'],
      fabric: 'Breathable Knit & Rubber Sole',
      description: 'Men\'s sneakers stylish sports shoes featuring high-rebound cushioning, responsive flexibility, and lightweight all-day comfort.',
    },
    {
      id: 'p_skybags',
      name: 'Skybags Backpack for College',
      category: 'Fashion',
      image: prodNavyBackpackImg,
      images: [
        prodNavyBackpackImg,
        'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1622560480605-d83c853bc5c3?w=800&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1546938576-6e6a64f317cc?w=800&auto=format&fit=crop&q=80',
      ],
      price: 1299,
      originalPrice: 2499,
      rating: 4.3,
      reviewsCount: 3820,
      discount: '48% OFF',
      inStock: true,
      stockQuantity: 25,
      seller: 'Skybags Certified Store',
      availableSizes: ['Standard 32L'],
      availableColors: ['Navy Blue', 'Carbon Black', 'Steel Grey'],
      fabric: 'Water Resistant Polyester Fabric',
      description: 'Spacious 3-compartment college backpack with padded laptop sleeve, organizer mesh pockets, and ergonomic shoulder straps.',
    },
    {
      id: 'p1',
      name: 'OnePlus Nord CE4 5G (8GB RAM, 128GB Storage)',
      category: 'Mobiles',
      image: 'https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=800&auto=format&fit=crop&q=80',
      images: [
        'https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=800&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=800&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1580910051074-3eb694886505?w=800&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1565849904461-04a58ad377e0?w=800&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1592899677977-9c10ca588bbd?w=800&auto=format&fit=crop&q=80',
      ],
      price: 19999,
      originalPrice: 24999,
      rating: 4.5,
      reviewsCount: 1420,
      discount: '20% OFF',
      inStock: true,
      stockQuantity: 14,
      seller: 'TechPro Retail India',
      description: 'Snapdragon 7 Gen 3 performance with 100W SuperVOOC fast charging, 120Hz Fluid AMOLED display, and 50MP Sony LYT-600 OIS camera.',
    },
    {
      id: 'p_boat',
      name: 'boAt Airdopes 141 ANC True Wireless Earbuds with Active Noise Cancellation (42H Playback)',
      category: 'Electronics',
      image: 'https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=800&auto=format&fit=crop&q=80',
      images: [
        'https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=800&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1606220588913-b3aacb4d2f46?w=800&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1572536147248-ac59a8abfa4b?w=800&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=800&auto=format&fit=crop&q=80',
      ],
      price: 1499,
      originalPrice: 3990,
      rating: 4.1,
      reviewsCount: 250673,
      discount: '62% off',
      inStock: true,
      stockQuantity: 28,
      seller: 'boAt Lifestyle Flagship',
      description: 'Experience pure acoustic immersion with 32dB Active Noise Cancellation, quad microphones with ENx technology, ASAP Charge, and 42 hours combined battery backup.',
    },
    {
      id: 'p2',
      name: 'Pure Cotton Embroidered Anarkali Kurta Set with Dupatta',
      category: 'Fashion',
      image: 'https://images.unsplash.com/photo-1610030469983-98e550d6193c?w=800&auto=format&fit=crop&q=80',
      images: [
        'https://images.unsplash.com/photo-1610030469983-98e550d6193c?w=800&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?w=800&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1617627143750-d86bc21e42bb?w=800&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1583391733981-81b0a8806a6b?w=800&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1509631179647-0177331693ae?w=800&auto=format&fit=crop&q=80',
      ],
      price: 1299,
      originalPrice: 3499,
      rating: 4.6,
      reviewsCount: 890,
      discount: '62% OFF',
      inStock: true,
      stockQuantity: 3,
      seller: 'Aarvo Trends Fashion',
      description: 'Handcrafted pure cambric cotton with detailed zari threadwork, tailored flare silhouette, and matching organza printed dupatta.',
    },
    {
      id: 'p_neem',
      name: 'Neem Datoon Organic Fresh Herbal Teeth Cleaning Sticks (Pack of 30)',
      category: 'Mouth Fresher',
      image: 'https://images.unsplash.com/photo-1540555700478-4be289fbecef?w=800&auto=format&fit=crop&q=80',
      images: [
        'https://images.unsplash.com/photo-1540555700478-4be289fbecef?w=800&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1608571423902-eed4a5ad8108?w=800&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1509316975850-ff9c5deb0cd9?w=800&auto=format&fit=crop&q=80',
      ],
      price: 19,
      originalPrice: 29,
      rating: 4.3,
      reviewsCount: 120,
      discount: '34% OFF',
      inStock: true,
      stockQuantity: 50,
      seller: 'Vedic Naturals Direct',
      description: 'Naturally harvested organic neem twigs for traditional dental hygiene, gum strengthening, and natural antibacterial breath freshening.',
    },
    {
      id: 'p_dung',
      name: "Cow's dung cakes 100% Pure Organic for Hawan & Puja (Pack of 20)",
      category: 'Home/Kitchen',
      image: 'https://images.unsplash.com/photo-1607613009820-a29f7bb81c04?w=800&auto=format&fit=crop&q=80',
      images: [
        'https://images.unsplash.com/photo-1607613009820-a29f7bb81c04?w=800&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1542601906990-b4d3fb778b09?w=800&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1518531933037-91b2f5f229cc?w=800&auto=format&fit=crop&q=80',
      ],
      price: 99,
      originalPrice: 129,
      rating: 4.3,
      reviewsCount: 120,
      discount: '23% OFF',
      inStock: true,
      stockQuantity: 40,
      seller: 'Gaushala Pure Organics',
      description: 'Authentic sun-dried desi cow dung cakes for sacred hawan, agnihotra, puja ceremonies, and organic home purifying.',
    },
    {
      id: 'p4',
      name: 'Ergonomic High Back Mesh Office & Study Chair with Lumbar Support',
      category: 'Home & Living',
      image: 'https://images.unsplash.com/photo-1580481077195-c22ae2804b7f?w=800&auto=format&fit=crop&q=80',
      images: [
        'https://images.unsplash.com/photo-1580481077195-c22ae2804b7f?w=800&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1505797149-43b0069ec26b?w=800&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=800&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1519947486511-46149fa0a254?w=800&auto=format&fit=crop&q=80',
      ],
      price: 4499,
      originalPrice: 8999,
      rating: 4.3,
      reviewsCount: 450,
      discount: '50% OFF',
      inStock: true,
      stockQuantity: 2,
      seller: 'Urban Home Comfort',
      description: 'Medical-grade ergonomic lumbar curve, breathable Korean mesh, pneumatic Class 4 gas lift, and smooth silent nylon castors.',
    },
    {
      id: 'p5',
      name: 'Philips Digital Air Fryer with Rapid Air Tech (4.1 Litre)',
      category: 'Appliances',
      image: 'https://images.unsplash.com/photo-1585659722983-3a675dabf23d?w=800&auto=format&fit=crop&q=80',
      images: [
        'https://images.unsplash.com/photo-1585659722983-3a675dabf23d?w=800&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1556911220-e15b29be8c8f?w=800&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1544025162-d76694265947?w=800&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=800&auto=format&fit=crop&q=80',
      ],
      price: 6499,
      originalPrice: 11995,
      rating: 4.7,
      reviewsCount: 2240,
      discount: '45% OFF',
      inStock: true,
      stockQuantity: 9,
      seller: 'KitchenWorld Express',
      description: 'Patented Rapid Air starfish technology cooks meals with up to 90% less fat. Touch screen with 7 presets and Keep Warm mode.',
    },
    {
      id: 'p7',
      name: 'Men Ultra-Light Breathable Running Shoes (Cushioned Grip)',
      category: 'Sports',
      image: 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800&auto=format&fit=crop&q=80',
      images: [
        'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1608231387042-66d1773070a5?w=800&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1600185365926-3a2ce3cdb9eb?w=800&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1584735935682-2f2b69dff9d2?w=800&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?w=800&auto=format&fit=crop&q=80',
      ],
      price: 1599,
      originalPrice: 3999,
      rating: 4.4,
      reviewsCount: 950,
      discount: '60% OFF',
      inStock: true,
      stockQuantity: 15,
      seller: 'Apex Sports Hub',
      description: 'Engineered mesh upper for maximum aeration, responsive EVA midsole rebound cushioning, and multi-directional anti-slip rubber traction.',
    },
    {
      id: 'p9',
      name: 'California Jumbo Almonds & Cashews Combo (1kg Fresh Harvest)',
      category: 'Grocery',
      image: 'https://images.unsplash.com/photo-1508061252224-237ff54ed5a3?w=800&auto=format&fit=crop&q=80',
      images: [
        'https://images.unsplash.com/photo-1508061252224-237ff54ed5a3?w=800&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1536816579748-4ecb3f03d72a?w=800&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1599599810769-bcde5a160d32?w=800&auto=format&fit=crop&q=80',
      ],
      price: 899,
      originalPrice: 1450,
      rating: 4.6,
      reviewsCount: 1380,
      discount: '38% OFF',
      inStock: true,
      stockQuantity: 24,
      seller: 'NutriHarvest Daily',
      description: 'Premium whole jumbo California almonds and W240 cashew nuts packed in airtight nitrogen-flushed zipper pouches for ultimate crunch.',
    },
    {
      id: 'p_blender',
      name: 'NutriBlend Pro 750W Compact Mixer & Smoothie Blender',
      category: 'Home & Kitchen',
      image: catHomeBlenderImg,
      images: [
        catHomeBlenderImg,
        'https://images.unsplash.com/photo-1570222094114-d054a817e56b?w=800&auto=format&fit=crop&q=80',
      ],
      price: 2199,
      originalPrice: 3999,
      rating: 4.4,
      reviewsCount: 640,
      discount: '45% OFF',
      inStock: true,
      stockQuantity: 18,
      seller: 'KitchenWorld Express',
      availableColors: ['Royal Blue & Silver', 'Jet Black'],
      description: 'Heavy-duty 750W copper motor smoothie blender with stainless steel blades and BPA-free Tritan jar.',
    },
    {
      id: 'p_serum',
      name: 'Botanica Glow 10% Vitamin C + Hyaluronic Face Serum (30ml)',
      category: 'Beauty & Personal Care',
      image: catBeautySerumImg,
      images: [
        catBeautySerumImg,
        'https://images.unsplash.com/photo-1620916566398-39f1143ab7be?w=800&auto=format&fit=crop&q=80',
      ],
      price: 499,
      originalPrice: 899,
      rating: 4.5,
      reviewsCount: 1120,
      discount: '44% OFF',
      inStock: true,
      stockQuantity: 32,
      seller: 'Botanica Skincare Official',
      description: 'Dermatologist-tested daily brightening serum for radiant skin tone and deep hydration without stickiness.',
    },
  ]);

  const [isProductsLoading, setIsProductsLoading] = useState<boolean>(true);
  const catalogFetchAbortRef = useRef<AbortController | null>(null);

  const fetchCatalogProducts = useCallback(async () => {
    catalogFetchAbortRef.current?.abort();
    const controller = new AbortController();
    catalogFetchAbortRef.current = controller;
    setIsProductsLoading(true);
    const startedAt = Date.now();

    try {
      const response = await fetch('/v1/products', {
        signal: controller.signal,
        headers: { Accept: 'application/json' },
      });

      if (response.ok) {
        const rows = await response.json();
        if (Array.isArray(rows) && rows.length > 0) {
          const normalized: Product[] = rows.map((row: any, idx: number) => {
            const rawPricePaise = Number(row.price_paise ?? row.pricePaise ?? 0);
            const rawMrpPaise = Number(
              row.original_price_paise ?? row.originalPricePaise ?? rawPricePaise
            );
            const price =
              rawPricePaise > 0
                ? Math.round(rawPricePaise / 100)
                : Math.max(1, Number(row.price ?? 499));
            const originalPrice =
              rawMrpPaise > 0
                ? Math.round(rawMrpPaise / 100)
                : Math.max(price, Number(row.originalPrice ?? price));
            const discountPct =
              originalPrice > price
                ? Math.round(((originalPrice - price) * 100) / originalPrice)
                : 0;
            const primaryImage =
              typeof row.image_url === 'string' && row.image_url.trim().length > 0
                ? row.image_url.trim()
                : typeof row.image === 'string' && row.image.trim().length > 0
                ? row.image.trim()
                : prodWhiteSneakersImg;
            const stockQuantity = Number(row.stock_quantity ?? row.stockQuantity ?? 10);

            return {
              id: String(row.id ?? `api_p_${idx}`),
              name: String(row.name ?? 'AARVO Product'),
              category: String(row.category ?? 'General'),
              image: primaryImage,
              images:
                Array.isArray(row.images) && row.images.length > 0
                  ? row.images
                  : [primaryImage],
              price,
              originalPrice,
              rating: Number(row.rating ?? 4.2),
              reviewsCount: Number(row.reviews_count ?? row.reviewsCount ?? 120),
              discount: discountPct > 0 ? `${discountPct}% OFF` : '',
              inStock: stockQuantity > 0,
              stockQuantity,
              seller: String(row.seller_name ?? row.seller ?? 'AARVO Verified Seller'),
              description: String(
                row.description ?? 'Genuine AARVO marketplace product with doorstep delivery.'
              ),
            };
          });

          if (!controller.signal.aborted) {
            setAllProducts(normalized);
          }
        }
      }
    } catch (err: any) {
      if (err?.name !== 'AbortError') {
        // Keep curated fallback catalog when backend API is unreachable in static preview
      }
    } finally {
      const elapsed = Date.now() - startedAt;
      const minSkeletonMs = 450;
      const waitMs = Math.max(0, minSkeletonMs - elapsed);
      setTimeout(() => {
        if (!controller.signal.aborted) {
          setIsProductsLoading(false);
        }
      }, waitMs);
    }
  }, []);

  useEffect(() => {
    if (isLoading) return;
    fetchCatalogProducts();
    return () => {
      catalogFetchAbortRef.current?.abort();
    };
  }, [isLoading, fetchCatalogProducts]);

  const restockProduct = (id: string, amount: number = 10) => {
    setAllProducts((prev) =>
      prev.map((item) =>
        item.id === id ? { ...item, stockQuantity: (item.stockQuantity ?? 0) + amount } : item
      )
    );
    haptics.subtle();
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

  const addToCart = (product: Product, quantity: number = 1) => {
    const qtyToAdd = Math.max(1, quantity);
    haptics.addToCart();
    setCartItems((prev) => {
      const existing = prev.find((item) => item.product.id === product.id);
      if (existing) {
        return prev.map((item) =>
          item.product.id === product.id ? { ...item, quantity: item.quantity + qtyToAdd } : item
        );
      }
      return [...prev, { product, quantity: qtyToAdd }];
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
  const totalCartCount = cartItems.reduce((acc, item) => acc + item.quantity, 0);
  const totalMRP = cartItems.reduce((acc, item) => acc + item.product.originalPrice * item.quantity, 0);
  const totalSellingPrice = cartItems.reduce((acc, item) => acc + item.product.price * item.quantity, 0);
  const totalDiscount = totalMRP - totalSellingPrice;
  const couponDiscount =
    appliedCouponCode === 'AARVO200' && totalSellingPrice >= 999
      ? 200
      : appliedCouponCode === 'WELCOME100' && totalSellingPrice >= 499
      ? 100
      : 0;
  const platformFee = cartItems.length > 0 ? 3 : 0;
  const finalPayable = Math.max(0, totalSellingPrice - couponDiscount + platformFee);
  const primaryCartCategory = cartItems[0]?.product?.category || '';

  useEffect(() => {
    const pin = (activeAddressObj?.pincode || '').replace(/\D/g, '').slice(0, 6);
    if (!/^[1-9]\d{5}$/.test(pin)) {
      setCartServiceability(null);
      return;
    }
    const controller = new AbortController();
    setIsCheckingCartEta(true);
    checkShiprocketServiceability(pin, {
      category: primaryCartCategory || undefined,
      signal: controller.signal,
    })
      .then((res) => {
        if (!controller.signal.aborted) setCartServiceability(res);
      })
      .finally(() => {
        if (!controller.signal.aborted) setIsCheckingCartEta(false);
      });
    return () => controller.abort();
  }, [activeAddressObj?.pincode, primaryCartCategory]);

  // Fetch and display user order history from persistent storage
  const handleViewMyOrders = useCallback(() => {
    haptics.selection();
    try {
      const saved = localStorage.getItem('aarvo_orders');
      if (saved) {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed) && parsed.length > 0) {
          setOrders(parsed);
        }
      }
    } catch (e) {
      console.error('Error fetching order history:', e);
    }
    setIsOrdersViewOpen(true);
  }, []);

  // Hero Banner Slides
  const slides = [
    {
      id: 1,
      badge: 'Exclusive',
      isGraphicBanner: true,
      titlePrimary: 'Big Savings',
      titleSecondary: 'Big Smiles',
      ctaText: 'Shop Now',
      image: heroBigSavingsWomanImg,
      bgGradient: 'from-[#1E40AF] via-[#2563EB] to-[#3B82F6]',
    },
    {
      id: 2,
      badge: 'Seller Hub',
      isGraphicBanner: false,
      titlePrimary: 'Verified Sellers',
      titleSecondary: 'Direct & Genuine',
      subtitle: 'Doorstep Delivery • GST Verified Invoices',
      ctaText: 'Explore Drops',
      bgGradient: 'from-[#EA580C] via-[#FF5722] to-[#FB8C00]',
    },
    {
      id: 3,
      badge: 'Mega Deals',
      isGraphicBanner: false,
      titlePrimary: 'Smart Gadgets',
      titleSecondary: 'Up to 60% OFF',
      subtitle: 'Smartphones, Audio & Wearables with 1 Yr Warranty',
      ctaText: 'Shop Electronics',
      bgGradient: 'from-[#1E293B] via-[#334155] to-[#FF5722]',
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

  // Products that match the active text search query (before category refinement)
  const queryMatchedProducts = useMemo(
    () => matchAndScoreProducts(allProducts, searchQuery),
    [allProducts, searchQuery]
  );

  const matchesCategory = useCallback((productCat: string, selectedCat: string) => {
    if (!selectedCat || selectedCat === 'All' || selectedCat === 'More') return true;
    const pCat = (productCat || '').toLowerCase();
    const sCat = selectedCat.toLowerCase();
    if (sCat.includes('home')) {
      return pCat.includes('home') || pCat.includes('kitchen') || pCat.includes('living');
    }
    if (sCat.includes('beauty')) {
      return pCat.includes('beauty') || pCat.includes('personal') || pCat.includes('mouth');
    }
    if (sCat.includes('sports') || sCat.includes('footwear')) {
      return pCat.includes('sport') || pCat.includes('footwear');
    }
    if (sCat.includes('toys')) {
      return pCat.includes('toy') || pCat.includes('baby') || pCat.includes('kid');
    }
    return pCat.startsWith(sCat.slice(0, 4)) || pCat.includes(sCat);
  }, []);

  // Dynamic category chips for refining search results with real-time matching product counts
  const searchCategoryChips = useMemo(() => {
    const baseCategories = [
      { id: 'All', label: 'All' },
      { id: 'Electronics', label: 'Electronics' },
      { id: 'Fashion', label: 'Fashion' },
      { id: 'Mobiles', label: 'Mobiles' },
      { id: 'Home & Kitchen', label: 'Home & Kitchen' },
      { id: 'Grocery', label: 'Grocery' },
      { id: 'Beauty & Personal Care', label: 'Beauty & Care' },
      { id: 'Appliances', label: 'Appliances' },
      { id: 'Sports', label: 'Sports' },
      { id: 'Toys', label: 'Toys & Kids' },
      { id: 'Footwear', label: 'Footwear' },
    ];

    return baseCategories.map((cat) => {
      if (cat.id === 'All') {
        return {
          ...cat,
          count: queryMatchedProducts.length,
        };
      }
      const count = queryMatchedProducts.filter((p) => matchesCategory(p.category, cat.id)).length;
      return {
        ...cat,
        count,
      };
    });
  }, [queryMatchedProducts, matchesCategory]);

  const hasPriceFilter = minPriceFilter !== null || maxPriceFilter !== null;

  const matchesPriceRange = useCallback(
    (product: Product) => {
      if (minPriceFilter !== null && product.price < minPriceFilter) return false;
      if (maxPriceFilter !== null && product.price > maxPriceFilter) return false;
      return true;
    },
    [minPriceFilter, maxPriceFilter]
  );

  const applyPricePreset = useCallback(
    (min: number | null, max: number | null, label: string) => {
      haptics.selection();
      setMinPriceFilter(min);
      setMaxPriceFilter(max);
      if (min === null && max === null) {
        showToast('Price range filter cleared', 'info');
      } else {
        showToast(`Price filter: ${label}`, { title: 'Price Filter Applied', variant: 'info' });
      }
    },
    [showToast]
  );

  const openPriceFilterModal = useCallback(() => {
    haptics.light();
    setPriceMinDraft(minPriceFilter !== null ? String(minPriceFilter) : '');
    setPriceMaxDraft(maxPriceFilter !== null ? String(maxPriceFilter) : '');
    setIsPriceFilterModalOpen(true);
  }, [minPriceFilter, maxPriceFilter]);

  const activePriceFilterLabel = useMemo(() => {
    if (minPriceFilter === null && maxPriceFilter === null) return null;
    if (minPriceFilter !== null && maxPriceFilter !== null) {
      return `₹${minPriceFilter.toLocaleString('en-IN')} – ₹${maxPriceFilter.toLocaleString('en-IN')}`;
    }
    if (maxPriceFilter !== null) {
      return `Under ₹${maxPriceFilter.toLocaleString('en-IN')}`;
    }
    return `₹${minPriceFilter!.toLocaleString('en-IN')}+`;
  }, [minPriceFilter, maxPriceFilter]);

  const PRICE_FILTER_PRESETS: { label: string; min: number | null; max: number | null }[] = [
    { label: 'All Prices', min: null, max: null },
    { label: 'Under ₹500', min: null, max: 500 },
    { label: '₹500 – ₹2K', min: 500, max: 2000 },
    { label: '₹2K – ₹10K', min: 2000, max: 10000 },
    { label: '₹10,000+', min: 10000, max: null },
  ];

  // Real-time filtered & sorted products matching search, category, price range, and sort order
  const filteredProducts = useMemo(() => {
    const categoryFiltered =
      selectedCategory === 'All' || selectedCategory === 'More'
        ? queryMatchedProducts
        : queryMatchedProducts.filter((p) => matchesCategory(p.category, selectedCategory));
    const priceFiltered = categoryFiltered.filter(matchesPriceRange);
    return sortProducts(priceFiltered, sortBy, searchQuery);
  }, [queryMatchedProducts, selectedCategory, matchesCategory, matchesPriceRange, sortBy, searchQuery]);

  const homeFeedProducts = useMemo(
    () => allProducts.filter(matchesPriceRange),
    [allProducts, matchesPriceRange]
  );

  if (isLoading) {
    return (
      <div className="min-h-screen bg-[#FFFDF9] dark:bg-[#120E0C] text-slate-900 dark:text-slate-100 flex justify-center pb-16 select-none font-sans transition-colors duration-300">
        <MainScreenSkeleton />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#FFFDF9] dark:bg-[#120E0C] text-slate-900 dark:text-slate-100 flex justify-center pb-16 select-none font-sans transition-colors duration-300 animate-in fade-in duration-300">
      <div className="w-full max-w-[440px] bg-[#FFFDF9] dark:bg-[#181311] min-h-screen shadow-2xl flex flex-col relative overflow-x-hidden transition-colors duration-300">

        {/* ================================================================= */}
        {/* FLOATING NETWORK CONNECTIVITY STATUS INDICATOR                    */}
        {/* (Uses navigator.onLine and 'online'/'offline' event listeners)     */}
        {/* ================================================================= */}
        {((!isOnline && showOfflineNotice && !isOfflineDismissed) || reconnectNotice) && (
          <aside
            role="status"
            aria-live="polite"
            className="fixed top-20 left-1/2 -translate-x-1/2 z-50 w-[92%] max-w-[410px] pointer-events-auto animate-in slide-in-from-top-3 duration-300 transition-all"
          >
            {reconnectNotice ? (
              // Floating "Back Online" banner
              <div className="flex items-center justify-between gap-2.5 px-3.5 py-2.5 rounded-2xl bg-emerald-950/95 text-emerald-100 text-xs font-semibold shadow-2xl border border-emerald-500/50 backdrop-blur-md">
                <div className="flex items-center gap-2.5 min-w-0">
                  <div className="w-6 h-6 rounded-full bg-emerald-800/80 text-emerald-200 flex items-center justify-center shrink-0">
                    <Wifi className="w-3.5 h-3.5 stroke-[2.5]" />
                  </div>
                  <div className="leading-tight min-w-0">
                    <span className="font-bold text-emerald-300 text-xs block">
                      Back Online
                    </span>
                    <span className="text-[10px] text-emerald-200/80 truncate block">
                      Internet connection restored
                    </span>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => setReconnectNotice(false)}
                  className="p-1 text-emerald-300 hover:text-white rounded-lg cursor-pointer"
                  aria-label="Dismiss back online alert"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              </div>
            ) : (
              // Floating "Offline / Lost Connectivity" banner
              <div className="flex items-center justify-between gap-2.5 px-3.5 py-2.5 rounded-2xl bg-slate-950/95 text-white text-xs font-semibold shadow-2xl border border-amber-500/60 dark:border-amber-400/50 backdrop-blur-md ring-2 ring-amber-500/20">
                <div className="flex items-center gap-2.5 min-w-0">
                  <div className="relative flex items-center justify-center w-7 h-7 rounded-full bg-amber-500/20 text-amber-400 shrink-0">
                    <span className="absolute w-2.5 h-2.5 rounded-full bg-amber-500 animate-ping opacity-75" />
                    <WifiOff className="w-4 h-4 stroke-[2.5] relative z-10" />
                  </div>
                  <div className="leading-tight min-w-0">
                    <div className="flex items-center gap-1.5">
                      <span className="font-black text-amber-400 text-xs">
                        No Internet Connection
                      </span>
                      <span className="text-[9px] bg-amber-400/20 text-amber-300 px-1.5 py-0.2 rounded font-bold">
                        Offline
                      </span>
                    </div>
                    <span className="text-[10px] text-slate-300 truncate block mt-0.5">
                      You lost connectivity. Cached catalog is still viewable.
                    </span>
                  </div>
                </div>

                <div className="flex items-center gap-1.5 shrink-0">
                  <button
                    type="button"
                    onClick={retryConnection}
                    className="px-2.5 py-1 bg-amber-500 hover:bg-amber-400 text-slate-950 text-[10px] font-black rounded-lg transition-transform active:scale-95 cursor-pointer shadow-xs"
                  >
                    Retry
                  </button>
                  <button
                    type="button"
                    onClick={() => setIsOfflineDismissed(true)}
                    className="p-1 text-slate-400 hover:text-white rounded-lg cursor-pointer"
                    aria-label="Dismiss offline alert"
                  >
                    <X className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            )}
          </aside>
        )}

        {isOrdersViewOpen ? (
          <Orders
            orders={orders}
            onOrdersLoaded={(loaded) => {
              if (loaded.length > 0) setOrders(loaded);
            }}
            onBack={() => setIsOrdersViewOpen(false)}
            customerName={userProfile.name}
            customerPhone={userProfile.phone}
            onOpenSupport={() => {
              setIsOrdersViewOpen(false);
              setIsHelpModalOpen(true);
            }}
            onReorder={(item) => {
              const matchedProduct = allProducts.find((p) => p.id === item.id) || {
                id: item.id,
                name: item.name,
                category: item.category,
                image: item.image,
                price: item.price,
                originalPrice: Math.round(item.price * 1.25),
                rating: 0,
                reviewsCount: 0,
                discount: '20% OFF',
                inStock: true,
                seller: 'AARVO Seller',
              };
              addToCart(matchedProduct);
              showToast(`Added ${item.name.slice(0, 20)}... to Cart!`);
            }}
            onReorderAll={(items) => {
              haptics.addToCart();
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
                    rating: 0,
                    reviewsCount: 0,
                    discount: '20% OFF',
                    inStock: true,
                    seller: 'AARVO Seller',
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
            {/* ================================================================= */}
            {/* 1. TOP HEADER MATCHING LEFT PHONE MOCKUP (AARVO BRANDING & SEARCH)*/}
            {/* ================================================================= */}
            <header className="bg-gradient-to-b from-[#FFE5CC] via-[#FFF4E8] to-white dark:from-[#26160C] dark:via-[#1B120E] dark:to-[#181311] text-slate-900 dark:text-white pt-[max(0.625rem,env(safe-area-inset-top))] pb-2 px-3.5 sticky top-0 z-30 border-b border-orange-100/80 dark:border-stone-800/80 shadow-xs">
              {/* Top Row: Menu + Search Input + Cart */}
              <div className="flex items-center gap-2 mb-2">
                {/* Left: Hamburger Menu */}
                <button
                  type="button"
                  onClick={() => {
                    haptics.light();
                    setIsSidebarOpen(true);
                  }}
                  className="p-1.5 text-slate-800 dark:text-slate-200 hover:bg-orange-100/70 dark:hover:bg-stone-800 rounded-full transition-colors shrink-0 cursor-pointer"
                  aria-label="Open menu"
                >
                  <Menu className="w-5 h-5 stroke-[2.2]" />
                </button>

                {/* Pill Search Bar */}
                <div className="flex-1 relative">
                  <div className="h-10 bg-white dark:bg-[#211A17] rounded-full flex items-center px-3 shadow-2xs border border-orange-200/80 dark:border-stone-700/80 focus-within:border-[#FF5722] focus-within:ring-2 focus-within:ring-[#FF5722]/20 transition-all">
                    <Search className="w-4 h-4 text-slate-400 dark:text-slate-500 shrink-0 pointer-events-none mr-2" />
                    <input
                      type="text"
                      placeholder="Search for products..."
                      value={searchQuery}
                      onChange={(e) => {
                        setSearchQuery(e.target.value);
                        if (activeTab !== 'categories' && activeTab !== 'home') {
                          setActiveTab('home');
                        }
                      }}
                      className="w-full text-xs text-slate-800 dark:text-slate-100 placeholder-slate-400 outline-none bg-transparent font-medium"
                    />
                    {searchQuery ? (
                      <button
                        type="button"
                        onClick={() => {
                          haptics.light();
                          setSearchQuery('');
                        }}
                        className="p-1 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 mr-0.5 cursor-pointer"
                      >
                        <X className="w-3.5 h-3.5" />
                      </button>
                    ) : (
                      <button
                        type="button"
                        onClick={() => {
                          haptics.medium();
                          setIsVoiceModalOpen(true);
                        }}
                        aria-label="Voice search"
                        className="p-1 text-slate-400 hover:text-[#FF5722] transition-colors rounded-full cursor-pointer"
                      >
                        <Mic className="w-3.5 h-3.5" />
                      </button>
                    )}
                  </div>
                </div>

                {/* Flipkart-style Delivery Location Pin Button (replaces Moon theme toggle in header) */}
                <button
                  type="button"
                  onClick={() => {
                    haptics.light();
                    setQuickPincodeInput(activeAddressObj?.pincode || '');
                    setPincodeStatusMessage(null);
                    setIsAddressModalOpen(true);
                  }}
                  className="relative p-1.5 text-slate-800 dark:text-slate-200 hover:bg-orange-100/70 dark:hover:bg-stone-800 rounded-full transition-colors shrink-0 cursor-pointer"
                  title={`Delivering to ${activeAddressObj?.city || 'Select Location'} (${activeAddressObj?.pincode || 'Set PIN'})`}
                  aria-label="Change delivery location"
                >
                  <MapPin className="w-5 h-5 stroke-[2] text-[#FF5722]" />
                  {activeAddressObj?.pincode && (
                    <span className="absolute top-1 right-1 w-2 h-2 rounded-full bg-emerald-500 ring-1 ring-white dark:ring-[#181311]" />
                  )}
                </button>

                {/* Right: Shopping Cart with Live Count Badge */}
                <button
                  type="button"
                  onClick={() => switchTab('cart')}
                  className="relative p-1.5 text-slate-800 dark:text-slate-100 hover:bg-orange-100/70 dark:hover:bg-stone-800 rounded-full transition-colors shrink-0 cursor-pointer"
                  aria-label="Cart"
                >
                  <ShoppingCart className="w-5 h-5 stroke-[2]" />
                  {totalCartCount > 0 && (
                    <span className="absolute -top-1 -right-1 min-w-[17px] h-4 px-1 bg-[#FF5722] text-white text-[9px] font-black rounded-full flex items-center justify-center shadow-xs">
                      {totalCartCount}
                    </span>
                  )}
                </button>
              </div>

              {/* Second Row: Centered AARVO Animated Logo */}
              <div className="flex items-center justify-center py-0.5">
                <AarvoAnimatedLogo
                  size="md"
                  onClick={() => switchTab('home')}
                />
              </div>

              {/* Flipkart-style Active Delivery Location Bar */}
              <button
                type="button"
                onClick={() => {
                  haptics.selection();
                  setQuickPincodeInput(activeAddressObj?.pincode || '');
                  setPincodeStatusMessage(null);
                  setIsAddressModalOpen(true);
                }}
                className="mt-1 w-full flex items-center justify-between gap-2 px-2.5 py-1.5 rounded-xl bg-white/80 dark:bg-[#211A17] border border-orange-200/70 dark:border-stone-800 hover:border-[#FF5722]/60 transition-colors text-left cursor-pointer"
              >
                <div className="flex items-center gap-1.5 min-w-0">
                  <MapPin className="w-3.5 h-3.5 text-[#FF5722] shrink-0" />
                  <span className="text-[11px] font-medium text-slate-600 dark:text-slate-300 truncate">
                    Deliver to{' '}
                    <strong className="font-bold text-slate-900 dark:text-white">
                      {activeAddressObj
                        ? `${activeAddressObj.fullName.split(' ')[0]} · ${activeAddressObj.city} - ${activeAddressObj.pincode}`
                        : 'Select Location'}
                    </strong>
                  </span>
                </div>
                <span className="shrink-0 flex items-center gap-0.5 text-[10px] font-black text-[#FF5722]">
                  <span>Change</span>
                  <ChevronDown className="w-3 h-3" />
                </span>
              </button>

              {/* Horizontal Scrollable Category Filter Bar */}
              <div
                className="mt-1.5 -mx-3.5 px-3.5 overflow-x-auto no-scrollbar scroll-smooth flex items-center gap-1.5 py-0.5"
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
                      className={`shrink-0 flex items-center gap-1 px-3 py-1 rounded-full text-[11px] font-bold transition-all duration-200 active:scale-95 cursor-pointer select-none ${
                        isSelected
                          ? 'bg-[#FF5722] text-white shadow-xs font-black'
                          : 'bg-white/80 dark:bg-[#211A17] hover:bg-orange-50 dark:hover:bg-stone-800 text-slate-700 dark:text-slate-300 border border-orange-200/70 dark:border-stone-700/70'
                      }`}
                    >
                      <span className="whitespace-nowrap">{dept.label}</span>
                    </button>
                  );
                })}
              </div>
            </header>

        {/* Rich Toast Notification Stack */}
        <ToastContainer toasts={toasts} onDismiss={dismissToast} />

        {/* ================================================================= */}
        {/* TAB 1: HOME VIEW                                                  */}
        {/* ================================================================= */}
        {activeTab === 'home' && (
          <div className="flex-1 flex flex-col pb-6">
            {searchQuery.trim() !== '' ? (
              /* REAL-TIME LIVE SEARCH RESULTS VIEW */
              <section className="p-3 animate-in fade-in duration-200">
                <div className="flex items-center justify-between mb-3 bg-[#FFF4E8] dark:bg-[#211A17] p-3 rounded-2xl border border-orange-200/70 dark:border-stone-800 shadow-2xs">
                  <div className="min-w-0 pr-2">
                    <div className="flex items-center gap-1.5">
                      <Search className="w-4 h-4 text-[#FF5722] dark:text-[#FF8A65] shrink-0" />
                      <h3 className="font-display font-black text-sm text-slate-900 dark:text-white truncate">
                        Search Results
                      </h3>
                    </div>
                    <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5 truncate">
                      {filteredProducts.length} {filteredProducts.length === 1 ? 'product' : 'products'} found for &ldquo;<span className="font-bold text-slate-800 dark:text-slate-200">{searchQuery}</span>&rdquo;
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() => {
                      haptics.light();
                      setSearchQuery('');
                    }}
                    className="shrink-0 text-xs font-bold text-rose-600 dark:text-rose-400 flex items-center gap-1 px-2.5 py-1 rounded-full bg-rose-50 dark:bg-rose-950/40 hover:bg-rose-100 dark:hover:bg-rose-900/50 active:scale-95 transition-all cursor-pointer"
                  >
                    <X className="w-3.5 h-3.5" />
                    <span>Clear</span>
                  </button>
                </div>

                {/* Horizontal row of clickable category chips to further refine search results */}
                <div className="mb-3.5">
                  <div className="flex items-center justify-between mb-2 px-0.5">
                    <span className="text-[11px] font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
                      <span>Refine by Category</span>
                      {selectedCategory !== 'All' && (
                        <span className="text-[#FF5722] dark:text-[#FF8A65] font-black normal-case">
                          &bull; {selectedCategory}
                        </span>
                      )}
                    </span>
                    {selectedCategory !== 'All' && (
                      <button
                        type="button"
                        onClick={() => {
                          haptics.light();
                          setSelectedCategory('All');
                        }}
                        className="text-[11px] font-bold text-[#FF5722] dark:text-[#FF8A65] hover:underline flex items-center gap-0.5 active:scale-95 transition-transform cursor-pointer"
                      >
                        <span>Show All</span>
                        <X className="w-3 h-3" />
                      </button>
                    )}
                  </div>

                  <div
                    className="-mx-3 px-3 overflow-x-auto no-scrollbar scroll-smooth flex items-center gap-2 py-1"
                    role="tablist"
                    aria-label="Filter search results by category"
                  >
                    {searchCategoryChips.map((chip) => {
                      const isSelected = selectedCategory === chip.id;
                      const hasMatches = chip.count > 0;
                      return (
                        <button
                          key={chip.id}
                          type="button"
                          role="tab"
                          aria-selected={isSelected}
                          onClick={() => {
                            haptics.selection();
                            setSelectedCategory(chip.id);
                          }}
                          className={`shrink-0 flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-bold transition-all duration-200 active:scale-95 cursor-pointer select-none ${
                            isSelected
                              ? 'bg-[#FF5722] text-white shadow-sm ring-2 ring-[#FF5722]/20 font-extrabold'
                              : hasMatches
                              ? 'bg-white dark:bg-[#211A17] text-slate-800 dark:text-slate-200 border border-orange-200/70 dark:border-stone-700/80 hover:border-[#FF5722]/50 shadow-2xs'
                              : 'bg-slate-50 dark:bg-stone-900/40 text-slate-400 dark:text-slate-500 border border-slate-100 dark:border-stone-800/60 opacity-60'
                          }`}
                        >
                          <span className="whitespace-nowrap">{chip.label}</span>
                          <span
                            className={`text-[10px] px-1.5 py-0.2 rounded-full font-semibold ${
                              isSelected
                                ? 'bg-white/25 text-white'
                                : hasMatches
                                ? 'bg-orange-50 dark:bg-stone-800 text-slate-600 dark:text-slate-400'
                                : 'bg-transparent text-slate-400'
                            }`}
                          >
                            {chip.count}
                          </span>
                        </button>
                      );
                    })}
                  </div>
                </div>

                {/* Sort Selector Row */}
                <div className="mb-3 flex items-center gap-1.5 overflow-x-auto no-scrollbar pb-0.5" role="group" aria-label="Sort search results">
                  <span className="text-[11px] font-semibold text-slate-500 dark:text-slate-400 shrink-0 mr-1">
                    Sort:
                  </span>
                  {PRODUCT_SORT_OPTIONS.map((opt) => {
                    const active = sortBy === opt.id;
                    return (
                      <button
                        key={opt.id}
                        type="button"
                        onClick={() => {
                          haptics.selection();
                          setSortBy(opt.id);
                        }}
                        className={`px-2.5 py-1 rounded-lg text-[11px] font-semibold whitespace-nowrap shrink-0 transition-colors cursor-pointer ${
                          active
                            ? 'bg-slate-900 text-white dark:bg-[#FF5722] dark:text-white shadow-2xs'
                            : 'bg-white dark:bg-[#211A17] text-slate-700 dark:text-slate-300 border border-orange-200/70 dark:border-stone-700 hover:border-[#FF5722]/60'
                        }`}
                      >
                        {opt.label}
                      </button>
                    );
                  })}
                </div>

                {/* Price Range Filter Selector Row */}
                <div className="mb-3 flex items-center gap-1.5 overflow-x-auto no-scrollbar pb-0.5" role="group" aria-label="Filter search results by price range">
                  <button
                    type="button"
                    onClick={openPriceFilterModal}
                    className={`px-2.5 py-1 rounded-lg text-[11px] font-bold whitespace-nowrap shrink-0 flex items-center gap-1 transition-colors cursor-pointer ${
                      hasPriceFilter
                        ? 'bg-[#FF5722] text-white shadow-2xs'
                        : 'bg-[#FFF4E8] dark:bg-[#2B1A12] text-[#E64A19] dark:text-[#FF8A65] border border-orange-200 dark:border-stone-700'
                    }`}
                  >
                    <SlidersHorizontal className="w-3 h-3" />
                    <span>{activePriceFilterLabel || 'Price Range'}</span>
                  </button>
                  {PRICE_FILTER_PRESETS.map((preset) => {
                    const active =
                      minPriceFilter === preset.min && maxPriceFilter === preset.max;
                    return (
                      <button
                        key={preset.label}
                        type="button"
                        onClick={() => applyPricePreset(preset.min, preset.max, preset.label)}
                        className={`px-2.5 py-1 rounded-lg text-[11px] font-semibold whitespace-nowrap shrink-0 transition-colors cursor-pointer ${
                          active
                            ? 'bg-slate-900 text-white dark:bg-[#FF5722] dark:text-white shadow-2xs'
                            : 'bg-white dark:bg-[#211A17] text-slate-700 dark:text-slate-300 border border-orange-200/70 dark:border-stone-700 hover:border-[#FF5722]/60'
                        }`}
                      >
                        {preset.label}
                      </button>
                    );
                  })}
                </div>

                {isProductsLoading ? (
                  <ProductGridSkeleton
                    count={4}
                    variant="compact"
                    label="Loading search results"
                  />
                ) : filteredProducts.length > 0 ? (
                  <div className="grid grid-cols-2 gap-3">
                    {filteredProducts.map((product) => (
                      <div
                        key={product.id}
                        onClick={() => {
                          haptics.light();
                          setSelectedProduct(product);
                        }}
                        className="bg-white dark:bg-[#211A17] rounded-2xl p-2.5 border border-orange-100/80 dark:border-stone-800/80 shadow-xs flex flex-col justify-between hover:shadow-md transition-all group cursor-pointer"
                      >
                        <div className="relative aspect-square w-full rounded-xl overflow-hidden bg-slate-50 dark:bg-slate-900 mb-2">
                          <img
                            src={product.image}
                            alt={product.name}
                            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                          />
                          {product.originalPrice > product.price && (
                            <span className="absolute top-1.5 left-1.5 bg-emerald-600 text-white text-[9px] font-black px-1.5 py-0.5 rounded-md shadow-xs">
                              {product.discount}
                            </span>
                          )}

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
                            {product.originalPrice > product.price && (
                              <span className="text-[10px] text-slate-400 dark:text-slate-500 line-through">
                                ₹{product.originalPrice.toLocaleString('en-IN')}
                              </span>
                            )}
                          </div>
                        </div>

                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation();
                            addToCart(product);
                          }}
                          className="mt-2.5 w-full py-1.5 bg-[#FF5722] hover:bg-[#E64A19] text-white text-xs font-bold rounded-xl flex items-center justify-center gap-1.5 active:scale-95 transition-all shadow-xs cursor-pointer"
                        >
                          <ShoppingCart className="w-3.5 h-3.5" />
                          <span>Add to Cart</span>
                        </button>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="py-12 px-4 text-center bg-[#FFF4E8] dark:bg-[#211A17] rounded-2xl border border-dashed border-orange-200 dark:border-stone-800">
                    <div className="w-12 h-12 mx-auto rounded-full bg-orange-100/70 dark:bg-stone-800/80 flex items-center justify-center text-[#FF5722] mb-3">
                      <Search className="w-6 h-6 stroke-[1.5]" />
                    </div>
                    <h4 className="font-bold text-sm text-slate-800 dark:text-white mb-1">
                      {selectedCategory !== 'All'
                        ? `No ${selectedCategory} products found`
                        : 'No matching products found'}
                    </h4>
                    <p className="text-xs text-slate-500 dark:text-slate-400 max-w-[260px] mx-auto mb-4">
                      {selectedCategory !== 'All' ? (
                        <>
                          We found no {selectedCategory} items matching &ldquo;{searchQuery}&rdquo;. Try clearing the category filter or switching categories.
                        </>
                      ) : (
                        <>
                          We couldn&apos;t find anything matching &ldquo;{searchQuery}&rdquo;. Try another keyword or popular categories below.
                        </>
                      )}
                    </p>
                    {selectedCategory !== 'All' && queryMatchedProducts.length > 0 ? (
                      <div className="flex flex-col items-center gap-2 mb-2">
                        <button
                          type="button"
                          onClick={() => {
                            haptics.light();
                            setSelectedCategory('All');
                          }}
                          className="px-4 py-2 bg-[#FF5722] text-white text-xs font-bold rounded-xl shadow-xs hover:bg-[#E64A19] active:scale-95 transition-all cursor-pointer"
                        >
                          Show All Categories ({queryMatchedProducts.length} items)
                        </button>
                        <button
                          type="button"
                          onClick={() => {
                            haptics.light();
                            setSearchQuery('');
                            setSelectedCategory('All');
                          }}
                          className="text-xs font-semibold text-slate-500 dark:text-slate-400 hover:underline cursor-pointer"
                        >
                          Clear Search
                        </button>
                      </div>
                    ) : (
                      <>
                        <div className="flex flex-wrap justify-center gap-1.5 mb-4">
                          {['Shoes', 'Phone', 'Kurta', 'Neem', 'Earbuds', 'Blender'].map((tag) => (
                            <button
                              key={tag}
                              type="button"
                              onClick={() => {
                                haptics.light();
                                setSearchQuery(tag);
                                setSelectedCategory('All');
                              }}
                              className="px-2.5 py-1 text-xs font-medium rounded-full bg-white dark:bg-stone-800 text-[#FF5722] dark:text-[#FF8A65] border border-orange-200 dark:border-stone-700 shadow-2xs hover:bg-orange-50 dark:hover:bg-stone-700 active:scale-95 cursor-pointer"
                            >
                              {tag}
                            </button>
                          ))}
                        </div>
                        <button
                          type="button"
                          onClick={() => {
                            haptics.light();
                            setSearchQuery('');
                            setSelectedCategory('All');
                          }}
                          className="px-4 py-2 bg-[#FF5722] text-white text-xs font-bold rounded-xl shadow-xs hover:bg-[#E64A19] active:scale-95 transition-all cursor-pointer"
                        >
                          Clear Search
                        </button>
                      </>
                    )}
                  </div>
                )}
              </section>
            ) : (
              <>
            {/* ================================================================= */}
            {/* 1. HERO BANNER CAROUSEL                                           */}
            {/* ================================================================= */}
            <section className="px-3.5 pt-2.5 pb-1.5">
              {(() => {
                const activeSlide = slides[currentSlide] || slides[0];
                return (
                  <div className={`relative h-[148px] sm:h-[164px] w-full rounded-2xl overflow-hidden shadow-md bg-gradient-to-r ${activeSlide.bgGradient} flex items-center justify-between px-4 py-3 sm:px-5 text-white transition-all duration-300`}>
                    <div className="max-w-[56%] z-10 flex flex-col justify-center pb-2">
                      <h2 className="text-lg sm:text-xl font-black font-display tracking-tight text-white leading-tight">
                        {activeSlide.titlePrimary}
                      </h2>
                      <h3 className="text-lg sm:text-xl font-black font-display tracking-tight text-white leading-tight">
                        {activeSlide.titleSecondary}
                      </h3>
                      {activeSlide.subtitle && !activeSlide.isGraphicBanner && (
                        <p className="text-[10px] text-white/90 font-medium mt-1 line-clamp-1">
                          {activeSlide.subtitle}
                        </p>
                      )}

                      <button
                        type="button"
                        onClick={() => {
                          if (activeSlide.id === 3) {
                            setSelectedCategory('Electronics');
                            switchTab('categories');
                          } else {
                            switchTab('categories');
                          }
                        }}
                        className="mt-2.5 w-fit px-3.5 py-1 bg-[#FFD700] hover:bg-[#FACC15] text-slate-950 font-black text-[11px] rounded-full shadow-sm flex items-center gap-1 active:scale-95 transition-transform cursor-pointer whitespace-nowrap"
                      >
                        <span>{activeSlide.ctaText}</span>
                        <span className="font-bold">&gt;</span>
                      </button>
                    </div>

                    {activeSlide.image && (
                      <div className="absolute right-0 bottom-0 h-full w-[48%] flex items-end justify-end pointer-events-none">
                        <img
                          src={activeSlide.image}
                          alt={`${activeSlide.titlePrimary} ${activeSlide.titleSecondary}`}
                          className="h-full w-full object-cover object-top filter drop-shadow-[0_4px_12px_rgba(0,0,0,0.28)]"
                        />
                        <div className="absolute inset-y-0 left-0 w-10 bg-gradient-to-r from-[#2563EB] to-transparent" />
                      </div>
                    )}

                    {/* Clickable Pagination Dots */}
                    <div className="absolute bottom-2 left-0 right-0 flex items-center justify-center gap-1.5 z-10">
                      {slides.map((s, idx) => (
                        <button
                          key={s.id}
                          type="button"
                          aria-label={`Slide ${idx + 1}`}
                          onClick={() => setCurrentSlide(idx)}
                          className={`h-1.5 rounded-full transition-all cursor-pointer ${
                            currentSlide === idx ? 'w-3.5 bg-[#FFD700]' : 'w-1.5 bg-white/60 hover:bg-white/90'
                          }`}
                        />
                      ))}
                    </div>
                  </div>
                );
              })()}
            </section>

            {/* ================================================================= */}
            {/* 2. SHOP BY CATEGORY SECTION                                       */}
            {/* ================================================================= */}
            <section className="px-3.5 py-2">
              <div className="flex items-center justify-between mb-2.5">
                <div className="flex items-center gap-2">
                  <LayoutGrid className="w-4 h-4 text-[#FF5722]" />
                  <h3 className="font-display font-black text-sm text-slate-900 dark:text-white">
                    Shop by Category
                  </h3>
                </div>
                <button
                  type="button"
                  onClick={() => {
                    setSelectedCategory('All');
                    switchTab('categories');
                  }}
                  className="text-xs font-bold text-[#FF5722] hover:underline flex items-center gap-0.5 cursor-pointer"
                >
                  <span>See All</span>
                  <span className="font-bold">&gt;</span>
                </button>
              </div>

              <div className="grid grid-cols-5 gap-2">
                {[
                  {
                    name: 'Fashion',
                    filterKey: 'Fashion',
                    img: catFashionBagImg,
                    bg: 'bg-[#FFF0F5] dark:bg-rose-950/30',
                  },
                  {
                    name: 'Mobiles',
                    filterKey: 'Mobiles',
                    img: catMobilesPhoneImg,
                    bg: 'bg-[#EEF4FF] dark:bg-blue-950/30',
                  },
                  {
                    name: 'Electronics',
                    filterKey: 'Electronics',
                    img: catElectronicsLaptopImg,
                    bg: 'bg-[#F0F2FF] dark:bg-indigo-950/30',
                  },
                  {
                    name: 'Home & Kitchen',
                    filterKey: 'Home',
                    img: catHomeBlenderImg,
                    bg: 'bg-[#FFF6EB] dark:bg-amber-950/30',
                  },
                  {
                    name: 'Beauty & Personal Care',
                    filterKey: 'Beauty',
                    img: catBeautySerumImg,
                    bg: 'bg-[#FFF1EA] dark:bg-orange-950/30',
                  },
                ].map((item, idx) => (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => {
                      haptics.selection();
                      setSelectedCategory(item.filterKey);
                      switchTab('categories');
                    }}
                    className="flex flex-col items-center group cursor-pointer transition-transform active:scale-95"
                  >
                    <div className={`w-full aspect-square rounded-2xl ${item.bg} border border-orange-100/80 dark:border-stone-800 overflow-hidden p-1 flex items-center justify-center shadow-2xs group-hover:shadow-xs transition-shadow`}>
                      <img
                        src={item.img}
                        alt={item.name}
                        className="w-full h-full object-cover rounded-xl group-hover:scale-105 transition-transform"
                      />
                    </div>
                    <span className="text-[10px] font-bold text-slate-700 dark:text-slate-300 mt-1.5 text-center leading-tight line-clamp-2">
                      {item.name}
                    </span>
                  </button>
                ))}
              </div>
            </section>

            {/* ================================================================= */}
            {/* 3. WARM GOLDEN PROMO BANNER: "Great Deals On Top Brands"          */}
            {/* ================================================================= */}
            <section className="px-3.5 py-1.5">
              <div className="relative h-[114px] sm:h-[124px] w-full rounded-2xl overflow-hidden shadow-sm bg-gradient-to-r from-[#FDE047] via-[#FBBF24] to-[#F59E0B] px-4 py-3 flex items-center justify-between">
                <div className="max-w-[56%] z-10">
                  <h3 className="text-base sm:text-lg font-black font-display tracking-tight text-slate-950 leading-tight">
                    Great Deals
                  </h3>
                  <h4 className="text-base sm:text-lg font-black font-display tracking-tight text-slate-950 leading-tight">
                    On Top Brands
                  </h4>
                  <button
                    type="button"
                    onClick={() => switchTab('offers')}
                    className="mt-2 px-3 py-1 bg-slate-950 hover:bg-black text-white font-bold text-[10px] rounded-full shadow-xs inline-flex items-center gap-1 active:scale-95 transition-transform cursor-pointer whitespace-nowrap"
                  >
                    <span>Shop Now</span>
                    <span className="font-bold">&gt;</span>
                  </button>
                </div>

                {/* Right Gadgets Image */}
                <div className="absolute right-0 bottom-0 h-full w-[50%] flex items-center justify-end pointer-events-none">
                  <img
                    src={promoGadgetsTrioImg}
                    alt="Top Brands Deals"
                    className="h-full w-full object-cover object-center"
                  />
                  <div className="absolute inset-y-0 left-0 w-12 bg-gradient-to-r from-[#FBBF24] to-transparent" />
                </div>
              </div>
            </section>

            {/* ================================================================= */}
            {/* 4. TOP DEALS (2-COLUMN CARDS MATCHING LEFT PHONE MOCKUP)          */}
            {/* ================================================================= */}
            <section className="px-3.5 py-2">
              {/* Header Row */}
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-1.5">
                  <span className="text-base">🔥</span>
                  <h3 className="font-display font-black text-sm text-slate-900 dark:text-white">
                    {hasPriceFilter ? `Top Deals (${homeFeedProducts.length})` : 'Top Deals'}
                  </h3>
                </div>
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={openPriceFilterModal}
                    className={`px-2 py-0.5 rounded-lg text-[11px] font-bold flex items-center gap-1 cursor-pointer transition-colors ${
                      hasPriceFilter
                        ? 'bg-[#FF5722] text-white'
                        : 'bg-[#FFF4E8] dark:bg-[#2B1A12] text-[#E64A19] dark:text-[#FF8A65] border border-orange-200/80 dark:border-stone-700'
                    }`}
                  >
                    <SlidersHorizontal className="w-3 h-3" />
                    <span>{activePriceFilterLabel || 'Price'}</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => switchTab('categories')}
                    className="text-xs font-bold text-[#FF5722] hover:underline flex items-center gap-0.5 cursor-pointer"
                  >
                    <span>See All</span>
                    <span className="font-bold">&gt;</span>
                  </button>
                </div>
              </div>

              {/* Inline Price Range Filter Presets on Home */}
              <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar pb-2 mb-1" role="group" aria-label="Home price range filter">
                {PRICE_FILTER_PRESETS.map((preset) => {
                  const active =
                    minPriceFilter === preset.min && maxPriceFilter === preset.max;
                  return (
                    <button
                      key={preset.label}
                      type="button"
                      onClick={() => applyPricePreset(preset.min, preset.max, preset.label)}
                      className={`px-2.5 py-1 rounded-lg text-[11px] font-semibold whitespace-nowrap shrink-0 transition-colors cursor-pointer ${
                        active
                          ? 'bg-slate-900 text-white dark:bg-[#FF5722] dark:text-white shadow-2xs'
                          : 'bg-white dark:bg-[#211A17] text-slate-700 dark:text-slate-300 border border-orange-200/70 dark:border-stone-700 hover:border-[#FF5722]/60'
                      }`}
                    >
                      {preset.label}
                    </button>
                  );
                })}
              </div>

              {/* 2-Column Product Cards */}
              {isProductsLoading ? (
                <ProductGridSkeleton
                  count={2}
                  variant="detailed"
                  label="Loading top deals"
                />
              ) : homeFeedProducts.length === 0 ? (
                <div className="py-6 px-4 text-center bg-[#FFF4E8] dark:bg-[#211A17] rounded-2xl border border-dashed border-orange-200 dark:border-stone-800 space-y-2">
                  <p className="text-xs font-bold text-slate-800 dark:text-white">
                    No products match {activePriceFilterLabel}
                  </p>
                  <button
                    type="button"
                    onClick={() => applyPricePreset(null, null, 'All Prices')}
                    className="px-3 py-1.5 bg-[#FF5722] text-white text-xs font-bold rounded-xl cursor-pointer"
                  >
                    Reset Price Filter
                  </button>
                </div>
              ) : (
                <div className="grid grid-cols-2 gap-3">
                  {homeFeedProducts.slice(0, 2).map((product) => (
                    <div
                      key={product.id}
                      onClick={() => {
                        haptics.light();
                        setSelectedProduct(product);
                      }}
                      className="bg-white dark:bg-[#211A17] rounded-2xl p-2.5 border border-orange-100/80 dark:border-stone-800 shadow-2xs hover:shadow-md transition-all flex flex-col justify-between group cursor-pointer"
                    >
                      <div className="relative aspect-square w-full rounded-xl overflow-hidden bg-[#FFF9F2] dark:bg-stone-900 mb-2">
                        <img
                          src={product.image}
                          alt={product.name}
                          className="w-full h-full object-contain p-2 group-hover:scale-105 transition-transform duration-300"
                        />
                        {/* Green Rating Badge only if product has genuine reviews */}
                        {product.reviewsCount > 0 && (
                          <span className="absolute bottom-1.5 left-1.5 bg-emerald-600 text-white text-[10px] font-black px-1.5 py-0.2 rounded flex items-center gap-0.5 shadow-xs">
                            <span>★</span>
                            <span>{product.rating.toFixed(1)}</span>
                          </span>
                        )}

                        {/* Wishlist Heart */}
                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation();
                            toggleWishlist(product.id, product.name);
                          }}
                          aria-label="Wishlist"
                          className="absolute top-1.5 right-1.5 w-6 h-6 rounded-full bg-white/85 dark:bg-stone-900/85 backdrop-blur-xs flex items-center justify-center text-slate-400 hover:text-rose-500 shadow-2xs cursor-pointer"
                        >
                          <Heart
                            className={`w-3.5 h-3.5 ${
                              wishlist.includes(product.id)
                                ? 'fill-rose-500 stroke-rose-500 text-rose-500'
                                : 'stroke-current fill-none'
                            }`}
                          />
                        </button>
                      </div>

                      <div>
                        <h4 className="text-xs font-bold text-slate-800 dark:text-slate-100 line-clamp-2 leading-tight">
                          {product.name}
                        </h4>

                        <div className="flex items-center gap-1.5 mt-1.5 flex-wrap">
                          <span className="text-sm font-black text-slate-900 dark:text-white">
                            ₹{product.price.toLocaleString('en-IN')}
                          </span>
                          {product.originalPrice > product.price && (
                            <span className="text-[11px] text-slate-400 line-through">
                              ₹{product.originalPrice.toLocaleString('en-IN')}
                            </span>
                          )}
                          {product.discount && (
                            <span className="text-[10px] font-black text-red-600 bg-red-50 dark:bg-red-950/60 px-1 py-0.2 rounded">
                              {product.discount}
                            </span>
                          )}
                        </div>
                      </div>

                      <div className="flex items-center justify-between pt-2 mt-1 border-t border-orange-100/70 dark:border-stone-800/80">
                        <span className="text-[10px] text-emerald-600 font-bold">
                          Free Delivery
                        </span>
                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation();
                            addToCart(product);
                          }}
                          className="p-1.5 rounded-lg bg-orange-50 dark:bg-stone-800 hover:bg-[#FF5722] text-[#FF5722] hover:text-white transition-colors cursor-pointer"
                          title="Add to Cart"
                        >
                          <ShoppingCart className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </section>

            {/* ================================================================= */}
            {/* 5. "MORE PRODUCTS FOR YOU"                                        */}
            {/* ================================================================= */}
            <section className="px-3.5 py-2 relative">
              <div className="flex items-center justify-between mb-2.5">
                <h3 className="font-display font-black text-sm text-slate-900 dark:text-white">
                  More products for you
                </h3>
                <button
                  type="button"
                  onClick={() => switchTab('categories')}
                  className="text-xs font-bold text-[#FF5722] hover:underline cursor-pointer"
                >
                  View All &gt;
                </button>
              </div>

              {isProductsLoading ? (
                <ProductGridSkeleton
                  count={4}
                  variant="compact"
                  label="Loading recommended products"
                />
              ) : (
                <div className="grid grid-cols-2 gap-3">
                  {(homeFeedProducts.length > 2 ? homeFeedProducts.slice(2, 6) : homeFeedProducts).map((product) => (
                    <div
                      key={product.id}
                      onClick={() => {
                        haptics.light();
                        setSelectedProduct(product);
                      }}
                      className="bg-white dark:bg-[#211A17] rounded-2xl p-2.5 border border-orange-100/80 dark:border-stone-800 shadow-2xs hover:shadow-md transition-all flex flex-col justify-between group cursor-pointer"
                    >
                      <div className="relative aspect-square w-full rounded-xl overflow-hidden bg-[#FFF9F2] dark:bg-stone-900 mb-2">
                        <img
                          src={product.image}
                          alt={product.name}
                          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                        />

                        {product.originalPrice > product.price && product.discount && (
                          <span className="absolute top-1.5 left-1.5 bg-emerald-600 text-white text-[9px] font-black px-1.5 py-0.5 rounded-md shadow-xs">
                            {product.discount}
                          </span>
                        )}

                        {/* Genuine Rating pill at bottom if rated */}
                        {product.reviewsCount > 0 && (
                          <span className="absolute bottom-1.5 left-1.5 bg-white/90 dark:bg-stone-900/90 backdrop-blur-xs text-slate-800 dark:text-slate-200 text-[10px] font-bold px-1.5 py-0.5 rounded shadow-xs flex items-center gap-1">
                            <span>{product.rating.toFixed(1)}</span>
                            <Star className="w-2.5 h-2.5 fill-emerald-600 text-emerald-600" />
                            <span className="text-slate-400 font-normal">({product.reviewsCount})</span>
                          </span>
                        )}
                      </div>

                      <div>
                        <h4 className="text-xs font-bold text-slate-800 dark:text-slate-100 line-clamp-2 leading-tight">
                          {product.name}
                        </h4>

                        <div className="flex items-baseline gap-1.5 mt-1">
                          <span className="text-sm font-black text-slate-900 dark:text-white">
                            ₹{product.price.toLocaleString('en-IN')}
                          </span>
                          {product.originalPrice > product.price && (
                            <span className="text-[10px] text-slate-400 line-through">
                              ₹{product.originalPrice.toLocaleString('en-IN')}
                            </span>
                          )}
                        </div>
                      </div>

                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          addToCart(product);
                        }}
                        className="mt-2 w-full py-1.5 bg-orange-50 dark:bg-stone-800 hover:bg-[#FF5722] text-[#FF5722] hover:text-white text-xs font-bold rounded-xl flex items-center justify-center gap-1.5 transition-colors cursor-pointer"
                      >
                        <ShoppingCart className="w-3.5 h-3.5" />
                        <span>Add to Cart</span>
                      </button>
                    </div>
                  ))}
                </div>
              )}

              {/* Floating "↓ More below" pill button */}
              <div className="flex justify-center mt-3">
                <button
                  type="button"
                  onClick={() => switchTab('categories')}
                  className="px-4 py-1.5 rounded-full bg-white dark:bg-[#211A17] border border-orange-200 dark:border-stone-700 shadow-md text-xs font-bold text-slate-800 dark:text-slate-200 flex items-center gap-1.5 hover:shadow-lg active:scale-95 transition-all cursor-pointer"
                >
                  <span className="text-[#FF5722] font-black">↓</span>
                  <span>More below</span>
                </button>
              </div>
            </section>
          </>
        )}
      </div>
    )}

        {/* ================================================================= */}
        {/* TAB 2: CATEGORIES VIEW                                            */}
        {/* ================================================================= */}
        {/* ================================================================= */}
        {/* TAB 2: FLIPKART-STYLE 2-PANE CATEGORIES EXPLORER                  */}
        {/* ================================================================= */}
        {activeTab === 'categories' && (
          <div className="flex-1 flex overflow-hidden bg-[#FFFDF9] dark:bg-[#120E0C]">
            {/* Left Vertical Category Rail */}
            <aside className="w-20 sm:w-24 shrink-0 overflow-y-auto no-scrollbar border-r border-orange-100/80 dark:border-stone-800 bg-[#FFF8F1] dark:bg-[#181311] divide-y divide-orange-100/60 dark:divide-stone-800/60">
              {/* All Products Option */}
              <button
                type="button"
                onClick={() => {
                  haptics.selection();
                  setSelectedCategory('All');
                }}
                className={`w-full py-3 px-1.5 flex flex-col items-center text-center transition-all cursor-pointer ${
                  selectedCategory === 'All'
                    ? 'bg-[#FFF4E8] dark:bg-[#2E1A12] text-[#FF5722] dark:text-[#FF8A65] font-black ring-1 ring-[#FF5722]/30 shadow-2xs'
                    : 'text-slate-600 dark:text-slate-400 hover:bg-orange-50/70 dark:hover:bg-stone-800/40 font-medium'
                }`}
              >
                <div className={`w-10 h-10 rounded-full flex items-center justify-center mb-1 text-base ${
                  selectedCategory === 'All'
                    ? 'bg-[#FF5722] text-white shadow-xs'
                    : 'bg-orange-100/80 dark:bg-stone-800 text-slate-700 dark:text-slate-300'
                }`}>
                  <LayoutGrid className="w-5 h-5 stroke-[2.2]" />
                </div>
                <span className="text-[10px] leading-tight line-clamp-1">All</span>
              </button>

              {/* Categorised Rail Items */}
              {categories.map((cat, idx) => {
                const isCatActive = selectedCategory === cat.name;
                const IconComponent = cat.icon;
                return (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => {
                      haptics.selection();
                      setSelectedCategory(cat.name);
                    }}
                    className={`w-full py-3 px-1 flex flex-col items-center text-center transition-all cursor-pointer ${
                      isCatActive
                        ? 'bg-[#FFF4E8] dark:bg-[#2E1A12] text-[#FF5722] dark:text-[#FF8A65] font-black ring-1 ring-[#FF5722]/30 shadow-2xs'
                        : 'text-slate-600 dark:text-slate-400 hover:bg-orange-50/70 dark:hover:bg-stone-800/40 font-medium'
                    }`}
                  >
                    <div className={`w-10 h-10 rounded-full flex items-center justify-center mb-1 transition-transform ${
                      isCatActive ? 'scale-105 shadow-xs ring-2 ring-[#FF5722]/25' : ''
                    } ${cat.color}`}>
                      <IconComponent className="w-5 h-5 stroke-[2.2]" />
                    </div>
                    <span className="text-[10px] leading-tight line-clamp-2 px-0.5">
                      {cat.name}
                    </span>
                  </button>
                );
              })}
            </aside>

            {/* Right Pane: Subcategories & Product Cards Grid */}
            <div className="flex-1 overflow-y-auto p-2.5 pb-24 no-scrollbar space-y-2.5">
              {/* Category Header Banner */}
              <div className="flex items-center justify-between p-2.5 rounded-xl bg-[#FFF4E8] dark:bg-[#211A17] border border-orange-100 dark:border-stone-800/70">
                <div>
                  <h3 className="font-display font-black text-xs text-slate-900 dark:text-white uppercase tracking-wider">
                    {selectedCategory === 'All' ? 'All Collections' : selectedCategory}
                  </h3>
                  <span className="text-[10px] text-slate-500 dark:text-slate-400">
                    {isProductsLoading
                      ? 'Loading items...'
                      : `${filteredProducts.length} items available`}
                  </span>
                </div>
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => {
                      haptics.selection();
                      fetchCatalogProducts();
                    }}
                    aria-label="Refresh products"
                    title="Refresh products"
                    className="p-1.5 rounded-lg bg-white/80 dark:bg-stone-800 text-slate-600 dark:text-slate-300 hover:text-[#FF5722] border border-orange-200/60 dark:border-stone-700 cursor-pointer"
                  >
                    <RotateCcw className={`w-3.5 h-3.5 ${isProductsLoading ? 'animate-spin text-[#FF5722]' : ''}`} />
                  </button>
                  {selectedCategory !== 'All' && (
                    <button
                      type="button"
                      onClick={() => setSelectedCategory('All')}
                      className="text-[10px] font-bold text-[#FF5722] dark:text-[#FF8A65] hover:underline cursor-pointer"
                    >
                      Show All
                    </button>
                  )}
                </div>
              </div>

              {/* Sort Bar for Categories */}
              <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar pb-0.5" role="group" aria-label="Sort category products">
                <span className="text-[11px] font-semibold text-slate-500 dark:text-slate-400 shrink-0 mr-0.5">
                  Sort:
                </span>
                {PRODUCT_SORT_OPTIONS.map((opt) => {
                  const active = sortBy === opt.id;
                  return (
                    <button
                      key={opt.id}
                      type="button"
                      onClick={() => {
                        haptics.selection();
                        setSortBy(opt.id);
                      }}
                      className={`px-2.5 py-1 rounded-lg text-[11px] font-semibold whitespace-nowrap shrink-0 transition-colors cursor-pointer ${
                        active
                          ? 'bg-slate-900 text-white dark:bg-[#FF5722] dark:text-white shadow-2xs'
                          : 'bg-white dark:bg-[#211A17] text-slate-700 dark:text-slate-300 border border-orange-200/70 dark:border-stone-700 hover:border-[#FF5722]/60'
                      }`}
                    >
                      {opt.label}
                    </button>
                  );
                })}
              </div>

              {/* Price Range Filter Bar for Categories */}
              <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar pb-0.5" role="group" aria-label="Filter category products by price range">
                <button
                  type="button"
                  onClick={openPriceFilterModal}
                  className={`px-2 py-1 rounded-lg text-[11px] font-bold whitespace-nowrap shrink-0 flex items-center gap-1 cursor-pointer transition-colors ${
                    hasPriceFilter
                      ? 'bg-[#FF5722] text-white'
                      : 'bg-[#FFF4E8] dark:bg-[#2B1A12] text-[#E64A19] dark:text-[#FF8A65] border border-orange-200 dark:border-stone-700'
                  }`}
                >
                  <SlidersHorizontal className="w-3 h-3" />
                  <span>{activePriceFilterLabel || 'Price'}</span>
                </button>
                {PRICE_FILTER_PRESETS.map((preset) => {
                  const active =
                    minPriceFilter === preset.min && maxPriceFilter === preset.max;
                  return (
                    <button
                      key={preset.label}
                      type="button"
                      onClick={() => applyPricePreset(preset.min, preset.max, preset.label)}
                      className={`px-2 py-1 rounded-lg text-[10px] font-semibold whitespace-nowrap shrink-0 transition-colors cursor-pointer ${
                        active
                          ? 'bg-slate-900 text-white dark:bg-[#FF5722] dark:text-white shadow-2xs'
                          : 'bg-white dark:bg-[#211A17] text-slate-700 dark:text-slate-300 border border-orange-200/70 dark:border-stone-700'
                      }`}
                    >
                      {preset.label}
                    </button>
                  );
                })}
              </div>

              {/* Products Listing Grid */}
              {isProductsLoading ? (
                <ProductGridSkeleton
                  count={6}
                  variant="compact"
                  gapClass="gap-2"
                  label="Loading category products"
                />
              ) : filteredProducts.length > 0 ? (
                <div className="grid grid-cols-2 gap-2">
                  {filteredProducts.map((product) => (
                    <div
                      key={product.id}
                      onClick={() => {
                        haptics.light();
                        setSelectedProduct(product);
                      }}
                      className="bg-white dark:bg-[#211A17] rounded-2xl p-2 border border-orange-100/80 dark:border-stone-800/80 shadow-2xs hover:shadow-md transition-all cursor-pointer flex flex-col justify-between group active:scale-[0.98]"
                    >
                      {/* Product Image Zone */}
                      <div className="relative aspect-square w-full rounded-xl overflow-hidden bg-[#FFF9F2] dark:bg-stone-900 mb-1.5">
                        <img
                          src={product.image}
                          alt={product.name}
                          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                        />
                        {product.originalPrice > product.price && (
                          <span className="absolute top-1.5 left-1.5 bg-emerald-600 text-white text-[9px] font-black px-1.5 py-0.5 rounded-md shadow-xs">
                            {product.discount}
                          </span>
                        )}

                        {/* Heart Wishlist Button */}
                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation();
                            toggleWishlist(product.id, product.name);
                          }}
                          aria-label={wishlist.includes(product.id) ? 'Remove from Wishlist' : 'Add to Wishlist'}
                          className={`absolute top-1.5 right-1.5 w-6 h-6 rounded-full flex items-center justify-center transition-all duration-200 active:scale-75 shadow-xs z-10 cursor-pointer ${
                            wishlist.includes(product.id)
                              ? 'bg-rose-50 dark:bg-rose-950/90 text-rose-500 scale-105'
                              : 'bg-white/90 dark:bg-stone-900/90 backdrop-blur-xs text-slate-400 dark:text-slate-300 hover:text-rose-500'
                          }`}
                        >
                          <Heart
                            className={`w-3.5 h-3.5 ${
                              wishlist.includes(product.id)
                                ? 'fill-rose-500 stroke-rose-500'
                                : 'stroke-current fill-none'
                            }`}
                          />
                        </button>
                      </div>

                      {/* Info Zone */}
                      <div className="flex-1 flex flex-col justify-between">
                        <div>
                          <span className="text-[9px] font-black text-[#FF5722] dark:text-[#FF8A65] uppercase tracking-wider block">
                            {product.category}
                          </span>
                          <h4 className="text-[11px] font-bold text-slate-800 dark:text-slate-100 line-clamp-2 leading-tight">
                            {product.name}
                          </h4>

                          <div className="flex items-center gap-1 my-1">
                            {product.reviewsCount > 0 ? (
                              <span className="flex items-center gap-0.5 bg-emerald-600 text-white text-[9px] font-black px-1.5 py-0.2 rounded">
                                <span>{product.rating.toFixed(1)}</span>
                                <Star className="w-2 h-2 fill-white text-white" />
                              </span>
                            ) : (
                              <span className="text-[9px] text-amber-600 dark:text-amber-400 font-bold bg-amber-50 dark:bg-amber-950/40 px-1 rounded">
                                New
                              </span>
                            )}
                            <span className="text-[9px] text-slate-400">
                              ({product.reviewsCount})
                            </span>
                          </div>

                          <div className="flex items-baseline gap-1.5">
                            <span className="text-xs font-black text-slate-900 dark:text-white">
                              ₹{product.price.toLocaleString('en-IN')}
                            </span>
                            {product.originalPrice > product.price && (
                              <span className="text-[9px] text-slate-400 line-through">
                                ₹{product.originalPrice.toLocaleString('en-IN')}
                              </span>
                            )}
                          </div>
                          <span className="text-[9px] text-emerald-600 dark:text-emerald-400 font-bold block">
                            FREE Delivery
                          </span>
                        </div>

                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation();
                            addToCart(product);
                          }}
                          className="mt-2 w-full py-1.5 bg-[#FF5722] hover:bg-[#E64A19] text-white text-[11px] font-extrabold rounded-xl flex items-center justify-center gap-1 active:scale-95 transition-all shadow-2xs cursor-pointer"
                        >
                          <ShoppingCart className="w-3 h-3" />
                          <span>Add to Cart</span>
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="py-12 px-3 text-center bg-[#FFF4E8] dark:bg-[#211A17] rounded-2xl border border-dashed border-orange-200 dark:border-stone-800">
                  <div className="w-10 h-10 mx-auto rounded-full bg-orange-100/70 dark:bg-stone-800 flex items-center justify-center text-[#FF5722] mb-2">
                    <Search className="w-5 h-5" />
                  </div>
                  <h4 className="font-bold text-xs text-slate-800 dark:text-white mb-1">
                    No items in this category
                  </h4>
                  <p className="text-[10px] text-slate-500 dark:text-slate-400 mb-3">
                    Try choosing another category from the left rail.
                  </p>
                  <button
                    type="button"
                    onClick={() => setSelectedCategory('All')}
                    className="px-3 py-1.5 bg-[#FF5722] text-white text-xs font-bold rounded-xl cursor-pointer"
                  >
                    View All Products
                  </button>
                </div>
              )}
            </div>
          </div>
        )}

        {/* ================================================================= */}
        {/* TAB 3: OFFERS VIEW                                                */}
        {/* ================================================================= */}
        {activeTab === 'offers' && (
          <div className="flex-1 flex flex-col p-3 pb-6 space-y-3.5">
            {/* Live Lightning Deal Header with countdown */}
            <div className="rounded-2xl bg-gradient-to-r from-[#FF7043] via-[#FF5722] to-[#E64A19] p-4 text-white shadow-md">
              <div className="flex items-center justify-between mb-2">
                <span className="flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-white/20 text-white text-[10px] font-black uppercase tracking-wider backdrop-blur-xs">
                  <Flame className="w-3.5 h-3.5 text-amber-200 fill-amber-200 animate-pulse" />
                  Flash Lightning Sale
                </span>
                <div className="flex items-center gap-1 text-xs font-mono font-bold bg-black/25 px-2 py-0.5 rounded-lg">
                  <Clock className="w-3.5 h-3.5 text-amber-200" />
                  <span>
                    {String(timeLeft.hours).padStart(2, '0')}:{String(timeLeft.minutes).padStart(2, '0')}:
                    {String(timeLeft.seconds).padStart(2, '0')}
                  </span>
                </div>
              </div>

              <h3 className="font-display font-black text-xl leading-tight">
                Up to 75% OFF Clearance
              </h3>
              <p className="text-xs text-orange-100 mt-1">
                Extra instant savings on eligible bank cards &amp; UPI
              </p>
            </div>

            {/* Coupons Section */}
            <div className="bg-white dark:bg-[#211A17] rounded-2xl p-3 border border-orange-100 dark:border-stone-800 shadow-xs space-y-2">
              <h4 className="font-display font-bold text-xs text-slate-800 dark:text-slate-200 flex items-center gap-1.5">
                <Tag className="w-4 h-4 text-[#FF5722]" />
                <span>Available Coupons</span>
              </h4>

              <div className="p-2.5 rounded-xl bg-[#FFF4E8] dark:bg-orange-950/30 border border-orange-200/80 dark:border-orange-900/50 flex items-center justify-between gap-2">
                <div>
                  <span className="font-mono font-black text-xs text-[#E64A19] dark:text-orange-300 tracking-wider bg-white dark:bg-[#181311] px-2 py-0.5 rounded border border-orange-200 dark:border-orange-800">
                    AARVO200
                  </span>
                  <p className="text-[10px] text-slate-600 dark:text-slate-300 mt-1">
                    Save flat ₹200 on orders above ₹999
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => {
                    haptics.selection();
                    if (appliedCouponCode === 'AARVO200') {
                      setAppliedCouponCode(null);
                      showToast('Coupon AARVO200 removed');
                    } else {
                      setAppliedCouponCode('AARVO200');
                      showToast('Coupon AARVO200 applied to Cart!');
                    }
                  }}
                  className={`px-3 py-1.5 text-[11px] font-bold rounded-lg shadow-xs active:scale-95 transition-all cursor-pointer ${
                    appliedCouponCode === 'AARVO200'
                      ? 'bg-emerald-600 text-white'
                      : 'bg-[#FF5722] hover:bg-[#E64A19] text-white'
                  }`}
                >
                  {appliedCouponCode === 'AARVO200' ? 'Applied ✓' : 'Apply'}
                </button>
              </div>

              <div className="p-2.5 rounded-xl bg-[#FFF4E8] dark:bg-orange-950/30 border border-orange-200/80 dark:border-orange-900/50 flex items-center justify-between gap-2">
                <div>
                  <span className="font-mono font-black text-xs text-[#E64A19] dark:text-orange-300 tracking-wider bg-white dark:bg-[#181311] px-2 py-0.5 rounded border border-orange-200 dark:border-orange-800">
                    WELCOME100
                  </span>
                  <p className="text-[10px] text-slate-600 dark:text-slate-300 mt-1">
                    Save flat ₹100 on orders above ₹499
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => {
                    haptics.selection();
                    if (appliedCouponCode === 'WELCOME100') {
                      setAppliedCouponCode(null);
                      showToast('Coupon WELCOME100 removed');
                    } else {
                      setAppliedCouponCode('WELCOME100');
                      showToast('Coupon WELCOME100 applied to Cart!');
                    }
                  }}
                  className={`px-3 py-1.5 text-[11px] font-bold rounded-lg shadow-xs active:scale-95 transition-all cursor-pointer ${
                    appliedCouponCode === 'WELCOME100'
                      ? 'bg-emerald-600 text-white'
                      : 'bg-[#FF5722] hover:bg-[#E64A19] text-white'
                  }`}
                >
                  {appliedCouponCode === 'WELCOME100' ? 'Applied ✓' : 'Apply'}
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
                    className="bg-white dark:bg-[#211A17] rounded-2xl p-2.5 border border-orange-100/80 dark:border-stone-800 shadow-xs flex flex-col justify-between hover:shadow-md transition-all cursor-pointer group"
                  >
                    <div className="relative aspect-square w-full rounded-xl overflow-hidden bg-orange-50/40 dark:bg-[#181311] mb-2">
                      <img src={product.image} alt={product.name} className="w-full h-full object-cover" />
                      {product.originalPrice > product.price && (
                        <span className="absolute top-1.5 left-1.5 bg-[#FF5722] text-white text-[9px] font-black px-1.5 py-0.5 rounded-md shadow-xs">
                          {product.discount}
                        </span>
                      )}

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
                            : 'bg-white/85 dark:bg-stone-900/85 backdrop-blur-xs text-slate-400 dark:text-slate-300 hover:text-rose-500 hover:bg-white dark:hover:bg-stone-800'
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
                      {product.originalPrice > product.price && (
                        <span className="text-[10px] text-slate-400 dark:text-slate-500 line-through">
                          ₹{product.originalPrice.toLocaleString('en-IN')}
                        </span>
                      )}
                    </div>

                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        addToCart(product);
                      }}
                      className="w-full py-1.5 bg-[#FF5722] hover:bg-[#E64A19] text-white text-xs font-bold rounded-xl flex items-center justify-center gap-1.5 active:scale-95 transition-all shadow-xs cursor-pointer"
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
            <div className="flex items-center justify-between mb-3 gap-2">
              <div>
                <h2 className="font-display font-black text-lg text-slate-900 dark:text-white">
                  Shopping Cart ({cartItems.length})
                </h2>
                <p className="text-xs text-slate-500 dark:text-slate-400">
                  All items are directly verified for quality and doorstep delivery
                </p>
              </div>
              {/* View My Orders Button in Cart Header */}
              <button
                type="button"
                onClick={handleViewMyOrders}
                className="shrink-0 px-3 py-1.5 rounded-xl bg-[#FFF4E8] dark:bg-orange-950/40 hover:bg-orange-100 dark:hover:bg-orange-900/60 border border-orange-200/90 dark:border-orange-800/60 text-[#E64A19] dark:text-orange-300 font-bold text-xs flex items-center gap-1.5 shadow-xs transition-all active:scale-95 cursor-pointer"
                title="View your previous orders and live delivery tracking"
              >
                <Package className="w-3.5 h-3.5" />
                <span>View My Orders</span>
              </button>
            </div>

            {cartItems.length === 0 ? (
              <div className="flex-1 flex flex-col items-center justify-center py-16 text-center">
                <div className="w-20 h-20 bg-[#FFF4E8] dark:bg-orange-950/30 text-[#FF5722] rounded-full flex items-center justify-center mb-4">
                  <ShoppingCart className="w-10 h-10 stroke-[1.5]" />
                </div>
                <h3 className="font-display font-bold text-base text-slate-800 dark:text-slate-200">
                  Your cart is empty!
                </h3>
                <p className="text-xs text-slate-500 dark:text-slate-400 max-w-xs mt-1 mb-5">
                  Explore quality products across categories and save with exclusive offers.
                </p>
                <div className="flex items-center gap-2.5 flex-wrap justify-center">
                  <button
                    type="button"
                    onClick={() => setActiveTab('home')}
                    className="px-5 py-2.5 bg-[#FF5722] hover:bg-[#E64A19] text-white font-bold text-xs rounded-xl shadow-md active:scale-95 transition-all cursor-pointer"
                  >
                    Explore Products
                  </button>
                  <button
                    type="button"
                    onClick={handleViewMyOrders}
                    className="px-5 py-2.5 bg-white dark:bg-[#211A17] border border-[#FF5722]/30 dark:border-orange-800/60 text-[#FF5722] dark:text-orange-300 font-bold text-xs rounded-xl shadow-xs hover:bg-[#FFF4E8] dark:hover:bg-orange-950/40 active:scale-95 transition-all flex items-center gap-1.5 cursor-pointer"
                  >
                    <Package className="w-4 h-4" />
                    <span>View My Orders</span>
                  </button>
                </div>
              </div>
            ) : (
              <div className="space-y-3.5">
                {/* Delivery Address Bar */}
                <div className="p-3 bg-[#FFF4E8] dark:bg-orange-950/25 border border-orange-200/70 dark:border-orange-900/50 rounded-2xl flex items-center justify-between gap-2">
                  <div className="flex items-center gap-2 min-w-0">
                    <MapPin className="w-4 h-4 text-[#FF5722] shrink-0" />
                    <div className="min-w-0">
                      <span className="text-[11px] font-bold text-slate-800 dark:text-slate-200 block truncate">
                        Deliver to {activeAddressObj?.fullName || userProfile.name} - {activeAddressObj?.pincode || '800001'}
                      </span>
                      <p className="text-[10px] text-slate-500 dark:text-slate-400 truncate">
                        {activeAddressObj ? `${activeAddressObj.addressLine}, ${activeAddressObj.city}` : activeAddressString}
                      </p>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => setIsAddressModalOpen(true)}
                    className="text-[11px] font-bold text-[#FF5722] hover:underline shrink-0 cursor-pointer"
                  >
                    Change
                  </button>
                </div>

                {/* Cart Items List */}
                <div className="space-y-2.5">
                  {cartItems.map((item) => (
                    <div
                      key={item.product.id}
                      className="bg-white dark:bg-[#211A17] rounded-2xl p-3 border border-orange-100/80 dark:border-stone-800 shadow-2xs space-y-2.5"
                    >
                      <div className="flex gap-3">
                        <div className="w-20 h-20 rounded-xl overflow-hidden bg-orange-50/40 dark:bg-[#181311] shrink-0 border border-orange-100/70 dark:border-stone-800">
                          <img
                            src={item.product.image}
                            alt={item.product.name}
                            className="w-full h-full object-cover"
                          />
                        </div>

                        <div className="flex-1 min-w-0 flex flex-col justify-between">
                          <div>
                            <h4 className="text-xs font-bold text-slate-800 dark:text-slate-100 line-clamp-2 leading-tight">
                              {item.product.name}
                            </h4>
                            <span className="text-[10px] text-slate-500 dark:text-slate-400 block mt-0.5">
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

                          <div className="flex items-center gap-1.5 text-[10px] text-emerald-600 dark:text-emerald-400 font-bold mt-1 tabular-nums">
                            <Truck className="w-3 h-3 text-emerald-600 shrink-0" />
                            <span>
                              {isCheckingCartEta
                                ? 'Checking delivery date…'
                                : cartServiceability?.serviceable
                                ? `${cartServiceability.etd ? `Delivery by ${cartServiceability.etd}` : `Delivery in ${cartServiceability.estimatedDaysText}`} | FREE`
                                : `Delivery to ${activeAddressObj?.pincode || '800001'} calculated at checkout`}
                            </span>
                          </div>
                        </div>
                      </div>

                      {/* Stepper & Actions (Flipkart signature) */}
                      <div className="flex items-center justify-between pt-2 border-t border-slate-100 dark:border-slate-800/80 text-xs">
                        {/* Quantity Stepper */}
                        <div className="flex items-center gap-2">
                          <span className="text-[10px] font-bold text-slate-400 uppercase">Qty:</span>
                          <div className="flex items-center gap-1.5 bg-slate-50 dark:bg-slate-800/80 p-0.5 rounded-lg border border-slate-200 dark:border-slate-700">
                            <button
                              type="button"
                              onClick={() => updateCartQuantity(item.product.id, -1)}
                              className="w-5 h-5 rounded flex items-center justify-center text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-700 cursor-pointer"
                            >
                              <Minus className="w-3 h-3" />
                            </button>
                            <span className="text-xs font-black min-w-[14px] text-center dark:text-white">
                              {item.quantity}
                            </span>
                            <button
                              type="button"
                              onClick={() => updateCartQuantity(item.product.id, 1)}
                              className="w-5 h-5 rounded flex items-center justify-center text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-700 cursor-pointer"
                            >
                              <Plus className="w-3 h-3" />
                            </button>
                          </div>
                        </div>

                        {/* Save for later & Remove */}
                        <div className="flex items-center gap-3">
                          <button
                            type="button"
                            onClick={() => {
                              toggleWishlist(item.product.id, item.product.name);
                              showToast(`Saved ${item.product.name} for later! ❤️`);
                            }}
                            className="text-[11px] font-bold text-slate-600 dark:text-slate-400 hover:text-[#FF5722] cursor-pointer"
                          >
                            Save for later
                          </button>
                          <span className="text-slate-300 dark:text-slate-700">|</span>
                          <button
                            type="button"
                            onClick={() => removeFromCart(item.product.id)}
                            className="text-[11px] font-bold text-rose-600 dark:text-rose-400 hover:underline cursor-pointer flex items-center gap-1"
                          >
                            <Trash2 className="w-3 h-3" />
                            <span>Remove</span>
                          </button>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>

                {/* Apply Promo / Coupon Bar */}
                <div className="bg-white dark:bg-[#211A17] rounded-2xl p-3 border border-orange-100/80 dark:border-stone-800 shadow-2xs space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-slate-800 dark:text-slate-200 flex items-center gap-1.5">
                      <Tag className="w-3.5 h-3.5 text-[#FF5722]" />
                      <span>Apply Coupon</span>
                    </span>
                    <button
                      type="button"
                      onClick={() => {
                        haptics.light();
                        setIsCouponsModalOpen(true);
                      }}
                      className="text-[11px] font-bold text-[#FF5722] hover:underline cursor-pointer"
                    >
                      View Offers
                    </button>
                  </div>
                  <div className="flex gap-2">
                    <input
                      type="text"
                      value={couponInputCode}
                      onChange={(e) => setCouponInputCode(e.target.value.toUpperCase())}
                      placeholder="Enter AARVO200 or WELCOME100"
                      className="flex-1 text-xs px-3 py-2 bg-[#FFF4E8]/60 dark:bg-[#181311] border border-orange-200/80 dark:border-stone-700 rounded-xl text-slate-900 dark:text-white placeholder:text-slate-400 focus:outline-hidden focus:border-[#FF5722] font-mono uppercase"
                    />
                    {appliedCouponCode ? (
                      <button
                        type="button"
                        onClick={() => {
                          haptics.warning();
                          setAppliedCouponCode(null);
                          setCouponInputCode('');
                          showToast('Coupon removed');
                        }}
                        className="px-3 py-2 bg-rose-50 dark:bg-rose-950/40 text-rose-600 dark:text-rose-300 text-xs font-bold rounded-xl cursor-pointer"
                      >
                        Remove
                      </button>
                    ) : (
                      <button
                        type="button"
                        onClick={() => {
                          const code = couponInputCode.trim().toUpperCase();
                          if (code === 'AARVO200' || code === 'WELCOME100') {
                            haptics.saved();
                            setAppliedCouponCode(code);
                            showToast(`Coupon ${code} applied!`);
                          } else {
                            haptics.warning();
                            showToast('Try AARVO200 (orders ₹999+) or WELCOME100 (orders ₹499+)');
                          }
                        }}
                        className="px-4 py-2 bg-[#FF5722] hover:bg-[#E64A19] text-white text-xs font-bold rounded-xl shadow-xs cursor-pointer"
                      >
                        Apply
                      </button>
                    )}
                  </div>
                </div>

                {/* Quick Link Card to View My Orders */}
                <div className="p-3 bg-white dark:bg-[#211A17] border border-orange-100/80 dark:border-stone-800 rounded-2xl flex items-center justify-between shadow-xs">
                  <div className="flex items-center gap-2.5">
                    <div className="w-8 h-8 rounded-xl bg-[#FFF4E8] dark:bg-orange-950/40 text-[#FF5722] flex items-center justify-center">
                      <Package className="w-4 h-4" />
                    </div>
                    <div>
                      <div className="flex items-center gap-1.5">
                        <span className="text-xs font-bold text-slate-800 dark:text-slate-200">
                          My Orders &amp; Tracking
                        </span>
                        <span className="text-[10px] font-semibold text-emerald-600 dark:text-emerald-400">
                          {orders.filter((o) => o.status === 'Processing' || o.status === 'Shipped').length > 0
                            ? `${orders.filter((o) => o.status === 'Processing' || o.status === 'Shipped').length} Active`
                            : `${orders.length} Placed`}
                        </span>
                      </div>
                      <p className="text-[10px] text-slate-500 dark:text-slate-400">
                        Check past order status, invoices, and reorder items
                      </p>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={handleViewMyOrders}
                    className="px-3 py-1.5 bg-[#FF5722] hover:bg-[#E64A19] text-white text-xs font-bold rounded-xl shadow-xs active:scale-95 transition-all flex items-center gap-1 cursor-pointer shrink-0"
                  >
                    <span>View My Orders</span>
                    <ChevronRight className="w-3.5 h-3.5" />
                  </button>
                </div>

                {/* Price Details Breakdown */}
                <div className="bg-white dark:bg-[#211A17] rounded-2xl p-3.5 border border-orange-100/80 dark:border-stone-800 shadow-xs space-y-2">
                  <h4 className="font-display font-bold text-xs text-slate-900 dark:text-white border-b border-orange-100/70 dark:border-stone-800 pb-2">
                    PRICE DETAILS ({totalCartCount} Item{totalCartCount !== 1 ? 's' : ''})
                  </h4>

                  <div className="flex items-center justify-between text-xs text-slate-600 dark:text-slate-400">
                    <span>Total MRP</span>
                    <span>₹{totalMRP.toLocaleString('en-IN')}</span>
                  </div>

                  <div className="flex items-center justify-between text-xs text-emerald-600 dark:text-emerald-400">
                    <span>Discount on MRP</span>
                    <span>-₹{totalDiscount.toLocaleString('en-IN')}</span>
                  </div>

                  {appliedCouponCode && couponDiscount > 0 && (
                    <div className="flex items-center justify-between text-xs text-emerald-600 dark:text-emerald-400">
                      <span>Coupon Discount ({appliedCouponCode})</span>
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

                  <div className="flex items-center justify-between text-sm font-black text-slate-900 dark:text-white border-t border-orange-100/70 dark:border-stone-800 pt-2">
                    <span>Total Amount</span>
                    <span>₹{finalPayable.toLocaleString('en-IN')}</span>
                  </div>

                  <p className="text-[10px] text-emerald-600 dark:text-emerald-400 font-semibold pt-1">
                    You will save ₹{(totalDiscount + couponDiscount).toLocaleString('en-IN')} on this order
                  </p>
                </div>

                {/* Interactive Payment Method Toggle & Selector */}
                <div className="bg-white dark:bg-[#211A17] border border-orange-100/80 dark:border-stone-800 rounded-2xl p-3.5 space-y-3 shadow-xs">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-black text-slate-900 dark:text-white uppercase tracking-wider">
                      Payment Method
                    </span>
                    <button
                      type="button"
                      onClick={() => {
                        haptics.light();
                        setShowAddPaymentModal(true);
                      }}
                      className="text-[11px] font-bold text-[#FF5722] hover:underline flex items-center gap-1 cursor-pointer"
                    >
                      <span>+ Add Method</span>
                    </button>
                  </div>

                  {/* Segmented Payment Toggle */}
                  <div className="grid grid-cols-2 p-1 bg-[#FFF4E8] dark:bg-[#181311] rounded-xl gap-1">
                    <button
                      type="button"
                      onClick={() => {
                        haptics.selection();
                        setPaymentMethodSelection('COD');
                      }}
                      className={`py-2 px-3 rounded-lg text-xs font-bold transition-all flex items-center justify-center gap-1.5 cursor-pointer ${
                        paymentMethodSelection === 'COD'
                          ? 'bg-amber-500 text-white shadow-sm'
                          : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'
                      }`}
                    >
                      <span>💵</span>
                      <span>Cash On Delivery</span>
                    </button>

                    <button
                      type="button"
                      onClick={() => {
                        haptics.selection();
                        setPaymentMethodSelection('UPI');
                      }}
                      className={`py-2 px-3 rounded-lg text-xs font-bold transition-all flex items-center justify-center gap-1.5 cursor-pointer ${
                        paymentMethodSelection === 'UPI'
                          ? 'bg-[#FF5722] text-white shadow-sm'
                          : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'
                      }`}
                    >
                      <span>⚡</span>
                      <span>Online UPI</span>
                    </button>
                  </div>

                  {/* Cash On Delivery Details */}
                  {paymentMethodSelection === 'COD' && (
                    <div className="p-3 bg-amber-50/70 dark:bg-amber-950/20 border border-amber-200/70 dark:border-amber-800/40 rounded-xl flex items-start gap-2.5">
                      <div className="w-5 h-5 rounded-full bg-amber-500 text-white flex items-center justify-center text-xs font-bold shrink-0 mt-0.5">
                        ✓
                      </div>
                      <div className="space-y-0.5">
                        <p className="text-xs font-bold text-amber-900 dark:text-amber-300">
                          Cash On Delivery (COD) Selected
                        </p>
                        <p className="text-[10px] text-amber-800/80 dark:text-amber-400/80 leading-relaxed">
                          Pay directly at your doorstep when the delivery rider arrives. Zero pre-payment required.
                        </p>
                      </div>
                    </div>
                  )}

                  {/* Online UPI Details & App selector */}
                  {paymentMethodSelection === 'UPI' && (
                    <div className="space-y-2.5 p-3 bg-[#FFF4E8] dark:bg-orange-950/25 border border-orange-200/70 dark:border-orange-900/50 rounded-xl">
                      <div className="flex items-center justify-between">
                        <span className="text-[11px] font-bold text-slate-900 dark:text-orange-200">
                          Select Instant UPI App:
                        </span>
                        <span className="text-[10px] font-bold text-emerald-700 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/50 px-2 py-0.5 rounded-full">
                          Instant Verification
                        </span>
                      </div>
                      <div className="grid grid-cols-4 gap-1.5">
                        {(['Google Pay', 'PhonePe', 'Paytm', 'Custom UPI'] as const).map((app) => (
                          <button
                            key={app}
                            type="button"
                            onClick={() => {
                              haptics.light();
                              setSelectedUpiApp(app);
                            }}
                            className={`py-1.5 px-1 rounded-lg text-[10px] font-bold border transition-all text-center cursor-pointer ${
                              selectedUpiApp === app
                                ? 'bg-[#FF5722] text-white border-[#FF5722] shadow-xs'
                                : 'bg-white dark:bg-[#181311] text-slate-700 dark:text-slate-300 border-orange-100 dark:border-stone-700 hover:border-[#FF5722]'
                            }`}
                          >
                            {app === 'Google Pay' ? 'GPay' : app === 'PhonePe' ? 'PhonePe' : app === 'Paytm' ? 'Paytm' : 'UPI ID'}
                          </button>
                        ))}
                      </div>

                      {selectedUpiApp === 'Custom UPI' && (
                        <div className="pt-1">
                          <input
                            type="text"
                            value={customUpiId}
                            onChange={(e) => setCustomUpiId(e.target.value)}
                            placeholder="e.g. yourname@okhdfcbank"
                            className="w-full text-xs px-3 py-2 bg-white dark:bg-[#181311] border border-orange-200/80 dark:border-stone-700 rounded-lg text-slate-900 dark:text-white placeholder:text-slate-400 focus:outline-hidden focus:border-[#FF5722]"
                          />
                        </div>
                      )}
                    </div>
                  )}
                </div>

                {/* Sticky Checkout Bottom Bar */}
                <div className="sticky bottom-14 left-0 right-0 z-30 -mx-3 -mb-6 p-3 bg-white/95 dark:bg-[#181311]/95 backdrop-blur-md border-t border-orange-100 dark:border-stone-800 flex items-center justify-between shadow-xl">
                  <div>
                    <span className="text-base font-black text-slate-900 dark:text-white font-display block">
                      ₹{finalPayable.toLocaleString('en-IN')}
                    </span>
                    <button
                      type="button"
                      onClick={() => showToast(`Total Savings: ₹${(totalDiscount + couponDiscount).toLocaleString('en-IN')}`)}
                      className="text-[10px] font-bold text-[#FF5722] hover:underline cursor-pointer"
                    >
                      View price details
                    </button>
                  </div>

                  {/* Place Order CTA Button */}
                  <button
                    type="button"
                    onClick={() => {
                      const orderId = `ARV-${Math.floor(100000 + Math.random() * 900000)}`;
                      const chosenPayment = paymentMethodSelection === 'COD'
                        ? 'Cash On Delivery (COD)'
                        : `Online UPI (${selectedUpiApp}${selectedUpiApp === 'Custom UPI' && customUpiId ? `: ${customUpiId}` : ''})`;
                      const etaSummary = cartServiceability?.serviceable
                        ? `${cartServiceability.estimatedDaysText}${cartServiceability.etd ? ` (by ${cartServiceability.etd})` : ''}`
                        : 'Updating after courier pickup';
                      const newOrder: Order = {
                        id: orderId,
                        date: 'Today, Just now',
                        total: finalPayable,
                        status: 'Processing',
                        statusStep: 1,
                        estimatedDelivery: etaSummary,
                        trackingNumber: `PENDING-AWB-${orderId.slice(-6)}`,
                        carrier: cartServiceability?.courierName || 'Express Delivery Partner',
                        paymentMethod: chosenPayment,
                        deliveryAddress: activeAddressString,
                        items: cartItems.map((ci) => ({
                          id: ci.product.id,
                          name: ci.product.name,
                          price: ci.product.price,
                          quantity: ci.quantity,
                          image: ci.product.image,
                          category: ci.product.category,
                        })),
                        timeline: [
                          { status: 'Order Verified', time: 'Just now', description: `Order verified via ${chosenPayment}`, completed: true },
                          { status: 'Processing at Warehouse', time: 'In progress', description: 'Seller preparing package for courier pickup', completed: false },
                          { status: 'Shipped', time: 'Pending', description: 'Will dispatch via verified courier network', completed: false },
                          { status: 'Delivered', time: cartServiceability?.etd ? `Est. ${cartServiceability.etd}` : 'Pending', description: 'Doorstep delivery with verification', completed: false },
                        ],
                      };
                      haptics.orderComplete();
                      setOrders((prev) => [newOrder, ...prev]);
                      setOrderSuccessModal(orderId);
                      setCartItems([]);
                      setAppliedCouponCode(null);
                      setCouponInputCode('');
                    }}
                    className="py-3 px-6 bg-[#FF5722] hover:bg-[#E64A19] text-white font-black text-xs uppercase tracking-wider rounded-xl shadow-md active:scale-95 transition-all flex items-center justify-center gap-1.5 cursor-pointer"
                  >
                    <span>Place Order</span>
                    <ChevronRight className="w-4 h-4" />
                  </button>
                </div>
              </div>
            )}
          </div>
        )}

        {/* ================================================================= */}
        {/* TAB 5: ACCOUNT VIEW                                               */}
        {/* ================================================================= */}
        {/* ================================================================= */}
        {/* TAB 5: FLIPKART-STYLE ACCOUNT VIEW                                */}
        {/* ================================================================= */}
        {activeTab === 'account' && (
          <div className="flex-1 flex flex-col p-3 pb-24 space-y-3.5 no-scrollbar overflow-y-auto">
            {/* 1. AARVO Warm Orange Profile Header Card (No Fake Badges / No Fake SuperCoins) */}
            <div className="rounded-2xl bg-gradient-to-r from-[#FF7043] via-[#FF5722] to-[#E64A19] p-4 text-white shadow-md flex items-center justify-between gap-2">
              <div className="flex items-center gap-3 min-w-0">
                <div className="w-12 h-12 rounded-full bg-white/20 border-2 border-white/80 flex items-center justify-center font-display font-black text-base shadow-xs shrink-0">
                  {(userProfile.name || 'AR')
                    .split(' ')
                    .filter(Boolean)
                    .slice(0, 2)
                    .map((n) => n[0].toUpperCase())
                    .join('')}
                </div>
                <div className="min-w-0">
                  <h3 className="font-display font-black text-sm sm:text-base truncate">
                    {userProfile.name}
                  </h3>
                  <p className="text-[11px] text-orange-100 truncate">{userProfile.phone}</p>
                  <p className="text-[10px] text-white/85 truncate mt-0.5">
                    {activeAddressObj ? `${activeAddressObj.city} - ${activeAddressObj.pincode}` : 'AARVO Account'}
                  </p>
                </div>
              </div>

              <button
                type="button"
                onClick={() => {
                  haptics.selection();
                  setProfileDraftName(userProfile.name);
                  setProfileDraftPhone(userProfile.phone);
                  setIsEditProfileOpen(true);
                }}
                className="px-3 py-1.5 rounded-xl bg-white text-[#E64A19] text-[11px] font-extrabold shadow-xs hover:bg-orange-50 active:scale-95 transition-all flex items-center gap-1 shrink-0 cursor-pointer"
              >
                <Edit2 className="w-3 h-3" />
                <span>Edit</span>
              </button>
            </div>

            {/* 2. 4-Action Quick Tiles (Orders, Wishlist, Coupons, Help Center) */}
            <div className="grid grid-cols-2 gap-2.5">
              {/* Tile 1: Orders */}
              <button
                type="button"
                onClick={() => {
                  haptics.selection();
                  setIsOrdersViewOpen(true);
                }}
                className="p-3.5 bg-white dark:bg-[#211A17] rounded-2xl border border-orange-100/80 dark:border-stone-800 shadow-2xs hover:shadow-sm active:scale-95 transition-all text-left flex items-start justify-between cursor-pointer group"
              >
                <div>
                  <div className="w-8 h-8 rounded-xl bg-[#FFF4E8] dark:bg-orange-950/50 text-[#FF5722] flex items-center justify-center mb-2 group-hover:scale-110 transition-transform">
                    <Package className="w-4 h-4 stroke-[2.2]" />
                  </div>
                  <h4 className="text-xs font-black text-slate-900 dark:text-white">Orders</h4>
                  <span className="text-[10px] text-slate-500 dark:text-slate-400">
                    {orders.filter((o) => o.status === 'Processing' || o.status === 'Shipped').length > 0
                      ? `${orders.filter((o) => o.status === 'Processing' || o.status === 'Shipped').length} Active Order`
                      : `${orders.length} Total Orders`}
                  </span>
                </div>
                <ChevronRight className="w-4 h-4 text-slate-400 mt-1" />
              </button>

              {/* Tile 2: Wishlist */}
              <button
                type="button"
                onClick={() => {
                  haptics.selection();
                  setIsWishlistModalOpen(true);
                }}
                className="p-3.5 bg-white dark:bg-[#211A17] rounded-2xl border border-orange-100/80 dark:border-stone-800 shadow-2xs hover:shadow-sm active:scale-95 transition-all text-left flex items-start justify-between cursor-pointer group"
              >
                <div>
                  <div className="w-8 h-8 rounded-xl bg-rose-50 dark:bg-rose-950/60 text-rose-500 flex items-center justify-center mb-2 group-hover:scale-110 transition-transform">
                    <Heart className="w-4 h-4 fill-rose-500 text-rose-500" />
                  </div>
                  <h4 className="text-xs font-black text-slate-900 dark:text-white">Wishlist</h4>
                  <span className="text-[10px] text-slate-500 dark:text-slate-400">
                    {wishlist.length} item{wishlist.length !== 1 ? 's' : ''} saved
                  </span>
                </div>
                <ChevronRight className="w-4 h-4 text-slate-400 mt-1" />
              </button>

              {/* Tile 3: Coupons */}
              <button
                type="button"
                onClick={() => {
                  haptics.selection();
                  setIsCouponsModalOpen(true);
                }}
                className="p-3.5 bg-white dark:bg-[#211A17] rounded-2xl border border-orange-100/80 dark:border-stone-800 shadow-2xs hover:shadow-sm active:scale-95 transition-all text-left flex items-start justify-between cursor-pointer group"
              >
                <div>
                  <div className="w-8 h-8 rounded-xl bg-amber-50 dark:bg-amber-950/60 text-amber-600 dark:text-amber-400 flex items-center justify-center mb-2 group-hover:scale-110 transition-transform">
                    <Tag className="w-4 h-4 stroke-[2.2]" />
                  </div>
                  <h4 className="text-xs font-black text-slate-900 dark:text-white">Coupons</h4>
                  <span className="text-[10px] text-emerald-600 dark:text-emerald-400 font-bold">
                    {appliedCouponCode ? `Applied: ${appliedCouponCode}` : 'Tap to apply promo'}
                  </span>
                </div>
                <ChevronRight className="w-4 h-4 text-slate-400 mt-1" />
              </button>

              {/* Tile 4: Help Center */}
              <button
                type="button"
                onClick={() => {
                  haptics.selection();
                  setIsHelpModalOpen(true);
                }}
                className="p-3.5 bg-white dark:bg-[#211A17] rounded-2xl border border-orange-100/80 dark:border-stone-800 shadow-2xs hover:shadow-sm active:scale-95 transition-all text-left flex items-start justify-between cursor-pointer group"
              >
                <div>
                  <div className="w-8 h-8 rounded-xl bg-emerald-50 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400 flex items-center justify-center mb-2 group-hover:scale-110 transition-transform">
                    <HelpCircle className="w-4 h-4 stroke-[2.2]" />
                  </div>
                  <h4 className="text-xs font-black text-slate-900 dark:text-white">Help Center</h4>
                  <span className="text-[10px] text-slate-500 dark:text-slate-400">
                    {supportTickets.length > 0 ? `${supportTickets.length} Ticket(s)` : 'FAQs & Support'}
                  </span>
                </div>
                <ChevronRight className="w-4 h-4 text-slate-400 mt-1" />
              </button>
            </div>

            {/* 3. Payment & UPI Options */}
            <div className="p-3.5 rounded-2xl bg-white dark:bg-[#211A17] border border-orange-100/80 dark:border-stone-800 space-y-2.5 shadow-2xs">
              <h4 className="font-display font-black text-xs uppercase tracking-wider text-slate-900 dark:text-white">
                Payments &amp; Preferences
              </h4>

              <div className="divide-y divide-orange-100/70 dark:divide-stone-800 text-xs">
                <div className="py-2 flex items-center justify-between">
                  <div className="flex items-center gap-2.5">
                    <div className="w-8 h-8 rounded-xl bg-[#FFF4E8] dark:bg-orange-950/40 text-[#FF5722] flex items-center justify-center font-bold">
                      💳
                    </div>
                    <div>
                      <span className="font-extrabold text-slate-900 dark:text-white block">
                        Saved Cards &amp; UPI
                      </span>
                      <span className="text-[10px] text-slate-500 dark:text-slate-400 font-semibold">
                        Manage quick UPI &amp; payment methods
                      </span>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => {
                      haptics.selection();
                      setShowAddPaymentModal(true);
                    }}
                    className="text-[11px] font-bold text-[#FF5722] hover:underline cursor-pointer"
                  >
                    Manage &gt;
                  </button>
                </div>

                <div className="py-2 flex items-center justify-between">
                  <div className="flex items-center gap-2.5">
                    <div className="w-8 h-8 rounded-xl bg-amber-50 dark:bg-amber-950/40 text-amber-600 dark:text-amber-400 flex items-center justify-center font-bold">
                      🏷️
                    </div>
                    <div>
                      <span className="font-extrabold text-slate-900 dark:text-white block">
                        Promo Coupons
                      </span>
                      <span className="text-[10px] text-slate-500 dark:text-slate-400">
                        Use AARVO200 or WELCOME100 at checkout
                      </span>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => {
                      haptics.selection();
                      setIsCouponsModalOpen(true);
                    }}
                    className="text-[11px] font-bold text-[#FF5722] hover:underline cursor-pointer"
                  >
                    View &gt;
                  </button>
                </div>
              </div>
            </div>

            {/* 4. Account Settings List */}
            <div className="bg-white dark:bg-[#211A17] rounded-2xl border border-orange-100/80 dark:border-stone-800 shadow-2xs divide-y divide-orange-100/70 dark:divide-stone-800 text-xs font-semibold text-slate-700 dark:text-slate-200 overflow-hidden">
              <h4 className="px-4 pt-3 pb-1 font-display font-black text-xs uppercase tracking-wider text-slate-900 dark:text-white">
                Account Settings
              </h4>

              <button
                type="button"
                onClick={() => {
                  haptics.selection();
                  setIsAddressModalOpen(true);
                }}
                className="w-full px-4 py-3 text-left hover:bg-[#FFF4E8]/60 dark:hover:bg-stone-800/40 flex items-center justify-between gap-2 transition-colors cursor-pointer"
              >
                <div className="flex items-center gap-3 min-w-0">
                  <MapPin className="w-4 h-4 text-[#FF5722] shrink-0" />
                  <div className="min-w-0">
                    <span className="block">Saved Delivery Addresses ({savedAddresses.length})</span>
                    <span className="text-[10px] text-slate-500 dark:text-slate-400 font-normal block truncate">
                      {activeAddressString}
                    </span>
                  </div>
                </div>
                <ChevronRight className="w-4 h-4 text-slate-400 shrink-0" />
              </button>

              <button
                type="button"
                onClick={() => {
                  haptics.selection();
                  setSellerModalTab('overview');
                  setIsSellerModalOpen(true);
                }}
                className="w-full px-4 py-3 text-left hover:bg-[#FFF4E8]/60 dark:hover:bg-stone-800/40 flex items-center justify-between transition-colors cursor-pointer"
              >
                <div className="flex items-center gap-3">
                  <Store className="w-4 h-4 text-[#FF5722]" />
                  <div className="flex items-center gap-1.5">
                    <span>Seller Hub &amp; Catalog Management</span>
                    {allProducts.filter((p) => (p.stockQuantity ?? 10) < 5).length > 0 && (
                      <span className="text-[9px] bg-red-100 dark:bg-red-950/60 text-red-600 dark:text-red-400 font-extrabold px-1.5 py-0.5 rounded-full">
                        {allProducts.filter((p) => (p.stockQuantity ?? 10) < 5).length} Low Stock
                      </span>
                    )}
                  </div>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="text-[10px] bg-orange-100 dark:bg-orange-950/60 text-[#E64A19] dark:text-orange-300 px-2 py-0.5 rounded-full font-bold">
                    Seller Panel
                  </span>
                  <ChevronRight className="w-4 h-4 text-slate-400" />
                </div>
              </button>

              <button
                type="button"
                onClick={() => {
                  haptics.selection();
                  setIsPolicyModalOpen(true);
                }}
                className="w-full px-4 py-3 text-left hover:bg-[#FFF4E8]/60 dark:hover:bg-stone-800/40 flex items-center justify-between transition-colors cursor-pointer"
              >
                <div className="flex items-center gap-3">
                  <ShieldCheck className="w-4 h-4 text-[#FF5722]" />
                  <div>
                    <span className="block">Privacy Policy, Terms &amp; Returns</span>
                    <span className="text-[10px] text-slate-500 dark:text-slate-400 font-normal block">
                      7-day return window &amp; data protection disclosures
                    </span>
                  </div>
                </div>
                <ChevronRight className="w-4 h-4 text-slate-400" />
              </button>

              <button
                type="button"
                onClick={() => {
                  haptics.warning();
                  setDeleteConfirmText('');
                  setDeleteAccountError(null);
                  setIsDeleteAccountOpen(true);
                }}
                className="w-full px-4 py-3 text-left hover:bg-rose-50/70 dark:hover:bg-rose-950/30 flex items-center justify-between transition-colors cursor-pointer text-rose-600 dark:text-rose-400"
              >
                <div className="flex items-center gap-3">
                  <Trash2 className="w-4 h-4 shrink-0" />
                  <div>
                    <span className="block font-bold">Delete My Account</span>
                    <span className="text-[10px] text-slate-500 dark:text-slate-400 font-normal block">
                      Permanently remove your profile &amp; saved addresses
                    </span>
                  </div>
                </div>
                <ChevronRight className="w-4 h-4 text-rose-400" />
              </button>

              {/* Theme Toggle Card in Account */}
              {onToggleTheme && (
                <div className="w-full px-4 py-2.5 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-7 h-7 rounded-lg bg-[#FFF4E8] dark:bg-orange-950/50 text-[#FF5722] flex items-center justify-center">
                      {theme === 'dark' ? <Moon className="w-4 h-4" /> : <Sun className="w-4 h-4" />}
                    </div>
                    <div>
                      <span className="text-xs font-bold text-slate-900 dark:text-white block">Theme Mode</span>
                      <span className="text-[10px] text-slate-400">Currently: {theme === 'dark' ? 'Dark' : 'Light'}</span>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={onToggleTheme}
                    className="px-2.5 py-1 bg-[#FF5722] hover:bg-[#E64A19] text-white text-[10px] font-bold rounded-lg shadow-2xs active:scale-95 transition-transform cursor-pointer"
                  >
                    Toggle
                  </button>
                </div>
              )}
            </div>

            {/* Logout button */}
            <button
              type="button"
              onClick={() => {
                haptics.light();
                if (onLogout) onLogout();
              }}
              className="w-full py-3 bg-rose-50 dark:bg-rose-950/30 hover:bg-rose-100 dark:hover:bg-rose-950/60 text-rose-600 dark:text-rose-400 font-bold text-xs rounded-2xl transition-colors text-center cursor-pointer shadow-2xs"
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
        <nav className="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[440px] bg-white dark:bg-[#181311] border-t border-orange-100 dark:border-stone-800 py-1.5 px-2 flex items-center justify-around shadow-2xl z-40 transition-colors duration-300">
          {/* 1. Home */}
          <button
            type="button"
            onClick={() => {
              setIsOrdersViewOpen(false);
              switchTab('home');
            }}
            className={`flex flex-col items-center gap-0.5 text-[10px] font-bold transition-colors py-1 cursor-pointer ${
              activeTab === 'home' && !isOrdersViewOpen
                ? 'text-[#FF5722] font-black'
                : 'text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-slate-200'
            }`}
          >
            <Home className="w-5 h-5 stroke-[2.2]" />
            <span className="whitespace-nowrap">Home</span>
          </button>

          {/* 2. Categories */}
          <button
            type="button"
            onClick={() => {
              setIsOrdersViewOpen(false);
              switchTab('categories');
            }}
            className={`flex flex-col items-center gap-0.5 text-[10px] font-bold transition-colors py-1 cursor-pointer ${
              activeTab === 'categories' && !isOrdersViewOpen
                ? 'text-[#FF5722] font-black'
                : 'text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-slate-200'
            }`}
          >
            <LayoutGrid className="w-5 h-5 stroke-[2.2]" />
            <span className="whitespace-nowrap">Categories</span>
          </button>

          {/* 3. Cart with live badge */}
          <button
            type="button"
            onClick={() => {
              setIsOrdersViewOpen(false);
              switchTab('cart');
            }}
            className={`relative flex flex-col items-center gap-0.5 text-[10px] font-bold transition-colors py-1 cursor-pointer ${
              activeTab === 'cart' && !isOrdersViewOpen
                ? 'text-[#FF5722] font-black'
                : 'text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-slate-200'
            }`}
          >
            <ShoppingCart className="w-5 h-5 stroke-[2.2]" />
            <span className="whitespace-nowrap">Cart</span>
            {totalCartCount > 0 && (
              <span className="absolute -top-1 right-1.5 px-1 min-w-[16px] h-4 bg-[#FF5722] text-white text-[9px] font-extrabold rounded-full flex items-center justify-center shadow-xs">
                {totalCartCount}
              </span>
            )}
          </button>

          {/* 4. Orders */}
          <button
            type="button"
            onClick={() => {
              haptics.selection();
              setIsOrdersViewOpen(true);
            }}
            className={`flex flex-col items-center gap-0.5 text-[10px] font-bold transition-colors py-1 cursor-pointer ${
              isOrdersViewOpen
                ? 'text-[#FF5722] font-black'
                : 'text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-slate-200'
            }`}
          >
            <Package className="w-5 h-5 stroke-[2.2]" />
            <span className="whitespace-nowrap">Orders</span>
          </button>

          {/* 5. Account */}
          <button
            type="button"
            onClick={() => {
              setIsOrdersViewOpen(false);
              switchTab('account');
            }}
            className={`flex flex-col items-center gap-0.5 text-[10px] font-bold transition-colors py-1 cursor-pointer ${
              activeTab === 'account' && !isOrdersViewOpen
                ? 'text-[#FF5722] font-black'
                : 'text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-slate-200'
            }`}
          >
            <User className="w-5 h-5 stroke-[2.2]" />
            <span className="whitespace-nowrap">Account</span>
          </button>
        </nav>

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

            <div className="relative w-72 max-w-[80%] bg-[#FFFDF9] dark:bg-[#181311] text-slate-800 dark:text-slate-100 h-full shadow-2xl flex flex-col justify-between p-5 pt-[max(1.25rem,env(safe-area-inset-top))] z-10 transition-colors">
              <div>
                <div className="flex items-center justify-between pb-4 border-b border-orange-100 dark:border-stone-800">
                  <div className="flex items-center gap-2">
                    <img
                      src="/src/assets/aarvo/aarvo_logo_transparent.png"
                      alt="AARVO Logo"
                      className="w-7 h-7 object-contain"
                    />
                    <span className="text-xl font-black font-display text-[#FF5722]">
                      AARVO
                    </span>
                  </div>
                  <button
                    onClick={() => setIsSidebarOpen(false)}
                    className="p-1 text-slate-400 hover:text-slate-800 dark:hover:text-white rounded-lg cursor-pointer"
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>

                {/* Theme Switcher inside Drawer */}
                {onToggleTheme && (
                  <div className="my-3 p-3 rounded-xl bg-[#FFF4E8] dark:bg-orange-950/30 border border-orange-200/70 dark:border-orange-900/40 flex items-center justify-between">
                    <div className="flex items-center gap-2.5">
                      {theme === 'dark' ? (
                        <Moon className="w-4 h-4 text-orange-400" />
                      ) : (
                        <Sun className="w-4 h-4 text-[#FF5722]" />
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
                      className="px-2.5 py-1 text-[11px] font-bold rounded-lg bg-[#FF5722] hover:bg-[#E64A19] text-white shadow-xs active:scale-95 transition-transform cursor-pointer"
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
                    className="w-full text-left py-2.5 px-3 rounded-xl hover:bg-[#FFF4E8] dark:hover:bg-orange-950/30 hover:text-[#FF5722] transition-colors flex items-center gap-3 cursor-pointer"
                  >
                    <Home className="w-4 h-4 text-[#FF5722]" />
                    <span>Home Marketplace</span>
                  </button>

                  <button
                    onClick={() => {
                      setIsSidebarOpen(false);
                      setActiveTab('categories');
                    }}
                    className="w-full text-left py-2.5 px-3 rounded-xl hover:bg-[#FFF4E8] dark:hover:bg-orange-950/30 hover:text-[#FF5722] transition-colors flex items-center gap-3 cursor-pointer"
                  >
                    <LayoutGrid className="w-4 h-4 text-[#FF5722]" />
                    <span>All Categories</span>
                  </button>

                  <button
                    onClick={() => {
                      setIsSidebarOpen(false);
                      setActiveTab('offers');
                    }}
                    className="w-full text-left py-2.5 px-3 rounded-xl hover:bg-[#FFF4E8] dark:hover:bg-orange-950/30 hover:text-[#FF5722] transition-colors flex items-center gap-3 cursor-pointer"
                  >
                    <Tag className="w-4 h-4 text-[#FF5722]" />
                    <span>Deals &amp; Offers</span>
                  </button>

                  <button
                    onClick={() => {
                      haptics.selection();
                      setIsSidebarOpen(false);
                      setIsOrdersViewOpen(true);
                    }}
                    className="w-full text-left py-2.5 px-3 rounded-xl hover:bg-[#FFF4E8] dark:hover:bg-orange-950/30 hover:text-[#FF5722] transition-colors flex items-center justify-between cursor-pointer"
                  >
                    <div className="flex items-center gap-3">
                      <Package className="w-4 h-4 text-[#FF5722]" />
                      <span>Orders &amp; Delivery</span>
                    </div>
                  </button>

                  <button
                    onClick={() => {
                      setIsSidebarOpen(false);
                      setIsWishlistModalOpen(true);
                    }}
                    className="w-full text-left py-2.5 px-3 rounded-xl hover:bg-rose-50 dark:hover:bg-rose-950/40 hover:text-rose-600 dark:hover:text-rose-400 transition-colors flex items-center justify-between cursor-pointer"
                  >
                    <div className="flex items-center gap-3">
                      <Heart className="w-4 h-4 text-rose-500 fill-rose-500" />
                      <span>My Wishlist</span>
                    </div>
                    {wishlist.length > 0 && (
                      <span className="text-[10px] font-bold bg-rose-100 dark:bg-rose-950 text-rose-600 dark:text-rose-300 px-2 py-0.5 rounded-full">
                        {wishlist.length}
                      </span>
                    )}
                  </button>

                  <button
                    onClick={() => {
                      setIsSidebarOpen(false);
                      setIsSellerModalOpen(true);
                    }}
                    className="w-full text-left py-2.5 px-3 rounded-xl hover:bg-amber-50 dark:hover:bg-amber-950/40 hover:text-amber-800 dark:hover:text-amber-300 transition-colors flex items-center gap-3 cursor-pointer"
                  >
                    <Store className="w-4 h-4 text-amber-600 dark:text-amber-400" />
                    <span>Seller Onboarding Hub</span>
                  </button>
                </div>
              </div>

              <div className="pt-4 border-t border-orange-100 dark:border-stone-800">
                <button
                  onClick={() => {
                    setIsSidebarOpen(false);
                    if (onLogout) onLogout();
                  }}
                  className="w-full py-2.5 px-3 rounded-xl bg-stone-100 dark:bg-stone-800 hover:bg-rose-50 dark:hover:bg-rose-950/40 hover:text-rose-600 dark:hover:text-rose-400 text-slate-700 dark:text-slate-300 font-bold text-xs transition-colors text-center cursor-pointer"
                >
                  Logout / Switch Account
                </button>
                <p className="text-[10px] text-slate-400 dark:text-slate-500 text-center mt-2">
                  AARVO &bull; Shop Smart, Live Better
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
            <div className="relative w-full max-w-sm bg-white dark:bg-[#211A17] text-slate-900 dark:text-slate-100 border border-orange-100 dark:border-stone-800 rounded-3xl p-6 shadow-2xl z-10 text-center flex flex-col items-center">
              <button
                onClick={() => setIsVoiceModalOpen(false)}
                className="absolute top-4 right-4 p-1 text-slate-400 hover:text-slate-800 dark:hover:text-white rounded-lg cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>

              <button
                onClick={() => {
                  if (isVoiceListening) {
                    stopVoiceSearch();
                  } else {
                    startVoiceSearch();
                  }
                }}
                className={`w-20 h-20 rounded-full flex items-center justify-center mb-4 relative transition-all active:scale-95 cursor-pointer ${
                  isVoiceListening
                    ? 'bg-gradient-to-tr from-[#FF5722] to-[#FF8A65] text-white shadow-lg shadow-orange-500/30'
                    : 'bg-[#FFF4E8] dark:bg-orange-950/40 text-[#FF5722]'
                }`}
                title={isVoiceListening ? "Tap to stop listening" : "Tap to speak"}
              >
                {isVoiceListening && (
                  <div className="absolute inset-0 rounded-full border-4 border-[#FF5722] animate-ping opacity-35" />
                )}
                <Mic className="w-9 h-9" />
              </button>

              <h3 className="font-display font-black text-lg text-slate-900 dark:text-white mb-1">
                {isVoiceListening ? 'Listening...' : voiceTranscript ? 'Recognized!' : 'Tap to Speak'}
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400 mb-3 px-2">
                {voiceTranscript ? (
                  <span className="font-medium text-slate-800 dark:text-slate-200 text-sm italic">
                    &ldquo;{voiceTranscript}&rdquo;
                  </span>
                ) : (
                  'Speak product name, category, or brand hands-free'
                )}
              </p>

              {voiceError && (
                <div className="mb-3 px-3 py-2 bg-rose-50 dark:bg-rose-950/50 border border-rose-200 dark:border-rose-800/60 rounded-xl text-xs text-rose-600 dark:text-rose-400 flex items-center gap-1.5 text-left">
                  <AlertTriangle className="w-4 h-4 shrink-0" />
                  <span>{voiceError}</span>
                </div>
              )}

              <div className="w-full pt-2 border-t border-orange-100/70 dark:border-stone-800">
                <p className="text-[11px] font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500 mb-2">
                  Or Quick Voice Prompts
                </p>
                <div className="flex flex-wrap justify-center gap-1.5">
                  {['Smartphones', 'Kurtis & Sarees', 'Running Shoes', 'Smart Watches'].map((sample) => (
                    <button
                      key={sample}
                      onClick={() => {
                        setSearchQuery(sample);
                        setIsVoiceModalOpen(false);
                      }}
                      className="px-2.5 py-1 bg-[#FFF4E8] dark:bg-[#181311] hover:bg-orange-100 dark:hover:bg-stone-800 text-slate-700 dark:text-slate-300 rounded-lg text-xs font-semibold transition-colors cursor-pointer"
                    >
                      {sample}
                    </button>
                  ))}
                </div>
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
            <div className="relative w-full max-w-sm bg-white dark:bg-[#211A17] text-slate-900 dark:text-slate-100 border border-orange-100 dark:border-stone-800 rounded-3xl p-5 shadow-2xl z-10">
              <div className="flex items-center justify-between pb-3 border-b border-orange-100/70 dark:border-stone-800 mb-3">
                <div className="flex items-center gap-2">
                  <Bell className="w-5 h-5 text-[#FF5722]" />
                  <h3 className="font-display font-black text-sm text-slate-900 dark:text-white">
                    Notifications
                  </h3>
                </div>
                <button
                  onClick={() => setIsNotificationModalOpen(false)}
                  className="p-1 text-slate-400 hover:text-slate-800 dark:hover:text-white rounded-lg cursor-pointer"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              <div className="space-y-2.5">
                <div className="p-3 bg-[#FFF4E8] dark:bg-orange-950/30 rounded-xl border border-orange-200/70 dark:border-orange-900/50">
                  <span className="text-[10px] font-bold text-[#FF5722]">
                    SEASONAL DEALS LIVE
                  </span>
                  <p className="text-xs font-semibold text-slate-800 dark:text-slate-200 mt-0.5">
                    Save flat ₹200 with AARVO200 on orders above ₹999.
                  </p>
                  <span className="text-[9px] text-slate-400 dark:text-slate-500 mt-1 block">Just now</span>
                </div>
                <div className="p-3 bg-stone-50 dark:bg-[#181311] rounded-xl border border-orange-100/60 dark:border-stone-800">
                  <span className="text-[10px] font-bold text-emerald-600 dark:text-emerald-400">
                    ORDER STATUS
                  </span>
                  <p className="text-xs font-semibold text-slate-800 dark:text-slate-200 mt-0.5">
                    {orders.length > 0
                      ? `Latest order ${orders[0].id} is currently ${orders[0].status}.`
                      : 'Place an order to receive live shipping notifications.'}
                  </p>
                  <span className="text-[9px] text-slate-400 dark:text-slate-500 mt-1 block">Updated live</span>
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
            <div className="relative w-full max-w-sm bg-white dark:bg-[#211A17] text-slate-900 dark:text-slate-100 border border-orange-100 dark:border-stone-800 rounded-3xl p-6 shadow-2xl z-10 text-center">
              <div className="w-16 h-16 rounded-full bg-emerald-100 dark:bg-emerald-950/50 text-emerald-600 dark:text-emerald-400 flex items-center justify-center mx-auto mb-3">
                <CheckCircle2 className="w-10 h-10 stroke-[2.5]" />
              </div>
              <h3 className="font-display font-black text-lg text-slate-900 dark:text-white">
                Order Placed Successfully!
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
                Order ID: <strong className="text-slate-900 dark:text-white font-mono">{orderSuccessModal}</strong>
              </p>
              <p className="text-xs text-slate-600 dark:text-slate-300 mt-2 bg-[#FFF4E8] dark:bg-[#181311] p-2.5 rounded-xl border border-orange-100 dark:border-stone-800">
                Your order is confirmed. Tracking updates will be sent to {userProfile.phone}.
              </p>

              <div className="flex flex-col gap-2 mt-4">
                <button
                  onClick={() => {
                    haptics.selection();
                    setOrderSuccessModal(null);
                    setIsOrdersViewOpen(true);
                  }}
                  className="w-full py-3 bg-[#FF5722] hover:bg-[#E64A19] text-white font-bold text-xs rounded-xl shadow-md active:scale-95 transition-all flex items-center justify-center gap-2 cursor-pointer"
                >
                  <Truck className="w-4 h-4" />
                  <span>Track Order in My Orders</span>
                </button>

                <button
                  onClick={() => {
                    haptics.light();
                    setOrderSuccessModal(null);
                    setActiveTab('home');
                  }}
                  className="w-full py-2.5 bg-stone-100 hover:bg-stone-200 dark:bg-stone-800 text-slate-700 dark:text-slate-300 font-bold text-xs rounded-xl active:scale-95 transition-all cursor-pointer"
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
              <div className="relative w-full max-w-md max-h-[92vh] overflow-y-auto bg-white dark:bg-[#211A17] text-slate-900 dark:text-slate-100 border border-orange-100 dark:border-stone-800 rounded-3xl p-5 shadow-2xl z-10">
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center gap-2">
                    <div className="w-8 h-8 rounded-xl bg-[#FFF4E8] dark:bg-orange-950/40 text-[#FF5722] flex items-center justify-center">
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

                {/* Tabs: Seller Overview, Add Product & Merchant Registration */}
                <div className="flex bg-[#FFF4E8] dark:bg-[#181311] p-1 rounded-xl mb-3.5 gap-1">
                  <button
                    type="button"
                    onClick={() => setSellerModalTab('overview')}
                    className={`flex-1 py-1.5 text-xs font-bold rounded-lg transition-all cursor-pointer ${
                      sellerModalTab === 'overview'
                        ? 'bg-white dark:bg-[#FF5722] text-slate-900 dark:text-white shadow-xs'
                        : 'text-slate-500 hover:text-slate-900 dark:text-slate-400'
                    }`}
                  >
                    Overview
                  </button>
                  <button
                    type="button"
                    onClick={() => setSellerModalTab('add-product')}
                    className={`flex-1 py-1.5 text-xs font-bold rounded-lg transition-all cursor-pointer flex items-center justify-center gap-1 ${
                      sellerModalTab === 'add-product'
                        ? 'bg-gradient-to-r from-[#FF5722] to-[#E64A19] text-white shadow-xs font-black'
                        : 'text-[#E64A19] dark:text-orange-400 font-bold hover:text-slate-900'
                    }`}
                  >
                    <Sparkles className="w-3 h-3" />
                    <span>Add Product</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => setSellerModalTab('register')}
                    className={`flex-1 py-1.5 text-xs font-bold rounded-lg transition-all cursor-pointer ${
                      sellerModalTab === 'register'
                        ? 'bg-white dark:bg-[#FF5722] text-slate-900 dark:text-white shadow-xs'
                        : 'text-slate-500 hover:text-slate-900 dark:text-slate-400'
                    }`}
                  >
                    Onboarding
                  </button>
                </div>

                {sellerModalTab === 'overview' ? (
                  <div className="space-y-3.5">
                    {/* Catalog Actions: Add Product, CSV Template & Bulk Upload */}
                    <div className="space-y-2">
                      <button
                        type="button"
                        onClick={() => setSellerModalTab('add-product')}
                        className="w-full py-2.5 px-3 bg-gradient-to-r from-[#FF7043] via-[#FF5722] to-[#E64A19] text-white font-extrabold text-xs rounded-xl shadow-md hover:opacity-95 active:scale-95 transition-all flex items-center justify-center gap-2 cursor-pointer"
                      >
                        <Sparkles className="w-4 h-4 text-amber-200" />
                        <span>+ Add Single Product with Custom Sizes</span>
                      </button>

                      <div className="grid grid-cols-2 gap-2">
                        <button
                          type="button"
                          onClick={downloadSellerCsvTemplate}
                          className="py-2 px-2.5 bg-[#FFF4E8] hover:bg-orange-100 dark:bg-[#181311] dark:hover:bg-stone-800 text-slate-800 dark:text-slate-200 font-bold text-[11px] rounded-xl border border-orange-200/70 dark:border-stone-700 active:scale-95 transition-all flex items-center justify-center gap-1.5 cursor-pointer"
                        >
                          <Download className="w-3.5 h-3.5 text-[#FF5722]" />
                          <span>CSV Template</span>
                        </button>
                        <button
                          type="button"
                          onClick={() => bulkCsvInputRef.current?.click()}
                          className="py-2 px-2.5 bg-slate-900 hover:bg-slate-800 dark:bg-[#FF5722] dark:hover:bg-[#E64A19] text-white font-bold text-[11px] rounded-xl shadow-xs active:scale-95 transition-all flex items-center justify-center gap-1.5 cursor-pointer"
                        >
                          <Upload className="w-3.5 h-3.5 text-emerald-400 dark:text-white" />
                          <span>Bulk Upload (CSV)</span>
                        </button>
                        <input
                          ref={bulkCsvInputRef}
                          type="file"
                          accept=".csv"
                          className="hidden"
                          onChange={handleBulkCsvUpload}
                        />
                      </div>
                    </div>
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
                              className="px-2.5 py-1 bg-[#FF5722] hover:bg-[#E64A19] text-white text-[10px] font-bold rounded-lg shadow-xs active:scale-95 transition-all shrink-0 cursor-pointer"
                            >
                              +10 Restock
                            </button>
                          </div>
                        ))}

                        {lowStockCount === 0 && (
                          <div className="p-3 text-center text-xs text-slate-500 dark:text-slate-400 bg-stone-50 dark:bg-[#181311] rounded-xl">
                            All products are well-stocked. No replenishment needed!
                          </div>
                        )}
                      </div>
                    </div>

                    {/* Link to Dedicated Web Seller Central */}
                    <div className="pt-2 border-t border-orange-100/70 dark:border-stone-800">
                      <a
                        href="/seller-panel/index.html"
                        className="w-full py-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 dark:bg-[#FF5722] dark:hover:bg-[#E64A19] text-white font-bold text-xs shadow-md flex items-center justify-center gap-2 transition-all active:scale-95 cursor-pointer"
                      >
                        <Store className="w-3.5 h-3.5" />
                        <span>Open Web Seller Central Dashboard</span>
                      </a>
                    </div>
                  </div>
                ) : sellerModalTab === 'add-product' ? (
                  <form
                    onSubmit={(e) => {
                      e.preventDefault();
                      haptics.saved();
                      const parsedPrice = parseInt(newProductPrice) || 1499;
                      const parsedMRP = parseInt(newProductMRP) || Math.round(parsedPrice * 1.6);
                      const discountPercent = Math.max(5, Math.round(((parsedMRP - parsedPrice) / parsedMRP) * 100));
                      const finalCategory =
                        newProductCategory === 'Other' && customCategory.trim()
                          ? customCategory.trim()
                          : newProductCategory;

                      const allSizes = [...selectedSizes];
                      if (customSizeText.trim() && !allSizes.includes(customSizeText.trim())) {
                        allSizes.push(customSizeText.trim());
                      }

                      const allColors = [...selectedColors];
                      if (customColorText.trim() && !allColors.includes(customColorText.trim())) {
                        allColors.push(customColorText.trim());
                      }

                      const createdProduct: Product = {
                        id: `prod-aarvo-${Date.now()}`,
                        name: newProductName.trim() || 'AARVO Luxury Edition Handcrafted Product',
                        category: finalCategory,
                        price: parsedPrice,
                        originalPrice: parsedMRP,
                        discount: `${discountPercent}% OFF`,
                        rating: 0.0,
                        reviewsCount: 0,
                        reviewsList: [],
                        inStock: true,
                        stockQuantity: parseInt(newProductStock) || 25,
                        image:
                          newProductImage.trim() ||
                          'https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=500&auto=format&fit=crop&q=80',
                        images: [
                          newProductImage.trim() ||
                            'https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=500&auto=format&fit=crop&q=80',
                          'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=500&auto=format&fit=crop&q=80',
                          'https://images.unsplash.com/photo-1592899677977-9c10ca588bbd?w=500&auto=format&fit=crop&q=80',
                        ],
                        seller: sellerForm.business.trim() || 'AARVO Marketplace Seller',
                        description:
                          newProductDescription.trim() ||
                          'No additional description provided by the seller.',
                        availableSizes: allSizes.length > 0 ? allSizes : undefined,
                        availableColors: allColors.length > 0 ? allColors : undefined,
                        fabric: selectedFabric || undefined,
                        codAvailable: codAllowed,
                        highlights: [
                          `Category: ${finalCategory}`,
                          allSizes.length > 0 ? `Sizes: ${allSizes.join(', ')}` : '',
                          allColors.length > 0 ? `Colours: ${allColors.join(', ')}` : '',
                          selectedFabric ? `Fabric: ${selectedFabric}` : '',
                          codAllowed ? 'Cash on Delivery (COD) Eligible' : 'Prepaid Only',
                          `${selectedReturnPolicy} Guaranteed`,
                        ].filter(Boolean),
                      };
                      setAllProducts((prev) => [createdProduct, ...prev]);
                      showToast(`✨ ${createdProduct.name.slice(0, 24)}... added to Catalog!`);
                      setNewProductName('');
                      setNewProductPrice('');
                      setNewProductMRP('');
                      setNewProductDescription('');
                      setCustomCategory('');
                      setCustomSizeText('');
                      setCustomColorText('');
                      setSellerModalTab('overview');
                    }}
                    className="space-y-3.5 animate-in fade-in duration-200"
                  >
                    {/* Header Banner */}
                    <div className="p-3 rounded-2xl bg-gradient-to-r from-[#FF7043] via-[#FF5722] to-[#E64A19] text-white shadow-sm">
                      <div className="flex items-center gap-2">
                        <Sparkles className="w-4 h-4 text-amber-200" />
                        <h4 className="font-display font-black text-xs sm:text-sm">
                          AARVO Merchant Product Studio
                        </h4>
                      </div>
                      <p className="text-[10px] text-orange-100 mt-0.5">
                        Publish catalog products with standard sizing, colors, and live customer reviews.
                      </p>
                    </div>

                    {/* Product Name */}
                    <div>
                      <label className="text-[11px] font-bold text-slate-700 dark:text-slate-300 block mb-1">
                        Product Title
                      </label>
                      <input
                        type="text"
                        required
                        placeholder="e.g. Pure Mulberry Silk Hand-Embroidered Kurta Set"
                        value={newProductName}
                        onChange={(e) => setNewProductName(e.target.value)}
                        className="w-full px-3 py-2 text-xs bg-[#FFF4E8]/60 dark:bg-[#181311] text-slate-900 dark:text-white border border-orange-200/70 dark:border-stone-700 rounded-xl outline-none focus:border-[#FF5722]"
                      />
                    </div>

                    {/* Auto Category Options with Other Input */}
                    <div className="space-y-1.5">
                      <label className="text-[11px] font-bold text-slate-700 dark:text-slate-300 block">
                        Product Category (Auto Options)
                      </label>
                      <div className="flex flex-wrap gap-1.5">
                        {[
                          'Fashion',
                          'Footwear',
                          'Mobiles',
                          'Electronics',
                          'Home & Kitchen',
                          'Beauty',
                          'Sports',
                          'Other',
                        ].map((cat) => {
                          const isSelected = newProductCategory === cat;
                          return (
                            <button
                              key={cat}
                              type="button"
                              onClick={() => {
                                haptics.selection();
                                setNewProductCategory(cat);
                              }}
                              className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                                isSelected
                                  ? 'bg-[#FF5722] text-white shadow-xs font-black'
                                  : 'bg-[#FFF4E8]/70 dark:bg-[#181311] text-slate-700 dark:text-slate-300 border border-orange-200/70 dark:border-stone-700 hover:border-[#FF5722]'
                              }`}
                            >
                              {cat}
                              {isSelected && <span className="ml-1 text-[10px] text-amber-200">✓</span>}
                            </button>
                          );
                        })}
                      </div>

                      {/* Custom Category Input if "Other" is selected */}
                      {newProductCategory === 'Other' && (
                        <div className="pt-1 animate-in fade-in">
                          <label className="text-[10px] font-bold text-[#FF5722] block mb-1">
                            Specify Custom Category Name
                          </label>
                          <input
                            type="text"
                            required
                            placeholder="e.g. Traditional Handcrafts, Books, Jewelry, Luggage..."
                            value={customCategory}
                            onChange={(e) => setCustomCategory(e.target.value)}
                            className="w-full px-3 py-2 text-xs bg-white dark:bg-[#181311] text-slate-900 dark:text-white border-2 border-[#FF5722] rounded-xl outline-none"
                          />
                        </div>
                      )}
                    </div>

                    {/* Pricing: Selling Price & MRP */}
                    <div className="grid grid-cols-2 gap-2">
                      <div>
                        <label className="text-[11px] font-bold text-slate-700 dark:text-slate-300 block mb-1">
                          Selling Price (₹)
                        </label>
                        <input
                          type="number"
                          required
                          placeholder="e.g. 1999"
                          value={newProductPrice}
                          onChange={(e) => setNewProductPrice(e.target.value)}
                          className="w-full px-3 py-2 text-xs bg-[#FFF4E8]/60 dark:bg-[#181311] text-slate-900 dark:text-white border border-orange-200/70 dark:border-stone-700 rounded-xl outline-none focus:border-[#FF5722]"
                        />
                      </div>
                      <div>
                        <label className="text-[11px] font-bold text-slate-700 dark:text-slate-300 block mb-1">
                          Original MRP (₹)
                        </label>
                        <input
                          type="number"
                          placeholder="e.g. 3999"
                          value={newProductMRP}
                          onChange={(e) => setNewProductMRP(e.target.value)}
                          className="w-full px-3 py-2 text-xs bg-[#FFF4E8]/60 dark:bg-[#181311] text-slate-900 dark:text-white border border-orange-200/70 dark:border-stone-700 rounded-xl outline-none focus:border-[#FF5722]"
                        />
                      </div>
                    </div>

                    {/* SIZES SPECIFICATION */}
                    <div className="p-3 rounded-2xl bg-[#FFF4E8]/60 dark:bg-[#181311] border border-orange-200/70 dark:border-stone-700 space-y-2">
                      <div className="flex items-center justify-between">
                        <div>
                          <span className="text-[11px] font-black text-slate-900 dark:text-white uppercase tracking-wider flex items-center gap-1">
                            <Tag className="w-3.5 h-3.5 text-[#FF5722]" />
                            <span>Available Size Variants</span>
                          </span>
                          <p className="text-[10px] text-slate-500 dark:text-slate-400">
                            Select standard apparel sizes or footwear dimensions
                          </p>
                        </div>
                        <span className="text-[9px] bg-orange-100 dark:bg-orange-950/60 text-[#E64A19] dark:text-orange-300 px-2 py-0.5 rounded font-black">
                          {selectedSizes.length} SELECTED
                        </span>
                      </div>

                      {/* Interactive Size Chips */}
                      <div className="flex flex-wrap gap-1.5 pt-0.5">
                        {[
                          'Zero Size',
                          'XS',
                          'S',
                          'M',
                          'L',
                          'XL',
                          'XXL',
                          '3XL',
                          'Free Size',
                          'UK 6',
                          'UK 7',
                          'UK 8',
                          'UK 9',
                          'UK 10',
                        ].map((size) => {
                          const isSelected = selectedSizes.includes(size);
                          return (
                            <button
                              key={size}
                              type="button"
                              onClick={() => {
                                haptics.selection();
                                setSelectedSizes((prev) =>
                                  isSelected ? prev.filter((s) => s !== size) : [...prev, size]
                                );
                              }}
                              className={`px-2.5 py-1 rounded-lg text-xs font-bold transition-all cursor-pointer ${
                                isSelected
                                  ? 'bg-[#FF5722] text-white shadow-xs font-black'
                                  : 'bg-white dark:bg-[#211A17] text-slate-700 dark:text-slate-300 border border-orange-200/70 dark:border-stone-700 hover:border-[#FF5722]'
                              }`}
                            >
                              <span>{size}</span>
                              {isSelected && <span className="ml-1 text-[10px] text-amber-200">✓</span>}
                            </button>
                          );
                        })}
                      </div>

                      {/* Custom Size Text Input */}
                      <div>
                        <label className="text-[10px] font-bold text-slate-600 dark:text-slate-400 block mb-1">
                          Custom Size Details / Tailored Dimension (Optional)
                        </label>
                        <input
                          type="text"
                          placeholder="e.g. Chest 42 in, Slim Tailored Fit, 32 Waist"
                          value={customSizeText}
                          onChange={(e) => setCustomSizeText(e.target.value)}
                          className="w-full px-3 py-1.5 text-xs bg-white dark:bg-[#211A17] text-slate-900 dark:text-white border border-orange-200/70 dark:border-stone-700 rounded-xl outline-none focus:border-[#FF5722]"
                        />
                      </div>
                    </div>

                    {/* COLOUR VARIANTS */}
                    <div className="p-3 rounded-2xl bg-[#FFF4E8]/60 dark:bg-[#181311] border border-orange-200/70 dark:border-stone-700 space-y-2">
                      <div className="flex items-center justify-between">
                        <div>
                          <span className="text-[11px] font-black text-slate-900 dark:text-white uppercase tracking-wider flex items-center gap-1">
                            <Sparkles className="w-3.5 h-3.5 text-[#FF5722]" />
                            <span>Available Colour Variants</span>
                          </span>
                          <p className="text-[10px] text-slate-500 dark:text-slate-400">
                            Select color options available for customer selection
                          </p>
                        </div>
                        <span className="text-[9px] bg-orange-100 dark:bg-orange-950/60 text-[#E64A19] dark:text-orange-300 px-2 py-0.5 rounded font-black">
                          {selectedColors.length} SELECTED
                        </span>
                      </div>

                      {/* Color Swatch Buttons */}
                      <div className="flex flex-wrap gap-1.5 pt-0.5">
                        {[
                          { name: 'Black', hex: '#0F172A' },
                          { name: 'White', hex: '#FFFFFF', border: true },
                          { name: 'Royal Blue', hex: '#2563EB' },
                          { name: 'Navy Blue', hex: '#1E293B' },
                          { name: 'Red', hex: '#DC2626' },
                          { name: 'Maroon', hex: '#881337' },
                          { name: 'Emerald Green', hex: '#047857' },
                          { name: 'Olive Green', hex: '#4D7C0F' },
                          { name: 'Yellow', hex: '#EAB308' },
                          { name: 'Beige', hex: '#D4B996' },
                          { name: 'Pink', hex: '#EC4899' },
                          { name: 'Grey', hex: '#64748B' },
                          { name: 'Orange', hex: '#EA580C' },
                          { name: 'Purple', hex: '#7E22CE' },
                        ].map((col) => {
                          const isSelected = selectedColors.includes(col.name);
                          return (
                            <button
                              key={col.name}
                              type="button"
                              onClick={() => {
                                haptics.selection();
                                setSelectedColors((prev) =>
                                  isSelected ? prev.filter((c) => c !== col.name) : [...prev, col.name]
                                );
                              }}
                              className={`flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-bold transition-all cursor-pointer ${
                                isSelected
                                  ? 'bg-[#FF5722] text-white shadow-xs font-black'
                                  : 'bg-white dark:bg-[#211A17] text-slate-700 dark:text-slate-300 border border-orange-200/70 dark:border-stone-700 hover:border-[#FF5722]'
                              }`}
                            >
                              <span
                                className={`w-3.5 h-3.5 rounded-full shrink-0 ${
                                  col.border ? 'border border-slate-300' : ''
                                }`}
                                style={{ backgroundColor: col.hex }}
                              />
                              <span>{col.name}</span>
                              {isSelected && <span className="text-[10px] text-amber-200">✓</span>}
                            </button>
                          );
                        })}
                      </div>

                      {/* Custom Color Input */}
                      <div>
                        <label className="text-[10px] font-bold text-slate-600 dark:text-slate-400 block mb-1">
                          Custom Shade / Multicolour (Optional)
                        </label>
                        <input
                          type="text"
                          placeholder="e.g. Dual Tone Pastel Blue, Gold Embroidered"
                          value={customColorText}
                          onChange={(e) => setCustomColorText(e.target.value)}
                          className="w-full px-3 py-1.5 text-xs bg-white dark:bg-[#211A17] text-slate-900 dark:text-white border border-orange-200/70 dark:border-stone-700 rounded-xl outline-none focus:border-[#FF5722]"
                        />
                      </div>
                    </div>

                    {/* Fabric / Material & Cash on Delivery Options */}
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                      <div>
                        <label className="text-[11px] font-bold text-slate-700 dark:text-slate-300 block mb-1">
                          Fabric / Material
                        </label>
                        <select
                          value={selectedFabric}
                          onChange={(e) => setSelectedFabric(e.target.value)}
                          className="w-full px-3 py-2 text-xs bg-[#FFF4E8]/60 dark:bg-[#181311] text-slate-900 dark:text-white border border-orange-200/70 dark:border-stone-700 rounded-xl outline-none focus:border-[#FF5722]"
                        >
                          <option value="Pure Cotton">Pure Cotton</option>
                          <option value="Mulberry Silk">Mulberry Silk</option>
                          <option value="Denim">Premium Denim</option>
                          <option value="Linen">Breathable Linen</option>
                          <option value="Rayon">Soft Rayon</option>
                          <option value="Polyester">Durable Polyester</option>
                          <option value="Genuine Leather">Genuine Leather</option>
                          <option value="Metal / Electronic">Hardware / Electronics</option>
                        </select>
                      </div>

                      <div>
                        <label className="text-[11px] font-bold text-slate-700 dark:text-slate-300 block mb-1">
                          Return Policy
                        </label>
                        <select
                          value={selectedReturnPolicy}
                          onChange={(e) => setSelectedReturnPolicy(e.target.value)}
                          className="w-full px-3 py-2 text-xs bg-[#FFF4E8]/60 dark:bg-[#181311] text-slate-900 dark:text-white border border-orange-200/70 dark:border-stone-700 rounded-xl outline-none focus:border-[#FF5722]"
                        >
                          <option value="7 Days Return">7 Days Return</option>
                          <option value="10 Days Replacement">10 Days Replacement</option>
                          <option value="No Returns">No Returns</option>
                        </select>
                      </div>
                    </div>

                    {/* Cash on Delivery Toggle */}
                    <div
                      onClick={() => {
                        haptics.selection();
                        setCodAllowed(!codAllowed);
                      }}
                      className="p-3 bg-white dark:bg-[#181311] rounded-xl border border-orange-200/70 dark:border-stone-700 flex items-center justify-between cursor-pointer"
                    >
                      <div className="flex items-center gap-2">
                        <Banknote className="w-4 h-4 text-emerald-600" />
                        <div>
                          <span className="text-xs font-bold text-slate-900 dark:text-white block">
                            Allow Cash on Delivery (COD)
                          </span>
                          <span className="text-[10px] text-slate-500">
                            Customers can pay cash at doorstep upon delivery
                          </span>
                        </div>
                      </div>
                      <div
                        className={`w-10 h-5 rounded-full p-0.5 transition-colors ${
                          codAllowed ? 'bg-emerald-600' : 'bg-slate-300 dark:bg-stone-600'
                        }`}
                      >
                        <div
                          className={`w-4 h-4 rounded-full bg-white transition-transform ${
                            codAllowed ? 'translate-x-5' : 'translate-x-0'
                          }`}
                        />
                      </div>
                    </div>

                    {/* Image URL & Thumbnail Preview */}
                    <div>
                      <label className="text-[11px] font-bold text-slate-700 dark:text-slate-300 block mb-1">
                        Product Photo URL (High-Res Showcase)
                      </label>
                      <input
                        type="url"
                        placeholder="https://..."
                        value={newProductImage}
                        onChange={(e) => setNewProductImage(e.target.value)}
                        className="w-full px-3 py-2 text-xs bg-[#FFF4E8]/60 dark:bg-[#181311] text-slate-900 dark:text-white border border-orange-200/70 dark:border-stone-700 rounded-xl outline-none focus:border-[#FF5722]"
                      />
                      {newProductImage && (
                        <div className="mt-2 flex items-center gap-2.5 p-2 bg-[#FFF4E8]/50 dark:bg-[#181311] rounded-xl border border-orange-200/70 dark:border-stone-800">
                          <img
                            src={newProductImage}
                            alt="Preview"
                            className="w-12 h-12 rounded-lg object-cover bg-white"
                          />
                          <div className="text-[11px] text-slate-500">
                            <span className="font-bold text-slate-800 dark:text-slate-200 block">
                              Photo Preview
                            </span>
                            <span>Supports sliding &amp; zoom view on product details</span>
                          </div>
                        </div>
                      )}
                    </div>

                    {/* Stock Quantity */}
                    <div>
                      <label className="text-[11px] font-bold text-slate-700 dark:text-slate-300 block mb-1">
                        Stock Quantity
                      </label>
                      <input
                        type="number"
                        placeholder="25"
                        value={newProductStock}
                        onChange={(e) => setNewProductStock(e.target.value)}
                        className="w-full px-3 py-2 text-xs bg-[#FFF4E8]/60 dark:bg-[#181311] text-slate-900 dark:text-white border border-orange-200/70 dark:border-stone-700 rounded-xl outline-none focus:border-[#FF5722]"
                      />
                    </div>

                    {/* Description */}
                    <div>
                      <label className="text-[11px] font-bold text-slate-700 dark:text-slate-300 block mb-1">
                        Product Description &amp; Specifications
                      </label>
                      <textarea
                        rows={2}
                        placeholder="Describe fabric, finish, and packaging details..."
                        value={newProductDescription}
                        onChange={(e) => setNewProductDescription(e.target.value)}
                        className="w-full px-3 py-2 text-xs bg-[#FFF4E8]/60 dark:bg-[#181311] text-slate-900 dark:text-white border border-orange-200/70 dark:border-stone-700 rounded-xl outline-none focus:border-[#FF5722]"
                      />
                    </div>

                    {/* Genuine Rating Note */}
                    <div className="p-2.5 bg-[#FFF4E8] dark:bg-orange-950/30 rounded-xl border border-orange-200/80 dark:border-orange-900/50 text-[10px] text-[#E64A19] dark:text-orange-300 flex items-center gap-2">
                      <CheckCircle2 className="w-4 h-4 text-[#FF5722] shrink-0" />
                      <span>
                        <strong>Honest Rating Policy:</strong> Ratings start cleanly from real buyer feedback with zero pre-filled reviews.
                      </span>
                    </div>

                    {/* Submit Button */}
                    <button
                      type="submit"
                      className="w-full py-3 bg-[#FF5722] hover:bg-[#E64A19] text-white font-extrabold text-xs rounded-xl shadow-lg active:scale-95 transition-all flex items-center justify-center gap-2 cursor-pointer"
                    >
                      <Sparkles className="w-4 h-4 text-amber-200" />
                      <span>Publish Product to AARVO Catalog</span>
                    </button>
                  </form>
                ) : !sellerRegistered ? (
                  <form
                    onSubmit={(e) => {
                      e.preventDefault();
                      haptics.saved();
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
                        className="w-full px-3 py-2 text-xs bg-[#FFF4E8]/60 dark:bg-[#181311] text-slate-900 dark:text-white border border-orange-200/70 dark:border-stone-700 rounded-xl outline-none focus:border-[#FF5722]"
                      />
                    </div>

                    <div>
                      <label className="text-[11px] font-bold text-slate-700 dark:text-slate-300 block mb-1">
                        Primary Category
                      </label>
                      <select
                        value={sellerForm.category}
                        onChange={(e) => setSellerForm({ ...sellerForm, category: e.target.value })}
                        className="w-full px-3 py-2 text-xs bg-[#FFF4E8]/60 dark:bg-[#181311] text-slate-900 dark:text-white border border-orange-200/70 dark:border-stone-700 rounded-xl outline-none focus:border-[#FF5722]"
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
                        className="w-full px-3 py-2 text-xs bg-[#FFF4E8]/60 dark:bg-[#181311] text-slate-900 dark:text-white border border-orange-200/70 dark:border-stone-700 rounded-xl outline-none focus:border-[#FF5722] font-mono"
                      />
                    </div>

                    <button
                      type="submit"
                      className="w-full py-2.5 rounded-xl bg-[#FF5722] hover:bg-[#E64A19] text-white font-bold text-xs shadow-md mt-2 transition-all active:scale-95 cursor-pointer"
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
                      Welcome <strong className="text-[#FF5722]">{sellerForm.business || 'Merchant'}</strong>! Our verification team will review your GST &amp; inventory within 24 hours.
                    </p>
                    <button
                      onClick={() => {
                        setSellerRegistered(false);
                        setIsSellerModalOpen(false);
                      }}
                      className="mt-3 px-4 py-1.5 rounded-xl bg-[#FF5722] text-white text-xs font-bold cursor-pointer"
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
          onAddToCart={(prod, qty) => {
            addToCart(prod, qty || 1);
          }}
          isWishlisted={selectedProduct ? wishlist.includes(selectedProduct.id) : false}
          onToggleWishlist={toggleWishlist}
          onOpenImageZoom={(prod) => setZoomImageProduct(prod)}
          cartBadgeCount={cartItems.reduce((acc, i) => acc + i.quantity, 0)}
          onOpenCart={() => {
            setSelectedProduct(null);
            switchTab('cart');
          }}
          onOpenOrders={() => {
            setSelectedProduct(null);
            switchTab('account');
            setIsOrdersViewOpen(true);
          }}
          savedAddress={activeAddressString}
          customerName={userProfile.name}
          onUpdateAddress={(newAddr) => {
            if (!activeAddressObj) return;
            const trimmed = newAddr.trim();
            const isOnlyPin = /^\d{6}$/.test(trimmed);
            const pincodeMatch = trimmed.match(/\b\d{6}\b/);
            setSavedAddresses((prev) =>
              prev.map((a) =>
                a.id === activeAddressObj.id
                  ? {
                      ...a,
                      addressLine: isOnlyPin ? a.addressLine : trimmed,
                      pincode: pincodeMatch ? pincodeMatch[0] : a.pincode,
                    }
                  : a
              )
            );
            showToast(isOnlyPin ? `Delivery pincode updated to ${trimmed}` : 'Delivery address updated!');
          }}
          onPlaceOrder={(newOrder) => {
            setOrders((prev) => {
              const updated = [newOrder, ...prev];
              try {
                localStorage.setItem('aarvo_orders', JSON.stringify(updated));
              } catch (e) {
                // ignore
              }
              return updated;
            });
            if (selectedProduct) {
              setAllProducts((prev) =>
                prev.map((item) =>
                  item.id === selectedProduct.id
                    ? {
                        ...item,
                        stockQuantity: Math.max(
                          0,
                          (item.stockQuantity ?? 10) - (newOrder.items[0]?.quantity || 1)
                        ),
                      }
                    : item
                )
              );
            }
            showToast('Order confirmed via Cash on Delivery!');
          }}
          onBuyNow={(prod, qty) => {
            addToCart(prod, qty || 1);
            setSelectedProduct(null);
            switchTab('cart');
          }}
          onAddReview={(productId, review) => {
            setAllProducts((prev) =>
              prev.map((item) => {
                if (item.id !== productId) return item;
                const existing = item.reviewsList || [];
                const updatedReviews = [review, ...existing];
                const newCount = (item.reviewsCount || 0) + 1;
                const totalRating = (item.rating || 0) * (item.reviewsCount || 0) + review.rating;
                const newAvg = Number((totalRating / newCount).toFixed(1));
                return {
                  ...item,
                  rating: newAvg,
                  reviewsCount: newCount,
                  reviewsList: updatedReviews,
                };
              })
            );
            if (selectedProduct && selectedProduct.id === productId) {
              const existing = selectedProduct.reviewsList || [];
              const updatedReviews = [review, ...existing];
              const newCount = (selectedProduct.reviewsCount || 0) + 1;
              const totalRating = (selectedProduct.rating || 0) * (selectedProduct.reviewsCount || 0) + review.rating;
              const newAvg = Number((totalRating / newCount).toFixed(1));
              setSelectedProduct({
                ...selectedProduct,
                rating: newAvg,
                reviewsCount: newCount,
                reviewsList: updatedReviews,
              });
            }
          }}
        />

        {/* ================================================================= */}
        {/* 14. AARVO FULL-SCREEN 360° PRODUCT VIEWER MODAL                   */}
        {/* ================================================================= */}
        <Product360ViewerModal
          product={product360Modal}
          isOpen={!!product360Modal}
          onClose={() => setProduct360Modal(null)}
          onAddToCart={(prod) => addToCart(prod)}
          onBuyNow={(prod) => {
            addToCart(prod);
            setProduct360Modal(null);
            switchTab('cart');
          }}
        />

        {/* ================================================================= */}
        {/* 15. FLIPKART-STYLE FULL-SCREEN IMAGE ZOOM LIGHTBOX MODAL          */}
        {/* ================================================================= */}
        <ProductImageZoomModal
          product={zoomImageProduct}
          isOpen={!!zoomImageProduct}
          onClose={() => setZoomImageProduct(null)}
          onAddToCart={(prod) => addToCart(prod)}
          onBuyNow={(prod) => {
            addToCart(prod);
            setZoomImageProduct(null);
            switchTab('cart');
          }}
          onOpen360={(prod) => {
            setZoomImageProduct(null);
            setProduct360Modal(prod);
          }}
        />

        {/* ================================================================= */}
        {/* 16. ADD PAYMENT METHOD MODAL (Cards & Wallets)                   */}
        {/* ================================================================= */}
        {showAddPaymentModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs">
            <div className="w-full max-w-md bg-white dark:bg-[#211A17] rounded-3xl p-5 border border-orange-100 dark:border-stone-800 shadow-2xl space-y-4">
              <div className="flex items-center justify-between border-b border-orange-100/70 dark:border-stone-800 pb-3">
                <div className="flex items-center gap-2">
                  <div className="w-8 h-8 rounded-full bg-[#FFF4E8] dark:bg-orange-950/50 text-[#FF5722] flex items-center justify-center font-bold">
                    💳
                  </div>
                  <h3 className="font-black text-slate-900 dark:text-white text-base">
                    Add Payment Method
                  </h3>
                </div>
                <button
                  type="button"
                  onClick={() => setShowAddPaymentModal(false)}
                  className="p-1.5 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 rounded-full cursor-pointer"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              <div className="space-y-3">
                <div>
                  <label className="text-[11px] font-bold text-slate-600 dark:text-slate-400 block mb-1">
                    Card Number
                  </label>
                  <input
                    type="text"
                    value={newCardNumber}
                    onChange={(e) => setNewCardNumber(e.target.value)}
                    placeholder="4532 •••• •••• 9821"
                    maxLength={19}
                    className="w-full text-xs px-3.5 py-2.5 rounded-xl border border-orange-200/70 dark:border-stone-700 bg-[#FFF4E8]/50 dark:bg-[#181311] text-slate-900 dark:text-white focus:outline-hidden focus:border-[#FF5722]"
                  />
                </div>

                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="text-[11px] font-bold text-slate-600 dark:text-slate-400 block mb-1">
                      Expiry Date
                    </label>
                    <input
                      type="text"
                      value={newCardExpiry}
                      onChange={(e) => setNewCardExpiry(e.target.value)}
                      placeholder="MM/YY"
                      maxLength={5}
                      className="w-full text-xs px-3.5 py-2.5 rounded-xl border border-orange-200/70 dark:border-stone-700 bg-[#FFF4E8]/50 dark:bg-[#181311] text-slate-900 dark:text-white focus:outline-hidden focus:border-[#FF5722]"
                    />
                  </div>
                  <div>
                    <label className="text-[11px] font-bold text-slate-600 dark:text-slate-400 block mb-1">
                      CVV
                    </label>
                    <input
                      type="password"
                      maxLength={3}
                      placeholder="•••"
                      className="w-full text-xs px-3.5 py-2.5 rounded-xl border border-orange-200/70 dark:border-stone-700 bg-[#FFF4E8]/50 dark:bg-[#181311] text-slate-900 dark:text-white focus:outline-hidden focus:border-[#FF5722]"
                    />
                  </div>
                </div>

                <div>
                  <label className="text-[11px] font-bold text-slate-600 dark:text-slate-400 block mb-1">
                    Name on Card
                  </label>
                  <input
                    type="text"
                    value={newCardHolder}
                    onChange={(e) => setNewCardHolder(e.target.value)}
                    placeholder={`e.g. ${userProfile.name}`}
                    className="w-full text-xs px-3.5 py-2.5 rounded-xl border border-orange-200/70 dark:border-stone-700 bg-[#FFF4E8]/50 dark:bg-[#181311] text-slate-900 dark:text-white focus:outline-hidden focus:border-[#FF5722]"
                  />
                </div>

                <div className="p-3 bg-[#FFF4E8] dark:bg-[#181311] rounded-xl flex items-center gap-2 text-[11px] text-slate-500 dark:text-slate-400">
                  <ShieldCheck className="w-4 h-4 text-emerald-600 shrink-0" />
                  <span>256-bit encrypted bank grade transaction security</span>
                </div>
              </div>

              <div className="flex gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowAddPaymentModal(false)}
                  className="flex-1 py-2.5 text-xs font-bold text-slate-600 dark:text-slate-300 bg-stone-100 dark:bg-stone-800 rounded-xl hover:bg-stone-200 dark:hover:bg-stone-700 transition cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={() => {
                    haptics.saved();
                    showToast('Payment method saved securely!');
                    setPaymentMethodSelection('UPI');
                    setShowAddPaymentModal(false);
                  }}
                  className="flex-1 py-2.5 text-xs font-bold text-white bg-[#FF5722] hover:bg-[#E64A19] rounded-xl shadow-md transition cursor-pointer"
                >
                  Save &amp; Use
                </button>
              </div>
            </div>
          </div>
        )}

        {/* 17. EDIT PROFILE MODAL */}
        {isEditProfileOpen && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs">
            <form
              onSubmit={(e) => {
                e.preventDefault();
                if (!profileDraftName.trim() || !profileDraftPhone.trim()) return;
                haptics.saved();
                setUserProfile({ name: profileDraftName.trim(), phone: profileDraftPhone.trim() });
                setIsEditProfileOpen(false);
                showToast('Profile updated!');
              }}
              className="w-full max-w-sm bg-white dark:bg-[#211A17] rounded-3xl p-5 border border-orange-100 dark:border-stone-800 shadow-2xl space-y-4"
            >
              <div className="flex items-center justify-between border-b border-orange-100/70 dark:border-stone-800 pb-3">
                <h3 className="font-display font-black text-sm text-slate-900 dark:text-white flex items-center gap-2">
                  <Edit2 className="w-4 h-4 text-[#FF5722]" />
                  <span>Edit Profile</span>
                </h3>
                <button
                  type="button"
                  onClick={() => setIsEditProfileOpen(false)}
                  className="p-1 text-slate-400 hover:text-slate-700 dark:hover:text-white rounded-lg cursor-pointer"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>
              <div className="space-y-3">
                <div>
                  <label className="text-[11px] font-bold text-slate-600 dark:text-slate-400 block mb-1">
                    Full Name
                  </label>
                  <input
                    type="text"
                    required
                    value={profileDraftName}
                    onChange={(e) => setProfileDraftName(e.target.value)}
                    className="w-full text-xs px-3.5 py-2.5 rounded-xl border border-orange-200/70 dark:border-stone-700 bg-[#FFF4E8]/50 dark:bg-[#181311] text-slate-900 dark:text-white outline-none focus:border-[#FF5722]"
                  />
                </div>
                <div>
                  <label className="text-[11px] font-bold text-slate-600 dark:text-slate-400 block mb-1">
                    Mobile Number
                  </label>
                  <input
                    type="tel"
                    required
                    value={profileDraftPhone}
                    onChange={(e) => setProfileDraftPhone(e.target.value)}
                    className="w-full text-xs px-3.5 py-2.5 rounded-xl border border-orange-200/70 dark:border-stone-700 bg-[#FFF4E8]/50 dark:bg-[#181311] text-slate-900 dark:text-white outline-none focus:border-[#FF5722]"
                  />
                </div>
              </div>
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setIsEditProfileOpen(false)}
                  className="flex-1 py-2.5 text-xs font-bold bg-stone-100 dark:bg-stone-800 text-slate-700 dark:text-slate-300 rounded-xl cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="flex-1 py-2.5 text-xs font-bold bg-[#FF5722] hover:bg-[#E64A19] text-white rounded-xl shadow-md cursor-pointer"
                >
                  Save Changes
                </button>
              </div>
            </form>
          </div>
        )}

        {/* 18. FLIPKART-STYLE DELIVERY LOCATION & SAVED ADDRESSES MODAL */}
        {isAddressModalOpen && (
          <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-0 sm:p-4 bg-black/60 backdrop-blur-xs">
            <div className="w-full max-w-md max-h-[90vh] overflow-y-auto bg-white dark:bg-[#211A17] rounded-t-3xl sm:rounded-3xl p-5 border border-orange-100 dark:border-stone-800 shadow-2xl space-y-4">
              <div className="flex items-center justify-between border-b border-orange-100/70 dark:border-stone-800 pb-3">
                <div>
                  <h3 className="font-display font-black text-sm text-slate-900 dark:text-white flex items-center gap-2">
                    <MapPin className="w-4 h-4 text-[#FF5722]" />
                    <span>Select Delivery Location</span>
                  </h3>
                  <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">
                    Currently delivering to{' '}
                    <span className="font-bold text-slate-800 dark:text-slate-200">
                      {activeAddressObj
                        ? `${activeAddressObj.city} - ${activeAddressObj.pincode}`
                        : 'Select Address'}
                    </span>
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => {
                    setIsAddressModalOpen(false);
                    setEditingAddressId(null);
                  }}
                  className="p-1 text-slate-400 hover:text-slate-700 dark:hover:text-white rounded-lg cursor-pointer"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              {/* Flipkart-style Enter Pincode Quick Bar + Detect Location */}
              <div className="p-3 rounded-2xl bg-[#FFF4E8] dark:bg-[#181311] border border-orange-200/70 dark:border-stone-800 space-y-2">
                <label className="text-[11px] font-black text-slate-800 dark:text-slate-200 block">
                  Enter Delivery Pincode
                </label>
                <div className="flex items-center gap-2">
                  <input
                    type="text"
                    inputMode="numeric"
                    maxLength={6}
                    placeholder="Enter 6-digit pincode"
                    value={quickPincodeInput}
                    onChange={(e) => {
                      setQuickPincodeInput(e.target.value.replace(/\D/g, '').slice(0, 6));
                      setPincodeStatusMessage(null);
                    }}
                    className="flex-1 text-xs font-bold tabular-nums px-3 py-2 rounded-xl bg-white dark:bg-[#211A17] border border-orange-200/80 dark:border-stone-700 text-slate-900 dark:text-white outline-none focus:border-[#FF5722]"
                  />
                  <button
                    type="button"
                    onClick={async () => {
                      const cleanPin = quickPincodeInput.trim();
                      if (!/^[1-9]\d{5}$/.test(cleanPin)) {
                        setPincodeStatusMessage('Please enter a valid 6-digit Indian pincode.');
                        showToast('Enter a valid 6-digit pincode', 'error');
                        return;
                      }
                      haptics.saved();
                      setPincodeStatusMessage(`Checking delivery serviceability for PIN ${cleanPin}…`);
                      const res = await checkShiprocketServiceability(cleanPin);
                      const existingMatch = savedAddresses.find((a) => a.pincode === cleanPin);
                      if (existingMatch) {
                        setActiveAddressId(existingMatch.id);
                      } else if (activeAddressObj) {
                        setSavedAddresses((prev) =>
                          prev.map((a) =>
                            a.id === activeAddressObj.id ? { ...a, pincode: cleanPin } : a
                          )
                        );
                      }
                      const msg = res
                        ? res.riderMessage
                        : `Delivery pincode updated to ${cleanPin}`;
                      setPincodeStatusMessage(msg);
                      showToast(msg, {
                        title: 'Delivery Pincode Updated',
                        variant: res && !res.serviceable ? 'warning' : 'success',
                      });
                    }}
                    className="px-3.5 py-2 rounded-xl bg-[#FF5722] hover:bg-[#E64A19] text-white text-xs font-black cursor-pointer shrink-0"
                  >
                    Apply
                  </button>
                </div>

                {pincodeStatusMessage && (
                  <p className="text-[11px] font-semibold text-emerald-700 dark:text-emerald-400">
                    {pincodeStatusMessage}
                  </p>
                )}

                <button
                  type="button"
                  disabled={isDetectingLocation}
                  onClick={() => {
                    haptics.light();
                    if (typeof navigator === 'undefined' || !navigator.geolocation) {
                      showToast('Location detection unavailable — choose or add an address below', 'info');
                      return;
                    }
                    setIsDetectingLocation(true);
                    navigator.geolocation.getCurrentPosition(
                      (pos) => {
                        setIsDetectingLocation(false);
                        const coords = `${pos.coords.latitude.toFixed(2)}, ${pos.coords.longitude.toFixed(2)}`;
                        setPincodeStatusMessage(
                          `GPS locked (${coords}). Select a saved address or enter your 6-digit PIN above.`
                        );
                        showToast('Current GPS coordinates detected', {
                          title: 'Location Detected',
                          variant: 'info',
                        });
                      },
                      () => {
                        setIsDetectingLocation(false);
                        showToast('Location permission denied — select a saved address or enter PIN', 'warning');
                      },
                      { timeout: 6000 }
                    );
                  }}
                  className="w-full py-1.5 px-2.5 rounded-xl bg-white dark:bg-[#211A17] border border-orange-200/70 dark:border-stone-700 text-[11px] font-bold text-[#FF5722] flex items-center justify-center gap-1.5 hover:bg-orange-50/70 dark:hover:bg-stone-800 transition-colors cursor-pointer"
                >
                  <LocateFixed className="w-3.5 h-3.5" />
                  <span>{isDetectingLocation ? 'Detecting location...' : 'Use my current location'}</span>
                </button>
              </div>

              <div className="space-y-2.5">
                <p className="text-[11px] font-black text-slate-600 dark:text-slate-400 uppercase tracking-wider">
                  Saved Addresses ({savedAddresses.length})
                </p>
                {savedAddresses.map((addr) => {
                  const isActive = addr.id === activeAddressObj?.id;
                  return (
                    <div
                      key={addr.id}
                      onClick={() => {
                        haptics.selection();
                        setActiveAddressId(addr.id);
                        setQuickPincodeInput(addr.pincode);
                        showToast(`Delivering to ${addr.label} (${addr.city} - ${addr.pincode})`, {
                          title: 'Delivery Address Changed',
                          variant: 'success',
                        });
                      }}
                      className={`p-3 rounded-2xl border cursor-pointer transition-all ${
                        isActive
                          ? 'bg-[#FFF4E8] dark:bg-orange-950/30 border-[#FF5722]'
                          : 'bg-stone-50 dark:bg-[#181311] border-orange-100/70 dark:border-stone-800'
                      }`}
                    >
                      <div className="flex items-center justify-between gap-2">
                        <div className="flex items-center gap-1.5">
                          <span className="px-2 py-0.5 text-[10px] font-black rounded bg-[#FF5722] text-white">
                            {addr.label}
                          </span>
                          <span className="text-xs font-bold text-slate-900 dark:text-white">
                            {addr.fullName}
                          </span>
                        </div>
                        <div className="flex items-center gap-2">
                          {isActive && (
                            <span className="text-[10px] font-bold text-[#E64A19] dark:text-orange-300">
                              Selected ✓
                            </span>
                          )}
                          <button
                            type="button"
                            onClick={(e) => {
                              e.stopPropagation();
                              haptics.light();
                              setEditingAddressId(addr.id);
                              setAddressDraft({
                                label: addr.label,
                                fullName: addr.fullName,
                                phone: addr.phone,
                                addressLine: addr.addressLine,
                                city: addr.city,
                                pincode: addr.pincode,
                              });
                            }}
                            className="text-[11px] font-bold text-[#FF5722] hover:underline cursor-pointer"
                          >
                            Edit
                          </button>
                          {savedAddresses.length > 1 && (
                            <button
                              type="button"
                              onClick={(e) => {
                                e.stopPropagation();
                                haptics.warning();
                                const remaining = savedAddresses.filter((a) => a.id !== addr.id);
                                setSavedAddresses(remaining);
                                if (activeAddressId === addr.id && remaining[0]) {
                                  setActiveAddressId(remaining[0].id);
                                }
                                showToast('Address removed', 'warning');
                              }}
                              className="text-[11px] font-bold text-rose-500 hover:underline cursor-pointer"
                            >
                              Delete
                            </button>
                          )}
                        </div>
                      </div>
                      <p className="text-xs text-slate-600 dark:text-slate-300 mt-1.5">
                        {addr.addressLine}, {addr.city} - {addr.pincode}
                      </p>
                      <p className="text-[10px] text-slate-500 dark:text-slate-400 mt-0.5">
                        {addr.phone}
                      </p>
                    </div>
                  );
                })}
              </div>

              <form
                onSubmit={(e) => {
                  e.preventDefault();
                  if (!addressDraft.fullName.trim() || !addressDraft.addressLine.trim() || !addressDraft.pincode.trim()) return;
                  haptics.saved();
                  if (editingAddressId) {
                    setSavedAddresses((prev) =>
                      prev.map((a) => (a.id === editingAddressId ? { id: a.id, ...addressDraft } : a))
                    );
                    setActiveAddressId(editingAddressId);
                    setEditingAddressId(null);
                    showToast(`Address updated (${addressDraft.city} - ${addressDraft.pincode})`, {
                      title: 'Address Saved',
                      variant: 'success',
                    });
                  } else {
                    const newId = `addr-${Date.now()}`;
                    setSavedAddresses((prev) => [...prev, { id: newId, ...addressDraft }]);
                    setActiveAddressId(newId);
                    showToast(`New address added (${addressDraft.city} - ${addressDraft.pincode})`, {
                      title: 'Address Saved',
                      variant: 'success',
                    });
                  }
                  setQuickPincodeInput(addressDraft.pincode);
                  setAddressDraft({
                    label: 'Home',
                    fullName: userProfile.name,
                    phone: userProfile.phone,
                    addressLine: '',
                    city: '',
                    pincode: '',
                  });
                }}
                className="p-3 rounded-2xl bg-[#FFF4E8]/60 dark:bg-[#181311] border border-orange-200/70 dark:border-stone-800 space-y-2"
              >
                <p className="text-xs font-black text-slate-900 dark:text-white">
                  {editingAddressId ? 'Edit Address' : '+ Add New Address'}
                </p>
                <div className="grid grid-cols-2 gap-2">
                  <input
                    type="text"
                    placeholder="Label (Home / Work)"
                    value={addressDraft.label}
                    onChange={(e) => setAddressDraft({ ...addressDraft, label: e.target.value })}
                    className="text-xs px-3 py-2 rounded-xl bg-white dark:bg-[#211A17] border border-orange-200/70 dark:border-stone-700 outline-none focus:border-[#FF5722]"
                  />
                  <input
                    type="text"
                    required
                    placeholder="Full Name"
                    value={addressDraft.fullName}
                    onChange={(e) => setAddressDraft({ ...addressDraft, fullName: e.target.value })}
                    className="text-xs px-3 py-2 rounded-xl bg-white dark:bg-[#211A17] border border-orange-200/70 dark:border-stone-700 outline-none focus:border-[#FF5722]"
                  />
                </div>
                <input
                  type="text"
                  required
                  placeholder="House / Street / Area"
                  value={addressDraft.addressLine}
                  onChange={(e) => setAddressDraft({ ...addressDraft, addressLine: e.target.value })}
                  className="w-full text-xs px-3 py-2 rounded-xl bg-white dark:bg-[#211A17] border border-orange-200/70 dark:border-stone-700 outline-none focus:border-[#FF5722]"
                />
                <div className="grid grid-cols-3 gap-2">
                  <input
                    type="text"
                    required
                    placeholder="City"
                    value={addressDraft.city}
                    onChange={(e) => setAddressDraft({ ...addressDraft, city: e.target.value })}
                    className="text-xs px-3 py-2 rounded-xl bg-white dark:bg-[#211A17] border border-orange-200/70 dark:border-stone-700 outline-none focus:border-[#FF5722]"
                  />
                  <input
                    type="text"
                    required
                    maxLength={6}
                    placeholder="Pincode"
                    value={addressDraft.pincode}
                    onChange={(e) => setAddressDraft({ ...addressDraft, pincode: e.target.value.replace(/\D/g, '') })}
                    className="text-xs px-3 py-2 rounded-xl bg-white dark:bg-[#211A17] border border-orange-200/70 dark:border-stone-700 outline-none focus:border-[#FF5722]"
                  />
                  <input
                    type="tel"
                    placeholder="Phone"
                    value={addressDraft.phone}
                    onChange={(e) => setAddressDraft({ ...addressDraft, phone: e.target.value })}
                    className="text-xs px-3 py-2 rounded-xl bg-white dark:bg-[#211A17] border border-orange-200/70 dark:border-stone-700 outline-none focus:border-[#FF5722]"
                  />
                </div>
                <button
                  type="submit"
                  className="w-full py-2 bg-[#FF5722] hover:bg-[#E64A19] text-white text-xs font-bold rounded-xl cursor-pointer"
                >
                  {editingAddressId ? 'Update Address' : 'Save & Use Address'}
                </button>
              </form>
            </div>
          </div>
        )}

        {/* 18B. PRICE RANGE FILTER MODAL */}
        {isPriceFilterModalOpen && (
          <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-0 sm:p-4 bg-black/60 backdrop-blur-xs">
            <div className="w-full max-w-md bg-white dark:bg-[#211A17] rounded-t-3xl sm:rounded-3xl p-5 border border-orange-100 dark:border-stone-800 shadow-2xl space-y-4">
              <div className="flex items-center justify-between border-b border-orange-100/70 dark:border-stone-800 pb-3">
                <h3 className="font-display font-black text-sm text-slate-900 dark:text-white flex items-center gap-2">
                  <SlidersHorizontal className="w-4 h-4 text-[#FF5722]" />
                  <span>Price Range Filter</span>
                </h3>
                <button
                  type="button"
                  onClick={() => setIsPriceFilterModalOpen(false)}
                  className="p-1 text-slate-400 hover:text-slate-700 dark:hover:text-white rounded-lg cursor-pointer"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              <div className="space-y-3">
                <p className="text-[11px] font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                  Quick Price Ranges
                </p>
                <div className="flex flex-wrap gap-2">
                  {PRICE_FILTER_PRESETS.map((preset) => {
                    const isMatch =
                      (priceMinDraft ? Number(priceMinDraft) : null) === preset.min &&
                      (priceMaxDraft ? Number(priceMaxDraft) : null) === preset.max;
                    return (
                      <button
                        key={preset.label}
                        type="button"
                        onClick={() => {
                          haptics.selection();
                          setPriceMinDraft(preset.min !== null ? String(preset.min) : '');
                          setPriceMaxDraft(preset.max !== null ? String(preset.max) : '');
                        }}
                        className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-colors cursor-pointer ${
                          isMatch
                            ? 'bg-[#FF5722] text-white'
                            : 'bg-[#FFF4E8] dark:bg-[#181311] text-slate-700 dark:text-slate-200 border border-orange-200/70 dark:border-stone-700'
                        }`}
                      >
                        {preset.label}
                      </button>
                    );
                  })}
                </div>

                <div className="grid grid-cols-2 gap-3 pt-1">
                  <div>
                    <label className="text-[11px] font-bold text-slate-600 dark:text-slate-400 block mb-1">
                      Min Price (₹)
                    </label>
                    <input
                      type="number"
                      min={0}
                      placeholder="0"
                      value={priceMinDraft}
                      onChange={(e) => setPriceMinDraft(e.target.value)}
                      className="w-full text-xs font-bold tabular-nums px-3 py-2 rounded-xl bg-[#FFF4E8]/50 dark:bg-[#181311] border border-orange-200/70 dark:border-stone-700 text-slate-900 dark:text-white outline-none focus:border-[#FF5722]"
                    />
                  </div>
                  <div>
                    <label className="text-[11px] font-bold text-slate-600 dark:text-slate-400 block mb-1">
                      Max Price (₹)
                    </label>
                    <input
                      type="number"
                      min={0}
                      placeholder="Any"
                      value={priceMaxDraft}
                      onChange={(e) => setPriceMaxDraft(e.target.value)}
                      className="w-full text-xs font-bold tabular-nums px-3 py-2 rounded-xl bg-[#FFF4E8]/50 dark:bg-[#181311] border border-orange-200/70 dark:border-stone-700 text-slate-900 dark:text-white outline-none focus:border-[#FF5722]"
                    />
                  </div>
                </div>

                <div>
                  <div className="flex items-center justify-between text-[11px] font-semibold text-slate-600 dark:text-slate-400 mb-1">
                    <span>Max Price Cap Slider</span>
                    <span className="font-black text-[#FF5722] tabular-nums">
                      {priceMaxDraft ? `Up to ₹${Number(priceMaxDraft).toLocaleString('en-IN')}` : 'No Limit'}
                    </span>
                  </div>
                  <input
                    type="range"
                    min={0}
                    max={25000}
                    step={250}
                    value={Math.min(25000, Number(priceMaxDraft) || 25000)}
                    onChange={(e) => {
                      const val = Number(e.target.value);
                      setPriceMaxDraft(val >= 25000 ? '' : String(val));
                    }}
                    className="w-full accent-[#FF5722] cursor-pointer"
                  />
                </div>
              </div>

              <div className="flex gap-2 pt-1">
                <button
                  type="button"
                  onClick={() => {
                    haptics.light();
                    setPriceMinDraft('');
                    setPriceMaxDraft('');
                    setMinPriceFilter(null);
                    setMaxPriceFilter(null);
                    setIsPriceFilterModalOpen(false);
                    showToast('Price filter cleared', 'info');
                  }}
                  className="flex-1 py-2.5 text-xs font-bold bg-stone-100 dark:bg-stone-800 text-slate-700 dark:text-slate-300 rounded-xl cursor-pointer"
                >
                  Reset
                </button>
                <button
                  type="button"
                  onClick={() => {
                    const parsedMin = priceMinDraft.trim() !== '' ? Math.max(0, Number(priceMinDraft)) : null;
                    const parsedMax = priceMaxDraft.trim() !== '' ? Math.max(0, Number(priceMaxDraft)) : null;
                    if (parsedMin !== null && parsedMax !== null && parsedMin > parsedMax) {
                      showToast('Min price cannot exceed Max price', 'error');
                      return;
                    }
                    haptics.saved();
                    setMinPriceFilter(parsedMin);
                    setMaxPriceFilter(parsedMax);
                    setIsPriceFilterModalOpen(false);
                    if (parsedMin === null && parsedMax === null) {
                      showToast('Showing all prices', 'info');
                    } else {
                      const label =
                        parsedMin !== null && parsedMax !== null
                          ? `₹${parsedMin.toLocaleString('en-IN')} – ₹${parsedMax.toLocaleString('en-IN')}`
                          : parsedMax !== null
                          ? `Under ₹${parsedMax.toLocaleString('en-IN')}`
                          : `₹${parsedMin!.toLocaleString('en-IN')}+`;
                      showToast(`Filtered products: ${label}`, {
                        title: 'Price Range Applied',
                        variant: 'success',
                      });
                    }
                  }}
                  className="flex-1 py-2.5 text-xs font-bold bg-[#FF5722] hover:bg-[#E64A19] text-white rounded-xl shadow-md cursor-pointer"
                >
                  Apply Filter
                </button>
              </div>
            </div>
          </div>
        )}

        {/* 19. WISHLIST MODAL */}
        {isWishlistModalOpen && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs">
            <div className="w-full max-w-md max-h-[85vh] overflow-y-auto bg-white dark:bg-[#211A17] rounded-3xl p-5 border border-orange-100 dark:border-stone-800 shadow-2xl space-y-3.5">
              <div className="flex items-center justify-between border-b border-orange-100/70 dark:border-stone-800 pb-3">
                <h3 className="font-display font-black text-sm text-slate-900 dark:text-white flex items-center gap-2">
                  <Heart className="w-4 h-4 text-rose-500 fill-rose-500" />
                  <span>My Wishlist ({wishlist.length})</span>
                </h3>
                <button
                  type="button"
                  onClick={() => setIsWishlistModalOpen(false)}
                  className="p-1 text-slate-400 hover:text-slate-700 dark:hover:text-white rounded-lg cursor-pointer"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              {allProducts.filter((p) => wishlist.includes(p.id)).length === 0 ? (
                <div className="py-10 text-center space-y-2">
                  <Heart className="w-10 h-10 text-slate-300 mx-auto" />
                  <p className="text-xs font-bold text-slate-600 dark:text-slate-300">
                    Your wishlist is empty
                  </p>
                  <p className="text-[11px] text-slate-400">
                    Tap the heart icon on any product to save it here.
                  </p>
                </div>
              ) : (
                <div className="space-y-2.5">
                  {allProducts
                    .filter((p) => wishlist.includes(p.id))
                    .map((item) => (
                      <div
                        key={item.id}
                        className="p-2.5 rounded-2xl bg-[#FFF4E8]/40 dark:bg-[#181311] border border-orange-100 dark:border-stone-800 flex items-center gap-3"
                      >
                        <img
                          src={item.image}
                          alt={item.name}
                          onClick={() => {
                            haptics.light();
                            setIsWishlistModalOpen(false);
                            setSelectedProduct(item);
                          }}
                          className="w-14 h-14 rounded-xl object-cover shrink-0 cursor-pointer"
                        />
                        <div className="flex-1 min-w-0">
                          <h4
                            onClick={() => {
                              haptics.light();
                              setIsWishlistModalOpen(false);
                              setSelectedProduct(item);
                            }}
                            className="text-xs font-bold text-slate-900 dark:text-white truncate cursor-pointer"
                          >
                            {item.name}
                          </h4>
                          <p className="text-xs font-black text-[#FF5722] mt-0.5">
                            ₹{item.price.toLocaleString('en-IN')}
                          </p>
                          <div className="flex items-center gap-2 mt-1.5">
                            <button
                              type="button"
                              onClick={() => addToCart(item)}
                              className="px-2.5 py-1 bg-[#FF5722] hover:bg-[#E64A19] text-white text-[10px] font-bold rounded-lg cursor-pointer"
                            >
                              Add to Cart
                            </button>
                            <button
                              type="button"
                              onClick={() => toggleWishlist(item.id, item.name)}
                              className="text-[10px] font-bold text-rose-500 hover:underline cursor-pointer"
                            >
                              Remove
                            </button>
                          </div>
                        </div>
                      </div>
                    ))}
                </div>
              )}
            </div>
          </div>
        )}

        {/* 20. COUPONS MODAL */}
        {isCouponsModalOpen && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs">
            <div className="w-full max-w-sm bg-white dark:bg-[#211A17] rounded-3xl p-5 border border-orange-100 dark:border-stone-800 shadow-2xl space-y-3.5">
              <div className="flex items-center justify-between border-b border-orange-100/70 dark:border-stone-800 pb-3">
                <h3 className="font-display font-black text-sm text-slate-900 dark:text-white flex items-center gap-2">
                  <Tag className="w-4 h-4 text-[#FF5722]" />
                  <span>Available Coupons</span>
                </h3>
                <button
                  type="button"
                  onClick={() => setIsCouponsModalOpen(false)}
                  className="p-1 text-slate-400 hover:text-slate-700 dark:hover:text-white rounded-lg cursor-pointer"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              {(
                [
                  { code: 'AARVO200', desc: 'Flat ₹200 off on orders above ₹999' },
                  { code: 'WELCOME100', desc: 'Flat ₹100 off on orders above ₹499' },
                ] as const
              ).map((c) => {
                const isApplied = appliedCouponCode === c.code;
                return (
                  <div
                    key={c.code}
                    className="p-3 rounded-2xl bg-[#FFF4E8] dark:bg-[#181311] border border-orange-200/80 dark:border-stone-800 space-y-2"
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-mono font-black text-xs text-[#E64A19] dark:text-orange-300 bg-white dark:bg-[#211A17] px-2.5 py-0.5 rounded border border-orange-200 dark:border-stone-700">
                        {c.code}
                      </span>
                      <button
                        type="button"
                        onClick={() => {
                          haptics.saved();
                          navigator.clipboard?.writeText(c.code).catch(() => {});
                          showToast(`Copied ${c.code}`);
                        }}
                        className="text-[10px] font-bold text-slate-500 hover:text-[#FF5722] flex items-center gap-1 cursor-pointer"
                      >
                        <Copy className="w-3 h-3" />
                        <span>Copy</span>
                      </button>
                    </div>
                    <p className="text-[11px] text-slate-600 dark:text-slate-300">{c.desc}</p>
                    <button
                      type="button"
                      onClick={() => {
                        if (isApplied) {
                          haptics.warning();
                        } else {
                          haptics.saved();
                        }
                        setAppliedCouponCode(isApplied ? null : c.code);
                        showToast(isApplied ? `Removed ${c.code}` : `Applied ${c.code} to your Cart!`);
                      }}
                      className={`w-full py-1.5 text-xs font-bold rounded-xl cursor-pointer ${
                        isApplied
                          ? 'bg-emerald-600 text-white'
                          : 'bg-[#FF5722] hover:bg-[#E64A19] text-white'
                      }`}
                    >
                      {isApplied ? 'Applied to Cart ✓ (Tap to Remove)' : 'Apply to Cart'}
                    </button>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* 21. HELP CENTER MODAL */}
        {isHelpModalOpen && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs">
            <div className="w-full max-w-md max-h-[88vh] overflow-y-auto bg-white dark:bg-[#211A17] rounded-3xl p-5 border border-orange-100 dark:border-stone-800 shadow-2xl space-y-4">
              <div className="flex items-center justify-between border-b border-orange-100/70 dark:border-stone-800 pb-3">
                <h3 className="font-display font-black text-sm text-slate-900 dark:text-white flex items-center gap-2">
                  <HelpCircle className="w-4 h-4 text-[#FF5722]" />
                  <span>AARVO Help Center</span>
                </h3>
                <button
                  type="button"
                  onClick={() => setIsHelpModalOpen(false)}
                  className="p-1 text-slate-400 hover:text-slate-700 dark:hover:text-white rounded-lg cursor-pointer"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              <button
                type="button"
                onClick={() => {
                  haptics.selection();
                  setIsHelpModalOpen(false);
                  setIsOrdersViewOpen(true);
                }}
                className="w-full p-3 rounded-2xl bg-[#FFF4E8] dark:bg-orange-950/30 border border-orange-200/80 dark:border-orange-900/50 flex items-center justify-between text-left cursor-pointer"
              >
                <div className="flex items-center gap-2.5">
                  <Package className="w-4 h-4 text-[#FF5722]" />
                  <div>
                    <p className="text-xs font-bold text-slate-900 dark:text-white">
                      Need help with a recent order?
                    </p>
                    <p className="text-[10px] text-slate-500 dark:text-slate-400">
                      Track shipments or manage returns ({orders.length} order{orders.length !== 1 ? 's' : ''})
                    </p>
                  </div>
                </div>
                <ChevronRight className="w-4 h-4 text-[#FF5722]" />
              </button>

              <div className="space-y-1.5 text-xs">
                <p className="font-black text-slate-900 dark:text-white text-[11px] uppercase tracking-wider">
                  Quick FAQs
                </p>
                <div className="p-2.5 rounded-xl bg-stone-50 dark:bg-[#181311] border border-orange-100/60 dark:border-stone-800">
                  <p className="font-bold text-slate-800 dark:text-slate-200">How do returns work?</p>
                  <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">
                    Eligible products offer 7-day easy pickup directly from your saved address.
                  </p>
                </div>
                <div className="p-2.5 rounded-xl bg-stone-50 dark:bg-[#181311] border border-orange-100/60 dark:border-stone-800">
                  <p className="font-bold text-slate-800 dark:text-slate-200">What payment modes are accepted?</p>
                  <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">
                    UPI (GPay, PhonePe, Paytm), Cards, and Cash On Delivery (COD).
                  </p>
                </div>
              </div>

              <form
                onSubmit={(e) => {
                  e.preventDefault();
                  if (!ticketMessage.trim()) return;
                  haptics.saved();
                  const newTicket = {
                    id: `TKT-${Math.floor(10000 + Math.random() * 90000)}`,
                    subject: ticketSubject,
                    message: ticketMessage.trim(),
                    status: 'Open',
                    createdAt: 'Just now',
                  };
                  setSupportTickets((prev) => [newTicket, ...prev]);
                  setTicketMessage('');
                  showToast(`Support ticket ${newTicket.id} submitted!`);
                }}
                className="space-y-2 pt-2 border-t border-orange-100/70 dark:border-stone-800"
              >
                <p className="text-xs font-black text-slate-900 dark:text-white flex items-center gap-1.5">
                  <MessageSquare className="w-3.5 h-3.5 text-[#FF5722]" />
                  <span>Raise a Support Ticket</span>
                </p>
                <select
                  value={ticketSubject}
                  onChange={(e) => setTicketSubject(e.target.value)}
                  className="w-full text-xs px-3 py-2 rounded-xl bg-[#FFF4E8]/60 dark:bg-[#181311] border border-orange-200/70 dark:border-stone-700 outline-none focus:border-[#FF5722]"
                >
                  <option value="Order & Delivery Inquiry">Order &amp; Delivery Inquiry</option>
                  <option value="Refund & Return Request">Refund &amp; Return Request</option>
                  <option value="Payment / UPI Issue">Payment / UPI Issue</option>
                  <option value="Seller / Product Feedback">Seller / Product Feedback</option>
                </select>
                <textarea
                  rows={2}
                  required
                  value={ticketMessage}
                  onChange={(e) => setTicketMessage(e.target.value)}
                  placeholder="Describe your issue..."
                  className="w-full text-xs px-3 py-2 rounded-xl bg-[#FFF4E8]/60 dark:bg-[#181311] border border-orange-200/70 dark:border-stone-700 outline-none focus:border-[#FF5722]"
                />
                <button
                  type="submit"
                  className="w-full py-2.5 bg-[#FF5722] hover:bg-[#E64A19] text-white text-xs font-bold rounded-xl shadow-sm cursor-pointer"
                >
                  Submit Ticket
                </button>
              </form>

              {supportTickets.length > 0 && (
                <div className="space-y-1.5">
                  <p className="text-[11px] font-black text-slate-700 dark:text-slate-300 uppercase">
                    Your Support Tickets
                  </p>
                  {supportTickets.map((t) => (
                    <div
                      key={t.id}
                      className="p-2.5 rounded-xl bg-[#FFF4E8]/40 dark:bg-[#181311] border border-orange-100 dark:border-stone-800 text-xs"
                    >
                      <div className="flex items-center justify-between">
                        <span className="font-mono font-bold text-[#FF5722]">{t.id}</span>
                        <span className="text-[10px] font-bold text-emerald-600">{t.status}</span>
                      </div>
                      <p className="font-bold text-slate-800 dark:text-slate-200 mt-0.5">{t.subject}</p>
                      <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">{t.message}</p>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}

        {/* 22. PRIVACY POLICY, TERMS & RETURN POLICY MODAL */}
        {isPolicyModalOpen && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs">
            <div className="w-full max-w-md max-h-[85vh] overflow-y-auto bg-white dark:bg-[#211A17] rounded-3xl p-5 border border-orange-100 dark:border-stone-800 shadow-2xl space-y-3.5 text-xs">
              <div className="flex items-center justify-between border-b border-orange-100/70 dark:border-stone-800 pb-3">
                <h3 className="font-display font-black text-sm text-slate-900 dark:text-white flex items-center gap-2">
                  <ShieldCheck className="w-4 h-4 text-[#FF5722]" />
                  <span>Privacy, Terms &amp; Return Policy</span>
                </h3>
                <button
                  type="button"
                  onClick={() => setIsPolicyModalOpen(false)}
                  className="p-1 text-slate-400 hover:text-slate-700 dark:hover:text-white rounded-lg cursor-pointer"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>
              <div className="space-y-3 text-slate-600 dark:text-slate-300 leading-relaxed">
                <div className="p-3 rounded-2xl bg-[#FFF4E8]/50 dark:bg-[#181311] border border-orange-100 dark:border-stone-800">
                  <p className="font-black text-slate-900 dark:text-white mb-1">Privacy &amp; Data Protection</p>
                  <p>AARVO collects only the mobile number, delivery address, and order details necessary to fulfill purchases and courier deliveries. We never sell personal data to third parties. You can permanently delete your account anytime from Account Settings.</p>
                </div>
                <div className="p-3 rounded-2xl bg-[#FFF4E8]/50 dark:bg-[#181311] border border-orange-100 dark:border-stone-800">
                  <p className="font-black text-slate-900 dark:text-white mb-1">Shipping &amp; Delivery Terms</p>
                  <p>Estimated delivery dates shown on product pages are calculated live based on product category, parcel size/weight, and courier serviceability between pickup &amp; destination pincodes.</p>
                </div>
                <div className="p-3 rounded-2xl bg-[#FFF4E8]/50 dark:bg-[#181311] border border-orange-100 dark:border-stone-800">
                  <p className="font-black text-slate-900 dark:text-white mb-1">7-Day Replacement &amp; Refund Policy</p>
                  <p>Damaged, defective, or wrong items can be requested for return within 7 days of delivery from My Orders. Approved refunds are processed back to your original payment method within 5–7 business days.</p>
                </div>
                <div className="flex flex-wrap gap-2 pt-1">
                  {(() => {
                    const apiBase = (
                      (import.meta as unknown as { env?: Record<string, string> }).env
                        ?.VITE_API_BASE_URL || 'https://aarvo-api.onrender.com'
                    ).replace(/\/$/, '');
                    return [
                      { label: 'Privacy Policy Web Page', href: `${apiBase}/privacy-policy` },
                      { label: 'Terms of Use', href: `${apiBase}/terms` },
                      { label: 'Refund Policy', href: `${apiBase}/refund-policy` },
                      { label: 'Account Deletion Info', href: `${apiBase}/delete-account` },
                    ].map((item) => (
                      <a
                        key={item.href}
                        href={item.href}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="px-2.5 py-1 rounded-lg bg-orange-50 dark:bg-stone-800 border border-orange-200 dark:border-stone-700 text-[11px] font-bold text-[#FF5722] hover:underline"
                      >
                        {item.label} ↗
                      </a>
                    ));
                  })()}
                </div>
              </div>
              <button
                type="button"
                onClick={() => setIsPolicyModalOpen(false)}
                className="w-full py-2.5 bg-[#FF5722] hover:bg-[#E64A19] text-white text-xs font-bold rounded-xl cursor-pointer"
              >
                Close
              </button>
            </div>
          </div>
        )}

        {/* 23. DELETE ACCOUNT CONFIRMATION MODAL (PLAY STORE COMPLIANCE) */}
        {isDeleteAccountOpen && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs">
            <div className="w-full max-w-sm bg-white dark:bg-[#211A17] rounded-3xl p-5 border border-rose-200 dark:border-rose-900/60 shadow-2xl space-y-3.5 text-xs">
              <div className="flex items-center justify-between border-b border-rose-100 dark:border-stone-800 pb-3">
                <h3 className="font-display font-black text-sm text-rose-600 dark:text-rose-400 flex items-center gap-2">
                  <Trash2 className="w-4 h-4" />
                  <span>Delete AARVO Account</span>
                </h3>
                <button
                  type="button"
                  disabled={isDeletingAccount}
                  onClick={() => setIsDeleteAccountOpen(false)}
                  className="p-1 text-slate-400 hover:text-slate-700 dark:hover:text-white rounded-lg cursor-pointer"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>
              <p className="text-slate-600 dark:text-slate-300 leading-relaxed">
                This will permanently deactivate your account, clear your saved cart and delivery addresses, and sign you out. Completed order invoices remain archived for statutory GST compliance.
              </p>
              <div className="space-y-1.5">
                <label className="text-[11px] font-bold text-slate-700 dark:text-slate-300 block">
                  Type <strong>DELETE</strong> to confirm:
                </label>
                <input
                  type="text"
                  value={deleteConfirmText}
                  onChange={(e) => setDeleteConfirmText(e.target.value)}
                  placeholder="DELETE"
                  className="w-full px-3 py-2 rounded-xl bg-rose-50/40 dark:bg-[#181311] border border-rose-200 dark:border-stone-700 text-slate-900 dark:text-white text-xs font-bold outline-none focus:border-rose-500"
                />
              </div>
              {deleteAccountError && (
                <p className="text-[11px] font-semibold text-rose-600 dark:text-rose-400">
                  {deleteAccountError}
                </p>
              )}
              <div className="flex items-center gap-2 pt-1">
                <button
                  type="button"
                  disabled={isDeletingAccount}
                  onClick={() => setIsDeleteAccountOpen(false)}
                  className="flex-1 py-2.5 rounded-xl bg-stone-100 dark:bg-stone-800 text-slate-700 dark:text-slate-300 font-bold cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  disabled={isDeletingAccount || deleteConfirmText.trim().toUpperCase() !== 'DELETE'}
                  onClick={async () => {
                    setIsDeletingAccount(true);
                    setDeleteAccountError(null);
                    try {
                      const token =
                        localStorage.getItem('aarvo_auth_token') ||
                        localStorage.getItem('aarvo_token');
                      if (token) {
                        const apiBase = (
                          (import.meta as unknown as { env?: Record<string, string> }).env
                            ?.VITE_API_BASE_URL || 'https://aarvo-api.onrender.com'
                        ).replace(/\/$/, '');
                        const resp = await fetch(`${apiBase}/v1/account`, {
                          method: 'DELETE',
                          headers: { Authorization: `Bearer ${token}` },
                        });
                        if (!resp.ok && resp.status !== 401) {
                          const errJson = await resp.json().catch(() => ({}));
                          throw new Error(
                            errJson.message || errJson.error || 'Unable to delete account'
                          );
                        }
                      }
                      [
                        'aarvo_profile',
                        'aarvo_addresses',
                        'aarvo_active_address_id',
                        'aarvo_cart',
                        'aarvo_wishlist',
                        'aarvo_orders',
                        'aarvo_support_tickets',
                        'aarvo_auth_token',
                        'aarvo_token',
                      ].forEach((k) => localStorage.removeItem(k));
                      setIsDeleteAccountOpen(false);
                      showToast('Your AARVO account has been deleted.', 'info');
                      onLogout?.();
                    } catch (err) {
                      setDeleteAccountError(
                        err instanceof Error ? err.message : 'Unable to delete account'
                      );
                    } finally {
                      setIsDeletingAccount(false);
                    }
                  }}
                  className="flex-1 py-2.5 rounded-xl bg-rose-600 hover:bg-rose-700 disabled:opacity-50 text-white font-bold cursor-pointer"
                >
                  {isDeletingAccount ? 'Deleting…' : 'Confirm Delete'}
                </button>
              </div>
            </div>
          </div>
        )}

      </div>
    </div>
  );
};
