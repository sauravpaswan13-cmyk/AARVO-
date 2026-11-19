export interface SafeAuthSessionInfo {
  hasToken: boolean;
  isExpired: boolean;
  role: string | null;
  subjectHint: string | null;
  expiresAtMs: number | null;
}

const TOKEN_STORAGE_KEYS = ['aarvo_auth_token', 'aarvo_token', 'token'] as const;

export function getStoredAuthToken(): string | null {
  if (typeof window === 'undefined') return null;
  try {
    for (const key of TOKEN_STORAGE_KEYS) {
      const value = window.localStorage.getItem(key)?.trim();
      if (value) return value;
    }
  } catch {
    // Ignore storage access errors in restricted frames
  }
  return null;
}

export function resolveApiBaseUrl(explicitBase?: string): string {
  if (explicitBase !== undefined && explicitBase.trim()) {
    return explicitBase.trim().replace(/\/$/, '');
  }
  const envBase =
    (typeof import.meta !== 'undefined' &&
      (import.meta as unknown as { env?: Record<string, string> }).env?.VITE_API_BASE_URL) ||
    'https://aarvo-api.onrender.com';
  return envBase.replace(/\/$/, '');
}

function decodeBase64UrlPayload(segment: string): Record<string, unknown> | null {
  try {
    const normalized = segment.replace(/-/g, '+').replace(/_/g, '/');
    const padded = normalized + '='.repeat((4 - (normalized.length % 4)) % 4);
    const jsonText = atob(padded);
    const parsed = JSON.parse(jsonText);
    return parsed && typeof parsed === 'object' ? (parsed as Record<string, unknown>) : null;
  } catch {
    return null;
  }
}

export function inspectAuthTokenSafely(token: string | null): SafeAuthSessionInfo {
  if (!token) {
    return {
      hasToken: false,
      isExpired: false,
      role: null,
      subjectHint: null,
      expiresAtMs: null,
    };
  }

  const parts = token.split('.');
  if (parts.length !== 3) {
    return {
      hasToken: true,
      isExpired: false,
      role: null,
      subjectHint: null,
      expiresAtMs: null,
    };
  }

  const payload = decodeBase64UrlPayload(parts[1]);
  if (!payload) {
    return {
      hasToken: true,
      isExpired: false,
      role: null,
      subjectHint: null,
      expiresAtMs: null,
    };
  }

  const expSeconds = typeof payload.exp === 'number' ? payload.exp : null;
  const expiresAtMs = expSeconds ? expSeconds * 1000 : null;
  const isExpired = expiresAtMs !== null ? Date.now() >= expiresAtMs : false;
  const rawSub = typeof payload.sub === 'string' ? payload.sub : null;
  const subjectHint = rawSub && rawSub.length > 6 ? `${rawSub.slice(0, 4)}…${rawSub.slice(-4)}` : rawSub;
  const role = typeof payload.role === 'string' ? payload.role : null;

  return {
    hasToken: true,
    isExpired,
    role,
    subjectHint,
    expiresAtMs,
  };
}
