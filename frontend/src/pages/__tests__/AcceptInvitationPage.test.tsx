import { render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import AcceptInvitationPage from "../AcceptInvitationPage";
import { useAuth } from "../../auth/AuthContext";
import { buildAuthorizeUrl } from "../../auth/auth";
import { useMutation } from "@tanstack/react-query";

vi.mock("../../auth/AuthContext");
vi.mock("../../auth/auth");
vi.mock("@tanstack/react-query");

const mockedUseAuth = vi.mocked(useAuth);
const mockedUseMutation = vi.mocked(useMutation);
const mockedBuildAuthorizeUrl = vi.mocked(buildAuthorizeUrl);

describe("AcceptInvitationPage", () => {
  beforeEach(() => {
    mockedBuildAuthorizeUrl.mockResolvedValue("https://login.example.com");
    Object.defineProperty(window, "location", {
      value: { assign: vi.fn() },
      writable: true
    });
    localStorage.clear();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  const renderWithRoute = (path: string) =>
    render(
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/accept" element={<AcceptInvitationPage />} />
          <Route path="*" element={<div>fallback</div>} />
        </Routes>
      </MemoryRouter>
    );

  it("shows error when token is missing", () => {
    mockedUseAuth.mockReturnValue({
      isAuthenticated: false,
      state: { tokens: null, profile: null },
      login: vi.fn(),
      handleCallback: vi.fn(),
      logout: vi.fn()
    });
    mockedUseMutation.mockReturnValue({} as any);

    renderWithRoute("/accept");
    expect(screen.getByText(/invalid invitation link/i)).toBeInTheDocument();
  });

  it("calls mutate when authenticated with token", async () => {
    const mutate = vi.fn();
    mockedUseAuth.mockReturnValue({
      isAuthenticated: true,
      state: { tokens: null, profile: {} },
      login: vi.fn(),
      handleCallback: vi.fn(),
      logout: vi.fn()
    });
    mockedUseMutation.mockReturnValue({
      mutate,
      isPending: false,
      isSuccess: false,
      isError: false
    } as any);

    renderWithRoute("/accept?token=abc123");

    await waitFor(() => {
      expect(mutate).toHaveBeenCalledWith("abc123");
    });
  });

  it("stores token and redirects when unauthenticated", async () => {
    mockedUseAuth.mockReturnValue({
      isAuthenticated: false,
      state: { tokens: null, profile: null },
      login: vi.fn(),
      handleCallback: vi.fn(),
      logout: vi.fn()
    });
    mockedUseMutation.mockReturnValue({
      mutate: vi.fn(),
      isPending: false,
      isSuccess: false,
      isError: false
    } as any);

    renderWithRoute("/accept?token=token-xyz");

    await waitFor(() => {
      expect(localStorage.getItem("pending-invitation-token")).toBe("token-xyz");
      expect(window.location.assign).toHaveBeenCalledWith("https://login.example.com");
    });
  });
});
