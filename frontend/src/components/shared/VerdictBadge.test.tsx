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
});
