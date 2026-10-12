import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { StatsStrip } from "./StatsStrip";

describe("StatsStrip", () => {
    it("định dạng số theo vi-VN và giữ nguyên giá trị dạng chữ", () => {
        render(
            <StatsStrip
                stats={[
                    { value: 1806, label: "bài tập có máy chấm" },
                    { value: "Thứ 7 hằng tuần", label: "contest SQL Weekly" },
                ]}
            />,
        );

        expect(screen.getByText("1.806")).toBeInTheDocument();
        expect(screen.getByText("bài tập có máy chấm")).toBeInTheDocument();
        expect(screen.getByText("Thứ 7 hằng tuần")).toBeInTheDocument();
    });
});
