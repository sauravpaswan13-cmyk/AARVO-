import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  APIProvider,
  AdvancedMarker,
  Map as GoogleMapView,
  useMap,
  useMapsLibrary,
} from '@vis.gl/react-google-maps';
import {
  CheckCircle2,
  Loader2,
  LocateFixed,
  MapPin,
  Navigation,
  Search,
  X,
} from 'lucide-react';
import { SavedAddress, UserProfile } from './SettingsView';
import { getStoredAuthToken, resolveApiBaseUrl } from '../utils/authSession';
import {
  checkShiprocketServiceability,
  ShiprocketServiceabilityResult,
} from '../utils/shiprocketServiceability';
import { haptics } from '../utils/haptics';

interface LatLngLiteral {
  lat: number;
  lng: number;
}

interface GeocodedFields {
  formattedAddress: string;
  line1: string;
  line2: string;
  city: string;
  state: string;
  postalCode: string;
  lat: number | null;
  lng: number | null;
}

interface ToastOptionsCompat {
  title?: string;
  variant?: 'success' | 'error' | 'warning' | 'info';
}

interface FlipkartLocationPickerModalProps {
  isOpen: boolean;
  onClose: () => void;
  savedAddresses: SavedAddress[];
  activeAddressObj?: SavedAddress;
  userProfile: UserProfile;
  initialEditingAddress?: SavedAddress | null;
  onSetSavedAddresses: React.Dispatch<React.SetStateAction<SavedAddress[]>>;
  onSetActiveAddressId: (id: string) => void;
  onToast: (
    msg: string,
    variantOrOptions?: 'success' | 'error' | 'warning' | 'info' | ToastOptionsCompat
  ) => void;
}

const DEFAULT_INDIA_CENTER: LatLngLiteral = { lat: 20.5937, lng: 78.9629 };
const DEFAULT_INDIA_ZOOM = 4.6;
const FOCUSED_ZOOM = 16;

function dispatchQuotaExceeded(message?: string) {
  if (typeof window === 'undefined') return;
  window.dispatchEvent(
    new CustomEvent('gmp-quota-exceeded', {
      detail: {
        message:
          message ||
          'Google Maps quota reached. You can still enter your 6-digit PIN or doorstep address below.',
      },
    })
  );
}

async function callServerGeocode(query: string): Promise<GeocodedFields | null> {
  const candidates = [`/api/maps/geocode?${query}`, `${resolveApiBaseUrl()}/v1/maps/geocode?${query}`];
  for (const endpoint of candidates) {
    try {
      const resp = await fetch(endpoint, { headers: { Accept: 'application/json' } });
      if (resp.status === 429) {
        const errBody = (await resp.json().catch(() => ({}))) as { message?: string };
        dispatchQuotaExceeded(errBody.message);
        return null;
      }
      if (!resp.ok) continue;
      const data = (await resp.json().catch(() => null)) as
        | (GeocodedFields & { found?: boolean; error?: string })
        | null;
      if (data?.error === 'RESOURCE_EXHAUSTED') {
        dispatchQuotaExceeded();
        return null;
      }
      if (data && data.found) {
        return {
          formattedAddress: data.formattedAddress || '',
          line1: data.line1 || '',
          line2: data.line2 || '',
          city: data.city || '',
          state: data.state || '',
          postalCode: (data.postalCode || '').replace(/\D/g, '').slice(0, 6),
          lat: typeof data.lat === 'number' ? data.lat : null,
          lng: typeof data.lng === 'number' ? data.lng : null,
        };
      }
    } catch {
      // try next proxy candidate
    }
  }
  return null;
}

interface PlaceAddressComponentLike {
  types?: string[];
  longText?: string | null;
}

interface PlacesPlaceLike {
  addressComponents?: PlaceAddressComponentLike[];
  formattedAddress?: string | null;
  displayName?: string | null;
  location?: { lat: () => number; lng: () => number } | null;
  fetchFields: (opts: { fields: string[] }) => Promise<unknown>;
}

interface AutocompleteSuggestionLike {
  placePrediction?: {
    toPlace: () => PlacesPlaceLike;
    text?: { text?: string };
  } | null;
}

function parsePlaceComponents(place: PlacesPlaceLike): Partial<GeocodedFields> {
  const components = place.addressComponents || [];
  const getByType = (types: string[]): string => {
    for (const t of types) {
      const found = components.find((c) => Array.isArray(c.types) && c.types.includes(t));
      if (found?.longText) return found.longText.trim();
    }
    return '';
  };

  const premise = getByType(['premise', 'subpremise']);
  const streetNum = getByType(['street_number']);
  const route = getByType(['route']);
  const sublocality = getByType([
    'sublocality_level_2',
    'sublocality_level_1',
    'sublocality',
    'neighborhood',
  ]);
  const city = getByType(['locality', 'administrative_area_level_2', 'postal_town']);
  const state = getByType(['administrative_area_level_1']);
  const postalCode = getByType(['postal_code']).replace(/\D/g, '').slice(0, 6);
  const formattedAddress = place.formattedAddress || '';
  const displayName = place.displayName || '';
  const streetLine = [premise, streetNum, route].filter(Boolean).join(' ').trim();
  const line1 =
    streetLine ||
    (displayName && sublocality ? `${displayName}, ${sublocality}` : displayName || sublocality) ||
    formattedAddress.split(',').slice(0, 2).join(', ').trim();

  return {
    formattedAddress,
    line1,
    line2: sublocality && !line1.includes(sublocality) ? sublocality : '',
    city,
    state,
    postalCode,
    lat: place.location ? place.location.lat() : null,
    lng: place.location ? place.location.lng() : null,
  };
}

interface PlacesAutocompleteSearchProps {
  onSelectPlace: (fields: Partial<GeocodedFields>, coords: LatLngLiteral | null) => void;
}

const PlacesAutocompleteSearch: React.FC<PlacesAutocompleteSearchProps> = ({ onSelectPlace }) => {
  const placesLib = useMapsLibrary('places');
  const map = useMap();
  const sessionTokenRef = useRef<unknown>(null);
  const [searchText, setSearchText] = useState('');
  const [suggestions, setSuggestions] = useState<AutocompleteSuggestionLike[]>([]);
  const [isSearching, setIsSearching] = useState(false);

  useEffect(() => {
    const trimmed = searchText.trim();
    if (!placesLib || trimmed.length < 2) {
      setSuggestions([]);
      return;
    }

    const timer = window.setTimeout(async () => {
      try {
        const { AutocompleteSessionToken, AutocompleteSuggestion } = placesLib;
        if (!sessionTokenRef.current) {
          sessionTokenRef.current = new AutocompleteSessionToken();
        }
        setIsSearching(true);
        const res = await AutocompleteSuggestion.fetchAutocompleteSuggestions({
          input: trimmed,
          includedRegionCodes: ['in'],
          language: 'en',
          sessionToken: sessionTokenRef.current as never,
        });
        setSuggestions((res.suggestions || []) as AutocompleteSuggestionLike[]);
      } catch (err) {
        const msg = err instanceof Error ? err.message : '';
        if (msg.includes('QUOTA') || msg.includes('RESOURCE_EXHAUSTED')) {
          dispatchQuotaExceeded();
        }
        setSuggestions([]);
      } finally {
        setIsSearching(false);
      }
    }, 220);

    return () => window.clearTimeout(timer);
  }, [placesLib, searchText]);

  const handlePickSuggestion = async (suggestion: AutocompleteSuggestionLike) => {
    if (!suggestion.placePrediction) return;
    haptics.selection();
    try {
      const place = suggestion.placePrediction.toPlace();
      await place.fetchFields({
        fields: ['formattedAddress', 'addressComponents', 'location', 'displayName'],
      });
      const parsed = parsePlaceComponents(place);
      const coords =
        place.location != null
          ? { lat: place.location.lat(), lng: place.location.lng() }
          : null;
      if (coords && map) {
        map.panTo(coords);
        map.setZoom(FOCUSED_ZOOM);
      }
      onSelectPlace(parsed, coords);
      setSearchText(parsed.formattedAddress || suggestion.placePrediction.text?.text || '');
    } catch {
      // ignore cancelled/incomplete fetchFields
    } finally {
      sessionTokenRef.current = null;
      setSuggestions([]);
    }
  };

  return (
    <div className="relative">
      <div className="flex items-center gap-2 px-3 py-2 rounded-xl bg-white dark:bg-[#181311] border border-orange-200/80 dark:border-stone-700 focus-within:border-[#FF5722]">
        <Search className="w-4 h-4 text-[#FF5722] shrink-0" />
        <input
          type="text"
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          placeholder="Search area, street, landmark or city..."
          className="flex-1 bg-transparent text-xs text-slate-900 dark:text-white placeholder:text-slate-400 outline-none"
        />
        {isSearching && <Loader2 className="w-3.5 h-3.5 text-[#FF5722] animate-spin shrink-0" />}
        {searchText && !isSearching && (
          <button
            type="button"
            onClick={() => {
              setSearchText('');
              setSuggestions([]);
            }}
            className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 cursor-pointer"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        )}
      </div>

      {suggestions.length > 0 && (
        <ul className="absolute left-0 right-0 top-full mt-1 z-30 max-h-48 overflow-y-auto rounded-xl bg-white dark:bg-[#211A17] border border-orange-200 dark:border-stone-700 shadow-xl divide-y divide-orange-100/70 dark:divide-stone-800">
          {suggestions.map((item, idx) => (
            <li key={idx}>
              <button
                type="button"
                onClick={() => handlePickSuggestion(item)}
                className="w-full text-left px-3 py-2 text-xs text-slate-800 dark:text-slate-100 hover:bg-[#FFF4E8] dark:hover:bg-orange-950/30 flex items-start gap-2 cursor-pointer"
              >
                <MapPin className="w-3.5 h-3.5 text-[#FF5722] shrink-0 mt-0.5" />
                <span className="line-clamp-2">{item.placePrediction?.text?.text}</span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

export const FlipkartLocationPickerModal: React.FC<FlipkartLocationPickerModalProps> = ({
  isOpen,
  onClose,
  savedAddresses,
  activeAddressObj,
  userProfile,
  initialEditingAddress = null,
  onSetSavedAddresses,
  onSetActiveAddressId,
  onToast,
}) => {
  const mapsApiKey = String(
    (import.meta as unknown as { env?: Record<string, string> }).env?.VITE_GOOGLE_MAPS_API_KEY || ''
  ).trim();

  const [quickPincodeInput, setQuickPincodeInput] = useState(activeAddressObj?.pincode || '');
  const [pincodeStatusMessage, setPincodeStatusMessage] = useState<string | null>(null);
  const [isDetectingLocation, setIsDetectingLocation] = useState(false);
  const [isGeocoding, setIsGeocoding] = useState(false);
  const [pinPosition, setPinPosition] = useState<LatLngLiteral>(DEFAULT_INDIA_CENTER);
  const [mapCenter, setMapCenter] = useState<LatLngLiteral>(DEFAULT_INDIA_CENTER);
  const [mapZoom, setMapZoom] = useState<number>(DEFAULT_INDIA_ZOOM);
  const [hasLockedPin, setHasLockedPin] = useState(false);
  const [resolvedBanner, setResolvedBanner] = useState<string>('');
  const [fallbackSearchQuery, setFallbackSearchQuery] = useState('');

  const [editingAddressId, setEditingAddressId] = useState<string | null>(null);
  const [addressDraftError, setAddressDraftError] = useState<string | null>(null);
  const [isSavingAddressModal, setIsSavingAddressModal] = useState(false);
  const [addressEditPinPreview, setAddressEditPinPreview] =
    useState<ShiprocketServiceabilityResult | null>(null);
  const addressFormRef = useRef<HTMLFormElement | null>(null);

  const [addressDraft, setAddressDraft] = useState({
    label: 'Home',
    fullName: userProfile.name || '',
    phone: userProfile.phone || '',
    addressLine: '',
    line2: '',
    city: '',
    state: '',
    pincode: '',
  });

  useEffect(() => {
    if (!isOpen) return;
    setQuickPincodeInput(activeAddressObj?.pincode || '');
    setPincodeStatusMessage(null);
    setAddressDraftError(null);
    if (initialEditingAddress) {
      setEditingAddressId(initialEditingAddress.id);
      setAddressDraft({
        label: initialEditingAddress.label || 'Home',
        fullName: initialEditingAddress.fullName || '',
        phone: initialEditingAddress.phone || '',
        addressLine: initialEditingAddress.addressLine || '',
        line2: initialEditingAddress.line2 || '',
        city: initialEditingAddress.city || '',
        state: initialEditingAddress.state || '',
        pincode: initialEditingAddress.pincode || '',
      });
    } else {
      setEditingAddressId(null);
      setAddressDraft((prev) => ({
        ...prev,
        fullName: prev.fullName || userProfile.name || '',
        phone: prev.phone || userProfile.phone || '',
      }));
    }
  }, [isOpen, activeAddressObj?.pincode, initialEditingAddress, userProfile.name, userProfile.phone]);

  const applyGeocodedFields = useCallback(
    async (fields: Partial<GeocodedFields>, coords: LatLngLiteral | null) => {
      if (coords) {
        setPinPosition(coords);
        setMapCenter(coords);
        setMapZoom(FOCUSED_ZOOM);
        setHasLockedPin(true);
      }
      if (fields.formattedAddress) {
        setResolvedBanner(fields.formattedAddress);
      }
      const nextPin = (fields.postalCode || '').replace(/\D/g, '').slice(0, 6);
      setAddressDraft((prev) => ({
        ...prev,
        addressLine: fields.line1 || prev.addressLine,
        line2: fields.line2 ?? prev.line2,
        city: fields.city || prev.city,
        state: fields.state || prev.state,
        pincode: nextPin || prev.pincode,
      }));
      if (/^[1-9]\d{5}$/.test(nextPin)) {
        setQuickPincodeInput(nextPin);
        const srv = await checkShiprocketServiceability(nextPin);
        setAddressEditPinPreview(srv);
        if (srv) {
          setPincodeStatusMessage(srv.riderMessage);
        }
      }
    },
    []
  );

  const reverseGeocodeCoords = useCallback(
    async (lat: number, lng: number) => {
      const coords = { lat, lng };
      setPinPosition(coords);
      setMapCenter(coords);
      setMapZoom(FOCUSED_ZOOM);
      setHasLockedPin(true);
      setIsGeocoding(true);
      try {
        const result = await callServerGeocode(
          `lat=${encodeURIComponent(lat)}&lng=${encodeURIComponent(lng)}`
        );
        if (result) {
          await applyGeocodedFields(result, coords);
        } else {
          setResolvedBanner(`Pinned at ${lat.toFixed(4)}, ${lng.toFixed(4)}`);
        }
      } finally {
        setIsGeocoding(false);
      }
    },
    [applyGeocodedFields]
  );

  const handleUseCurrentLocation = () => {
    haptics.light();
    if (typeof navigator === 'undefined' || !navigator.geolocation) {
      onToast('Location detection unavailable — enter PIN or search below', 'info');
      return;
    }
    setIsDetectingLocation(true);
    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        const lat = pos.coords.latitude;
        const lng = pos.coords.longitude;
        await reverseGeocodeCoords(lat, lng);
        setIsDetectingLocation(false);
        onToast('Map centered on your current location', {
          title: 'Location Locked',
          variant: 'success',
        });
      },
      () => {
        setIsDetectingLocation(false);
        onToast('Location permission denied — search on map or enter PIN', 'warning');
      },
      { enableHighAccuracy: true, timeout: 8000 }
    );
  };

  const handleFallbackForwardSearch = async () => {
    const q = fallbackSearchQuery.trim();
    if (q.length < 3) return;
    setIsGeocoding(true);
    try {
      const result = await callServerGeocode(`address=${encodeURIComponent(q)}`);
      if (result) {
        const coords =
          result.lat != null && result.lng != null ? { lat: result.lat, lng: result.lng } : null;
        await applyGeocodedFields(result, coords);
      } else {
        onToast('No matching address found — try a nearby landmark or 6-digit PIN', 'warning');
      }
    } finally {
      setIsGeocoding(false);
    }
  };

  const startEditingAddress = (addr: SavedAddress) => {
    haptics.light();
    setEditingAddressId(addr.id);
    setAddressDraftError(null);
    setAddressEditPinPreview(null);
    setAddressDraft({
      label: addr.label || 'Home',
      fullName: addr.fullName,
      phone: addr.phone,
      addressLine: addr.addressLine,
      line2: addr.line2 || '',
      city: addr.city,
      state: addr.state || '',
      pincode: addr.pincode,
    });
    window.setTimeout(() => {
      addressFormRef.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }, 80);
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-0 sm:p-4 bg-black/60 backdrop-blur-xs">
      <div className="w-full max-w-lg max-h-[92vh] overflow-y-auto bg-white dark:bg-[#211A17] rounded-t-3xl sm:rounded-3xl p-4 sm:p-5 border border-orange-100 dark:border-stone-800 shadow-2xl space-y-3.5">
        <div className="flex items-center justify-between border-b border-orange-100/70 dark:border-stone-800 pb-3">
          <div className="min-w-0">
            <h3 className="font-display font-black text-sm text-slate-900 dark:text-white flex items-center gap-2">
              <MapPin className="w-4 h-4 text-[#FF5722] shrink-0" />
              <span>Select Delivery Location</span>
            </h3>
            <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5 truncate">
              {activeAddressObj
                ? `Delivering to ${activeAddressObj.city} - ${activeAddressObj.pincode}`
                : 'Drop pin on map, use GPS, or enter 6-digit PIN'}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close location picker"
            className="p-1 text-slate-400 hover:text-slate-700 dark:hover:text-white rounded-lg cursor-pointer shrink-0"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Flipkart-style Interactive Google Map + Places Autocomplete */}
        <div className="space-y-2.5 p-3 rounded-2xl bg-[#FFF4E8]/80 dark:bg-[#181311] border border-orange-200/70 dark:border-stone-800">
          {mapsApiKey ? (
            <APIProvider apiKey={mapsApiKey} language="en" region="IN">
              <PlacesAutocompleteSearch onSelectPlace={applyGeocodedFields} />

              <div className="relative w-full h-52 sm:h-60 rounded-2xl overflow-hidden border border-orange-200/80 dark:border-stone-700 bg-stone-100 dark:bg-stone-900">
                <GoogleMapView
                  mapId="DEMO_MAP_ID"
                  defaultCenter={DEFAULT_INDIA_CENTER}
                  center={hasLockedPin ? mapCenter : undefined}
                  defaultZoom={DEFAULT_INDIA_ZOOM}
                  zoom={hasLockedPin ? mapZoom : undefined}
                  gestureHandling="greedy"
                  disableDefaultUI={true}
                  internalUsageAttributionIds={['gmp_mcp_codeassist_v1_aistudio']}
                  style={{ width: '100%', height: '100%' }}
                  onClick={(ev) => {
                    const latLng = ev.detail.latLng;
                    if (latLng) {
                      haptics.light();
                      reverseGeocodeCoords(latLng.lat, latLng.lng);
                    }
                  }}
                >
                  <AdvancedMarker
                    position={pinPosition}
                    draggable={true}
                    onDragEnd={(ev) => {
                      if (ev.latLng) {
                        haptics.light();
                        reverseGeocodeCoords(ev.latLng.lat(), ev.latLng.lng());
                      }
                    }}
                  />
                </GoogleMapView>

                <button
                  type="button"
                  disabled={isDetectingLocation}
                  onClick={handleUseCurrentLocation}
                  className="absolute bottom-2.5 right-2.5 px-3 py-1.5 rounded-xl bg-white/95 dark:bg-[#211A17]/95 border border-orange-200 dark:border-stone-700 shadow-md text-[11px] font-black text-[#FF5722] flex items-center gap-1.5 hover:bg-orange-50 cursor-pointer"
                >
                  {isDetectingLocation ? (
                    <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  ) : (
                    <LocateFixed className="w-3.5 h-3.5" />
                  )}
                  <span>{isDetectingLocation ? 'Locating…' : 'Use Current Location'}</span>
                </button>
              </div>
            </APIProvider>
          ) : (
            <div className="space-y-2">
              <div className="flex items-center gap-2">
                <input
                  type="text"
                  value={fallbackSearchQuery}
                  onChange={(e) => setFallbackSearchQuery(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') {
                      e.preventDefault();
                      handleFallbackForwardSearch();
                    }
                  }}
                  placeholder="Search street, locality, landmark or city..."
                  className="flex-1 text-xs px-3 py-2 rounded-xl bg-white dark:bg-[#211A17] border border-orange-200/80 dark:border-stone-700 text-slate-900 dark:text-white outline-none focus:border-[#FF5722]"
                />
                <button
                  type="button"
                  disabled={isGeocoding}
                  onClick={handleFallbackForwardSearch}
                  className="px-3 py-2 rounded-xl bg-[#FF5722] text-white text-xs font-bold cursor-pointer shrink-0"
                >
                  {isGeocoding ? '…' : 'Find'}
                </button>
              </div>
              <button
                type="button"
                disabled={isDetectingLocation}
                onClick={handleUseCurrentLocation}
                className="w-full py-2 px-3 rounded-xl bg-white dark:bg-[#211A17] border border-orange-200/70 dark:border-stone-700 text-xs font-bold text-[#FF5722] flex items-center justify-center gap-1.5 hover:bg-orange-50/70 dark:hover:bg-stone-800 transition-colors cursor-pointer"
              >
                <LocateFixed className="w-3.5 h-3.5" />
                <span>{isDetectingLocation ? 'Detecting GPS location...' : 'Use Current Location (GPS)'}</span>
              </button>
            </div>
          )}

          {(isGeocoding || resolvedBanner) && (
            <div className="flex items-start gap-2 px-2.5 py-2 rounded-xl bg-white/90 dark:bg-[#211A17] border border-orange-100 dark:border-stone-800 text-[11px]">
              <Navigation className="w-3.5 h-3.5 text-emerald-600 shrink-0 mt-0.5" />
              <div className="min-w-0 flex-1">
                <span className="font-bold text-slate-900 dark:text-white block">
                  {isGeocoding ? 'Fetching street & pincode from Google Maps…' : 'Selected Map Pin'}
                </span>
                {resolvedBanner && !isGeocoding && (
                  <p className="text-[10px] text-slate-600 dark:text-slate-300 leading-snug">
                    {resolvedBanner}
                  </p>
                )}
              </div>
            </div>
          )}
        </div>

        {/* Quick 6-digit Delivery Pincode Checker */}
        <div className="p-3 rounded-2xl bg-[#FFF4E8] dark:bg-[#181311] border border-orange-200/70 dark:border-stone-800 space-y-2">
          <label className="text-[11px] font-black text-slate-800 dark:text-slate-200 block">
            Check Delivery Pincode
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
                  onToast('Enter a valid 6-digit pincode', 'error');
                  return;
                }
                haptics.saved();
                setPincodeStatusMessage(`Checking delivery serviceability for PIN ${cleanPin}…`);
                const res = await checkShiprocketServiceability(cleanPin);
                setAddressDraft((prev) => ({ ...prev, pincode: cleanPin }));
                const existingMatch = savedAddresses.find((a) => a.pincode === cleanPin);
                if (existingMatch) {
                  onSetActiveAddressId(existingMatch.id);
                }
                const msg = res ? res.riderMessage : `Serviceability checked for PIN ${cleanPin}`;
                setPincodeStatusMessage(msg);
                onToast(msg, {
                  title: 'Delivery Pincode Checked',
                  variant: res && !res.serviceable ? 'warning' : 'success',
                });
              }}
              className="px-3.5 py-2 rounded-xl bg-[#FF5722] hover:bg-[#E64A19] text-white text-xs font-black cursor-pointer shrink-0"
            >
              Check
            </button>
          </div>
          {pincodeStatusMessage && (
            <p className="text-[11px] font-semibold text-emerald-700 dark:text-emerald-400">
              {pincodeStatusMessage}
            </p>
          )}
        </div>

        {/* Saved Addresses */}
        {savedAddresses.length > 0 && (
          <div className="space-y-2">
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
                    onSetActiveAddressId(addr.id);
                    setQuickPincodeInput(addr.pincode);
                    onToast(`Delivering to ${addr.label} (${addr.city} - ${addr.pincode})`, {
                      title: 'Delivery Address Changed',
                      variant: 'success',
                    });
                    onClose();
                  }}
                  className={`p-3 rounded-2xl border cursor-pointer transition-all ${
                    isActive
                      ? 'bg-[#FFF4E8] dark:bg-orange-950/30 border-[#FF5722]'
                      : 'bg-stone-50 dark:bg-[#181311] border-orange-100/70 dark:border-stone-800'
                  }`}
                >
                  <div className="flex items-center justify-between gap-2">
                    <div className="flex items-center gap-1.5 min-w-0">
                      <span className="px-2 py-0.5 text-[10px] font-black rounded bg-[#FF5722] text-white shrink-0">
                        {addr.label}
                      </span>
                      <span className="text-xs font-bold text-slate-900 dark:text-white truncate">
                        {addr.fullName}
                      </span>
                    </div>
                    <div className="flex items-center gap-2 shrink-0">
                      {isActive && (
                        <span className="text-[10px] font-bold text-[#E64A19] dark:text-orange-300 flex items-center gap-0.5">
                          <CheckCircle2 className="w-3 h-3" /> Selected
                        </span>
                      )}
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          startEditingAddress(addr);
                        }}
                        className="text-[11px] font-bold text-[#FF5722] hover:underline cursor-pointer"
                      >
                        {editingAddressId === addr.id ? 'Editing…' : 'Edit'}
                      </button>
                      <button
                        type="button"
                        onClick={async (e) => {
                          e.stopPropagation();
                          haptics.warning();
                          const remaining = savedAddresses.filter((a) => a.id !== addr.id);
                          onSetSavedAddresses(remaining);
                          if (activeAddressObj?.id === addr.id) {
                            onSetActiveAddressId(remaining[0]?.id || '');
                          }
                          if (editingAddressId === addr.id) {
                            setEditingAddressId(null);
                          }
                          const token = getStoredAuthToken();
                          if (
                            token &&
                            /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(
                              addr.id
                            )
                          ) {
                            try {
                              await fetch(
                                `${resolveApiBaseUrl()}/v1/addresses/${encodeURIComponent(addr.id)}`,
                                {
                                  method: 'DELETE',
                                  headers: { Authorization: `Bearer ${token}` },
                                }
                              );
                            } catch {
                              // Local state already updated
                            }
                          }
                          onToast('Address removed', 'warning');
                        }}
                        className="text-[11px] font-bold text-rose-500 hover:underline cursor-pointer"
                      >
                        Delete
                      </button>
                    </div>
                  </div>
                  <p className="text-xs text-slate-600 dark:text-slate-300 mt-1.5">
                    {addr.addressLine}
                    {addr.line2 ? `, ${addr.line2}` : ''}, {addr.city}
                    {addr.state ? `, ${addr.state}` : ''} - {addr.pincode}
                  </p>
                  {addr.phone && (
                    <p className="text-[10px] text-slate-500 dark:text-slate-400 mt-0.5 tabular-nums">
                      {addr.phone}
                    </p>
                  )}
                </div>
              );
            })}
          </div>
        )}

        {/* Save & Deliver Here Form */}
        <form
          ref={addressFormRef}
          onSubmit={async (e) => {
            e.preventDefault();
            setAddressDraftError(null);
            const cleanName = addressDraft.fullName.trim();
            const cleanLine1 = addressDraft.addressLine.trim();
            const cleanLine2 = addressDraft.line2.trim();
            const cleanCity = addressDraft.city.trim();
            const cleanState = addressDraft.state.trim() || 'India';
            const cleanPin = addressDraft.pincode.trim();
            const rawDigits = addressDraft.phone.replace(/\D/g, '');
            const cleanPhone =
              rawDigits.startsWith('91') && rawDigits.length === 12
                ? rawDigits.slice(2)
                : rawDigits;

            if (!cleanName || !cleanLine1 || !cleanCity) {
              setAddressDraftError('Full name, street address, and city are required.');
              return;
            }
            if (!/^[1-9]\d{5}$/.test(cleanPin)) {
              setAddressDraftError('Enter a valid 6-digit Indian PIN code.');
              return;
            }
            if (cleanPhone && !/^[6-9]\d{9}$/.test(cleanPhone)) {
              setAddressDraftError('Enter a valid 10-digit Indian mobile number.');
              return;
            }

            setIsSavingAddressModal(true);
            haptics.saved();

            const draftPayload: SavedAddress = {
              id: editingAddressId || `addr-${Date.now()}`,
              label: addressDraft.label.trim() || 'Home',
              fullName: cleanName,
              phone: cleanPhone || userProfile.phone || '',
              addressLine: cleanLine1,
              line2: cleanLine2 || undefined,
              city: cleanCity,
              state: cleanState,
              pincode: cleanPin,
              isDefault: true,
            };

            const commitAddress = (savedItem: SavedAddress) => {
              if (editingAddressId) {
                onSetSavedAddresses((prev) =>
                  prev.map((a) => (a.id === editingAddressId ? savedItem : a))
                );
                onSetActiveAddressId(savedItem.id);
                setEditingAddressId(null);
                onToast(`Delivering to ${savedItem.city} - ${savedItem.pincode}`, {
                  title: 'Address Updated',
                  variant: 'success',
                });
              } else {
                onSetSavedAddresses((prev) => [savedItem, ...prev]);
                onSetActiveAddressId(savedItem.id);
                onToast(`Delivering to ${savedItem.city} - ${savedItem.pincode}`, {
                  title: 'Address Saved',
                  variant: 'success',
                });
              }
              onClose();
            };

            const token = getStoredAuthToken();
            if (!token || !draftPayload.phone) {
              commitAddress(draftPayload);
              setIsSavingAddressModal(false);
              return;
            }

            try {
              const isRemoteEdit = Boolean(
                editingAddressId &&
                  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(
                    editingAddressId
                  )
              );
              const url = isRemoteEdit
                ? `${resolveApiBaseUrl()}/v1/addresses/${encodeURIComponent(editingAddressId!)}`
                : `${resolveApiBaseUrl()}/v1/addresses`;
              const resp = await fetch(url, {
                method: isRemoteEdit ? 'PUT' : 'POST',
                headers: {
                  'Content-Type': 'application/json',
                  Accept: 'application/json',
                  Authorization: `Bearer ${token}`,
                },
                body: JSON.stringify({
                  label: draftPayload.label,
                  fullName: draftPayload.fullName,
                  phone: draftPayload.phone,
                  line1: draftPayload.addressLine,
                  line2: draftPayload.line2 || '',
                  city: draftPayload.city,
                  state: draftPayload.state || 'India',
                  postalCode: draftPayload.pincode,
                  country: 'IN',
                  isDefault: true,
                }),
              });

              if (resp.ok) {
                const row = (await resp.json()) as Record<string, unknown>;
                commitAddress({
                  id: String(row.id || draftPayload.id),
                  label: String(row.label || draftPayload.label),
                  fullName: String(row.full_name || draftPayload.fullName),
                  phone: String(row.phone || draftPayload.phone),
                  addressLine: String(row.line1 || draftPayload.addressLine),
                  line2: row.line2 ? String(row.line2) : draftPayload.line2,
                  city: String(row.city || draftPayload.city),
                  state: String(row.state || draftPayload.state || ''),
                  pincode: String(row.postal_code || draftPayload.pincode),
                  isDefault: Boolean(row.is_default ?? true),
                });
              } else {
                commitAddress(draftPayload);
              }
            } catch {
              commitAddress(draftPayload);
            } finally {
              setIsSavingAddressModal(false);
            }
          }}
          className={`p-3 rounded-2xl border space-y-2.5 transition-colors ${
            editingAddressId
              ? 'bg-orange-50/90 dark:bg-orange-950/25 border-[#FF5722]'
              : 'bg-[#FFF4E8]/60 dark:bg-[#181311] border-orange-200/70 dark:border-stone-800'
          }`}
        >
          <div className="flex items-center justify-between gap-2">
            <p className="text-xs font-black text-slate-900 dark:text-white">
              {editingAddressId
                ? `Edit "${addressDraft.label || 'Saved'}" Address`
                : 'Complete Doorstep Address'}
            </p>
            {editingAddressId && (
              <button
                type="button"
                onClick={() => {
                  setEditingAddressId(null);
                  setAddressDraftError(null);
                  setAddressEditPinPreview(null);
                }}
                className="text-[11px] font-bold text-slate-500 hover:text-slate-800 dark:hover:text-white cursor-pointer"
              >
                Cancel Edit
              </button>
            )}
          </div>

          <div className="flex items-center gap-1.5">
            {(['Home', 'Work', 'Other'] as const).map((lbl) => (
              <button
                key={lbl}
                type="button"
                onClick={() => setAddressDraft({ ...addressDraft, label: lbl })}
                className={`px-2.5 py-1 rounded-lg text-[11px] font-bold transition-colors cursor-pointer ${
                  addressDraft.label === lbl
                    ? 'bg-[#FF5722] text-white'
                    : 'bg-white dark:bg-[#211A17] text-slate-600 dark:text-slate-300 border border-orange-200/70 dark:border-stone-700'
                }`}
              >
                {lbl}
              </button>
            ))}
          </div>

          <div className="grid grid-cols-2 gap-2">
            <input
              type="text"
              required
              placeholder="Full Name"
              value={addressDraft.fullName}
              onChange={(e) => setAddressDraft({ ...addressDraft, fullName: e.target.value })}
              className="text-xs px-3 py-2 rounded-xl bg-white dark:bg-[#211A17] border border-orange-200/70 dark:border-stone-700 outline-none focus:border-[#FF5722]"
            />
            <input
              type="tel"
              inputMode="numeric"
              maxLength={10}
              placeholder="10-digit Mobile"
              value={addressDraft.phone}
              onChange={(e) =>
                setAddressDraft({
                  ...addressDraft,
                  phone: e.target.value.replace(/\D/g, '').slice(0, 10),
                })
              }
              className="text-xs tabular-nums px-3 py-2 rounded-xl bg-white dark:bg-[#211A17] border border-orange-200/70 dark:border-stone-700 outline-none focus:border-[#FF5722]"
            />
          </div>

          <input
            type="text"
            required
            placeholder="House / Building / Street"
            value={addressDraft.addressLine}
            onChange={(e) => setAddressDraft({ ...addressDraft, addressLine: e.target.value })}
            className="w-full text-xs px-3 py-2 rounded-xl bg-white dark:bg-[#211A17] border border-orange-200/70 dark:border-stone-700 outline-none focus:border-[#FF5722]"
          />

          <input
            type="text"
            placeholder="Locality / Landmark (Optional)"
            value={addressDraft.line2}
            onChange={(e) => setAddressDraft({ ...addressDraft, line2: e.target.value })}
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
              placeholder="State"
              value={addressDraft.state}
              onChange={(e) => setAddressDraft({ ...addressDraft, state: e.target.value })}
              className="text-xs px-3 py-2 rounded-xl bg-white dark:bg-[#211A17] border border-orange-200/70 dark:border-stone-700 outline-none focus:border-[#FF5722]"
            />
            <input
              type="text"
              inputMode="numeric"
              required
              maxLength={6}
              placeholder="6-digit PIN"
              value={addressDraft.pincode}
              onChange={async (e) => {
                const nextPin = e.target.value.replace(/\D/g, '').slice(0, 6);
                setAddressDraft({ ...addressDraft, pincode: nextPin });
                if (/^[1-9]\d{5}$/.test(nextPin)) {
                  const res = await checkShiprocketServiceability(nextPin);
                  setAddressEditPinPreview(res);
                } else {
                  setAddressEditPinPreview(null);
                }
              }}
              className="text-xs tabular-nums px-3 py-2 rounded-xl bg-white dark:bg-[#211A17] border border-orange-200/70 dark:border-stone-700 outline-none focus:border-[#FF5722]"
            />
          </div>

          {addressEditPinPreview && (
            <p
              className={`text-[11px] font-semibold ${
                addressEditPinPreview.serviceable
                  ? 'text-emerald-700 dark:text-emerald-400'
                  : 'text-amber-700 dark:text-amber-400'
              }`}
            >
              {addressEditPinPreview.riderMessage}
            </p>
          )}
          {addressDraftError && (
            <p role="alert" className="text-[11px] font-semibold text-rose-600 dark:text-rose-400">
              {addressDraftError}
            </p>
          )}

          <button
            type="submit"
            disabled={isSavingAddressModal}
            className="w-full min-h-[42px] py-2 bg-[#FF5722] hover:bg-[#E64A19] disabled:opacity-60 text-white text-xs font-black rounded-xl cursor-pointer"
          >
            {isSavingAddressModal
              ? 'Saving Address...'
              : editingAddressId
              ? 'Save & Deliver Here'
              : 'Save & Deliver Here'}
          </button>
        </form>
      </div>
    </div>
  );
};
