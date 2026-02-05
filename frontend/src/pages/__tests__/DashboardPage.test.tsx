import { render, screen, waitFor } from "@testing-library/react";
import DashboardPage from "../DashboardPage";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { AuthProvider } from "../../auth/AuthContext";
import { NotificationProvider } from "../../components/Notifications";
import { vi } from "vitest";

import * as client from "../../api/client";
import * as auditApi from "../../api/audit";

vi.mock("../../api/client");
vi.mock("../../api/audit");

const apiRequest = vi.mocked(client.apiRequest);
const listAuditLogs = vi.mocked(auditApi.listAuditLogs);

function renderWithProviders(ui: React.ReactElement) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <MemoryRouter>
      <QueryClientProvider client={client}>
        <AuthProvider>
          <NotificationProvider>{ui}</NotificationProvider>
        </AuthProvider>
      </QueryClientProvider>
    </MemoryRouter>
  );
}

describe("DashboardPage", () => {
  it("renders metrics and activity stream", async () => {
    apiRequest.mockResolvedValue({ activeProjects: 3, myTickets: 4, members: 5 });
    listAuditLogs.mockResolvedValue({
      content: [
        {
          id: "1",
          action: "PROJECT_CREATE",
          details: "P1 created",
          entityType: "PROJECT",
          entityId: "p1",
          actorUserId: "u1",
          createdAt: new Date().toISOString()
        }
      ],
      page: { number: 0, size: 20, totalElements: 1, totalPages: 1 }
    });

    renderWithProviders(<DashboardPage />);

    await waitFor(() => {
      expect(screen.getByText(/Active Projects/i).parentElement).toHaveTextContent("3");
      expect(screen.getByText(/My Tickets/i).parentElement).toHaveTextContent("4");
      expect(screen.getByText(/P1 created/i)).toBeInTheDocument();
    });
  });
});
