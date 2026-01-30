import { describe, expect, it, vi } from "vitest";
import * as client from "./client";
import { createProject, listProjects } from "./projects";

vi.mock("./client");

describe("projects api", () => {
  beforeEach(() => {
    vi.mocked(client.apiRequest).mockClear();
  });

  it("listProjects forwards query params", async () => {
    vi.mocked(client.apiRequest).mockResolvedValue([] as any);
    await listProjects();
    expect(client.apiRequest).toHaveBeenCalledWith("/projects");
  });

  it("createProject passes body", async () => {
    vi.mocked(client.apiRequest).mockResolvedValue({ id: "p1" } as any);
    const payload = { key: "P1", name: "Proj", description: "d" };
    await createProject(payload);
    expect(client.apiRequest).toHaveBeenCalledWith("/projects", {
      method: "POST",
      body: JSON.stringify(payload)
    });
  });
});
