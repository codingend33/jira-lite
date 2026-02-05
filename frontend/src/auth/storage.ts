export type AuthTokens = {
  accessToken: string;
  idToken: string;
  expiresAt: number;
  refreshToken?: string;
};

const STORAGE_KEY = "jira-lite-auth";

export function saveTokens(tokens: AuthTokens): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(tokens));
}

export function loadTokens(): AuthTokens | null {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as AuthTokens;
  } catch {
    return null;
  }
}

export function clearTokens(): void {
  localStorage.removeItem(STORAGE_KEY);
}

export function getIdToken(): string | null {
  const tokens = loadTokens();
  if (!tokens) {
    return null;
  }
  return tokens.idToken;
}

/**
 * Deprecated: use ensureAccessToken from api/client instead.
 * Kept for legacy calls (e.g., change password) where caller already ensures validity.
 */
export function getAccessToken(): string | null {
  const tokens = loadTokens();
  if (!tokens) return null;
  if (tokens.expiresAt * 1000 < Date.now()) {
    clearTokens();
    return null;
  }
  return tokens.accessToken;
}
