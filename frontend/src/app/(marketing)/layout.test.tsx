import { screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { renderWithProviders } from "@/test/render";

import MarketingLayout from "./layout";

vi.mock("next/navigation", () => ({
    usePathname: () => "/",
    useRouter: () => ({ push: vi.fn() }),
}));

describe("MarketingLayout", () => {
    it("có vùng header và footer bao quanh nội dung", () => {
        renderWithProviders(
            <MarketingLayout>
                <main>Nội dung</main>
            </MarketingLayout>,
        );

        expect(screen.getByRole("banner")).toBeInTheDocument();
        expect(screen.getByRole("main")).toHaveTextContent("Nội dung");
        expect(screen.getByRole("contentinfo")).toBeInTheDocument();
    });

    it("link bỏ qua tới nội dung chính đứng đầu trang", () => {
        renderWithProviders(
            <MarketingLayout>
                <main>Nội dung</main>
            </MarketingLayout>,
        );

        const firstLink = screen.getAllByRole("link")[0];

        expect(firstLink).toHaveAccessibleName("Bỏ qua tới nội dung chính");
        expect(firstLink).toHaveAttribute("href", "#main-content");
    });
});
