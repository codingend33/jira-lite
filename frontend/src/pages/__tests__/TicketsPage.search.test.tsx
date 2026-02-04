import { render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { vi } from "vitest";
import TicketsPage from "../TicketsPage";
import * as projectQueries from "../../query/projectQueries";

vi.mock("../../auth/AuthContext", () => ({
  useAuth: () => ({ state: { profile: { email: "me@test.com" } }, isAuthenticated: true })
}));
vi.mock("../../query/projectQueries", () => ({
  useProjects: vi.fn()
}));
vi.mock("../../query/memberQueries", () => ({
  useOrgMembers: () => ({ data: [] })
}));
vi.mock("../../query/ticketQueries", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../../query/ticketQueries")>();
  return {
    ...actual,
    useSearchTickets: vi.fn((keyword: string) => ({
      data: [
        { id: "s1", key: "T-1", title: `Match ${keyword}`, description: "", status: "OPEN", priority: "LOW", projectId: "p1", assigneeId: null }
      ],
      isLoading: false
    })),
    useTickets: vi.fn(() => ({ data: { content: [] }, isLoading: false }))
  };
});

function renderWith(keyword: string) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <MemoryRouter initialEntries={[`/tickets?keyword=${encodeURIComponent(keyword)}`]}>
      <QueryClientProvider client={qc}>
        <TicketsPage />
      </QueryClientProvider>
    </MemoryRouter>
  );
}

describe("TicketsPage search mode", () => {
  it("renders search results without pagination", async () => {
    vi.mocked(projectQueries.useProjects).mockReturnValue({ data: [] } as any);
    renderWith("bug");
    await waitFor(() => expect(screen.getByText(/Match bug/)).toBeInTheDocument());
    expect(screen.queryByText(/page/i)).not.toBeInTheDocument();
  });
});
