import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { VerdictBadge } from "./VerdictBadge";

describe("VerdictBadge", () => {
    it("hiện mã verdict kèm giải thích tiếng Việt ở tooltip", () => {
        render(<VerdictBadge verdict="WA" />);

        expect(screen.getByText("WA")).toHaveAttribute("title", "Sai kết quả");
    });

    it("hiện thêm chi tiết sau mã verdict", () => {
        render(<VerdictBadge verdict="AC" detail="48 ms" />);

        expect(screen.getByText("AC · 48 ms")).toHaveAttribute("title", "Đúng");
    });

    it("hiện trạng thái đang chấm bằng chữ và màu verdict-pending", () => {
        render(<VerdictBadge verdict="PENDING" />);

        const badge = screen.getByText("Đang chấm");
        expect(badge).toHaveAttribute("title", "Máy chấm đang chạy testcase");
        expect(badge).toHaveClass("text-verdict-pending");
    });

    it("tô nền verdict-pending khi đang chấm ở dạng solid", () => {
        render(<VerdictBadge verdict="PENDING" variant="solid" detail="3/8" />);

        expect(screen.getByText("Đang chấm · 3/8")).toHaveClass("bg-verdict-pending");
    });
});
