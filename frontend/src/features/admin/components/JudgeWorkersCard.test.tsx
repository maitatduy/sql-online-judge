import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { server } from "@/mocks/server";
import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import type { JudgeWorker } from "../types";
import { JudgeWorkersCard } from "./JudgeWorkersCard";

const HEALTHY_WORKER: JudgeWorker = {
    name: "judge-pg-01",
    dialect: "POSTGRESQL",
    status: "ONLINE",
    judgedPerHour: 1250,
    cpuPercent: 41,
    disconnectedAt: null,
};

function renderCard() {
    writeMockSession(ADMIN_SESSION_USER);
    return renderWithProviders(
        <ToastProvider>
            <JudgeWorkersCard />
        </ToastProvider>,
    );
}

function respondWithWorkers(items: JudgeWorker[]) {
    server.use(http.get("*/api/admin/judge/workers", () => HttpResponse.json({ items })));
}

async function findWorkerRow(name: string) {
    return within(await screen.findByRole("row", { name: new RegExp(`^${name}`) }));
}

describe("JudgeWorkersCard", () => {
    it("liệt kê mọi worker với đủ bốn cột", async () => {
        renderCard();
        await findWorkerRow("judge-pg-01");

        expect(screen.getAllByRole("columnheader").map((header) => header.textContent)).toEqual([
            "Worker",
            "Trạng thái",
            "Đã chấm/giờ",
            "CPU",
        ]);
        expect(screen.getAllByRole("row")).toHaveLength(7);
    });

    it("hiện trạng thái bằng chữ, số bài đã chấm và CPU của worker đang bận", async () => {
        renderCard();

        const busyWorker = await findWorkerRow("judge-pg-01");

        expect(busyWorker.getByText("Bận")).toBeInTheDocument();
        expect(busyWorker.getByText("212")).toBeInTheDocument();
        expect(busyWorker.getByText("92%")).toBeInTheDocument();
    });

    it("không hiện CPU của worker mất kết nối", async () => {
        renderCard();

        const lostWorker = await findWorkerRow("judge-my-02");

        expect(lostWorker.getByText("Mất kết nối")).toBeInTheDocument();
        expect(lostWorker.getByText("Không có số liệu CPU")).toBeInTheDocument();
        expect(lostWorker.queryByText(/%$/)).not.toBeInTheDocument();
    });

    it("nêu số worker đang hoạt động trên tổng số", async () => {
        renderCard();

        expect(await screen.findByText("5/6 hoạt động")).toBeInTheDocument();
    });

    it("tính đủ khi mọi worker đều hoạt động", async () => {
        respondWithWorkers([HEALTHY_WORKER, { ...HEALTHY_WORKER, name: "judge-pg-02" }]);
        renderCard();

        expect(await screen.findByText("2/2 hoạt động")).toBeInTheDocument();
        expect((await findWorkerRow("judge-pg-01")).getByText("1.250")).toBeInTheDocument();
    });

    it("gọi worker đang khởi động lại đúng tên trạng thái", async () => {
        respondWithWorkers([{ ...HEALTHY_WORKER, status: "RESTARTING", cpuPercent: null }]);
        renderCard();

        expect((await findWorkerRow("judge-pg-01")).getByText("Khởi động lại")).toBeInTheDocument();
        expect(screen.getByText("0/1 hoạt động")).toBeInTheDocument();
    });

    it("có tiêu đề và đường tới trang chi tiết ngay cả khi đang tải", () => {
        renderCard();

        expect(screen.getByRole("heading", { level: 2, name: /^Máy chấm/ })).toBeInTheDocument();
        expect(screen.getByRole("link", { name: "Chi tiết máy chấm" })).toHaveAttribute(
            "href",
            "/admin/judge",
        );
        expect(screen.getByRole("status", { name: "Đang tải trạng thái máy chấm" })).toBeVisible();
    });

    it("ghi thời điểm cập nhật trạng thái worker", async () => {
        renderCard();

        expect(await screen.findByText(/^Cập nhật lúc \d{2}:\d{2}$/)).toBeInTheDocument();
    });

    it("báo trạng thái đã cũ khi làm mới nền bị lỗi nhưng vẫn giữ bảng worker", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
        render(
            <QueryClientProvider client={queryClient}>
                <ToastProvider>
                    <JudgeWorkersCard />
                </ToastProvider>
            </QueryClientProvider>,
        );
        await screen.findByText("5/6 hoạt động");

        expect(screen.queryByText(/Chưa cập nhật được/)).not.toBeInTheDocument();

        server.use(
            http.get("*/api/admin/judge/workers", () =>
                HttpResponse.json(
                    { status: 500, code: "INTERNAL_ERROR", message: "Đã có lỗi xảy ra" },
                    { status: 500 },
                ),
            ),
        );
        await queryClient.invalidateQueries({ queryKey: ["admin", "judge-workers"] });
        await waitFor(() => expect(queryClient.isFetching()).toBe(0));

        expect(
            await screen.findByText(
                /^Chưa cập nhật được số liệu mới\. Đang hiện số liệu lúc \d{2}:\d{2}\.$/,
            ),
        ).toBeInTheDocument();
        expect(screen.getByText("5/6 hoạt động")).toBeInTheDocument();
        expect(screen.getAllByRole("row")).toHaveLength(7);
    });

    it("báo trống khi chưa có worker nào", async () => {
        respondWithWorkers([]);
        renderCard();

        expect(await screen.findByText("Chưa có worker nào đăng ký")).toBeInTheDocument();
        expect(screen.queryByRole("table")).not.toBeInTheDocument();
    });

    it("hiện lỗi kèm mã lỗi và tải lại được", async () => {
        server.use(
            http.get(
                "*/api/admin/judge/workers",
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
        renderCard();

        expect(await screen.findByRole("alert")).toHaveTextContent("4bf92f3577b34da6");

        fireEvent.click(screen.getByRole("button", { name: "Thử lại" }));

        expect(await screen.findByText("5/6 hoạt động")).toBeInTheDocument();
    });
});
