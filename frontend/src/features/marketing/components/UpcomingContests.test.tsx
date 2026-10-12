import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import type { UpcomingContest } from "../types";

import { UpcomingContests } from "./UpcomingContests";

const contests: UpcomingContest[] = [
    {
        title: "SQL Weekly #38",
        startLabel: "Thứ 7 · 19:30",
        problemCount: 4,
        durationMinutes: 90,
        note: "Rated",
    },
    {
        title: "Tân binh #12",
        startLabel: "T2 · 20:00",
        problemCount: 3,
        durationMinutes: 60,
        note: "Cho người mới",
    },
];

describe("UpcomingContests", () => {
    it("hiện từng contest với lịch và thông tin tóm tắt", () => {
        render(<UpcomingContests contests={contests} />);

        expect(
            screen.getByRole("heading", { level: 3, name: "SQL Weekly #38" }),
        ).toBeInTheDocument();
        expect(screen.getByText("Thứ 7 · 19:30")).toBeInTheDocument();
        expect(screen.getByText("4 bài · 90 phút · Rated")).toBeInTheDocument();
        expect(screen.getAllByRole("heading", { level: 3 })).toHaveLength(2);
    });

    it("link xem tất cả và link đăng ký trỏ tới danh sách contest", () => {
        render(<UpcomingContests contests={contests} />);

        expect(screen.getByRole("link", { name: "Xem tất cả contest" })).toHaveAttribute(
            "href",
            "/contests",
        );
        expect(screen.getByRole("link", { name: "Đăng ký Tân binh #12" })).toHaveAttribute(
            "href",
            "/contests",
        );
    });
});
