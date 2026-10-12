import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { CtaBanner } from "./CtaBanner";

describe("CtaBanner", () => {
    it("kêu gọi đăng ký", () => {
        render(<CtaBanner />);

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
        render(<CtaBanner />);

        expect(screen.getByRole("link", { name: "Xem bài tập" })).toHaveAttribute(
            "href",
            "/problems",
        );
    });

    it("minh hoạ câu truy vấn đầu tiên bằng khung editor có kết quả", () => {
        render(<CtaBanner />);

        const example = screen.getByRole("figure", { name: "Ví dụ câu truy vấn đầu tiên" });
        const headers = within(example)
            .getAllByRole("columnheader")
            .map((header) => header.textContent);

        expect(headers).toEqual(["name", "salary"]);
        expect(within(example).getByText("AC · 12 ms")).toBeInTheDocument();
    });

    it("có câu mô tả ngắn và ba lý do để bắt đầu", () => {
        render(<CtaBanner />);

        const reasons = within(screen.getByRole("list")).getAllByRole("listitem");

        expect(
            screen.getByText("Không cần cài đặt cơ sở dữ liệu. Miễn phí cho người học."),
        ).toBeInTheDocument();
        expect(reasons.map((reason) => reason.textContent)).toEqual([
            "Hỗ trợ PostgreSQL, MySQL và SQLite",
            "Máy chấm trả kết quả trong vài giây",
            "Trợ lý AI gợi ý khi bạn bí",
        ]);
    });
});
