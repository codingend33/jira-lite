import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it, vi } from "vitest";
import { useTicket, useTickets } from "./ticketQueries";
import * as ticketsApi from "../api/tickets";

vi.mock("../api/tickets");

const wrapper = ({ children }: { children: React.ReactNode }) => {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
};

describe("ticketQueries error flows", () => {
  it("useTickets surfaces error", async () => {
    const error = new Error("fail");
    vi.mocked(ticketsApi.listTickets).mockRejectedValue(error);
    const { result } = renderHook(() => useTickets({ status: "OPEN" } as any), { wrapper });
    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(result.current.error).toBe(error);
  });

  it("useTicket surfaces error", async () => {
    const error = new Error("fail");
    vi.mocked(ticketsApi.getTicket).mockRejectedValue(error);
    const { result } = renderHook(() => useTicket("id-1"), { wrapper });
    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(result.current.error).toBe(error);
  });
});
