import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import ErrorBanner from "../ErrorBanner";
import { ApiError } from "../../api/client";

describe("ErrorBanner", () => {
  it("renders nothing when no error", () => {
    const { container } = render(<ErrorBanner error={null} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("renders ApiError with payload", () => {
    const err = new ApiError(500, "oops", { code: "INTERNAL", message: "Boom", traceId: "t1" } as any);
    render(<ErrorBanner error={err} />);
    expect(screen.getByText(/INTERNAL/)).toBeInTheDocument();
    expect(screen.getByText(/Boom/)).toBeInTheDocument();
    expect(screen.getByText(/traceId: t1/)).toBeInTheDocument();
  });

  it("renders generic error", () => {
    render(<ErrorBanner error={new Error("x")} />);
    expect(screen.getByText(/Unexpected error occurred/i)).toBeInTheDocument();
  });
});
