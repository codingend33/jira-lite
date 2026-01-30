import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest, ApiError } from "./client";
import * as storage from "../auth/storage";

const originalFetch = globalThis.fetch;

describe("apiRequest", () => {
  beforeEach(() => {
    globalThis.fetch = vi.fn() as any;
    vi.spyOn(storage, "getAccessToken").mockReturnValue("token-123");
    vi.spyOn(storage, "clearTokens").mockImplementation(() => {});
  });

  afterEach(() => {
    globalThis.fetch = originalFetch;
    vi.restoreAllMocks();
  });

  it("adds Authorization header when token exists and returns JSON", async () => {
    (globalThis.fetch as any).mockResolvedValue(
      new Response(JSON.stringify({ ok: true }), { status: 200 })
    );
    const result = await apiRequest<{ ok: boolean }>("/path");
    expect(result.ok).toBe(true);
    const headers = (globalThis.fetch as any).mock.calls[0][1].headers;
    expect(headers.get("Authorization")).toBe("Bearer token-123");
  });

  it("dispatches auth-failed on 401 and throws ApiError", async () => {
    const dispatchSpy = vi.spyOn(window, "dispatchEvent");
    (globalThis.fetch as any).mockResolvedValue(new Response("", { status: 401 }));
    await expect(apiRequest("/path")).rejects.toBeInstanceOf(ApiError);
    expect(storage.clearTokens).toHaveBeenCalled();
    expect(dispatchSpy).toHaveBeenCalledWith(
      expect.objectContaining({ type: "api:auth-failed" })
    );
  });

  it("dispatches forbidden on 403", async () => {
    const dispatchSpy = vi.spyOn(window, "dispatchEvent");
    (globalThis.fetch as any).mockResolvedValue(new Response("", { status: 403 }));
    await expect(apiRequest("/path")).rejects.toBeInstanceOf(ApiError);
    expect(dispatchSpy).toHaveBeenCalledWith(
      expect.objectContaining({ type: "api:forbidden" })
    );
  });

  it("parses error payload and exposes message", async () => {
    const error = { message: "Boom" };
    (globalThis.fetch as any).mockResolvedValue(
      new Response(JSON.stringify(error), { status: 500 })
    );
    await expect(apiRequest("/path")).rejects.toMatchObject({
      status: 500,
      message: "Boom",
      payload: error
    });
  });
});
