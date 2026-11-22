import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { useToast } from "@/components/shared/ToastProvider";

import AppLayout from "./layout";

function ToastingPage() {
    const showToast = useToast();
    return (
        <main>
            <button type="button" onClick={() => showToast("Đã lưu.")}>
                Lưu
            </button>
        </main>
    );
}

vi.mock("next/navigation", () => ({
    usePathname: () => "/u/minh.anh",
    useRouter: () => ({ push: vi.fn() }),
}));

describe("AppLayout", () => {
    it("có top bar với điều hướng chính phía trên nội dung", () => {
        render(
            <AppLayout>
                <main>Nội dung</main>
            </AppLayout>,
        );

        expect(screen.getByRole("banner")).toBeInTheDocument();
        expect(screen.getByRole("navigation", { name: "Điều hướng chính" })).toBeInTheDocument();
        expect(screen.getByRole("main")).toHaveTextContent("Nội dung");
    });

    it("link bỏ qua tới nội dung chính đứng đầu trang", () => {
        render(
            <AppLayout>
                <main>Nội dung</main>
            </AppLayout>,
        );

        const firstLink = screen.getAllByRole("link")[0];

        expect(firstLink).toHaveAccessibleName("Bỏ qua tới nội dung chính");
        expect(firstLink).toHaveAttribute("href", "#main-content");
    });

    it("trang con dùng được toast", async () => {
        render(
            <AppLayout>
                <ToastingPage />
            </AppLayout>,
        );

        fireEvent.click(screen.getByRole("button", { name: "Lưu" }));

        expect(await screen.findByText("Đã lưu.")).toBeInTheDocument();
    });
});
