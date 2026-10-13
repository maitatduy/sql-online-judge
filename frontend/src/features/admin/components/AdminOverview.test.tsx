import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { buildAdminOverview } from "@/mocks/admin-fixtures";
import { server } from "@/mocks/server";
import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { AdminOverview } from "./AdminOverview";

function renderOverview() {
    writeMockSession(ADMIN_SESSION_USER);
    return renderWithProviders(
        <ToastProvider>
            <AdminOverview />
        </ToastProvider>,
    );
}

async function findTile(name: string) {
    return within(await screen.findByRole("group", { name }));
}

describe("AdminOverview", () => {
    it("hiện số liệu tính từ trước đến giờ kèm mức tăng", async () => {
        renderOverview();

        const users = await findTile("Tổng người dùng");

        expect(users.getByText("48.260")).toBeInTheDocument();
        expect(users.getByText("Tăng 2% so với tuần trước")).toBeInTheDocument();
        expect((await findTile("Bài nộp")).getByText("1.284.530")).toBeInTheDocument();
        expect((await findTile("Tin nhắn trợ lý AI")).getByText("3.905.110")).toBeInTheDocument();
    });

    it("không có bộ lọc khoảng thời gian và dòng thời điểm cập nhật dưới tiêu đề", async () => {
        renderOverview();
        await findTile("Tổng người dùng");

        expect(screen.queryByRole("group", { name: "Khoảng thời gian" })).not.toBeInTheDocument();
        expect(screen.queryByRole("button", { name: "24 giờ" })).not.toBeInTheDocument();
        expect(screen.queryByText(/Cập nhật lúc \d{2}:\d{2}, /)).not.toBeInTheDocument();
    });

    it("hiện tổng số bài nộp kèm biểu đồ tròn phân bố kết quả chấm", async () => {
        renderOverview();

        expect(
            await screen.findByRole("heading", { level: 2, name: "Tổng số bài nộp" }),
        ).toBeInTheDocument();
        expect(
            screen.getByRole("img", { name: /^Tổng 1\.284\.530 bài nộp: AC 43%/ }),
        ).toBeInTheDocument();
        expect(screen.queryByText(/Bài nộp theo giờ/)).not.toBeInTheDocument();
        expect(
            screen.getByRole("heading", { level: 3, name: /^Bài nộp theo tháng năm \d{4}$/ }),
        ).toBeInTheDocument();
        expect(
            within(screen.getByRole("group", { name: "Hôm nay" })).getByText("9.807"),
        ).toBeInTheDocument();
    });

    it("hiện danh sách việc cần xử lý", async () => {
        renderOverview();

        expect(screen.getByRole("heading", { level: 2, name: "Cần xử lý" })).toBeInTheDocument();
        expect(await screen.findByRole("link", { name: /^Bài chờ duyệt/ })).toHaveAttribute(
            "href",
            "/admin/problem-reviews",
        );
    });

    it("hiện tình trạng máy chấm", async () => {
        renderOverview();

        expect(screen.getByRole("heading", { level: 2, name: /^Máy chấm/ })).toBeInTheDocument();
        expect(await screen.findByText("5/6 hoạt động")).toBeInTheDocument();
    });

    it("cảnh báo bằng chữ khi hàng đợi chấm đang cao", async () => {
        renderOverview();

        const queue = await findTile("Hàng đợi chấm");

        expect(queue.getByText("146")).toBeInTheDocument();
        expect(queue.getByText("Chờ trung bình 3,8 giây")).toBeInTheDocument();
        expect(queue.getByText("Cao hơn bình thường")).toBeInTheDocument();
    });

    it("không cảnh báo khi hàng đợi chấm bình thường", async () => {
        server.use(
            http.get("*/api/admin/overview", () =>
                HttpResponse.json({
                    ...buildAdminOverview(),
                    queue: { size: 107, averageWaitMs: 2100, isDegraded: false },
                }),
            ),
        );
        renderOverview();

        const queue = await findTile("Hàng đợi chấm");

        expect(queue.getByText("107")).toBeInTheDocument();
        expect(queue.queryByText("Cao hơn bình thường")).not.toBeInTheDocument();
    });

    it("hiện chữ Không đổi khi chỉ số không thay đổi", async () => {
        server.use(
            http.get("*/api/admin/overview", () =>
                HttpResponse.json({
                    ...buildAdminOverview(),
                    submissions: {
                        value: 1284530,
                        changePercent: 0,
                        changeNote: "so với tuần trước",
                    },
                }),
            ),
        );
        renderOverview();

        expect(
            (await findTile("Bài nộp")).getByText("Không đổi so với tuần trước"),
        ).toBeInTheDocument();
    });

    it("báo số liệu đã cũ kèm thời điểm khi làm mới nền bị lỗi", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
        render(
            <QueryClientProvider client={queryClient}>
                <ToastProvider>
                    <AdminOverview />
                </ToastProvider>
            </QueryClientProvider>,
        );
        await findTile("Tổng người dùng");

        expect(screen.queryByText(/Chưa cập nhật được số liệu mới/)).not.toBeInTheDocument();

        server.use(
            http.get("*/api/admin/overview", () =>
                HttpResponse.json(
                    { status: 500, code: "INTERNAL_ERROR", message: "Đã có lỗi xảy ra" },
                    { status: 500 },
                ),
            ),
        );
        await queryClient.invalidateQueries({ queryKey: ["admin", "overview"] });
        await waitFor(() => expect(queryClient.isFetching()).toBe(0));

        expect(
            await screen.findByText(
                /^Chưa cập nhật được số liệu mới\. Đang hiện số liệu lúc \d{2}:\d{2}\.$/,
            ),
        ).toBeInTheDocument();
        expect((await findTile("Tổng người dùng")).getByText("48.260")).toBeInTheDocument();
    });

    it("báo đang tải trước khi có số liệu", () => {
        renderOverview();

        expect(screen.getByRole("status", { name: "Đang tải số liệu tổng quan" })).toBeVisible();
        expect(screen.getByRole("heading", { level: 1, name: "Tổng quan" })).toBeInTheDocument();
    });

    it("hiện lỗi kèm mã lỗi và tải lại được", async () => {
        server.use(
            http.get(
                "*/api/admin/overview",
                () =>
                    HttpResponse.json(
                        {
                            status: 500,
                            code: "INTERNAL_ERROR",
                            message: "Đã có lỗi xảy ra",
                            traceId: "4bf92f3577b34da6",
                        },
                        { status: 500 },
                    ),
                { once: true },
            ),
        );
        renderOverview();

        expect(await screen.findByRole("alert")).toHaveTextContent("4bf92f3577b34da6");

        fireEvent.click(screen.getByRole("button", { name: "Thử lại" }));

        expect((await findTile("Tổng người dùng")).getByText("48.260")).toBeInTheDocument();
    });
});
