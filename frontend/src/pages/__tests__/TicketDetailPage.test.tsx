import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import TicketDetailPage from "../TicketDetailPage";
import * as ticketQueries from "../../query/ticketQueries";
import * as attachmentQueries from "../../query/attachmentQueries";
import * as commentQueries from "../../query/commentQueries";
import * as memberQueries from "../../query/memberQueries";
import * as projectQueries from "../../query/projectQueries";
import { useNotify } from "../../components/Notifications";
import { AuthProvider } from "../../auth/AuthContext";

vi.mock("../../query/ticketQueries");
vi.mock("../../components/Notifications");
vi.mock("../../query/attachmentQueries", () => ({
  useUploadAttachment: vi.fn(),
  useDownloadAttachment: vi.fn(),
  useDeleteAttachment: vi.fn(),
  useAttachments: vi.fn()
}));
vi.mock("../../query/commentQueries", () => ({
  useComments: vi.fn(),
  useCreateComment: vi.fn()
}));
vi.mock("../../query/memberQueries", () => ({
  useOrgMembers: vi.fn()
}));
vi.mock("../../query/projectQueries", () => ({
  useProjects: vi.fn()
}));

const wrapper = ({ children }: { children: React.ReactNode }) => {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return (
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={["/tickets/1"]}>
        <AuthProvider>
          <Routes>
            <Route path="/tickets/:ticketId" element={children} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    </QueryClientProvider>
  );
};

describe("TicketDetailPage", () => {
  beforeEach(() => {
    vi.mocked(attachmentQueries.useAttachments).mockReturnValue({ data: [], error: null } as any);
    vi.mocked(attachmentQueries.useDeleteAttachment).mockReturnValue({
      mutateAsync: vi.fn(),
      isPending: false,
      error: null
    } as any);
    vi.mocked(attachmentQueries.useDownloadAttachment).mockReturnValue({
      mutateAsync: vi.fn().mockResolvedValue({ downloadUrl: "http://example.com/file" }),
      isPending: false,
      error: null
    } as any);
    vi.mocked(attachmentQueries.useUploadAttachment).mockReturnValue({ mutateAsync: vi.fn(), error: null } as any);
    vi.mocked(useNotify).mockReturnValue({ notifySuccess: vi.fn(), notifyError: vi.fn() } as any);
    vi.mocked(memberQueries.useOrgMembers).mockReturnValue({ data: [], isLoading: false, error: null } as any);
    vi.mocked(projectQueries.useProjects).mockReturnValue({ data: [], isLoading: false, error: null } as any);
    vi.mocked(commentQueries.useComments).mockReturnValue({ data: [], isLoading: false, error: null } as any);
    vi.mocked(commentQueries.useCreateComment).mockReturnValue({ mutateAsync: vi.fn(), isPending: false, error: null } as any);
    vi.mocked(ticketQueries.useTransitionTicket).mockReturnValue({
      mutateAsync: vi.fn(),
      isPending: false,
      error: null
    } as any);
  });

  it("shows error banner on query error", () => {
    vi.mocked(ticketQueries.useTicket).mockReturnValue({
      isLoading: false,
      data: {
        id: "1",
        key: "T-1",
        title: "Title",
        description: "",
        status: "OPEN",
        priority: "LOW",
        projectId: "proj-1234",
        assigneeId: null
      },
      error: new Error("boom")
    } as any);
    render(<TicketDetailPage />, { wrapper });
    expect(screen.getByText(/Unexpected error occurred/i)).toBeInTheDocument();
  });

  it("deletes attachment and shows success", async () => {
    const deleteAsync = vi.fn().mockResolvedValue({});
    const notifySuccess = vi.fn();
    vi.mocked(ticketQueries.useTicket).mockReturnValue({
      isLoading: false,
      data: {
        id: "1",
        key: "T1",
        title: "T1",
        status: "OPEN",
        description: "",
        priority: "LOW",
        projectId: "proj-1234",
        assigneeId: null
      }
    } as any);
    vi.mocked(attachmentQueries.useAttachments).mockReturnValue({
      data: [{ id: "att1", fileName: "file.txt", contentType: "text/plain", fileSize: 1024 }]
    } as any);
    vi.mocked(attachmentQueries.useDeleteAttachment).mockReturnValue({
      mutateAsync: deleteAsync,
      isPending: false,
      error: null
    } as any);
    vi.mocked(attachmentQueries.useDownloadAttachment).mockReturnValue({ mutate: vi.fn(), error: null } as any);
    vi.mocked(attachmentQueries.useUploadAttachment).mockReturnValue({ mutateAsync: vi.fn(), error: null } as any);
    vi.mocked(useNotify).mockReturnValue({ notifySuccess, notifyError: vi.fn() } as any);

    render(<TicketDetailPage />, { wrapper });
    fireEvent.click(screen.getByRole("button", { name: /Delete attachment/i }));
    await waitFor(() => expect(deleteAsync).toHaveBeenCalledWith("att1"));
    expect(notifySuccess).toHaveBeenCalled();
  });
});
