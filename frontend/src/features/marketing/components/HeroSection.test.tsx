import { screen } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";

import { clearMockSession, DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { HeroSection } from "./HeroSection";

describe("HeroSection", () => {
    beforeEach(() => {
        clearMockSession();
    });

    it("có tiêu đề chính của trang", () => {
        renderWithProviders(<HeroSection />);

        expect(
            screen.getByRole("heading", { level: 1, name: "Học SQL bằng cách viết SQL." }),
        ).toBeInTheDocument();
    });

    it("chưa đăng nhập thì có hai nút kêu gọi hành động trỏ đúng đích", () => {
        renderWithProviders(<HeroSection />);

        expect(screen.getByRole("link", { name: "Bắt đầu miễn phí" })).toHaveAttribute(
            "href",
            "/register",
        );
        expect(screen.getByRole("link", { name: "Xem bài tập" })).toHaveAttribute(
            "href",
            "/problems",
        );
    });

    it("đã đăng nhập thì mời tiếp tục luyện tập thay vì đăng ký", async () => {
        writeMockSession(DEMO_SESSION_USER);

        renderWithProviders(<HeroSection />);

        expect(await screen.findByRole("link", { name: "Tiếp tục luyện tập" })).toHaveAttribute(
            "href",
            "/problems",
        );
        expect(screen.queryByRole("link", { name: "Bắt đầu miễn phí" })).not.toBeInTheDocument();
    });

    it("bảng kết quả minh hoạ có cột khớp với câu truy vấn", () => {
        renderWithProviders(<HeroSection />);

        const headers = screen.getAllByRole("columnheader").map((header) => header.textContent);

        expect(headers).toEqual(["department", "name", "salary"]);
    });
});
