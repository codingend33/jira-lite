import { apiRequest, ApiError } from "./client";
import { getAccessToken, clearTokens } from "../auth/storage";
import { vi, type Mock } from "vitest";

vi.mock("../auth/storage");

describe("apiRequest", () => {
  const mockedGetAccessToken = vi.mocked(getAccessToken);
  const mockedClearTokens = vi.mocked(clearTokens);

  beforeEach(() => {
    vi.restoreAllMocks();
    vi.useRealTimers();
    mockedGetAccessToken.mockReturnValue(null);
    mockedClearTokens.mockReset();
    global.fetch = vi.fn() as any;
  });

  it("attaches bearer token when available", async () => {
    mockedGetAccessToken.mockReturnValue("test-token");
    (global.fetch as Mock).mockResolvedValue(
      new Response(JSON.stringify({ ok: true }), { status: 200, headers: { "Content-Type": "application/json" } })
    );

    await apiRequest("/hello", { method: "GET" });

    const call = (global.fetch as Mock).mock.calls[0];
    const reqInit = call[1] as RequestInit;
    expect((reqInit.headers as Headers).get("Authorization")).toBe("Bearer test-token");
  });

  it("fires auth-failed event and clears tokens on 401", async () => {
    const handler = vi.fn();
    window.addEventListener("api:auth-failed", handler);
    (global.fetch as Mock).mockResolvedValue(new Response("", { status: 401 }));

    await expect(apiRequest("/secure")).rejects.toBeInstanceOf(ApiError);

    expect(mockedClearTokens).toHaveBeenCalled();
    expect(handler).toHaveBeenCalled();
  });

  it("fires forbidden event on 403", async () => {
    const handler = vi.fn();
    window.addEventListener("api:forbidden", handler);
    (global.fetch as Mock).mockResolvedValue(new Response("", { status: 403 }));

    await expect(apiRequest("/secure")).rejects.toBeInstanceOf(ApiError);

    expect(handler).toHaveBeenCalled();
  });
});
