import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { MINH_ANH_PROFILE, NEWBIE_PROFILE } from "@/mocks/profile-fixtures";

import { SolvedDonut } from "./SolvedDonut";

describe("SolvedDonut", () => {
    it("mô tả tổng số bài đã giải cho trình đọc màn hình", () => {
        render(<SolvedDonut stats={MINH_ANH_PROFILE.stats} />);

        expect(screen.getByRole("img", { name: "Đã giải 63 trên 330 bài" })).toBeInTheDocument();
    });

    it("có tiêu đề để thẻ không vô danh với trình đọc màn hình", () => {
        render(<SolvedDonut stats={MINH_ANH_PROFILE.stats} />);

        expect(
            screen.getByRole("heading", { level: 2, name: "Tiến độ theo độ khó" }),
        ).toBeInTheDocument();
    });

    it.each([
        ["Dễ", "42/120"],
        ["Trung bình", "18/150"],
        ["Khó", "3/60"],
    ])("độ khó %s hiện %s", (label, progress) => {
        render(<SolvedDonut stats={MINH_ANH_PROFILE.stats} />);

        const row = screen.getByText(label).closest("li");

        expect(row).not.toBeNull();
        expect(within(row as HTMLElement).getByText(progress)).toBeInTheDocument();
    });

    it("vẫn hiện vòng rỗng và 0 bài cho người chưa giải bài nào", () => {
        render(<SolvedDonut stats={NEWBIE_PROFILE.stats} />);

        expect(screen.getByRole("img", { name: "Đã giải 0 trên 330 bài" })).toBeInTheDocument();
        expect(screen.getByText("0/120")).toBeInTheDocument();
    });
});
