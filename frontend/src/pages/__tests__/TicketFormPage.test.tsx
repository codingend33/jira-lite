import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it, vi } from "vitest";
import TicketFormPage from "../TicketFormPage";
import * as ticketQueries from "../../query/ticketQueries";
import * as projectQueries from "../../query/projectQueries";
import * as memberQueries from "../../query/memberQueries";
import { useNotify } from "../../components/Notifications";

vi.mock("../../query/ticketQueries");
vi.mock("../../query/projectQueries");
vi.mock("../../query/memberQueries");
vi.mock("../../components/Notifications");

const wrapper = ({ children }: { children: React.ReactNode }) => {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return (
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={["/tickets/new"]}>
        <Routes>
          <Route path="/tickets/new" element={children} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  );
};

describe("TicketFormPage create mode", () => {
  it("disables submit when required fields empty", () => {
    vi.mocked(projectQueries.useProjects).mockReturnValue({ data: [], error: null } as any);
    vi.mocked(memberQueries.useOrgMembers).mockReturnValue({ data: [], isLoading: false, error: null } as any);
    vi.mocked(ticketQueries.useTicket).mockReturnValue({ data: null, isLoading: false } as any);
    vi.mocked(ticketQueries.useCreateTicket).mockReturnValue({ mutateAsync: vi.fn(), error: null } as any);
    vi.mocked(ticketQueries.useUpdateTicket).mockReturnValue({ mutateAsync: vi.fn(), error: null } as any);
    vi.mocked(useNotify).mockReturnValue({ notifySuccess: vi.fn(), notifyError: vi.fn() } as any);

    render(<TicketFormPage mode="create" />, { wrapper });
    expect(screen.getByRole("button", { name: /create/i })).toBeDisabled();
  });

  it("submits create and navigates when form valid", async () => {
    const mutateAsync = vi.fn().mockResolvedValue({ id: "t-1" });
    const notifySuccess = vi.fn();
    vi.mocked(projectQueries.useProjects).mockReturnValue({
      data: [{ id: "p1", key: "P1", name: "Project 1" }],
      error: null
    } as any);
    vi.mocked(memberQueries.useOrgMembers).mockReturnValue({ data: [], isLoading: false, error: null } as any);
    vi.mocked(ticketQueries.useTicket).mockReturnValue({ data: null, isLoading: false } as any);
    vi.mocked(ticketQueries.useCreateTicket).mockReturnValue({ mutateAsync, error: null } as any);
    vi.mocked(ticketQueries.useUpdateTicket).mockReturnValue({ mutateAsync: vi.fn(), error: null } as any);
    vi.mocked(useNotify).mockReturnValue({ notifySuccess, notifyError: vi.fn() } as any);

    render(<TicketFormPage mode="create" />, { wrapper });

    fireEvent.mouseDown(screen.getAllByRole("combobox")[0]);
    fireEvent.click(screen.getByText(/P1 - Project 1/i));
    fireEvent.change(screen.getByLabelText(/Title/i), { target: { value: "New ticket" } });
    fireEvent.click(screen.getByRole("button", { name: /create/i }));

    await waitFor(() =>
      expect(mutateAsync).toHaveBeenCalledWith({
        projectId: "p1",
        title: "New ticket",
        description: undefined,
        priority: "MEDIUM",
        assigneeId: undefined
      })
    );
    expect(notifySuccess).toHaveBeenCalledWith("Ticket created");
  });
});
