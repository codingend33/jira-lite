import { renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import {
  useCreateTicket,
  useTicket,
  useTickets,
  useTransitionTicket,
  useUpdateTicket,
  ticketKeys
} from "./ticketQueries";
import * as ticketsApi from "../api/tickets";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

vi.mock("../api/tickets");

const wrapper = ({ children }: { children: React.ReactNode }) => {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
};

describe("ticketQueries", () => {
  it("ticketKeys produce stable arrays", () => {
    expect(ticketKeys.detail("1")).toEqual(["tickets", "detail", "1"]);
  });

  it("useTickets calls listTickets", async () => {
    vi.mocked(ticketsApi.listTickets).mockResolvedValue([] as any);
    const { result } = renderHook(() => useTickets({ status: "OPEN" } as any), { wrapper });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(ticketsApi.listTickets).toHaveBeenCalledWith({ status: "OPEN" });
  });

  it("useTicket calls getTicket", async () => {
    vi.mocked(ticketsApi.getTicket).mockResolvedValue({ id: "1" } as any);
    const { result } = renderHook(() => useTicket("1"), { wrapper });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(ticketsApi.getTicket).toHaveBeenCalledWith("1");
  });

  it("useCreateTicket invalidates tickets list on success", async () => {
    vi.mocked(ticketsApi.createTicket).mockResolvedValue({ id: "1" } as any);
    const { result, rerender } = renderHook(() => useCreateTicket(), { wrapper });
    await result.current.mutateAsync({ title: "x" } as any);
    rerender();
    expect(result.current.status).toBe("success");
  });

  it("useUpdateTicket resolves mutate promise", async () => {
    vi.mocked(ticketsApi.updateTicket).mockResolvedValue({} as any);
    const { result } = renderHook(() => useUpdateTicket(), { wrapper });
    await expect(
      result.current.mutateAsync({ id: "1", payload: { title: "t" } })
    ).resolves.toEqual({});
  });

  it("useTransitionTicket resolves mutate promise", async () => {
    vi.mocked(ticketsApi.transitionTicket).mockResolvedValue({} as any);
    const { result } = renderHook(() => useTransitionTicket(), { wrapper });
    await expect(
      result.current.mutateAsync({ id: "1", status: "DONE" })
    ).resolves.toEqual({});
  });
});
