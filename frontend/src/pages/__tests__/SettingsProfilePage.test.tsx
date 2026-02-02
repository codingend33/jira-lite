import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { vi } from "vitest";
import SettingsProfilePage from "../SettingsProfilePage";

vi.mock("../../api/profile", async () => {
  return {
    getProfile: vi.fn(),
    updateProfile: vi.fn(),
    presignAvatar: vi.fn()
  };
});

vi.mock("../../components/Notifications", async (orig) => {
  const actual = await orig<typeof import("../../components/Notifications")>();
  return {
    ...actual,
    useNotify: () => ({ notifySuccess: vi.fn(), notifyError: vi.fn() })
  };
});

const profileApi = await import("../../api/profile");
const getProfile = vi.mocked(profileApi.getProfile);
const updateProfile = vi.mocked(profileApi.updateProfile);
const presignAvatar = vi.mocked(profileApi.presignAvatar);

function renderPage() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <MemoryRouter>
      <QueryClientProvider client={qc}>
        <SettingsProfilePage />
      </QueryClientProvider>
    </MemoryRouter>
  );
}

describe("SettingsProfilePage", () => {
  beforeEach(() => {
    getProfile.mockResolvedValue({
      id: "u1",
      email: "me@test.com",
      displayName: "Me",
      avatarS3Key: "avatars/u1.png"
    });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("updates display name and avatar key on save", async () => {
    updateProfile.mockResolvedValue({} as any);
    renderPage();

    await screen.findByDisplayValue("Me");
    fireEvent.change(screen.getByLabelText(/Display Name/i), { target: { value: "NewMe" } });
    fireEvent.change(screen.getByLabelText(/Avatar S3 Key/i), { target: { value: "avatars/new.png" } });
    fireEvent.click(screen.getByRole("button", { name: /save/i }));

    await waitFor(() => {
      expect(updateProfile).toHaveBeenCalledWith({ displayName: "NewMe", avatarS3Key: "avatars/new.png" });
    });
  });

  it("uploads avatar via presign and fills key", async () => {
    presignAvatar.mockResolvedValue({
      uploadUrl: "https://s3/upload",
      headers: {},
      key: "avatars/u1/avatar.png"
    });
    updateProfile.mockResolvedValue({} as any);
    global.fetch = vi.fn().mockResolvedValue({ ok: true }) as any;

    renderPage();
    await screen.findByDisplayValue("Me");

    const file = new File(["avatar"], "avatar.png", { type: "image/png" });
    const input = screen.getByLabelText(/upload avatar/i) as HTMLInputElement;
    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => expect(presignAvatar).toHaveBeenCalled());
    expect(screen.getByDisplayValue("avatars/u1/avatar.png")).toBeInTheDocument();
  });
});
