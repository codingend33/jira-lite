import { describe, expect, it, vi } from "vitest";
import * as client from "./client";
import { createTicket, getTicket, listTickets, transitionTicket, updateTicket } from "./tickets";

vi.mock("./client");

describe("tickets api", () => {
  beforeEach(() => {
    vi.mocked(client.apiRequest).mockClear();
  });

  it("listTickets forwards filters", async () => {
    vi.mocked(client.apiRequest).mockResolvedValue([] as any);
    await listTickets({ status: "OPEN" } as any);
    expect(client.apiRequest).toHaveBeenCalledWith("/tickets?status=OPEN");
  });

  it("getTicket hits detail endpoint", async () => {
    vi.mocked(client.apiRequest).mockResolvedValue({ id: "1" } as any);
    await getTicket("1");
    expect(client.apiRequest).toHaveBeenCalledWith("/tickets/1");
  });

  it("createTicket posts body", async () => {
    vi.mocked(client.apiRequest).mockResolvedValue({ id: "1" } as any);
    const payload = { projectId: "p1", title: "t" };
    await createTicket(payload as any);
    expect(client.apiRequest).toHaveBeenCalledWith("/tickets", {
      method: "POST",
      body: JSON.stringify(payload)
    });
  });

  it("updateTicket puts body", async () => {
    vi.mocked(client.apiRequest).mockResolvedValue({ id: "1" } as any);
    const payload = { title: "t2" };
    await updateTicket("1", payload);
    expect(client.apiRequest).toHaveBeenCalledWith("/tickets/1", {
      method: "PATCH",
      body: JSON.stringify(payload)
    });
  });

  it("transitionTicket posts status", async () => {
    vi.mocked(client.apiRequest).mockResolvedValue({ id: "1" } as any);
    await transitionTicket("1", "DONE");
    expect(client.apiRequest).toHaveBeenCalledWith("/tickets/1/transition", {
      method: "POST",
      body: JSON.stringify({ status: "DONE" })
    });
  });
});
