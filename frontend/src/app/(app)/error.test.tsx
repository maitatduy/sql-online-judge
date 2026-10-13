import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import AppErrorPage from "./error";

describe("AppErrorPage", () => {
    it("báo lỗi trong vùng nội dung chính và có lối về trang chủ", () => {
        render(<AppErrorPage error={new Error("boom")} reset={vi.fn()} />);

        expect(screen.getByRole("main")).toHaveAttribute("id", "main-content");
        expect(
            screen.getByRole("heading", { level: 1, name: "Đã có lỗi xảy ra" }),
        ).toBeInTheDocument();
        expect(screen.getByRole("link", { name: "Về trang chủ" })).toHaveAttribute("href", "/");
    });

    it("không lộ chi tiết kỹ thuật của lỗi", () => {
        render(<AppErrorPage error={new Error("boom")} reset={vi.fn()} />);

        expect(screen.queryByText(/boom/)).not.toBeInTheDocument();
    });

    it("nút thử lại gọi hàm reset", () => {
        const reset = vi.fn();
        render(<AppErrorPage error={new Error("boom")} reset={reset} />);

        fireEvent.click(screen.getByRole("button", { name: "Thử lại" }));

        expect(reset).toHaveBeenCalledTimes(1);
    });
});
