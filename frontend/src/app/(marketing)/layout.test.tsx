import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import MarketingLayout from "./layout";

describe("MarketingLayout", () => {
    it("có vùng header và footer bao quanh nội dung", () => {
        render(
            <MarketingLayout>
                <main>Nội dung</main>
            </MarketingLayout>,
        );

        expect(screen.getByRole("banner")).toBeInTheDocument();
        expect(screen.getByRole("main")).toHaveTextContent("Nội dung");
        expect(screen.getByRole("contentinfo")).toBeInTheDocument();
    });

    it("link bỏ qua tới nội dung chính đứng đầu trang", () => {
        render(
            <MarketingLayout>
                <main>Nội dung</main>
            </MarketingLayout>,
        );

        const firstLink = screen.getAllByRole("link")[0];

        expect(firstLink).toHaveAccessibleName("Bỏ qua tới nội dung chính");
        expect(firstLink).toHaveAttribute("href", "#main-content");
    });
});
