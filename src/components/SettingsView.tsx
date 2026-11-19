import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  ArrowLeft,
  Bell,
  Check,
  Edit2,
  LocateFixed,
  LogOut,
  Mail,
  MapPin,
  Moon,
  Package,
  Phone,
  Plus,
  RefreshCw,
  ShieldCheck,
  Sun,
  Trash2,
  Truck,
  User,
} from 'lucide-react';
import { Orders } from './Orders';
import { Order, OrderItem } from './OrdersSection';
import { haptics } from '../utils/haptics';
import {
  getStoredAuthToken,
  inspectAuthTokenSafely,
  resolveApiBaseUrl,
} from '../utils/authSession';
import {
  checkShiprocketServiceability,
  ShiprocketServiceabilityResult,
} from '../utils/shiprocketServiceability';

export type SettingsSection = 'contact' | 'addresses' | 'orders';

export interface UserProfile {
  name: string;
  phone: string;
  email?: string;
}

export interface SavedAddress {
  id: string;
  label: string;
  fullName: string;
  phone: string;
  addressLine: string;
  line2?: string;
  city: string;
  state?: string;
  pincode: string;
  isDefault?: boolean;
}

interface SettingsPreferences {
  orderAlerts: boolean;
  deliveryAlerts: boolean;
}

interface SettingsViewProps {
  userProfile: UserProfile;
  onUpdateProfile: (next: UserProfile) => void;
  savedAddresses: SavedAddress[];
  activeAddressId: string;
  onUpdateAddresses: (nextAddresses: SavedAddress[], nextActiveId?: string) => void;
  onSelectActiveAddress: (addressId: string) => void;
  orders: Order[];
  onOrdersLoaded?: (orders: Order[]) => void;
  onReorderItem?: (item: OrderItem) => void;
  onReorderAll?: (items: OrderItem[]) => void;
  onNavigateToCart?: () => void;
  onOpenSupport?: () => void;
  onShowToast: (
    message: string,
    variantOrOptions?:
      | 'info'
      | 'success'
      | 'warning'
      | 'error'
      | { title?: string; variant?: 'info' | 'success' | 'warning' | 'error' }
  ) => void;
  onBack: () => void;
  theme?: 'light' | 'dark';
  onToggleTheme?: () => void;
  onOpenDeleteAccount?: () => void;
  onLogout?: () => void;
  initialSection?: SettingsSection | null;
}

const SECTION_STORAGE_KEY = 'aarvo_settings_section';
const PREFS_STORAGE_KEY = 'aarvo_settings_prefs';

function readStoredSection(fallback: SettingsSection = 'contact'): SettingsSection {
  try {
    const raw = window.localStorage.getItem(SECTION_STORAGE_KEY);
    if (raw === 'contact' || raw === 'addresses' || raw === 'orders') {
      return raw;
    }
  } catch {
    // ignore
  }
  return fallback;
}

function readStoredPrefs(): SettingsPreferences {
  try {
    const raw = window.localStorage.getItem(PREFS_STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw) as Partial<SettingsPreferences>;
      return {
        orderAlerts: parsed.orderAlerts !== false,
        deliveryAlerts: parsed.deliveryAlerts !== false,
      };
    }
  } catch {
    // ignore
  }
  return { orderAlerts: true, deliveryAlerts: true };
}

function normalizeServerAddress(row: Record<string, unknown>): SavedAddress {
  return {
    id: String(row.id ?? `addr-${Date.now()}`),
    label: String(row.label || 'Home'),
    fullName: String(row.full_name ?? row.fullName ?? ''),
    phone: String(row.phone || ''),
    addressLine: String(row.line1 ?? row.addressLine ?? ''),
    line2: row.line2 ? String(row.line2) : undefined,
    city: String(row.city || ''),
    state: row.state ? String(row.state) : '',
    pincode: String(row.postal_code ?? row.postalCode ?? row.pincode ?? ''),
    isDefault: Boolean(row.is_default ?? row.isDefault),
  };
}

function isUuidLike(id: string): boolean {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(id);
}

export const SettingsView: React.FC<SettingsViewProps> = ({
  userProfile,
  onUpdateProfile,
  savedAddresses,
  activeAddressId,
  onUpdateAddresses,
  onSelectActiveAddress,
  orders,
  onOrdersLoaded,
  onReorderItem,
  onReorderAll,
  onNavigateToCart,
  onOpenSupport,
  onShowToast,
  onBack,
  theme = 'light',
  onToggleTheme,
  onOpenDeleteAccount,
  onLogout,
  initialSection,
}) => {
  const apiBaseUrl = useMemo(() => resolveApiBaseUrl(), []);
  const [authToken, setAuthToken] = useState<string | null>(() => getStoredAuthToken());
  const sessionInfo = useMemo(() => inspectAuthTokenSafely(authToken), [authToken]);

  const [activeSection, setActiveSection] = useState<SettingsSection>(() =>
    initialSection || readStoredSection('contact')
  );

  useEffect(() => {
    if (initialSection) {
      setActiveSection(initialSection);
    }
  }, [initialSection]);

  useEffect(() => {
    try {
      window.localStorage.setItem(SECTION_STORAGE_KEY, activeSection);
    } catch {
      // ignore
    }
  }, [activeSection]);

  const [prefs, setPrefs] = useState<SettingsPreferences>(readStoredPrefs);
  useEffect(() => {
    try {
      window.localStorage.setItem(PREFS_STORAGE_KEY, JSON.stringify(prefs));
    } catch {
      // ignore
    }
  }, [prefs]);

  // Contact Info state
  const [contactName, setContactName] = useState(userProfile.name || '');
  const [contactEmail, setContactEmail] = useState(userProfile.email || '');
  const [contactPhone, setContactPhone] = useState(userProfile.phone || '');
  const [phoneVerifiedOnServer, setPhoneVerifiedOnServer] = useState<boolean>(false);
  const [isSyncing, setIsSyncing] = useState<boolean>(false);
  const [isSavingContact, setIsSavingContact] = useState<boolean>(false);
  const [contactError, setContactError] = useState<string | null>(null);
  const [syncNotice, setSyncNotice] = useState<string | null>(null);

  useEffect(() => {
    setContactName(userProfile.name || '');
    setContactEmail(userProfile.email || '');
    setContactPhone(userProfile.phone || '');
  }, [userProfile.name, userProfile.email, userProfile.phone]);

  // Address Form state
  const [isAddressFormOpen, setIsAddressFormOpen] = useState(false);
  const [editingAddressId, setEditingAddressId] = useState<string | null>(null);
  const [isSavingAddress, setIsSavingAddress] = useState(false);
  const [addressError, setAddressError] = useState<string | null>(null);
  const [pinPreview, setPinPreview] = useState<ShiprocketServiceabilityResult | null>(null);
  const [isCheckingPin, setIsCheckingPin] = useState(false);
  const [addressForm, setAddressForm] = useState({
    label: 'Home',
    fullName: userProfile.name || '',
    phone: userProfile.phone || '',
    addressLine: '',
    line2: '',
    city: '',
    state: '',
    pincode: '',
    isDefault: true,
  });

  const refreshFromServer = useCallback(
    async (manual = false) => {
      const latestToken = getStoredAuthToken();
      setAuthToken(latestToken);

      if (!latestToken) {
        setSyncNotice('Local mode · Sign in with OTP to sync profile & addresses across devices.');
        if (manual) {
          onShowToast('No active auth session — showing saved device settings', 'info');
        }
        return;
      }

      const tokenStatus = inspectAuthTokenSafely(latestToken);
      if (tokenStatus.isExpired) {
        setSyncNotice('Session expired · Re-authenticate to sync updates to your account.');
        if (manual) {
          onShowToast('Session expired — please sign in again', 'warning');
        }
        return;
      }

      setIsSyncing(true);
      setSyncNotice(null);

      try {
        const headers: Record<string, string> = {
          Accept: 'application/json',
          Authorization: `Bearer ${latestToken}`,
        };

        const [profileRes, addressesRes] = await Promise.all([
          fetch(`${apiBaseUrl}/v1/profile`, { headers }),
          fetch(`${apiBaseUrl}/v1/addresses`, { headers }),
        ]);

        if (profileRes.status === 401 || profileRes.status === 403 || addressesRes.status === 401 || addressesRes.status === 403) {
          setSyncNotice('Session authorization required · Showing saved local data.');
          if (manual) {
            onShowToast('Please sign in again to sync live account data', 'warning');
          }
          return;
        }

        if (profileRes.ok) {
          const p = (await profileRes.json()) as Record<string, unknown>;
          const nextProfile: UserProfile = {
            name: String(p.display_name || p.displayName || userProfile.name || 'AARVO Shopper'),
            phone: String(p.phone || userProfile.phone || ''),
            email: p.email ? String(p.email) : userProfile.email || '',
          };
          setPhoneVerifiedOnServer(Boolean(p.phone_verified));
          onUpdateProfile(nextProfile);
        }

        if (addressesRes.ok) {
          const rows = await addressesRes.json();
          if (Array.isArray(rows) && rows.length > 0) {
            const mapped = rows.map((r) => normalizeServerAddress(r as Record<string, unknown>));
            const defaultAddr = mapped.find((a) => a.isDefault) || mapped[0];
            onUpdateAddresses(mapped, defaultAddr?.id);
          }
        }

        if (manual) {
          onShowToast('Account settings synced securely', 'success');
        }
      } catch {
        setSyncNotice('Offline or server unreachable · Changes are stored safely on this device.');
        if (manual) {
          onShowToast('Saved settings loaded locally (network unavailable)', 'warning');
        }
      } finally {
        setIsSyncing(false);
      }
    },
    [apiBaseUrl, onShowToast, onUpdateAddresses, onUpdateProfile, userProfile.email, userProfile.name, userProfile.phone]
  );

  useEffect(() => {
    refreshFromServer(false);
    // Run once on Settings mount to sync latest profile and saved addresses using existing token
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSaveContact = async (e: React.FormEvent) => {
    e.preventDefault();
    setContactError(null);

    const trimmedName = contactName.trim();
    const trimmedEmail = contactEmail.trim().toLowerCase();
    const digitsPhone = contactPhone.replace(/\D/g, '');
    const normalizedPhone =
      digitsPhone.startsWith('91') && digitsPhone.length === 12
        ? digitsPhone.slice(2)
        : digitsPhone;

    if (!trimmedName || trimmedName.length < 2) {
      setContactError('Please enter your full name (at least 2 characters).');
      return;
    }

    if (trimmedEmail && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmedEmail)) {
      setContactError('Please enter a valid email address.');
      return;
    }

    if (normalizedPhone && !/^[6-9]\d{9}$/.test(normalizedPhone)) {
      setContactError('Enter a valid 10-digit Indian mobile number.');
      return;
    }

    setIsSavingContact(true);
    haptics.saved();

    const localNext: UserProfile = {
      name: trimmedName,
      phone: normalizedPhone || userProfile.phone,
      email: trimmedEmail,
    };

    const token = getStoredAuthToken();
    if (!token) {
      onUpdateProfile(localNext);
      setIsSavingContact(false);
      onShowToast('Contact information saved on this device', 'success');
      return;
    }

    try {
      const resp = await fetch(`${apiBaseUrl}/v1/profile`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Accept: 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          displayName: trimmedName,
          email: trimmedEmail,
        }),
      });

      if (resp.status === 401 || resp.status === 403) {
        onUpdateProfile(localNext);
        setSyncNotice('Session expired · Contact info saved locally.');
        onShowToast('Saved locally; sign in to update server profile', 'warning');
        return;
      }

      if (resp.status === 409) {
        setContactError('This email address is already linked to another AARVO account.');
        return;
      }

      if (resp.ok) {
        const updated = (await resp.json()) as Record<string, unknown>;
        onUpdateProfile({
          name: String(updated.display_name || trimmedName),
          phone: String(updated.phone || localNext.phone),
          email: updated.email ? String(updated.email) : trimmedEmail,
        });
        setPhoneVerifiedOnServer(Boolean(updated.phone_verified));
        onShowToast('Contact information updated', 'success');
      } else {
        onUpdateProfile(localNext);
        onShowToast('Contact info saved locally', 'info');
      }
    } catch {
      onUpdateProfile(localNext);
      onShowToast('Offline · Contact info saved locally', 'info');
    } finally {
      setIsSavingContact(false);
    }
  };

  const openAddAddressForm = () => {
    haptics.light();
    setEditingAddressId(null);
    setAddressError(null);
    setPinPreview(null);
    setAddressForm({
      label: 'Home',
      fullName: userProfile.name || '',
      phone: userProfile.phone || '',
      addressLine: '',
      line2: '',
      city: '',
      state: '',
      pincode: '',
      isDefault: savedAddresses.length === 0,
    });
    setIsAddressFormOpen(true);
  };

  const openEditAddressForm = (addr: SavedAddress) => {
    haptics.light();
    setEditingAddressId(addr.id);
    setAddressError(null);
    setPinPreview(null);
    setAddressForm({
      label: addr.label || 'Home',
      fullName: addr.fullName,
      phone: addr.phone,
      addressLine: addr.addressLine,
      line2: addr.line2 || '',
      city: addr.city,
      state: addr.state || '',
      pincode: addr.pincode,
      isDefault: addr.id === activeAddressId || Boolean(addr.isDefault),
    });
    setIsAddressFormOpen(true);
  };

  const handleCheckPincode = async (pin: string) => {
    const clean = pin.replace(/\D/g, '').slice(0, 6);
    if (!/^[1-9]\d{5}$/.test(clean)) {
      setAddressError('Enter a valid 6-digit Indian PIN code first.');
      return;
    }
    setAddressError(null);
    setIsCheckingPin(true);
    try {
      const res = await checkShiprocketServiceability(clean);
      setPinPreview(res);
    } finally {
      setIsCheckingPin(false);
    }
  };

  const handleSaveAddress = async (e: React.FormEvent) => {
    e.preventDefault();
    setAddressError(null);

    const fullName = addressForm.fullName.trim();
    const digitsPhone = addressForm.phone.replace(/\D/g, '');
    const normalizedPhone =
      digitsPhone.startsWith('91') && digitsPhone.length === 12
        ? digitsPhone.slice(2)
        : digitsPhone;
    const line1 = addressForm.addressLine.trim();
    const line2 = addressForm.line2.trim();
    const city = addressForm.city.trim();
    const state = addressForm.state.trim() || 'India';
    const pincode = addressForm.pincode.trim();

    if (!fullName || !line1 || !city) {
      setAddressError('Recipient name, street address, and city are required.');
      return;
    }
    if (!/^[6-9]\d{9}$/.test(normalizedPhone)) {
      setAddressError('Enter a valid 10-digit mobile number for delivery updates.');
      return;
    }
    if (!/^[1-9]\d{5}$/.test(pincode)) {
      setAddressError('Enter a valid 6-digit delivery PIN code.');
      return;
    }

    setIsSavingAddress(true);
    haptics.saved();

    const token = getStoredAuthToken();
    const localId = editingAddressId || `addr-${Date.now()}`;
    const localAddress: SavedAddress = {
      id: localId,
      label: addressForm.label.trim() || 'Home',
      fullName,
      phone: normalizedPhone,
      addressLine: line1,
      line2: line2 || undefined,
      city,
      state,
      pincode,
      isDefault: addressForm.isDefault,
    };

    const applyLocalSave = (savedItem: SavedAddress) => {
      const nextList = editingAddressId
        ? savedAddresses.map((a) =>
            a.id === editingAddressId
              ? savedItem
              : savedItem.isDefault
              ? { ...a, isDefault: false }
              : a
          )
        : [
            ...(savedItem.isDefault
              ? savedAddresses.map((a) => ({ ...a, isDefault: false }))
              : savedAddresses),
            savedItem,
          ];
      const nextActive = savedItem.isDefault ? savedItem.id : activeAddressId || savedItem.id;
      onUpdateAddresses(nextList, nextActive);
      setIsAddressFormOpen(false);
      setEditingAddressId(null);
    };

    if (!token) {
      applyLocalSave(localAddress);
      setIsSavingAddress(false);
      onShowToast(
        editingAddressId ? 'Delivery address updated' : 'New delivery address saved',
        'success'
      );
      return;
    }

    try {
      const isExistingRemote = Boolean(editingAddressId && isUuidLike(editingAddressId));
      const endpoint = isExistingRemote
        ? `${apiBaseUrl}/v1/addresses/${encodeURIComponent(editingAddressId!)}`
        : `${apiBaseUrl}/v1/addresses`;
      const method = isExistingRemote ? 'PUT' : 'POST';

      const resp = await fetch(endpoint, {
        method,
        headers: {
          'Content-Type': 'application/json',
          Accept: 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          label: localAddress.label,
          fullName: localAddress.fullName,
          phone: localAddress.phone,
          line1: localAddress.addressLine,
          line2: localAddress.line2 || '',
          city: localAddress.city,
          state: localAddress.state || 'India',
          postalCode: localAddress.pincode,
          country: 'IN',
          isDefault: Boolean(localAddress.isDefault),
        }),
      });

      if (resp.ok) {
        const created = normalizeServerAddress((await resp.json()) as Record<string, unknown>);
        applyLocalSave(created);
        onShowToast('Delivery address saved to your account', 'success');
      } else if (resp.status === 401 || resp.status === 403) {
        applyLocalSave(localAddress);
        onShowToast('Saved locally · Sign in to sync addresses', 'warning');
      } else {
        applyLocalSave(localAddress);
        onShowToast('Address saved locally', 'info');
      }
    } catch {
      applyLocalSave(localAddress);
      onShowToast('Offline · Address saved on this device', 'info');
    } finally {
      setIsSavingAddress(false);
    }
  };

  const handleSetDefaultAddress = async (addr: SavedAddress) => {
    haptics.selection();
    const nextList = savedAddresses.map((a) => ({
      ...a,
      isDefault: a.id === addr.id,
    }));
    onUpdateAddresses(nextList, addr.id);
    onSelectActiveAddress(addr.id);

    const token = getStoredAuthToken();
    if (token && isUuidLike(addr.id)) {
      try {
        await fetch(`${apiBaseUrl}/v1/addresses/${encodeURIComponent(addr.id)}/default`, {
          method: 'POST',
          headers: { Authorization: `Bearer ${token}` },
        });
      } catch {
        // Local active address already updated
      }
    }
    onShowToast(`Default delivery set to ${addr.label} (${addr.city} - ${addr.pincode})`, 'success');
  };

  const handleDeleteAddress = async (addr: SavedAddress) => {
    if (savedAddresses.length <= 1) {
      onShowToast('Keep at least one delivery address in your account', 'warning');
      return;
    }
    haptics.warning();
    const remaining = savedAddresses.filter((a) => a.id !== addr.id);
    const nextActive =
      activeAddressId === addr.id ? remaining[0]?.id || '' : activeAddressId;
    onUpdateAddresses(remaining, nextActive);

    const token = getStoredAuthToken();
    if (token && isUuidLike(addr.id)) {
      try {
        await fetch(`${apiBaseUrl}/v1/addresses/${encodeURIComponent(addr.id)}`, {
          method: 'DELETE',
          headers: { Authorization: `Bearer ${token}` },
        });
      } catch {
        // Local list already updated
      }
    }
    onShowToast('Address removed', 'info');
  };

  return (
    <section
      aria-label="User Settings"
      className="flex-1 flex flex-col bg-[#FFFDF9] dark:bg-[#120E0C] min-h-screen pb-24"
    >
      {/* Top Sticky Settings Header */}
      <header className="sticky top-0 z-30 bg-[#FFFDF9]/95 dark:bg-[#181311]/95 backdrop-blur-md border-b border-orange-100 dark:border-stone-800 px-3.5 pt-[max(0.75rem,env(safe-area-inset-top))] pb-3">
        <div className="flex items-center justify-between gap-2 mb-3">
          <div className="flex items-center gap-2.5 min-w-0">
            <button
              type="button"
              onClick={() => {
                haptics.light();
                onBack();
              }}
              aria-label="Back to Account"
              className="min-h-[44px] min-w-[44px] flex items-center justify-center rounded-xl bg-orange-50/80 hover:bg-orange-100 dark:bg-[#241B17] dark:hover:bg-[#30241E] text-slate-800 dark:text-slate-100 transition-colors cursor-pointer shrink-0"
            >
              <ArrowLeft className="w-4 h-4" />
            </button>
            <div className="min-w-0">
              <h1 className="font-display font-bold text-base sm:text-lg text-slate-900 dark:text-white truncate">
                Account Settings
              </h1>
              <p className="text-xs text-slate-500 dark:text-slate-400 truncate">
                {userProfile.name || 'AARVO Shopper'} &middot; {savedAddresses.length} addresses &middot; {orders.length} orders
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={() => refreshFromServer(true)}
            disabled={isSyncing}
            className="min-h-[40px] px-3 py-1.5 rounded-xl bg-orange-50 hover:bg-orange-100 dark:bg-[#241B17] dark:hover:bg-[#30241E] text-[#FF5722] dark:text-[#FF8A65] text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer whitespace-nowrap shrink-0"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isSyncing ? 'animate-spin' : ''}`} />
            <span>{isSyncing ? 'Syncing' : 'Sync'}</span>
          </button>
        </div>

        {/* Segmented Section Switcher */}
        <div
          role="tablist"
          aria-label="Settings sections"
          className="grid grid-cols-3 gap-1.5 p-1 rounded-xl bg-orange-50/80 dark:bg-[#211A17] border border-orange-100/80 dark:border-stone-800"
        >
          {(
            [
              { id: 'contact', label: 'Contact Info' },
              { id: 'addresses', label: 'Addresses' },
              { id: 'orders', label: 'Order History' },
            ] as const
          ).map((tab) => {
            const isSelected = activeSection === tab.id;
            return (
              <button
                key={tab.id}
                role="tab"
                aria-selected={isSelected}
                type="button"
                onClick={() => {
                  haptics.selection();
                  setActiveSection(tab.id);
                }}
                className={`min-h-[40px] px-2.5 py-2 rounded-lg text-xs font-semibold transition-colors whitespace-nowrap truncate cursor-pointer ${
                  isSelected
                    ? 'bg-[#FF5722] text-white shadow-xs'
                    : 'text-slate-700 dark:text-slate-300 hover:bg-white/70 dark:hover:bg-stone-800/60'
                }`}
              >
                {tab.label}
              </button>
            );
          })}
        </div>
      </header>

      {/* Main Content */}
      <div className="p-3.5 space-y-4">
        {/* Session Security Strip (Clean unboxed metadata, never displays raw JWT) */}
        <div className="rounded-2xl bg-white dark:bg-[#211A17] border border-orange-100 dark:border-stone-800 p-3.5 flex items-center justify-between gap-3">
          <div className="flex items-start gap-2.5 min-w-0">
            <ShieldCheck
              className={`w-4 h-4 mt-0.5 shrink-0 ${
                sessionInfo.hasToken && !sessionInfo.isExpired
                  ? 'text-emerald-600 dark:text-emerald-400'
                  : 'text-amber-600 dark:text-amber-400'
              }`}
            />
            <div className="min-w-0">
              <p className="text-xs font-bold text-slate-900 dark:text-white">
                {sessionInfo.hasToken && !sessionInfo.isExpired
                  ? 'Encrypted Session Active'
                  : sessionInfo.isExpired
                  ? 'Session Expired'
                  : 'Device-Only Mode'}
              </p>
              <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5 leading-snug">
                {syncNotice ||
                  (sessionInfo.hasToken && !sessionInfo.isExpired
                    ? `Signed in securely${sessionInfo.role ? ` · ${sessionInfo.role}` : ''}${sessionInfo.subjectHint ? ` · Account ${sessionInfo.subjectHint}` : ''}`
                    : 'Profile, addresses, and saved orders persist on this device. Sign in to sync with cloud account.')}
              </p>
            </div>
          </div>

          {(!sessionInfo.hasToken || sessionInfo.isExpired) && onLogout && (
            <button
              type="button"
              onClick={() => {
                haptics.selection();
                onLogout();
              }}
              className="min-h-[36px] px-3 py-1.5 rounded-xl bg-[#FF5722] hover:bg-[#E64A19] text-white text-xs font-semibold whitespace-nowrap shrink-0 cursor-pointer"
            >
              Sign In
            </button>
          )}
        </div>

        {/* =============================================================== */}
        {/* SECTION 1: CONTACT INFORMATION & APP PREFERENCES                */}
        {/* =============================================================== */}
        {activeSection === 'contact' && (
          <div className="space-y-4">
            <form
              onSubmit={handleSaveContact}
              className="rounded-2xl bg-white dark:bg-[#211A17] border border-orange-100 dark:border-stone-800 p-4 space-y-3.5 shadow-2xs"
            >
              <div className="flex items-center justify-between gap-2 pb-2.5 border-b border-orange-100/80 dark:border-stone-800">
                <div className="flex items-center gap-2">
                  <User className="w-4 h-4 text-[#FF5722]" />
                  <h2 className="font-display font-bold text-sm text-slate-900 dark:text-white">
                    Contact Information
                  </h2>
                </div>
                {phoneVerifiedOnServer && (
                  <span className="text-[11px] font-semibold text-emerald-600 dark:text-emerald-400 flex items-center gap-1">
                    <Check className="w-3.5 h-3.5" />
                    OTP Verified
                  </span>
                )}
              </div>

              <div>
                <label
                  htmlFor="settings-contact-name"
                  className="text-xs font-semibold text-slate-700 dark:text-slate-300 block mb-1"
                >
                  Full Name
                </label>
                <div className="relative">
                  <User className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                  <input
                    id="settings-contact-name"
                    type="text"
                    required
                    value={contactName}
                    onChange={(e) => setContactName(e.target.value)}
                    placeholder="Enter your full name"
                    className="w-full min-h-[44px] pl-9 pr-3 py-2 rounded-xl bg-[#FFFDF9] dark:bg-[#120E0C] border border-orange-200/80 dark:border-stone-700 text-xs font-medium text-slate-900 dark:text-white outline-none focus:border-[#FF5722]"
                  />
                </div>
              </div>

              <div>
                <label
                  htmlFor="settings-contact-email"
                  className="text-xs font-semibold text-slate-700 dark:text-slate-300 block mb-1"
                >
                  Email Address
                </label>
                <div className="relative">
                  <Mail className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                  <input
                    id="settings-contact-email"
                    type="email"
                    value={contactEmail}
                    onChange={(e) => setContactEmail(e.target.value)}
                    placeholder="name@example.com"
                    className="w-full min-h-[44px] pl-9 pr-3 py-2 rounded-xl bg-[#FFFDF9] dark:bg-[#120E0C] border border-orange-200/80 dark:border-stone-700 text-xs font-medium text-slate-900 dark:text-white outline-none focus:border-[#FF5722]"
                  />
                </div>
              </div>

              <div>
                <label
                  htmlFor="settings-contact-phone"
                  className="text-xs font-semibold text-slate-700 dark:text-slate-300 block mb-1"
                >
                  Mobile Number
                </label>
                <div className="relative">
                  <Phone className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                  <input
                    id="settings-contact-phone"
                    type="tel"
                    inputMode="numeric"
                    maxLength={12}
                    value={contactPhone}
                    onChange={(e) => setContactPhone(e.target.value.replace(/\D/g, '').slice(0, 12))}
                    placeholder="10-digit mobile number"
                    className="w-full min-h-[44px] pl-9 pr-3 py-2 rounded-xl bg-[#FFFDF9] dark:bg-[#120E0C] border border-orange-200/80 dark:border-stone-700 text-xs font-medium tabular-nums text-slate-900 dark:text-white outline-none focus:border-[#FF5722]"
                  />
                </div>
                <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-1">
                  Used for order confirmations and courier delivery coordination.
                </p>
              </div>

              {contactError && (
                <p role="alert" className="text-xs font-semibold text-rose-600 dark:text-rose-400">
                  {contactError}
                </p>
              )}

              <button
                type="submit"
                disabled={isSavingContact}
                className="w-full min-h-[44px] py-2.5 px-4 rounded-xl bg-[#FF5722] hover:bg-[#E64A19] disabled:opacity-60 text-white text-xs font-bold shadow-sm active:scale-[0.99] transition-all cursor-pointer"
              >
                {isSavingContact ? 'Saving Contact Info...' : 'Save Contact Information'}
              </button>
            </form>

            {/* Preferences & Security Actions */}
            <div className="rounded-2xl bg-white dark:bg-[#211A17] border border-orange-100 dark:border-stone-800 divide-y divide-orange-100/80 dark:divide-stone-800 shadow-2xs">
              <div className="p-4 pb-2.5">
                <h2 className="font-display font-bold text-sm text-slate-900 dark:text-white">
                  Preferences &amp; Security
                </h2>
              </div>

              {onToggleTheme && (
                <div className="px-4 py-3 flex items-center justify-between gap-3">
                  <div className="flex items-center gap-3 min-w-0">
                    {theme === 'dark' ? (
                      <Moon className="w-4 h-4 text-[#FF8A65] shrink-0" />
                    ) : (
                      <Sun className="w-4 h-4 text-[#FF5722] shrink-0" />
                    )}
                    <div>
                      <p className="text-xs font-bold text-slate-900 dark:text-white">Appearance</p>
                      <p className="text-[11px] text-slate-500 dark:text-slate-400">
                        Active theme · {theme === 'dark' ? 'Dark mode' : 'Light mode'}
                      </p>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={onToggleTheme}
                    className="min-h-[36px] px-3 py-1.5 rounded-xl bg-orange-50 hover:bg-orange-100 dark:bg-[#2A201C] dark:hover:bg-[#352924] text-[#FF5722] dark:text-[#FF8A65] text-xs font-semibold cursor-pointer whitespace-nowrap shrink-0"
                  >
                    Switch to {theme === 'dark' ? 'Light' : 'Dark'}
                  </button>
                </div>
              )}

              <div className="px-4 py-3 flex items-center justify-between gap-3">
                <div className="flex items-center gap-3 min-w-0">
                  <Bell className="w-4 h-4 text-[#FF5722] shrink-0" />
                  <div>
                    <p className="text-xs font-bold text-slate-900 dark:text-white">
                      Order Dispatch Updates
                    </p>
                    <p className="text-[11px] text-slate-500 dark:text-slate-400">
                      Notify when orders ship or reach your PIN
                    </p>
                  </div>
                </div>
                <button
                  type="button"
                  role="switch"
                  aria-checked={prefs.orderAlerts}
                  onClick={() => {
                    haptics.selection();
                    setPrefs((p) => ({ ...p, orderAlerts: !p.orderAlerts }));
                  }}
                  className={`min-h-[36px] px-3 py-1.5 rounded-xl text-xs font-semibold transition-colors cursor-pointer shrink-0 ${
                    prefs.orderAlerts
                      ? 'bg-[#FF5722] text-white'
                      : 'bg-stone-100 dark:bg-stone-800 text-slate-600 dark:text-slate-300'
                  }`}
                >
                  {prefs.orderAlerts ? 'On' : 'Off'}
                </button>
              </div>

              <div className="px-4 py-3 flex items-center justify-between gap-3">
                <div className="flex items-center gap-3 min-w-0">
                  <Truck className="w-4 h-4 text-[#FF5722] shrink-0" />
                  <div>
                    <p className="text-xs font-bold text-slate-900 dark:text-white">
                      Live Courier ETA Sync
                    </p>
                    <p className="text-[11px] text-slate-500 dark:text-slate-400">
                      Auto-estimate delivery dates using default PIN
                    </p>
                  </div>
                </div>
                <button
                  type="button"
                  role="switch"
                  aria-checked={prefs.deliveryAlerts}
                  onClick={() => {
                    haptics.selection();
                    setPrefs((p) => ({ ...p, deliveryAlerts: !p.deliveryAlerts }));
                  }}
                  className={`min-h-[36px] px-3 py-1.5 rounded-xl text-xs font-semibold transition-colors cursor-pointer shrink-0 ${
                    prefs.deliveryAlerts
                      ? 'bg-[#FF5722] text-white'
                      : 'bg-stone-100 dark:bg-stone-800 text-slate-600 dark:text-slate-300'
                  }`}
                >
                  {prefs.deliveryAlerts ? 'On' : 'Off'}
                </button>
              </div>

              {onOpenDeleteAccount && (
                <button
                  type="button"
                  onClick={() => {
                    haptics.warning();
                    onOpenDeleteAccount();
                  }}
                  className="w-full px-4 py-3 text-left flex items-center justify-between gap-2 text-rose-600 dark:text-rose-400 hover:bg-rose-50/70 dark:hover:bg-rose-950/30 transition-colors cursor-pointer"
                >
                  <div className="flex items-center gap-3">
                    <Trash2 className="w-4 h-4 shrink-0" />
                    <div>
                      <p className="text-xs font-bold">Delete AARVO Account</p>
                      <p className="text-[11px] text-slate-500 dark:text-slate-400">
                        Erase profile, saved addresses &amp; sign out
                      </p>
                    </div>
                  </div>
                </button>
              )}

              {onLogout && (
                <button
                  type="button"
                  onClick={() => {
                    haptics.light();
                    onLogout();
                  }}
                  className="w-full px-4 py-3 text-left flex items-center gap-3 text-slate-700 dark:text-slate-200 hover:bg-orange-50/60 dark:hover:bg-stone-800/60 transition-colors rounded-b-2xl cursor-pointer"
                >
                  <LogOut className="w-4 h-4 text-[#FF5722] shrink-0" />
                  <span className="text-xs font-bold">Sign Out / Switch Account</span>
                </button>
              )}
            </div>
          </div>
        )}

        {/* =============================================================== */}
        {/* SECTION 2: SAVED DELIVERY ADDRESSES                             */}
        {/* =============================================================== */}
        {activeSection === 'addresses' && (
          <div className="space-y-4">
            <div className="flex items-center justify-between gap-2">
              <div>
                <h2 className="font-display font-bold text-sm text-slate-900 dark:text-white">
                  Saved Delivery Addresses
                </h2>
                <p className="text-[11px] text-slate-500 dark:text-slate-400">
                  {savedAddresses.length} address{savedAddresses.length === 1 ? '' : 'es'} &middot; Select default for checkout &amp; delivery ETA
                </p>
              </div>
              <button
                type="button"
                onClick={openAddAddressForm}
                className="min-h-[40px] px-3 py-2 rounded-xl bg-[#FF5722] hover:bg-[#E64A19] text-white text-xs font-bold flex items-center gap-1.5 shadow-xs cursor-pointer whitespace-nowrap shrink-0"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>Add Address</span>
              </button>
            </div>

            {isAddressFormOpen && (
              <form
                onSubmit={handleSaveAddress}
                className="rounded-2xl bg-white dark:bg-[#211A17] border border-orange-200 dark:border-stone-700 p-4 space-y-3 shadow-sm"
              >
                <div className="flex items-center justify-between pb-2 border-b border-orange-100 dark:border-stone-800">
                  <h3 className="font-display font-bold text-sm text-slate-900 dark:text-white">
                    {editingAddressId ? 'Edit Delivery Address' : 'New Delivery Address'}
                  </h3>
                  <button
                    type="button"
                    onClick={() => {
                      setIsAddressFormOpen(false);
                      setEditingAddressId(null);
                    }}
                    className="text-xs font-semibold text-slate-500 hover:text-slate-800 dark:hover:text-white cursor-pointer"
                  >
                    Cancel
                  </button>
                </div>

                {/* Label selector */}
                <div className="flex items-center gap-2">
                  {(['Home', 'Work', 'Other'] as const).map((lbl) => (
                    <button
                      key={lbl}
                      type="button"
                      onClick={() => setAddressForm((f) => ({ ...f, label: lbl }))}
                      className={`min-h-[36px] px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors cursor-pointer ${
                        addressForm.label === lbl
                          ? 'bg-[#FF5722] text-white'
                          : 'bg-orange-50/70 dark:bg-[#120E0C] text-slate-700 dark:text-slate-300 border border-orange-100 dark:border-stone-700'
                      }`}
                    >
                      {lbl}
                    </button>
                  ))}
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                  <div>
                    <label className="text-[11px] font-semibold text-slate-600 dark:text-slate-400 block mb-1">
                      Recipient Name
                    </label>
                    <input
                      type="text"
                      required
                      value={addressForm.fullName}
                      onChange={(e) => setAddressForm((f) => ({ ...f, fullName: e.target.value }))}
                      placeholder="Full name"
                      className="w-full min-h-[42px] px-3 py-2 rounded-xl bg-[#FFFDF9] dark:bg-[#120E0C] border border-orange-200/80 dark:border-stone-700 text-xs text-slate-900 dark:text-white outline-none focus:border-[#FF5722]"
                    />
                  </div>
                  <div>
                    <label className="text-[11px] font-semibold text-slate-600 dark:text-slate-400 block mb-1">
                      10-Digit Mobile
                    </label>
                    <input
                      type="tel"
                      inputMode="numeric"
                      maxLength={10}
                      required
                      value={addressForm.phone}
                      onChange={(e) =>
                        setAddressForm((f) => ({
                          ...f,
                          phone: e.target.value.replace(/\D/g, '').slice(0, 10),
                        }))
                      }
                      placeholder="Mobile for rider"
                      className="w-full min-h-[42px] px-3 py-2 rounded-xl bg-[#FFFDF9] dark:bg-[#120E0C] border border-orange-200/80 dark:border-stone-700 text-xs tabular-nums text-slate-900 dark:text-white outline-none focus:border-[#FF5722]"
                    />
                  </div>
                </div>

                <div>
                  <label className="text-[11px] font-semibold text-slate-600 dark:text-slate-400 block mb-1">
                    House No., Building, Street
                  </label>
                  <input
                    type="text"
                    required
                    value={addressForm.addressLine}
                    onChange={(e) => setAddressForm((f) => ({ ...f, addressLine: e.target.value }))}
                    placeholder="Flat / House No., Street name"
                    className="w-full min-h-[42px] px-3 py-2 rounded-xl bg-[#FFFDF9] dark:bg-[#120E0C] border border-orange-200/80 dark:border-stone-700 text-xs text-slate-900 dark:text-white outline-none focus:border-[#FF5722]"
                  />
                </div>

                <div>
                  <label className="text-[11px] font-semibold text-slate-600 dark:text-slate-400 block mb-1">
                    Locality / Landmark (Optional)
                  </label>
                  <input
                    type="text"
                    value={addressForm.line2}
                    onChange={(e) => setAddressForm((f) => ({ ...f, line2: e.target.value }))}
                    placeholder="Area, Colony, or Landmark"
                    className="w-full min-h-[42px] px-3 py-2 rounded-xl bg-[#FFFDF9] dark:bg-[#120E0C] border border-orange-200/80 dark:border-stone-700 text-xs text-slate-900 dark:text-white outline-none focus:border-[#FF5722]"
                  />
                </div>

                <div className="grid grid-cols-2 gap-2.5">
                  <div>
                    <label className="text-[11px] font-semibold text-slate-600 dark:text-slate-400 block mb-1">
                      City
                    </label>
                    <input
                      type="text"
                      required
                      value={addressForm.city}
                      onChange={(e) => setAddressForm((f) => ({ ...f, city: e.target.value }))}
                      placeholder="City"
                      className="w-full min-h-[42px] px-3 py-2 rounded-xl bg-[#FFFDF9] dark:bg-[#120E0C] border border-orange-200/80 dark:border-stone-700 text-xs text-slate-900 dark:text-white outline-none focus:border-[#FF5722]"
                    />
                  </div>
                  <div>
                    <label className="text-[11px] font-semibold text-slate-600 dark:text-slate-400 block mb-1">
                      State
                    </label>
                    <input
                      type="text"
                      required
                      value={addressForm.state}
                      onChange={(e) => setAddressForm((f) => ({ ...f, state: e.target.value }))}
                      placeholder="State (e.g. Bihar)"
                      className="w-full min-h-[42px] px-3 py-2 rounded-xl bg-[#FFFDF9] dark:bg-[#120E0C] border border-orange-200/80 dark:border-stone-700 text-xs text-slate-900 dark:text-white outline-none focus:border-[#FF5722]"
                    />
                  </div>
                </div>

                <div>
                  <label className="text-[11px] font-semibold text-slate-600 dark:text-slate-400 block mb-1">
                    Delivery PIN Code
                  </label>
                  <div className="flex items-center gap-2">
                    <input
                      type="text"
                      inputMode="numeric"
                      maxLength={6}
                      required
                      value={addressForm.pincode}
                      onChange={(e) => {
                        const clean = e.target.value.replace(/\D/g, '').slice(0, 6);
                        setAddressForm((f) => ({ ...f, pincode: clean }));
                        setPinPreview(null);
                      }}
                      placeholder="6-digit PIN"
                      className="flex-1 min-h-[42px] px-3 py-2 rounded-xl bg-[#FFFDF9] dark:bg-[#120E0C] border border-orange-200/80 dark:border-stone-700 text-xs font-bold tabular-nums text-slate-900 dark:text-white outline-none focus:border-[#FF5722]"
                    />
                    <button
                      type="button"
                      disabled={isCheckingPin}
                      onClick={() => handleCheckPincode(addressForm.pincode)}
                      className="min-h-[42px] px-3 py-2 rounded-xl bg-orange-50 hover:bg-orange-100 dark:bg-[#2A201C] dark:hover:bg-[#362924] text-[#FF5722] dark:text-[#FF8A65] text-xs font-semibold flex items-center gap-1.5 cursor-pointer whitespace-nowrap shrink-0"
                    >
                      <LocateFixed className="w-3.5 h-3.5" />
                      <span>{isCheckingPin ? 'Checking' : 'Check PIN'}</span>
                    </button>
                  </div>
                  {pinPreview && (
                    <p
                      className={`text-[11px] font-semibold mt-1.5 ${
                        pinPreview.serviceable
                          ? 'text-emerald-700 dark:text-emerald-400'
                          : 'text-amber-700 dark:text-amber-400'
                      }`}
                    >
                      {pinPreview.riderMessage}
                    </p>
                  )}
                </div>

                <label className="flex items-center gap-2 text-xs font-semibold text-slate-700 dark:text-slate-300 pt-1 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={addressForm.isDefault}
                    onChange={(e) => setAddressForm((f) => ({ ...f, isDefault: e.target.checked }))}
                    className="rounded border-orange-300 text-[#FF5722] focus:ring-[#FF5722]"
                  />
                  <span>Set as default delivery address</span>
                </label>

                {addressError && (
                  <p role="alert" className="text-xs font-semibold text-rose-600 dark:text-rose-400">
                    {addressError}
                  </p>
                )}

                <button
                  type="submit"
                  disabled={isSavingAddress}
                  className="w-full min-h-[44px] py-2.5 px-4 rounded-xl bg-[#FF5722] hover:bg-[#E64A19] disabled:opacity-60 text-white text-xs font-bold shadow-sm cursor-pointer"
                >
                  {isSavingAddress
                    ? 'Saving Address...'
                    : editingAddressId
                    ? 'Update Address'
                    : 'Save Delivery Address'}
                </button>
              </form>
            )}

            {/* Saved Address List */}
            <div className="space-y-2.5">
              {savedAddresses.map((addr) => {
                const isDefault = addr.id === activeAddressId;
                return (
                  <article
                    key={addr.id}
                    className={`rounded-2xl bg-white dark:bg-[#211A17] border p-4 transition-colors ${
                      isDefault
                        ? 'border-[#FF5722]'
                        : 'border-orange-100 dark:border-stone-800'
                    }`}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div className="min-w-0">
                        <div className="flex items-center gap-2 text-xs text-slate-500 dark:text-slate-400">
                          <MapPin className="w-3.5 h-3.5 text-[#FF5722] shrink-0" />
                          <span className="font-bold text-slate-900 dark:text-white">
                            {addr.fullName}
                          </span>
                          <span aria-hidden="true">&middot;</span>
                          <span>{addr.label}</span>
                          {isDefault && (
                            <>
                              <span aria-hidden="true">&middot;</span>
                              <span className="font-semibold text-[#FF5722]">Default</span>
                            </>
                          )}
                        </div>
                        <p className="text-xs text-slate-700 dark:text-slate-300 mt-1.5 leading-relaxed">
                          {addr.addressLine}
                          {addr.line2 ? `, ${addr.line2}` : ''}, {addr.city}
                          {addr.state ? `, ${addr.state}` : ''} -{' '}
                          <span className="font-semibold tabular-nums">{addr.pincode}</span>
                        </p>
                        {addr.phone && (
                          <p className="text-[11px] text-slate-500 dark:text-slate-400 tabular-nums mt-1">
                            Phone: {addr.phone}
                          </p>
                        )}
                      </div>
                    </div>

                    <div className="flex flex-wrap items-center gap-2 mt-3 pt-2.5 border-t border-orange-100/80 dark:border-stone-800">
                      {!isDefault && (
                        <button
                          type="button"
                          onClick={() => handleSetDefaultAddress(addr)}
                          className="min-h-[36px] px-3 py-1.5 rounded-xl bg-orange-50 hover:bg-orange-100 dark:bg-[#2A201C] dark:hover:bg-[#362924] text-[#FF5722] dark:text-[#FF8A65] text-xs font-semibold cursor-pointer"
                        >
                          Set Default
                        </button>
                      )}
                      <button
                        type="button"
                        onClick={() => openEditAddressForm(addr)}
                        className="min-h-[36px] px-3 py-1.5 rounded-xl bg-stone-100 hover:bg-stone-200 dark:bg-[#181311] dark:hover:bg-stone-800 text-slate-700 dark:text-slate-200 text-xs font-semibold flex items-center gap-1.5 cursor-pointer"
                      >
                        <Edit2 className="w-3 h-3" />
                        <span>Edit</span>
                      </button>
                      {savedAddresses.length > 1 && (
                        <button
                          type="button"
                          onClick={() => handleDeleteAddress(addr)}
                          className="min-h-[36px] px-3 py-1.5 rounded-xl text-rose-600 dark:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/40 text-xs font-semibold flex items-center gap-1.5 cursor-pointer ml-auto"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                          <span>Remove</span>
                        </button>
                      )}
                    </div>
                  </article>
                );
              })}
            </div>
          </div>
        )}

        {/* =============================================================== */}
        {/* SECTION 3: SECURE ORDER HISTORY (UTILIZING STORED AUTH TOKEN)   */}
        {/* =============================================================== */}
        {activeSection === 'orders' && (
          <div className="space-y-3">
            <div className="flex items-center gap-2 text-xs text-slate-600 dark:text-slate-400 px-0.5">
              <Package className="w-4 h-4 text-[#FF5722] shrink-0" />
              <span>
                {sessionInfo.hasToken && !sessionInfo.isExpired
                  ? 'Authenticated order history synced via your AARVO token. Tap any order for full breakdown & tracking.'
                  : 'Showing device order history. Sign in to sync live orders from your AARVO account.'}
              </span>
            </div>

            <Orders
              embedded
              orders={orders}
              onOrdersLoaded={onOrdersLoaded}
              apiBaseUrl={apiBaseUrl}
              authToken={authToken}
              customerName={userProfile.name}
              customerPhone={userProfile.phone}
              onOpenSupport={onOpenSupport}
              onReorder={onReorderItem}
              onReorderAll={onReorderAll}
              onNavigateToCart={onNavigateToCart}
              onShowToast={(msg) => onShowToast(msg, 'info')}
            />
          </div>
        )}
      </div>
    </section>
  );
};
