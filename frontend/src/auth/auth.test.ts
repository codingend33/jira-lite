import { beforeEach, describe, expect, it, vi } from "vitest";
import { buildAuthorizeUrl, buildLogoutUrl, decodeJwt, exchangeCodeForTokens } from "./auth";
import * as pkce from "./pkce";

describe("decodeJwt", () => {
  it("returns empty object for invalid token", () => {
    const profile = decodeJwt("not-a-jwt");
    expect(profile).toEqual({});
  });

  it("parses payload when token is valid", () => {
    // header {"alg":"none"} payload {"sub":"123","email":"a@example.com"}
    const token =
      "eyJhbGciOiJub25lIn0.eyJzdWIiOiIxMjMiLCJlbWFpbCI6ImFAZXhhbXBsZS5jb20ifQ.";
    const profile = decodeJwt(token);
    expect(profile.sub).toBe("123");
    expect(profile.email).toBe("a@example.com");
  });
});

describe("auth flows", () => {
  beforeEach(() => {
    sessionStorage.clear();
    vi.restoreAllMocks();
  });

  it("buildAuthorizeUrl stores verifier and includes optional params", async () => {
    vi.spyOn(pkce, "generateVerifier").mockReturnValue("verifier");
    vi.spyOn(pkce, "generateChallenge").mockResolvedValue("challenge");
    const url = await buildAuthorizeUrl("Google", "pending");
    expect(url).toContain("identity_provider=Google");
    expect(url).toContain("state=pending");
    expect(sessionStorage.getItem("jira-lite-pkce")).toBe("verifier");
  });

  it("buildLogoutUrl returns logout endpoint", () => {
    const url = buildLogoutUrl();
    expect(url).toContain("/logout?");
  });

  it("exchangeCodeForTokens throws if verifier missing", async () => {
    await expect(exchangeCodeForTokens("code")).rejects.toThrow("Missing PKCE verifier");
  });

  it("exchangeCodeForTokens posts code and returns tokens", async () => {
    sessionStorage.setItem("jira-lite-pkce", "verifier");
    const fetchSpy = vi.spyOn(globalThis, "fetch" as any).mockResolvedValue(
      new Response(
        JSON.stringify({
          access_token: "a",
          id_token: "i",
          refresh_token: "r",
          expires_in: 3600
        }),
        { status: 200 }
      )
    );

    const tokens = await exchangeCodeForTokens("code123");
    expect(fetchSpy).toHaveBeenCalled();
    expect(tokens.accessToken).toBe("a");
    expect(sessionStorage.getItem("jira-lite-pkce")).toBeNull();
  });
});
