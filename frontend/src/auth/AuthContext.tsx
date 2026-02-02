import React, { createContext, useContext, useMemo, useState } from "react";
import { buildAuthorizeUrl, buildLogoutUrl, decodeJwt, exchangeCodeForTokens } from "./auth";
import { AuthTokens, clearTokens, loadTokens, saveTokens } from "./storage";
import { syncLogin } from "../api/profile";

export type AuthState = {
  tokens?: AuthTokens | null;
  profile?: ReturnType<typeof decodeJwt> | null;
};

export type AuthContextValue = {
  state: AuthState;
  isAuthenticated: boolean;
  login: (identityProvider?: string) => Promise<void>;
  handleCallback: (code: string) => Promise<void>;
  logout: () => void;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<AuthState>(() => {
    const tokens = loadTokens();
    return {
      tokens,
      profile: tokens ? decodeJwt(tokens.idToken) : null
    };
  });

  const handleCallback = async (code: string) => {
    const tokens = await exchangeCodeForTokens(code);
    saveTokens(tokens);
    setState({
      tokens,
      profile: decodeJwt(tokens.idToken)
    });
    // fire and forget; no need to block UI
    syncLogin().catch(() => {
      /* ignore */
    });
  };

  const logout = () => {
    clearTokens();
    setState({ tokens: null, profile: null });
    window.location.assign(buildLogoutUrl());
  };

  const value = useMemo<AuthContextValue>(
    () => ({
      state,
      isAuthenticated: Boolean(state.tokens),
      login: (provider?: string) => buildAuthorizeUrl(provider).then((url) => window.location.assign(url)),
      handleCallback,
      logout
    }),
    [state]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("AuthContext not initialized");
  }
  return ctx;
}
