import { apiRequest, ApiError } from "./client";
import { clearTokens, loadTokens, saveTokens } from "../auth/storage";
import { refreshTokens } from "../auth/auth";
import { vi, type Mock } from "vitest";

vi.mock("../auth/storage");
vi.mock("../auth/auth");

describe("apiRequest", () => {
  const mockedLoadTokens = vi.mocked(loadTokens);
  const mockedClearTokens = vi.mocked(clearTokens);
  const mockedSaveTokens = vi.mocked(saveTokens);
  const mockedRefresh = vi.mocked(refreshTokens);

  beforeEach(() => {
    vi.restoreAllMocks();
    mockedLoadTokens.mockReturnValue(null);
    mockedClearTokens.mockReset();
    mockedSaveTokens.mockReset();
    mockedRefresh.mockReset();
    global.fetch = vi.fn() as any;
  });

  it("refreshes when token nearly expired and attaches new bearer token", async () => {
    mockedLoadTokens.mockReturnValue({
      accessToken: "old",
      idToken: "id",
      expiresAt: Math.floor(Date.now() / 1000) - 5, // expired
      refreshToken: "r1"
    });
    mockedRefresh.mockResolvedValue({
      accessToken: "new-token",
      idToken: "id2",
      refreshToken: "r1",
      expiresAt: Math.floor(Date.now() / 1000) + 3600
    });
    (global.fetch as Mock).mockResolvedValue(
      new Response(JSON.stringify({ ok: true }), { status: 200, headers: { "Content-Type": "application/json" } })
    );

    await apiRequest("/hello", { method: "GET" });

    const call = (global.fetch as Mock).mock.calls[0];
    const reqInit = call[1] as RequestInit;
    expect((reqInit.headers as Headers).get("Authorization")).toBe("Bearer new-token");
    expect(mockedSaveTokens).toHaveBeenCalled();
  });

  it("fires auth-failed event and clears tokens on 401", async () => {
    mockedLoadTokens.mockReturnValue({
      accessToken: "old",
      idToken: "id",
      expiresAt: Math.floor(Date.now() / 1000) + 3600,
      refreshToken: "r1"
    });
    const handler = vi.fn();
    window.addEventListener("api:auth-failed", handler);
    (global.fetch as Mock).mockResolvedValue(new Response("", { status: 401 }));

    await expect(apiRequest("/secure")).rejects.toBeInstanceOf(ApiError);

    expect(mockedClearTokens).toHaveBeenCalled();
    expect(handler).toHaveBeenCalled();
  });

  it("fires forbidden event on 403", async () => {
    mockedLoadTokens.mockReturnValue({
      accessToken: "old",
      idToken: "id",
      expiresAt: Math.floor(Date.now() / 1000) + 3600,
      refreshToken: "r1"
    });
    const handler = vi.fn();
    window.addEventListener("api:forbidden", handler);
    (global.fetch as Mock).mockResolvedValue(new Response("", { status: 403 }));

    await expect(apiRequest("/secure")).rejects.toBeInstanceOf(ApiError);

    expect(handler).toHaveBeenCalled();
  });
});
