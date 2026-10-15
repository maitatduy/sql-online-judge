import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import AppLayout from "./layout";

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
});
