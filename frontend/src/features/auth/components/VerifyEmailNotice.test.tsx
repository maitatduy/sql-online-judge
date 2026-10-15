import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";

import { savePendingVerificationEmail } from "../pending-verification";

import { VerifyEmailNotice } from "./VerifyEmailNotice";

describe("VerifyEmailNotice", () => {
    beforeEach(() => {
        sessionStorage.clear();
    });

    it("cho biết mã đã gửi tới email vừa đăng ký", () => {
        savePendingVerificationEmail("minh.anh@gmail.com");

        render(<VerifyEmailNotice />);

        expect(
            screen.getByRole("heading", { level: 1, name: "Xác thực email của bạn" }),
        ).toBeInTheDocument();
        expect(screen.getByText("minh.anh@gmail.com")).toBeInTheDocument();
        expect(screen.getByText(/mã 6 số/)).toBeInTheDocument();
    });

    it("không có email đang chờ thì vẫn hướng dẫn kiểm tra hộp thư", () => {
        render(<VerifyEmailNotice />);

        expect(screen.getByText(/email bạn đã đăng ký/)).toBeInTheDocument();
    });

    it("cho đổi email bằng cách quay lại trang đăng ký", () => {
        render(<VerifyEmailNotice />);

        expect(screen.getByRole("link", { name: "Đổi email" })).toHaveAttribute(
            "href",
            "/register",
        );
    });
});
