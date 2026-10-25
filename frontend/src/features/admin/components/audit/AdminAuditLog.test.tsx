import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { http } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { apiError } from "@/mocks/api-error";
import { server } from "@/mocks/server";
import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";
import { stubDownloads } from "@/test/downloads";
import { renderWithProviders } from "@/test/render";
import { visit } from "@/test/url-search-params";

import { AdminAuditLog } from "./AdminAuditLog";

vi.mock("next/navigation", async () => ({
    useSearchParams: (await import("@/test/url-search-params")).useUrlSearchParams,
}));

async function renderLog(search = "") {
    visit(`/admin/audit-log${search}`);
    writeMockSession(ADMIN_SESSION_USER);
    renderWithProviders(
        <ToastProvider>
            <AdminAuditLog />
        </ToastProvider>,
    );
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
        expect(firstRow).toHaveTextContent("datahust: LEARNER → SETTER");
        expect(firstRow).toHaveTextContent("CLB được duyệt làm người ra đề");
        expect(screen.getByRole("navigation", { name: "Phân trang" })).toBeInTheDocument();
        expect(screen.getByText("1-20 trong 24 thao tác")).toBeInTheDocument();
        expect(table.getByRole("columnheader", { name: "Hành động" })).toBeVisible();
    });

    it("bấm Xuất CSV thì tải file theo đúng bộ lọc đang xem", async () => {
        const downloads = stubDownloads();
        const exportUrls: string[] = [];
        server.events.on("request:start", ({ request }) => {
            if (request.url.includes("/api/admin/audit-log/export")) {
                exportUrls.push(request.url);
            }
        });
        await renderLog("?q=spam");

        fireEvent.click(screen.getByRole("button", { name: "Xuất CSV" }));

        await waitFor(() =>
            expect(downloads.fileNames).toEqual([
                expect.stringMatching(/^audit-log-\d{8}-\d{4}\.csv$/),
            ]),
        );
        const query = new URL(exportUrls[0]).searchParams;
        expect(query.get("q")).toBe("spam");
        expect(query.get("from")).not.toBeNull();
        server.events.removeAllListeners();
    });

    it("xuất CSV lỗi thì báo bằng thông báo", async () => {
        server.use(
            http.get("*/api/admin/audit-log/export", () =>
                apiError(500, "INTERNAL_ERROR", "Đã có lỗi xảy ra, vui lòng thử lại sau"),
            ),
        );
        await renderLog();

        fireEvent.click(screen.getByRole("button", { name: "Xuất CSV" }));

        expect(
            await screen.findByText(
                "Chưa xuất được nhật ký. Đã có lỗi xảy ra, vui lòng thử lại sau",
            ),
        ).toBeInTheDocument();
    });

    it("thanh lọc có ô tìm kiếm và ba ô chọn như thiết kế", async () => {
        await renderLog();
        const filters = within(screen.getByRole("search", { name: "Bộ lọc nhật ký" }));

        expect(filters.getByRole("searchbox", { name: "Tìm trong nhật ký" })).toHaveAttribute(
            "placeholder",
            "Người thực hiện, đối tượng, lý do…",
        );
        const optionsOf = (name: string) =>
            within(filters.getByRole("combobox", { name }))
                .getAllByRole("option")
                .map((option) => option.textContent);
        expect(optionsOf("Loại thao tác")).toEqual([
            "Mọi thao tác",
            "Tài khoản",
            "Bài tập (sắp có)",
            "Bài nộp (sắp có)",
            "Contest (sắp có)",
            "Cấu hình (sắp có)",
        ]);
        expect(filters.getByRole("option", { name: "Tài khoản" })).toBeEnabled();
        expect(filters.getByRole("option", { name: "Bài tập (sắp có)" })).toBeDisabled();
        expect(optionsOf("Khoảng thời gian")).toEqual([
            "7 ngày qua",
            "Hôm nay",
            "30 ngày qua",
            "Mọi lúc",
        ]);
        expect(filters.getByRole("combobox", { name: "Khoảng thời gian" })).toHaveValue("7d");
        await waitFor(() =>
            expect(optionsOf("Người thực hiện")).toEqual(["Mọi người", "admin.huy"]),
        );
    });

    it("tìm theo từ khoá thì ghi lên URL và chỉ còn dòng khớp", async () => {
        const table = await renderLog();
        await findDetailButtons(table);

        fireEvent.change(screen.getByRole("searchbox", { name: "Tìm trong nhật ký" }), {
            target: { value: "spam" },
        });

        await waitFor(() => expect(window.location.search).toBe("?q=spam"));
        await waitFor(() =>
            table
                .getAllByRole("row")
                .slice(1)
                .forEach((row) => expect(row).toHaveTextContent(/spam/i)),
        );
    });

    it("chọn nhóm Tài khoản thì ghi lên URL", async () => {
        await renderLog();

        fireEvent.change(screen.getByRole("combobox", { name: "Loại thao tác" }), {
            target: { value: "ACCOUNT" },
        });

        await waitFor(() => expect(window.location.search).toBe("?category=ACCOUNT"));
    });

    it("chọn người thực hiện thì ghi lên URL", async () => {
        await renderLog();
        const actorSelect = screen.getByRole("combobox", { name: "Người thực hiện" });
        await within(actorSelect).findByRole("option", { name: "admin.huy" });

        fireEvent.change(actorSelect, { target: { value: "admin.huy" } });

        await waitFor(() => expect(window.location.search).toBe("?actor=admin.huy"));
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

    it("chưa bấm nút xem thì không mở chi tiết nào", async () => {
        const table = await renderLog();
        await findDetailButtons(table);

        expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
        expect(screen.queryByRole("complementary")).not.toBeInTheDocument();
    });

    it("bấm nút xem thì mở modal chi tiết đúng dòng đó", async () => {
        const table = await renderLog();

        fireEvent.click((await findDetailButtons(table))[0]);

        const detail = within(await screen.findByRole("dialog", { name: "Chi tiết thao tác" }));
        expect(detail.getByText("Thay đổi")).toBeInTheDocument();
        expect(detail.getByText('- role: "LEARNER"')).toBeInTheDocument();
        expect(detail.getByText('+ role: "SETTER"')).toBeInTheDocument();
        expect(detail.getByText("CLB được duyệt làm người ra đề")).toBeInTheDocument();
        expect(detail.getByRole("link", { name: "datahust" })).toHaveAttribute(
            "href",
            "/admin/users/datahust",
        );
    });

    it("đóng modal thì trả focus về nút xem đã mở nó", async () => {
        const table = await renderLog();
        const secondView = (await findDetailButtons(table))[1];
        secondView.focus();

        fireEvent.click(secondView);
        const detail = within(await screen.findByRole("dialog", { name: "Chi tiết thao tác" }));
        expect(detail.getByRole("link", { name: "spam.bot88" })).toBeInTheDocument();
        fireEvent.click(detail.getByRole("button", { name: "Đóng" }));

        await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
        expect(secondView).toHaveFocus();
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
        await renderLog();

        expect(await screen.findByText("Chưa tải được nhật ký")).toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Thử lại" })).toBeInTheDocument();
    });
});
