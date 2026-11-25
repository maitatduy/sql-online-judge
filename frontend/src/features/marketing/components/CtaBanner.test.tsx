import { screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { renderWithProviders } from "@/test/render";

import { CtaBanner } from "./CtaBanner";

describe("CtaBanner", () => {
    it("kêu gọi đăng ký", () => {
        renderWithProviders(<CtaBanner />);

        expect(
            screen.getByRole("heading", {
                level: 2,
                name: "Câu truy vấn đầu tiên chỉ mất 30 giây.",
            }),
        ).toBeInTheDocument();
        expect(screen.getByRole("link", { name: "Bắt đầu miễn phí" })).toHaveAttribute(
            "href",
            "/register",
        );
    });

    it("có lối xem bài tập trước khi đăng ký", () => {
        renderWithProviders(<CtaBanner />);

        expect(screen.getByRole("link", { name: "Xem bài tập" })).toHaveAttribute(
            "href",
            "/problems",
        );
    });

    it("minh hoạ câu truy vấn đầu tiên bằng khung editor có kết quả", () => {
        renderWithProviders(<CtaBanner />);

        const example = screen.getByRole("figure", { name: "Ví dụ câu truy vấn đầu tiên" });
        const headers = within(example)
            .getAllByRole("columnheader")
            .map((header) => header.textContent);

        expect(headers).toEqual(["name", "salary"]);
        expect(within(example).getByText("AC - 12 ms")).toBeInTheDocument();
    });

    it("chỉ có một câu mô tả ngắn", () => {
        renderWithProviders(<CtaBanner />);

        expect(screen.getByText("Không cần cài database. Miễn phí.")).toBeInTheDocument();
        expect(screen.queryByRole("list")).not.toBeInTheDocument();
    });
});
