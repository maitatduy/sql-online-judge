import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { http } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { apiError } from "@/mocks/api-error";
import { server } from "@/mocks/server";
import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";
import { visit } from "@/test/url-search-params";

import { AdminAuditLog } from "./AdminAuditLog";

vi.mock("next/navigation", async () => ({
    useSearchParams: (await import("@/test/url-search-params")).useUrlSearchParams,
}));

async function renderLog(search = "") {
    visit(`/admin/audit-log${search}`);
    writeMockSession(ADMIN_SESSION_USER);
    renderWithProviders(<AdminAuditLog />);
    return within(await screen.findByRole("region", { name: "Bảng nhật ký" }));
}

const findDetailButtons = (table: ReturnType<typeof within>) =>
    table.findAllByRole("button", { name: /^Xem chi tiết/ });

describe("trang nhật ký thao tác", () => {
    beforeEach(() => {
        visit("/admin/audit-log");
    });

    it("hiện thao tác mới nhất trước cùng người thực hiện, đối tượng và lý do", async () => {
        const table = await renderLog();
        await findDetailButtons(table);

        expect(screen.getByRole("heading", { level: 1, name: "Nhật ký thao tác" })).toBeVisible();
        const firstRow = table.getAllByRole("row")[1];
        expect(firstRow).toHaveTextContent("admin.huy");
        expect(firstRow).toHaveTextContent("Đổi vai trò");
        expect(firstRow).toHaveTextContent("datahust");
        expect(firstRow).toHaveTextContent("CLB được duyệt làm người ra đề");
        expect(screen.getByRole("navigation", { name: "Phân trang" })).toBeInTheDocument();
    });

    it("mở từ trang người dùng thì chỉ hiện thao tác trên tài khoản đó", async () => {
        const table = await renderLog("?user=viet.luu");

        await waitFor(() =>
            table
                .getAllByRole("row")
                .slice(1)
                .forEach((row) => expect(row).toHaveTextContent("viet.luu")),
        );
        expect(screen.getByText("Đối tượng: viet.luu")).toBeInTheDocument();

        fireEvent.click(screen.getByRole("button", { name: "Bỏ lọc đối tượng" }));

        await waitFor(() => expect(window.location.search).toBe(""));
    });

    it("lọc theo loại thao tác và ghi bộ lọc lên URL", async () => {
        const table = await renderLog();

        fireEvent.change(screen.getByRole("combobox", { name: "Loại thao tác" }), {
            target: { value: "BAN" },
        });

        await waitFor(() => expect(window.location.search).toBe("?action=BAN"));
        await waitFor(() =>
            table
                .getAllByRole("row")
                .slice(1)
                .forEach((row) => expect(row).toHaveTextContent("Khoá tài khoản")),
        );
    });

    it("bấm một dòng thì panel chi tiết hiện giá trị trước, sau và lý do", async () => {
        const table = await renderLog();

        fireEvent.click((await findDetailButtons(table))[0]);

        const detail = within(screen.getByRole("complementary", { name: "Chi tiết thao tác" }));
        expect(detail.getByText("Trước: LEARNER")).toBeInTheDocument();
        expect(detail.getByText("Sau: SETTER")).toBeInTheDocument();
        expect(detail.getByText("CLB được duyệt làm người ra đề")).toBeInTheDocument();
        expect(detail.getByRole("link", { name: "datahust" })).toHaveAttribute(
            "href",
            "/admin/users/datahust",
        );
    });

    it("chọn dòng thì chuyển focus tới panel, bấm Đóng thì panel biến mất", async () => {
        const table = await renderLog();

        fireEvent.click((await findDetailButtons(table))[0]);

        expect(screen.getByRole("heading", { level: 2, name: "Chi tiết" })).toHaveFocus();
        fireEvent.click(screen.getByRole("button", { name: "Đóng chi tiết" }));
        expect(
            screen.queryByRole("complementary", { name: "Chi tiết thao tác" }),
        ).not.toBeInTheDocument();
    });

    it("đổi bộ lọc làm dòng đang chọn biến khỏi bảng thì panel cũng biến mất", async () => {
        const table = await renderLog();
        fireEvent.click((await findDetailButtons(table))[0]);

        fireEvent.change(screen.getByRole("combobox", { name: "Loại thao tác" }), {
            target: { value: "BAN" },
        });

        await waitFor(() =>
            expect(
                screen.queryByRole("complementary", { name: "Chi tiết thao tác" }),
            ).not.toBeInTheDocument(),
        );
    });

    it("không có kết quả thì hiện trạng thái trống", async () => {
        await renderLog("?actor=khong.ai");

        expect(await screen.findByText("Không có thao tác nào")).toBeInTheDocument();
    });

    it("lỗi tải thì hiện trạng thái lỗi kèm nút thử lại", async () => {
        server.use(
            http.get("*/api/admin/audit-log", () =>
                apiError(500, "INTERNAL_ERROR", "Đã có lỗi xảy ra, vui lòng thử lại sau"),
            ),
        );
        visit("/admin/audit-log");
        writeMockSession(ADMIN_SESSION_USER);
        renderWithProviders(<AdminAuditLog />);

        expect(await screen.findByText("Chưa tải được nhật ký")).toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Thử lại" })).toBeInTheDocument();
    });
});
