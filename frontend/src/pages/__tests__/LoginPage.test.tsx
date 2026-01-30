import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import LoginPage from "../LoginPage";
import { BrowserRouter } from "react-router-dom";

// Mock useAuth
const mockLogin = vi.fn();
vi.mock("../../auth/AuthContext", () => ({
    useAuth: () => ({
        login: mockLogin,
        handleCallback: vi.fn(),
    }),
}));

describe("LoginPage", () => {
    it("renders login button", () => {
        render(
            <BrowserRouter>
                <LoginPage />
            </BrowserRouter>
        );
        expect(screen.getByRole("button", { name: /login with cognito/i })).toBeInTheDocument();
    });

    it("calls login on button click", () => {
        render(
            <BrowserRouter>
                <LoginPage />
            </BrowserRouter>
        );

        fireEvent.click(screen.getByRole("button", { name: /login with cognito/i }));
        expect(mockLogin).toHaveBeenCalled();
    });
});
