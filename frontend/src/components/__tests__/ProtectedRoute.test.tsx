import { render } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useAuth } from "../../auth/AuthContext";
import ProtectedRoute from "../ProtectedRoute";

vi.mock("../../auth/AuthContext");
const mockedUseAuth = vi.mocked(useAuth);

const renderWithRouter = (initialPath = "/projects") =>
  render(
    <MemoryRouter initialEntries={[initialPath]}>
      <Routes>
        <Route path="/login" element={<div>login</div>} />
        <Route path="/create-org" element={<div>create-org</div>} />
        <Route path="*" element={<ProtectedRoute><div>ok</div></ProtectedRoute>} />
      </Routes>
    </MemoryRouter>
  );

beforeEach(() => {
  mockedUseAuth.mockReset();
});

describe("ProtectedRoute", () => {
  it("renders children when authenticated and has org", () => {
    mockedUseAuth.mockReturnValue({
      isAuthenticated: true,
      state: { profile: { "custom:org_id": "org-1" } },
      login: vi.fn(),
      handleCallback: vi.fn(),
      logout: vi.fn()
    });
    const { getByText } = renderWithRouter();
    expect(getByText("ok")).toBeInTheDocument();
  });

  it("redirects to /create-org when missing org_id", () => {
    mockedUseAuth.mockReturnValue({
      isAuthenticated: true,
      state: { profile: {} },
      login: vi.fn(),
      handleCallback: vi.fn(),
      logout: vi.fn()
    });
    const { getByText } = renderWithRouter();
    expect(getByText("create-org")).toBeInTheDocument();
  });

  it("redirects to /login when unauthenticated", () => {
    mockedUseAuth.mockReturnValue({
      isAuthenticated: false,
      state: { profile: null },
      login: vi.fn(),
      handleCallback: vi.fn(),
      logout: vi.fn()
    });
    const { getByText } = renderWithRouter();
    expect(getByText("login")).toBeInTheDocument();
  });
});
