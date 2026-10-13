import { fireEvent, render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { MINH_ANH_PROFILE } from "@/mocks/profile-fixtures";

import { BadgeGrid } from "./BadgeGrid";

describe("BadgeGrid", () => {
    it("ghi số huy hiệu ở tiêu đề", () => {
        render(<BadgeGrid badges={MINH_ANH_PROFILE.badges} />);

        expect(screen.getByRole("heading", { level: 2, name: "Huy hiệu · 7" })).toBeInTheDocument();
    });

    it("mỗi huy hiệu là một ô có tên, tới được bằng bàn phím", () => {
        render(<BadgeGrid badges={MINH_ANH_PROFILE.badges} />);

        const badges = within(screen.getByRole("list", { name: "Huy hiệu" })).getAllByRole(
            "button",
        );

        expect(badges.map((badge) => badge.getAttribute("aria-label"))).toEqual([
            "Chuỗi 10 ngày",
            "Bậc thầy JOIN",
            "Top 20% contest",
            "50 bài AC",
            "Contest đầu tiên",
            "10 bài AC",
            "AC đầu tiên",
        ]);
    });

    it("focus vào ô thì hiện tên và ngày đạt huy hiệu", async () => {
        render(<BadgeGrid badges={MINH_ANH_PROFILE.badges} />);

        fireEvent.focus(screen.getByRole("button", { name: "Bậc thầy JOIN" }));

        const tooltip = await screen.findByRole("tooltip");
        expect(tooltip).toHaveTextContent("Bậc thầy JOIN");
        expect(tooltip).toHaveTextContent("Đạt ngày 12/09/2026");
    });

    it("huy hiệu số bài AC hiện con số trong ô", () => {
        render(<BadgeGrid badges={MINH_ANH_PROFILE.badges} />);

        expect(screen.getByRole("button", { name: "50 bài AC" })).toHaveTextContent("50");
        expect(screen.getByRole("button", { name: "10 bài AC" })).toHaveTextContent("10");
    });

    it("vẫn hiện được huy hiệu có mã chưa biết", () => {
        render(
            <BadgeGrid
                badges={[
                    {
                        code: "SOMETHING_NEW",
                        name: "Huy hiệu mới",
                        earnedAt: "2026-10-01T00:00:00Z",
                    },
                ]}
            />,
        );

        expect(screen.getByRole("button", { name: "Huy hiệu mới" })).toBeInTheDocument();
    });

    it("báo chưa có huy hiệu khi danh sách rỗng", () => {
        render(<BadgeGrid badges={[]} />);

        expect(screen.getByRole("heading", { level: 2, name: "Huy hiệu · 0" })).toBeInTheDocument();
        expect(screen.getByText("Chưa có huy hiệu nào")).toBeInTheDocument();
        expect(screen.queryByRole("list")).not.toBeInTheDocument();
    });
});
