import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { server } from "@/mocks/server";
import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";
import { visit } from "@/test/url-search-params";

import { AdminUsers } from "./AdminUsers";

vi.mock("next/navigation", async () => ({
    useSearchParams: (await import("@/test/url-search-params")).useUrlSearchParams,
}));

const DIALOG_NAME = "Chi tiết người dùng";

function renderUsers(search = "") {
    visit(`/admin/users${search}`);
    writeMockSession(ADMIN_SESSION_USER);
    renderWithProviders(
        <ToastProvider>
            <AdminUsers />
        </ToastProvider>,
    );
}

async function findDetail() {
    return within(await screen.findByRole("dialog", { name: DIALOG_NAME }));
}

function factOf(detail: ReturnType<typeof within>, label: string) {
    return detail.getByText(label).nextElementSibling;
}

describe("modal chi tiết người dùng", () => {
    beforeEach(() => {
        visit("/admin/users");
    });

    it("không mở khi chưa chọn tài khoản nào", async () => {
        renderUsers();
        await screen.findByRole("row", { name: "tam.nguyen" });

        expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    });

    it("bấm Xem chi tiết thì mở modal, ghi tài khoản lên URL và đưa focus vào modal", async () => {
        renderUsers();

        fireEvent.click(await screen.findByRole("button", { name: "Xem chi tiết tam.nguyen" }));

        const detail = await findDetail();

        expect(await detail.findByText("Nguyễn Thành Tâm")).toBeInTheDocument();
        expect(factOf(detail, "Email")).toHaveTextContent("tam.nguyen@example.com");
        expect(factOf(detail, "Xác minh email")).toHaveTextContent("Chưa xác minh");
        expect(factOf(detail, "Đăng nhập gần nhất")).toHaveTextContent("10 phút trước");
        expect(factOf(detail, "Tham gia")).toHaveTextContent("11/10/2026 qua email");
        expect(detail.getByText("Chưa có vi phạm nào.")).toBeInTheDocument();
        expect(window.location.search).toBe("?user=tam.nguyen");
        expect(
            screen.getByRole("dialog", { name: DIALOG_NAME }).contains(document.activeElement),
        ).toBe(true);
    });

    it("mở theo URL kể cả khi tài khoản không nằm ở trang đang xem", async () => {
        renderUsers("?user=viet.luu");

        const detail = await findDetail();

        expect(await detail.findByText("Lưu Quốc Việt")).toBeInTheDocument();
        expect(detail.getByText("Học viên")).toBeInTheDocument();
        expect(factOf(detail, "Rating")).toHaveTextContent("1.212");
        expect(factOf(detail, "Bài AC")).toHaveTextContent("41");
        expect(factOf(detail, "Vi phạm")).toHaveTextContent("2");
        expect(factOf(detail, "Tham gia")).toHaveTextContent("03/02/2026 qua GitHub");
        expect(detail.getByText("Nghi đạo code trong contest")).toBeInTheDocument();
        expect(detail.getByRole("link", { name: "Mở trang chi tiết" })).toHaveAttribute(
            "href",
            "/admin/users/viet.luu",
        );
        expect(detail.getByRole("link", { name: "Xem hồ sơ công khai" })).toHaveAttribute(
            "href",
            "/u/viet.luu",
        );
    });

    it("nêu thời hạn khoá và ghi Chưa đặt tên khi tài khoản không có tên", async () => {
        renderUsers("?user=copy.paste");

        const detail = await findDetail();

        expect(await detail.findByText(/^đến \d{2}\/\d{2}\/\d{4}$/)).toBeInTheDocument();
        expect(detail.getByText("Chưa đặt tên")).toBeInTheDocument();
    });

    it("ghi vĩnh viễn khi khoá không có thời hạn", async () => {
        renderUsers("?user=spam.bot88");

        expect(await (await findDetail()).findByText("vĩnh viễn")).toBeInTheDocument();
    });

    it("đóng bằng nút Đóng và giữ bộ lọc trên URL", async () => {
        renderUsers("?role=LEARNER&user=viet.luu");
        const detail = await findDetail();

        fireEvent.click(detail.getByRole("button", { name: "Đóng" }));

        await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
        expect(window.location.search).toBe("?role=LEARNER");
    });

    it("đóng bằng phím Esc", async () => {
        renderUsers("?user=viet.luu");
        await (await findDetail()).findByText("Lưu Quốc Việt");

        fireEvent.keyDown(screen.getByRole("dialog", { name: DIALOG_NAME }), { key: "Escape" });

        await waitFor(() => expect(window.location.search).toBe(""));
    });

    it("báo đang tải trước khi có chi tiết", async () => {
        renderUsers("?user=viet.luu");

        expect(
            await screen.findByRole("status", { name: "Đang tải chi tiết người dùng" }),
        ).toBeInTheDocument();
    });

    it("hiện lỗi trong modal và tải lại được", async () => {
        server.use(
            http.get(
                "*/api/admin/users/viet.luu",
                () =>
                    HttpResponse.json(
                        {
                            status: 500,
                            code: "INTERNAL_ERROR",
                            message: "Đã có lỗi xảy ra",
                            traceId: "9f1c",
                        },
                        { status: 500 },
                    ),
                { once: true },
            ),
        );
        renderUsers("?user=viet.luu");

        const detail = await findDetail();

        expect(await detail.findByRole("alert")).toHaveTextContent("9f1c");

        fireEvent.click(detail.getByRole("button", { name: "Thử lại" }));

        expect(await detail.findByText("Lưu Quốc Việt")).toBeInTheDocument();
    });

    it("đóng và báo khi tài khoản trên URL không tồn tại", async () => {
        renderUsers("?user=khong.co");

        expect(await screen.findByText("Không tìm thấy người dùng khong.co")).toBeInTheDocument();
        await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
        expect(window.location.search).toBe("");
    });
});
