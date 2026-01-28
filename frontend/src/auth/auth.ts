import { generateChallenge, generateVerifier } from "./pkce";
import { AuthTokens } from "./storage";

const domain = import.meta.env.VITE_COGNITO_DOMAIN;
const clientId = import.meta.env.VITE_COGNITO_CLIENT_ID;
const redirectUri = import.meta.env.VITE_COGNITO_REDIRECT_URI;
const logoutUri = import.meta.env.VITE_COGNITO_LOGOUT_URI;

const verifierKey = "jira-lite-pkce";

export async function buildAuthorizeUrl(identityProvider?: string, state?: string): Promise<string> {
  const verifier = generateVerifier();
  const challenge = await generateChallenge(verifier);
  sessionStorage.setItem(verifierKey, verifier);

  const params = new URLSearchParams({
    response_type: "code",
    client_id: clientId,
    redirect_uri: redirectUri,
    scope: "openid email profile",
    code_challenge_method: "S256",
    code_challenge: challenge
  });

  if (identityProvider) {
    params.set("identity_provider", identityProvider);
  }

  if (state) {
    params.set("state", state);
  }

  return `https://${domain}/oauth2/authorize?${params.toString()}`;
}

export async function exchangeCodeForTokens(code: string): Promise<AuthTokens> {
  const verifier = sessionStorage.getItem(verifierKey);
  if (!verifier) {
    throw new Error("Missing PKCE verifier");
  }

  const body = new URLSearchParams({
    grant_type: "authorization_code",
    client_id: clientId,
    code,
    redirect_uri: redirectUri,
    code_verifier: verifier
  });

  const response = await fetch(`https://${domain}/oauth2/token`, {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded"
    },
    body: body.toString()
  });

  if (!response.ok) {
    throw new Error("Failed to exchange code for token");
  }

  const payload = (await response.json()) as {
    access_token: string;
    id_token: string;
    expires_in: number;
    refresh_token?: string;
  };

  sessionStorage.removeItem(verifierKey);

  return {
    accessToken: payload.access_token,
    idToken: payload.id_token,
    refreshToken: payload.refresh_token,
    expiresAt: Math.floor(Date.now() / 1000) + payload.expires_in
  };
}

export function buildLogoutUrl(): string {
  const params = new URLSearchParams({
    client_id: clientId,
    logout_uri: logoutUri
  });
  return `https://${domain}/logout?${params.toString()}`;
}

export type JwtProfile = {
  sub?: string;
  email?: string;
  name?: string;
  "cognito:groups"?: string[];
};

export function decodeJwt(token: string): JwtProfile {
  const parts = token.split(".");
  if (parts.length < 2) {
    return {};
  }
  const payload = parts[1].replace(/-/g, "+").replace(/_/g, "/");
  const decoded = atob(payload + "==".slice((payload.length + 2) % 4));
  try {
    return JSON.parse(decoded) as JwtProfile;
  } catch {
    return {};
  }
}
