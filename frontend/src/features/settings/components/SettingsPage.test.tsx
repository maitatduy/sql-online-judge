import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { MOCK_CURRENT_PASSWORD, resetMockAccount } from "@/mocks/account";
import { DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { SettingsPage } from "./SettingsPage";

function renderSettings() {
    return renderWithProviders(
        <ToastProvider>
            <SettingsPage />
        </ToastProvider>,
    );
}

function profileSection(): HTMLElement {
    return screen.getByRole("region", { name: "Hồ sơ công khai" });
}

function passwordSection(): HTMLElement {
    return screen.getByRole("region", { name: "Đổi mật khẩu" });
}

describe("SettingsPage", () => {
    beforeEach(() => {
        resetMockAccount();
        writeMockSession(DEMO_SESSION_USER);
    });

    it("điền sẵn hồ sơ hiện tại và lưu thay đổi", async () => {
        renderSettings();

        const displayName = await screen.findByLabelText("Tên hiển thị");
        expect(displayName).toHaveValue("Nguyễn Minh Anh");
        expect(screen.getByLabelText("Trường, công ty")).toHaveValue("ĐH Bách khoa Hà Nội");

        const save = within(profileSection()).getByRole("button", { name: "Lưu thay đổi" });
        expect(save).toBeDisabled();

        fireEvent.change(screen.getByLabelText("Nơi sống"), { target: { value: "Đà Nẵng" } });
        fireEvent.click(save);

        expect(await screen.findByText("Đã lưu hồ sơ")).toBeInTheDocument();
        expect(screen.getByLabelText("Nơi sống")).toHaveValue("Đà Nẵng");
    });

    it("báo lỗi ngay ở trường khi tên trống hoặc website sai dạng", async () => {
        renderSettings();

        fireEvent.change(await screen.findByLabelText("Tên hiển thị"), {
            target: { value: " " },
        });
        fireEvent.change(screen.getByLabelText("Website"), {
            target: { value: "javascript:alert(1)" },
        });
        fireEvent.click(within(profileSection()).getByRole("button", { name: "Lưu thay đổi" }));

        expect(await screen.findByText("Vui lòng nhập tên hiển thị")).toBeInTheDocument();
        expect(
            screen.getByText("Đường dẫn phải bắt đầu bằng http:// hoặc https://"),
        ).toBeInTheDocument();
        expect(screen.getByLabelText("Website")).toHaveAttribute("aria-invalid", "true");
    });

    it("gắn lỗi sai mật khẩu hiện tại vào đúng trường, đúng thì báo đã đổi", async () => {
        renderSettings();

        const section = await screen.findByRole("region", { name: "Đổi mật khẩu" });
        fireEvent.change(within(section).getByLabelText("Mật khẩu hiện tại"), {
            target: { value: "sai-mat-khau" },
        });
        fireEvent.change(within(section).getByLabelText("Mật khẩu mới"), {
            target: { value: "matkhau-moi-2026" },
        });
        fireEvent.change(within(section).getByLabelText("Nhập lại mật khẩu mới"), {
            target: { value: "matkhau-moi-2026" },
        });
        fireEvent.click(within(section).getByRole("button", { name: "Đổi mật khẩu" }));

        expect(
            await within(section).findByText("Mật khẩu hiện tại không đúng"),
        ).toBeInTheDocument();

        fireEvent.change(within(passwordSection()).getByLabelText("Mật khẩu hiện tại"), {
            target: { value: MOCK_CURRENT_PASSWORD },
        });
        fireEvent.click(within(passwordSection()).getByRole("button", { name: "Đổi mật khẩu" }));

        expect(await screen.findByText("Đã đổi mật khẩu")).toBeInTheDocument();
        await waitFor(() =>
            expect(within(passwordSection()).getByLabelText("Mật khẩu mới")).toHaveValue(""),
        );
    });

    it("báo mật khẩu nhập lại không khớp mà không gửi lên máy chủ", async () => {
        renderSettings();

        const section = await screen.findByRole("region", { name: "Đổi mật khẩu" });
        fireEvent.change(within(section).getByLabelText("Mật khẩu mới"), {
            target: { value: "matkhau-moi-2026" },
        });
        fireEvent.change(within(section).getByLabelText("Nhập lại mật khẩu mới"), {
            target: { value: "khac-hoan-toan" },
        });
        fireEvent.click(within(section).getByRole("button", { name: "Đổi mật khẩu" }));

        expect(
            await within(section).findByText("Mật khẩu nhập lại không khớp"),
        ).toBeInTheDocument();
    });

    it("đổi rồi gỡ ảnh đại diện", async () => {
        renderSettings();

        const fileInput = await screen.findByLabelText("Chọn ảnh đại diện");
        fireEvent.change(fileInput, {
            target: { files: [new File(["png"], "anh.png", { type: "image/png" })] },
        });

        expect(await screen.findByText("Đã đổi ảnh đại diện")).toBeInTheDocument();
        fireEvent.click(await screen.findByRole("button", { name: "Gỡ ảnh" }));

        expect(await screen.findByText("Đã gỡ ảnh đại diện")).toBeInTheDocument();
        await waitFor(() =>
            expect(screen.queryByRole("button", { name: "Gỡ ảnh" })).not.toBeInTheDocument(),
        );
    });

    it("chặn ảnh lớn hơn 2 MB ngay ở trình duyệt", async () => {
        renderSettings();

        const tooLarge = new File([new Uint8Array(2 * 1024 * 1024 + 1)], "lon.png", {
            type: "image/png",
        });
        fireEvent.change(await screen.findByLabelText("Chọn ảnh đại diện"), {
            target: { files: [tooLarge] },
        });

        expect(await screen.findByText("Ảnh tối đa 2 MB")).toBeInTheDocument();
    });

    it("mời đăng nhập khi chưa đăng nhập", async () => {
        sessionStorage.clear();
        renderSettings();

        expect(await screen.findByText("Đăng nhập để chỉnh cài đặt")).toBeInTheDocument();
        expect(screen.getByRole("link", { name: "Đăng nhập" })).toHaveAttribute(
            "href",
            "/login?next=%2Fsettings",
        );
    });
});
