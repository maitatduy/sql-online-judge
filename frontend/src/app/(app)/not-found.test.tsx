import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import AppNotFound from "./not-found";

describe("AppNotFound", () => {
    it("báo không tìm thấy trang trong vùng nội dung chính", () => {
        render(<AppNotFound />);

        expect(screen.getByRole("main")).toHaveAttribute("id", "main-content");
        expect(
            screen.getByRole("heading", { level: 1, name: "Không tìm thấy trang" }),
        ).toBeInTheDocument();
        expect(
            screen.getByText("0 dòng. Trang bạn tìm không tồn tại hoặc đã bị xoá."),
        ).toBeInTheDocument();
    });

    it("có lối về trang chủ và danh sách bài tập", () => {
        render(<AppNotFound />);

        expect(screen.getByRole("link", { name: "Về trang chủ" })).toHaveAttribute("href", "/");
        expect(screen.getByRole("link", { name: "Xem bài tập" })).toHaveAttribute(
            "href",
            "/problems",
        );
    });
});
