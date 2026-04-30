import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import Messages from "@/pages/Messages";

describe("Messages page", () => {
  it("shows the latest message in a collapsed thread and expands on click", () => {
    render(<Messages />);

    expect(
      screen.getByText("Your export is being prepared. We will notify you here when the archive is ready to download."),
    ).toBeInTheDocument();
    expect(
      screen.queryByText("I requested an export of my account data this morning. Can you confirm when it will be ready?"),
    ).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Expand thread with R8N Support" }));

    expect(
      screen.getByText("I requested an export of my account data this morning. Can you confirm when it will be ready?"),
    ).toBeInTheDocument();
  });

  it("does not show incoming and outgoing direction labels", () => {
    render(<Messages />);

    expect(screen.queryByText("To you")).not.toBeInTheDocument();
    expect(screen.queryByText("From you")).not.toBeInTheDocument();
  });

  it("shows last seen information when hovering a thread participant avatar", async () => {
    render(<Messages />);

    const trigger = screen.getByRole("button", { name: "Marta Keller presence" });
    fireEvent.pointerEnter(trigger, { pointerType: "mouse" });
    fireEvent.mouseEnter(trigger);

    expect(await screen.findByText("Last seen 18 minutes ago")).toBeInTheDocument();
  });

  it("filters support conversations", () => {
    render(<Messages />);

    fireEvent.click(screen.getByRole("button", { name: "Support" }));

    expect(screen.getByText("Export archive request")).toBeInTheDocument();
    expect(screen.queryByText("Question about your coffee grinder review")).not.toBeInTheDocument();
    expect(screen.queryByText("Supplier recommendation follow-up")).not.toBeInTheDocument();
  });

  it("sends a new message in an expanded thread", () => {
    render(<Messages />);

    fireEvent.click(screen.getByRole("button", { name: "Expand thread with R8N Support" }));
    fireEvent.change(
      screen.getByPlaceholderText("Message R8N Support..."),
      { target: { value: "Thanks, please send it here once it is ready." } },
    );
    fireEvent.click(screen.getByRole("button", { name: "Send" }));

    expect(
      screen.getByText("Thanks, please send it here once it is ready."),
    ).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Message R8N Support...")).toHaveValue("");
  });

  it("creates a new thread from the new message dialog", () => {
    render(<Messages />);

    fireEvent.click(screen.getByRole("button", { name: "New message" }));
    fireEvent.change(screen.getByLabelText("Recipient"), {
      target: { value: "Lina Hartmann" },
    });
    fireEvent.change(screen.getByLabelText("Message"), {
      target: { value: "Hi, I wanted to ask about your supplier shortlist." },
    });
    fireEvent.click(screen.getByRole("button", { name: "Start thread" }));

    expect(screen.getByText("Conversation with Lina Hartmann")).toBeInTheDocument();
    expect(
      screen.getByText("Hi, I wanted to ask about your supplier shortlist."),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Collapse thread with Lina Hartmann" })).toBeInTheDocument();
  });
});
