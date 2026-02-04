import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import TrashPage from "../TrashPage";
import * as trashQueries from "../../query/trashQueries";
import { useAuth } from "../../auth/AuthContext";

vi.mock("../../auth/AuthContext");
vi.mock("../../query/trashQueries");

const qcWrapper = ({ children }: { children: React.ReactNode }) => {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return (
    <QueryClientProvider client={client}>
      <MemoryRouter>{children}</MemoryRouter>
    </QueryClientProvider>
  );
};

const mockUseAuth = vi.mocked(useAuth);

describe("TrashPage", () => {
  const items = [
    { id: "p1", type: "PROJECT", name: "Proj One", key: "P-1", deletedAt: new Date().toISOString(), daysRemaining: 10 },
    { id: "t1", type: "TICKET", name: "Ticket One", key: "T-1", deletedAt: new Date().toISOString(), daysRemaining: 5 }
  ];

  beforeEach(() => {
    vi.mocked(trashQueries.useTrash).mockReturnValue({ data: items, isLoading: false } as any);
    vi.mocked(trashQueries.useRestoreProject).mockReturnValue({ mutateAsync: vi.fn(), isPending: false, error: null } as any);
    vi.mocked(trashQueries.useRestoreTicket).mockReturnValue({ mutateAsync: vi.fn(), isPending: false, error: null } as any);
  });

  it("shows restore button for admin", () => {
    mockUseAuth.mockReturnValue({ state: { profile: { "cognito:groups": ["ADMIN"] } } } as any);

    render(<TrashPage />, { wrapper: qcWrapper });

    expect(screen.getAllByRole("button", { name: /restore/i })).not.toHaveLength(0);
  });

  it("hides restore button for member", () => {
    mockUseAuth.mockReturnValue({ state: { profile: { "cognito:groups": ["MEMBER"] } } } as any);

    render(<TrashPage />, { wrapper: qcWrapper });

    expect(screen.queryByRole("button", { name: /restore/i })).not.toBeInTheDocument();
  });
});
