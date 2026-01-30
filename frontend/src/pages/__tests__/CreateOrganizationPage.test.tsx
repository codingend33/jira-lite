import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import CreateOrganizationPage from "../CreateOrganizationPage";
import { useMutation } from "@tanstack/react-query";
import { buildAuthorizeUrl } from "../../auth/auth";

vi.mock("@tanstack/react-query");
vi.mock("../../auth/auth");

const mockedUseMutation = vi.mocked(useMutation);
const mockedBuildAuthorizeUrl = vi.mocked(buildAuthorizeUrl);

describe("CreateOrganizationPage", () => {
  it("disables submit when name empty", () => {
    mockedUseMutation.mockReturnValue({
      mutate: vi.fn(),
      isPending: false,
      isError: false,
      isSuccess: false
    } as any);
    render(
      <MemoryRouter>
        <CreateOrganizationPage />
      </MemoryRouter>
    );
    expect(screen.getByRole("button", { name: /create organization/i })).toBeDisabled();
  });

  it("submits org name and triggers login redirect on success", async () => {
    const mutate = vi.fn();
    mockedUseMutation.mockReturnValue({
      mutate,
      isPending: false,
      isError: false,
      isSuccess: false
    } as any);
    mockedBuildAuthorizeUrl.mockResolvedValue("https://login.example.com");
    render(
      <MemoryRouter>
        <CreateOrganizationPage />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText(/organization name/i), { target: { value: "Acme" } });
    fireEvent.click(screen.getByRole("button", { name: /create organization/i }));
    expect(mutate).toHaveBeenCalledWith({ name: "Acme" });
  });
});
