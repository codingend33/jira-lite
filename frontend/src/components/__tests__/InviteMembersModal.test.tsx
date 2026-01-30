import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import InviteMembersModal from "../InviteMembersModal";
import { useAuth } from "../../auth/AuthContext";
import { useMutation } from "@tanstack/react-query";

vi.mock("../../auth/AuthContext");
vi.mock("@tanstack/react-query");

const mockedUseAuth = vi.mocked(useAuth);
const mockedUseMutation = vi.mocked(useMutation);

describe("InviteMembersModal", () => {
  beforeEach(() => {
    mockedUseAuth.mockReturnValue({
      state: { profile: { "custom:org_id": "org-1" } },
      isAuthenticated: true,
      login: vi.fn(),
      handleCallback: vi.fn(),
      logout: vi.fn()
    } as any);
  });

  it("disables send when email is empty", () => {
    mockedUseMutation.mockReturnValue({
      mutate: vi.fn(),
      isPending: false,
      isSuccess: false,
      isError: false,
      reset: vi.fn()
    } as any);

    render(<InviteMembersModal open onClose={() => {}} />);
    expect(screen.getByRole("button", { name: /send invitation/i })).toBeDisabled();
  });

  it("shows success alert when mutation succeeded", () => {
    mockedUseMutation.mockReturnValue({
      mutate: vi.fn(),
      isPending: false,
      isSuccess: true,
      isError: false,
      data: { invitationUrl: "https://invite.link" },
      reset: vi.fn()
    } as any);

    render(<InviteMembersModal open onClose={() => {}} />);
    expect(screen.getByText(/invitation created successfully/i)).toBeInTheDocument();
    expect(screen.getByDisplayValue("https://invite.link")).toBeInTheDocument();
  });
});
