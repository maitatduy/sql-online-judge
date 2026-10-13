import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { MINH_ANH_PROFILE, RATING_HISTORY_BY_USERNAME } from "@/mocks/profile-fixtures";

import { RatingSummary } from "./RatingSummary";

const POINTS = RATING_HISTORY_BY_USERNAME[MINH_ANH_PROFILE.username];

function stat(label: string): HTMLElement {
    const group = screen.getByText(label).parentElement;
    if (!group) {
        throw new Error(`Không thấy số liệu ${label}`);
    }
    return group;
}

describe("RatingSummary", () => {
    it("hiện rating, mức tăng và contest của điểm đang xem", () => {
        render(<RatingSummary points={POINTS} activeIndex={7} />);

        expect(
            screen.getByRole("heading", { level: 2, name: "Rating contest" }),
        ).toBeInTheDocument();
        expect(screen.getByRole("heading", { level: 2 }).parentElement).toHaveTextContent(
            "Rating contest1.487",
        );
        expect(screen.getByText("Tăng 46")).toBeInTheDocument();
        expect(
            screen.getByText("SQL Judge Weekly #19 · 20/09/2026 · Hạng #36"),
        ).toBeInTheDocument();
    });

    it("ghi rõ mức giảm bằng chữ, không chỉ bằng màu", () => {
        render(<RatingSummary points={POINTS} activeIndex={2} />);

        expect(screen.getByText("1.338")).toBeInTheDocument();
        expect(screen.getByText("Giảm 14")).toBeInTheDocument();
        expect(
            screen.getByText("SQL Judge Weekly #14 · 14/06/2026 · Hạng #77"),
        ).toBeInTheDocument();
    });

    it("contest đầu tiên không có mức thay đổi", () => {
        render(<RatingSummary points={POINTS} activeIndex={0} />);

        expect(screen.getByText("1.320")).toBeInTheDocument();
        expect(screen.queryByText(/^(Tăng|Giảm)/)).not.toBeInTheDocument();
    });

    it("luôn hiện rating cao nhất và số contest đã thi", () => {
        render(<RatingSummary points={POINTS} activeIndex={0} />);

        expect(stat("Cao nhất")).toHaveTextContent("Cao nhất1.487");
        expect(stat("Đã thi")).toHaveTextContent("Đã thi8 contest");
    });
});
