import { afterEach, describe, expect, it, vi } from "vitest";
import {
  saveTokens,
  loadTokens,
  getAccessToken,
  clearTokens,
  getIdToken
} from "./storage";

describe("auth/storage", () => {
  afterEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it("saves and loads tokens", () => {
    saveTokens({ accessToken: "a", idToken: "i", expiresAt: 9999999999 });
    expect(loadTokens()).toMatchObject({ accessToken: "a", idToken: "i" });
  });

  it("returns null and clears when expired", () => {
    const past = Math.floor(Date.now() / 1000) - 10;
    saveTokens({ accessToken: "expired", idToken: "id", expiresAt: past });
    expect(getAccessToken()).toBeNull();
    expect(loadTokens()).toBeNull();
  });

  it("gets current access/id token when valid", () => {
    const future = Math.floor(Date.now() / 1000) + 3600;
    saveTokens({ accessToken: "ok", idToken: "id-ok", expiresAt: future });
    expect(getAccessToken()).toBe("ok");
    expect(getIdToken()).toBe("id-ok");
  });

  it("clearTokens removes storage", () => {
    saveTokens({ accessToken: "x", idToken: "y", expiresAt: 999999 });
    clearTokens();
    expect(loadTokens()).toBeNull();
  });
});
