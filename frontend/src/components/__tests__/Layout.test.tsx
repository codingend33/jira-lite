import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { vi } from "vitest";
import Layout from "../Layout";
import { ThemeContextProvider } from "../../theme/ThemeContext";
import { AuthContextValue, AuthState } from "../../auth/AuthContext";
import { apiRequest } from "../../api/client";

vi.mock("../../api/client", () => {
  return {
    apiRequest: vi.fn()
  };
});

// Provide AuthContext manually (useAuth will throw otherwise)
vi.mock("../../auth/AuthContext", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../../auth/AuthContext")>();
const mockContext: AuthContextValue = {
  state: {
    tokens: { accessToken: "a", idToken: "i", refreshToken: "r", expiresIn: 3600, expiresAt: Date.now() + 3600_000 },
    profile: { email: "user@test.com" }
  } as AuthState,
    isAuthenticated: true,
    login: vi.fn(),
    handleCallback: vi.fn(),
    logout: vi.fn()
  };
  return {
    ...actual,
    useAuth: vi.fn(() => mockContext)
  };
});

const mockedApiRequest = vi.mocked(apiRequest);

function renderWithProviders() {
  const qc = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } }
  });
  return render(
    <ThemeContextProvider>
      <QueryClientProvider client={qc}>
        <MemoryRouter>
          <Layout />
        </MemoryRouter>
      </QueryClientProvider>
    </ThemeContextProvider>
  );
}

describe("Layout", () => {
  afterEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it("shows unread notifications and marks them as read", async () => {
    mockedApiRequest.mockResolvedValueOnce([
      { id: "1", content: "Message", isRead: false, createdAt: new Date().toISOString() }
    ]);
    mockedApiRequest.mockResolvedValueOnce([]); // mark read response

    renderWithProviders();

    // open menu
    fireEvent.click(screen.getByRole("button", { name: /notifications/i }));

    expect(await screen.findByText("Message")).toBeInTheDocument();
    fireEvent.click(screen.getByText("Message"));

    await waitFor(() => {
      expect(mockedApiRequest).toHaveBeenCalledWith("/notifications/1/read", { method: "PATCH" });
    });
  });

  it("toggles dark mode and persists to localStorage", () => {
    mockedApiRequest.mockResolvedValue([]);
    renderWithProviders();

    const toggle = screen.getByRole("button", { name: /toggle theme/i });
    fireEvent.click(toggle);
    expect(localStorage.getItem("jira-lite-theme")).toBe("dark");
  });
});
