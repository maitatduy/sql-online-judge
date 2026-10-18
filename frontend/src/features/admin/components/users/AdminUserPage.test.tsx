import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { server } from "@/mocks/server";
import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";
import { visit } from "@/test/url-search-params";

import { AdminUserPage } from "./AdminUserPage";

vi.mock("next/navigation", async () => ({
    useSearchParams: (await import("@/test/url-search-params")).useUrlSearchParams,
}));

async function renderPage(username: string, search = "") {
    visit(`/admin/users/${username}${search}`);
    writeMockSession(ADMIN_SESSION_USER);
    renderWithProviders(
        <ToastProvider>
            <AdminUserPage username={username} />
        </ToastProvider>,
    );
}

async function findLoadedPage(username: string) {
    await screen.findByRole("heading", { level: 1, name: username });
}

function accountInfoLines() {
    const info = within(screen.getByRole("list", { name: "Thông tin tài khoản" }));
    return info.getAllByRole("listitem").map((line) => line.textContent);
}

function typeReason(dialog: ReturnType<typeof within>, reason: string) {
    fireEvent.change(dialog.getByRole("textbox", { name: /^Lý do/ }), {
        target: { value: reason },
    });
}

describe("trang chi tiết người dùng", () => {
    beforeEach(() => {
        writeMockSession(ADMIN_SESSION_USER);
    });

    it("báo đang tải trước khi có dữ liệu", async () => {
        await renderPage("viet.luu");

        expect(
            screen.getByRole("status", { name: "Đang tải thông tin người dùng" }),
        ).toBeInTheDocument();
    });

    it("hiện đầu trang, số liệu, vi phạm và thông tin tài khoản", async () => {
        await renderPage("viet.luu");
        await findLoadedPage("viet.luu");

        const breadcrumb = within(screen.getByRole("navigation", { name: "Đường dẫn" }));

        expect(breadcrumb.getByRole("link", { name: "Người dùng" })).toHaveAttribute(
            "href",
            "/admin/users",
        );
        const header = within(
            screen.getByRole("heading", { level: 1, name: "viet.luu" }).closest("section")!,
        );

        expect(header.getByText("Học viên")).toBeInTheDocument();
        expect(header.getByText("Hoạt động")).toBeInTheDocument();
        expect(screen.getByText("Rating").nextElementSibling).toHaveTextContent("1.212");
        expect(screen.getByText("Bài AC").nextElementSibling).toHaveTextContent("41");
        expect(screen.getByText("Vi phạm").nextElementSibling).toHaveTextContent("2");
        expect(screen.getByText("Nghi đạo code trong contest")).toBeInTheDocument();
        expect(screen.getByText("Tham gia").nextElementSibling).toHaveTextContent(
            "03/02/2026 qua GitHub",
        );
        expect(screen.getByRole("link", { name: "Hồ sơ công khai" })).toHaveAttribute(
            "href",
            "/u/viet.luu",
        );
    });

    it("mở modal sửa từ trang", async () => {
        await renderPage("viet.luu");
        await findLoadedPage("viet.luu");

        fireEvent.click(screen.getByRole("button", { name: "Sửa" }));

        expect(await screen.findByRole("dialog", { name: "Sửa viet.luu" })).toBeInTheDocument();
    });

    it("khoá tài khoản ngay trên trang", async () => {
        await renderPage("viet.luu");
        await findLoadedPage("viet.luu");

        fireEvent.click(await screen.findByRole("button", { name: "Khoá tạm thời" }));
        const dialog = within(await screen.findByRole("dialog", { name: "Khoá viet.luu" }));
        typeReason(dialog, "Spam bình luận");
        fireEvent.click(dialog.getByRole("button", { name: "Khoá tài khoản" }));

        expect(
            await screen.findByRole("button", { name: "Mở khoá tài khoản" }),
        ).toBeInTheDocument();
        expect(screen.getByText("Bị khoá")).toBeInTheDocument();
    });

    it("xoá rồi khôi phục tài khoản ngay trên trang", async () => {
        await renderPage("viet.luu");
        await findLoadedPage("viet.luu");

        fireEvent.click(await screen.findByRole("button", { name: "Xoá tài khoản" }));
        const deleteDialog = within(await screen.findByRole("dialog", { name: "Xoá viet.luu?" }));
        typeReason(deleteDialog, "Tạo nhầm");
        fireEvent.click(deleteDialog.getByRole("button", { name: "Xoá tài khoản" }));

        expect(
            await screen.findByText(/^Tài khoản đã bị xoá ngày \d{2}\/\d{2}\/\d{4}\.$/),
        ).toBeInTheDocument();
        expect(screen.queryByRole("button", { name: "Sửa" })).not.toBeInTheDocument();
        expect(screen.queryByRole("button", { name: "Khoá tạm thời" })).not.toBeInTheDocument();

        fireEvent.click(screen.getByRole("button", { name: "Khôi phục" }));
        const restoreDialog = within(
            await screen.findByRole("dialog", { name: "Khôi phục viet.luu?" }),
        );
        fireEvent.click(restoreDialog.getByRole("button", { name: "Khôi phục" }));

        await waitFor(() =>
            expect(screen.queryByText(/^Tài khoản đã bị xoá ngày/)).not.toBeInTheDocument(),
        );
        expect(await screen.findByRole("button", { name: "Sửa" })).toBeInTheDocument();
    });

    it("không cho khoá hay xoá tài khoản của chính mình", async () => {
        await renderPage("admin.huy");
        await findLoadedPage("admin.huy");
        await screen.findByRole("button", { name: "Sửa" });

        expect(screen.queryByRole("button", { name: "Khoá tạm thời" })).not.toBeInTheDocument();
        expect(screen.queryByRole("button", { name: "Khoá vĩnh viễn" })).not.toBeInTheDocument();
        expect(screen.queryByRole("button", { name: "Xoá tài khoản" })).not.toBeInTheDocument();
    });

    it("báo không tìm thấy kèm đường quay lại danh sách", async () => {
        await renderPage("khong.co");

        expect(await screen.findByText("Không tìm thấy người dùng")).toBeInTheDocument();
        expect(screen.getByRole("link", { name: "Về danh sách người dùng" })).toHaveAttribute(
            "href",
            "/admin/users",
        );
    });

    it("hiện lỗi và tải lại được", async () => {
        server.use(
            http.get(
                "*/api/admin/users/viet.luu",
                () =>
                    HttpResponse.json(
                        { status: 500, code: "INTERNAL_ERROR", message: "Đã có lỗi xảy ra" },
                        { status: 500 },
                    ),
                { once: true },
            ),
        );
        await renderPage("viet.luu");

        expect(await screen.findByRole("alert")).toBeInTheDocument();

        fireEvent.click(screen.getByRole("button", { name: "Thử lại" }));

        await findLoadedPage("viet.luu");
    });
});

describe("trang chi tiết người dùng - đầu trang và thẻ số", () => {
    beforeEach(() => {
        writeMockSession(ADMIN_SESSION_USER);
    });

    it("ghi tên, trường, email, xác minh và kênh tham gia, mỗi mục một dòng", async () => {
        await renderPage("viet.luu");
        await findLoadedPage("viet.luu");

        expect(accountInfoLines()).toEqual([
            "Lưu Quốc Việt",
            "ĐH Mở Hà Nội",
            "viet.luu@example.com",
            "Đã xác minh",
            "Tham gia 03/02/2026 qua GitHub",
        ]);
    });

    it("bỏ qua dòng không có dữ liệu trong thông tin tài khoản", async () => {
        await renderPage("tam.nguyen");
        await findLoadedPage("tam.nguyen");

        expect(accountInfoLines()).toEqual([
            "Nguyễn Thành Tâm",
            "tam.nguyen@example.com",
            "Chưa xác minh",
            "Tham gia 11/10/2026 qua email",
        ]);
    });

    it("hiện đủ 5 thẻ số", async () => {
        await renderPage("viet.luu");
        await findLoadedPage("viet.luu");

        const stats = within(screen.getByRole("region", { name: "Số liệu" }));

        expect(stats.getByText("Rating").nextElementSibling).toHaveTextContent("1.212");
        expect(stats.getByText("Bài AC").nextElementSibling).toHaveTextContent("41");
        expect(stats.getByText("Bài nộp").nextElementSibling).toHaveTextContent("286");
        expect(stats.getByText("Contest").nextElementSibling).toHaveTextContent("9");
        expect(stats.getByText("Vi phạm").nextElementSibling).toHaveTextContent("2");
    });

    it.each([
        ["Khoá tạm thời", "7 ngày"],
        ["Khoá vĩnh viễn", "Vĩnh viễn"],
    ])("nút %s mở hộp thoại khoá chọn sẵn %s", async (button, duration) => {
        await renderPage("viet.luu");
        await findLoadedPage("viet.luu");

        fireEvent.click(await screen.findByRole("button", { name: button }));
        const dialog = within(await screen.findByRole("dialog", { name: "Khoá viet.luu" }));

        expect(dialog.getByRole("radio", { name: duration })).toBeChecked();
    });

    it("chưa có nút gửi tin nhắn", async () => {
        await renderPage("viet.luu");
        await findLoadedPage("viet.luu");
        await screen.findByRole("button", { name: "Khoá tạm thời" });

        expect(screen.queryByRole("button", { name: "Gửi tin nhắn" })).not.toBeInTheDocument();
        expect(screen.queryByRole("link", { name: "Gửi tin nhắn" })).not.toBeInTheDocument();
    });
});

describe("trang chi tiết người dùng - tab và vi phạm", () => {
    const tabs = () => within(screen.getByRole("navigation", { name: "Mục chi tiết" }));

    beforeEach(() => {
        writeMockSession(ADMIN_SESSION_USER);
    });

    it("có 4 tab trong trang và 2 liên kết sang màn khác", async () => {
        await renderPage("viet.luu");
        await findLoadedPage("viet.luu");

        expect(tabs().getByRole("button", { name: "Tổng quan" })).toHaveAttribute(
            "aria-current",
            "page",
        );
        expect(tabs().getByRole("button", { name: "Contest" })).toBeInTheDocument();
        expect(tabs().getByRole("button", { name: "Vi phạm · 2" })).toBeInTheDocument();
        expect(tabs().getByRole("button", { name: "Phiên đăng nhập" })).toBeInTheDocument();
        expect(tabs().getByRole("link", { name: "Bài nộp" })).toHaveAttribute(
            "href",
            "/admin/submissions?user=viet.luu",
        );
        expect(tabs().getByRole("link", { name: "Nhật ký" })).toHaveAttribute(
            "href",
            "/admin/audit-log?user=viet.luu",
        );
    });

    it("tổng quan hiện vi phạm gần nhất kèm trạng thái và liên kết hàng đợi báo cáo", async () => {
        await renderPage("viet.luu");
        await findLoadedPage("viet.luu");

        const recent = within(screen.getByRole("region", { name: "Vi phạm gần đây" }));

        expect(
            recent
                .getAllByRole("listitem")
                .map((item) => item.querySelector("strong")?.textContent),
        ).toEqual(["Spam bình luận", "Nghi đạo code trong contest"]);
        expect(recent.getByText("Chờ xử lý")).toBeInTheDocument();
        expect(recent.getByText("Đã xử lý")).toBeInTheDocument();
        expect(recent.getByRole("link", { name: "Mở hàng đợi báo cáo" })).toHaveAttribute(
            "href",
            "/admin/reports",
        );
    });

    it("chuyển sang tab Vi phạm, ghi tab lên URL và hiện người xử lý", async () => {
        await renderPage("viet.luu");
        await findLoadedPage("viet.luu");

        fireEvent.click(tabs().getByRole("button", { name: "Vi phạm · 2" }));

        const history = within(await screen.findByRole("region", { name: "Lịch sử vi phạm" }));

        expect(window.location.search).toBe("?tab=violations");
        expect(tabs().getByRole("button", { name: "Vi phạm · 2" })).toHaveAttribute(
            "aria-current",
            "page",
        );
        expect(history.getByText("Xử lý bởi mod.thu · 12/09/2026")).toBeInTheDocument();
        expect(history.getAllByRole("listitem")).toHaveLength(2);
        expect(screen.queryByRole("region", { name: "Vi phạm gần đây" })).not.toBeInTheDocument();
    });

    it("mở đúng tab ghi trên URL", async () => {
        await renderPage("viet.luu", "?tab=violations");

        expect(await screen.findByRole("region", { name: "Lịch sử vi phạm" })).toBeInTheDocument();
    });

    it("về tab Tổng quan khi tab trên URL không hợp lệ", async () => {
        await renderPage("viet.luu", "?tab=khong-co");
        await findLoadedPage("viet.luu");

        expect(screen.getByRole("region", { name: "Vi phạm gần đây" })).toBeInTheDocument();
    });

    it("báo chưa có vi phạm ở tab Vi phạm của tài khoản sạch", async () => {
        await renderPage("tam.nguyen", "?tab=violations");

        const history = within(await screen.findByRole("region", { name: "Lịch sử vi phạm" }));

        expect(history.getByText("Chưa có vi phạm nào.")).toBeInTheDocument();
    });
});
