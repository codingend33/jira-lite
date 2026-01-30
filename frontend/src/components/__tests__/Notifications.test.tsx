import { render, screen, act } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { NotificationProvider, useNotify } from "../Notifications";

function Trigger() {
  const { notifySuccess } = useNotify();
  return (
    <button onClick={() => notifySuccess("hello")}>trigger</button>
  );
}

describe("Notifications", () => {
  it("shows success toast when notifySuccess called", () => {
    render(
      <NotificationProvider>
        <Trigger />
      </NotificationProvider>
    );
    act(() => {
      screen.getByText("trigger").click();
    });
    expect(screen.getByText("hello")).toBeInTheDocument();
  });
});
