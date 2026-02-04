import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi, beforeEach } from "vitest";
import ProjectsPage from "../ProjectsPage";
import * as projectQueries from "../../query/projectQueries";
import { useAuth } from "../../auth/AuthContext";
import * as memberQueries from "../../query/memberQueries";

vi.mock("../../auth/AuthContext");
vi.mock("../../query/projectQueries");
vi.mock("../../query/memberQueries");

const mockedUseAuth = vi.mocked(useAuth);
const qcWrapper = ({ children }: { children: React.ReactNode }) => {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return (
    <QueryClientProvider client={client}>
      <MemoryRouter>{children}</MemoryRouter>
    </QueryClientProvider>
  );
};

describe("ProjectsPage", () => {
  beforeEach(() => {
    mockedUseAuth.mockReturnValue({
      state: { profile: { "cognito:groups": ["ADMIN"] } },
      isAuthenticated: true,
      login: vi.fn(),
      handleCallback: vi.fn(),
      logout: vi.fn()
    } as any);
    vi.mocked(memberQueries.useOrgMembers).mockReturnValue({ data: [], error: null } as any);
  });

  it("shows loading spinner while fetching", () => {
    vi.mocked(projectQueries.useProjects).mockReturnValue({ isLoading: true } as any);
    render(<ProjectsPage />, { wrapper: qcWrapper });
    expect(screen.getByRole("progressbar")).toBeInTheDocument();
  });

  it("renders project list and action buttons for admin", () => {
    vi.mocked(projectQueries.useProjects).mockReturnValue({
      isLoading: false,
      data: [
        { id: "1", key: "OPS", name: "Ops", description: "desc", status: "ACTIVE" },
        { id: "2", key: "ARC", name: "Old", description: "", status: "ARCHIVED" }
      ]
    } as any);
    vi.mocked(projectQueries.useCreateProject).mockReturnValue({ mutateAsync: vi.fn(), isPending: false } as any);
    vi.mocked(projectQueries.useArchiveProject).mockReturnValue({ mutate: vi.fn() } as any);
    vi.mocked(projectQueries.useUnarchiveProject).mockReturnValue({ mutate: vi.fn() } as any);
    vi.mocked(projectQueries.useDeleteProject).mockReturnValue({ mutate: vi.fn() } as any);

    render(<ProjectsPage />, { wrapper: qcWrapper });

    expect(screen.getByText(/Projects/i)).toBeInTheDocument();
    expect(screen.getByText(/OPS\s*-\s*Ops/)).toBeInTheDocument();
    expect(screen.getByText(/ARC\s*-\s*Old/)).toBeInTheDocument();
    // admin buttons
    expect(screen.getByRole("button", { name: /Invite Members/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /New Project/i })).toBeInTheDocument();
  });

  it("opens dialog and calls createProject when form valid", async () => {
    const mutateAsync = vi.fn().mockResolvedValue({});
    vi.mocked(projectQueries.useProjects).mockReturnValue({ isLoading: false, data: [] } as any);
    vi.mocked(projectQueries.useCreateProject).mockReturnValue({ mutateAsync, isPending: false } as any);
    vi.mocked(projectQueries.useArchiveProject).mockReturnValue({ mutate: vi.fn() } as any);
    vi.mocked(projectQueries.useUnarchiveProject).mockReturnValue({ mutate: vi.fn() } as any);
    vi.mocked(projectQueries.useDeleteProject).mockReturnValue({ mutate: vi.fn() } as any);

    render(<ProjectsPage />, { wrapper: qcWrapper });

    fireEvent.click(screen.getByRole("button", { name: /New Project/i }));
    fireEvent.change(screen.getByLabelText(/Key/i), { target: { value: "APP" } });
    fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: "App" } });
    fireEvent.click(screen.getByRole("button", { name: /^Create$/i }));

    await waitFor(() => expect(mutateAsync).toHaveBeenCalledWith({ key: "APP", name: "App", description: undefined }));
  });
});
