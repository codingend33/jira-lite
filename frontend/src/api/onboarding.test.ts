import { describe, expect, it, vi } from "vitest";
import * as client from "./client";
import { acceptInvitation, createInvitation, createOrganization } from "./onboarding";

vi.mock("./client");

describe("onboarding api", () => {
  it("calls createOrganization endpoint", async () => {
    vi.mocked(client.apiRequest).mockResolvedValue({ orgId: "1" } as any);
    await createOrganization({ name: "Acme" });
    expect(client.apiRequest).toHaveBeenCalledWith("/orgs", {
      method: "POST",
      body: JSON.stringify({ name: "Acme" })
    });
  });

  it("calls createInvitation endpoint with orgId", async () => {
    vi.mocked(client.apiRequest).mockResolvedValue({ token: "t" } as any);
    await createInvitation("org-1", { email: "a@a.com", role: "ADMIN" });
    expect(client.apiRequest).toHaveBeenCalledWith("/orgs/org-1/invitations", {
      method: "POST",
      body: JSON.stringify({ email: "a@a.com", role: "ADMIN" })
    });
  });

  it("calls acceptInvitation endpoint", async () => {
    vi.mocked(client.apiRequest).mockResolvedValue({ message: "ok" } as any);
    await acceptInvitation("token123");
    expect(client.apiRequest).toHaveBeenCalledWith("/invitations/token123/accept", {
      method: "POST"
    });
  });
});
