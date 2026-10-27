import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { Markdown } from "./Markdown";

describe("Markdown", () => {
    it("hiện đoạn văn, chữ đậm và mã nội dòng", () => {
        render(<Markdown>{"Bảng `employees` lưu **nhân viên**."}</Markdown>);

        expect(screen.getByText("employees", { selector: "code" })).toHaveClass("font-mono");
        expect(screen.getByText("nhân viên", { selector: "strong" })).toBeInTheDocument();
    });

    it("bỏ HTML nguy hiểm thay vì chạy nó", () => {
        const { container } = render(
            <Markdown>{'Đề <img src="x" onerror="alert(1)"> <script>alert(2)</script>'}</Markdown>,
        );

        expect(container.querySelector("script")).toBeNull();
        expect(container.querySelector("[onerror]")).toBeNull();
    });

    it("không cho link javascript: và mở link ngoài an toàn", () => {
        render(
            <Markdown>
                {"[tài liệu](https://www.postgresql.org) [xấu](javascript:alert(1))"}
            </Markdown>,
        );

        expect(screen.getByRole("link", { name: "tài liệu" })).toHaveAttribute(
            "rel",
            "noopener noreferrer nofollow ugc",
        );
        expect(screen.queryByRole("link", { name: "xấu" })).not.toBeInTheDocument();
    });
});
