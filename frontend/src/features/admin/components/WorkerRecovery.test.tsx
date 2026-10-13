import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { buildJudgeWorkers } from "@/mocks/admin-fixtures";
import { server } from "@/mocks/server";
import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";

import type { WorkerStatus } from "../types";
import { JudgeWorkersCard } from "./JudgeWorkersCard";

const WORKERS_QUERY_KEY = ["admin", "judge-workers"];

function respondWithLostWorkerStatuses(statuses: WorkerStatus[]) {
    let responseIndex = 0;
    server.use(
        http.get("*/api/admin/judge/workers", () => {
            const status = statuses[Math.min(responseIndex, statuses.length - 1)];
            responseIndex += 1;
            return HttpResponse.json({
                items: buildJudgeWorkers(new Date()).map((worker) =>
                    worker.name === "judge-my-02"
                        ? { ...worker, status, cpuPercent: 35, disconnectedAt: null }
                        : worker,
                ),
            });
        }),
    );
}

function renderCard() {
    writeMockSession(ADMIN_SESSION_USER);
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
        <QueryClientProvider client={queryClient}>
            <ToastProvider>
                <JudgeWorkersCard />
            </ToastProvider>
        </QueryClientProvider>,
    );
    return queryClient;
}

describe("worker hoạt động lại", () => {
    it("báo bằng toast và cập nhật số worker hoạt động", async () => {
        respondWithLostWorkerStatuses(["RESTARTING", "ONLINE"]);
        const queryClient = renderCard();
        await screen.findByText("5/6 hoạt động");

        await queryClient.invalidateQueries({ queryKey: WORKERS_QUERY_KEY });

        expect(await screen.findByText("judge-my-02 đã hoạt động trở lại.")).toBeInTheDocument();
        expect(screen.getByText("6/6 hoạt động")).toBeInTheDocument();
        expect(screen.queryByText(/Đang khởi động lại/)).not.toBeInTheDocument();
    });

    it("làm mới hàng đợi và nhãn sidebar khi worker hoạt động lại", async () => {
        respondWithLostWorkerStatuses(["RESTARTING", "ONLINE"]);
        const queryClient = renderCard();
        await screen.findByText("5/6 hoạt động");
        const invalidateQueries = vi.spyOn(queryClient, "invalidateQueries");

        await queryClient.invalidateQueries({ queryKey: WORKERS_QUERY_KEY });
        await screen.findByText("judge-my-02 đã hoạt động trở lại.");

        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: ["admin", "overview"] });
        expect(invalidateQueries).toHaveBeenCalledWith({
            queryKey: ["admin", "navigation-counts"],
        });
    });

    it("không báo gì khi worker vẫn đang khởi động lại", async () => {
        respondWithLostWorkerStatuses(["RESTARTING", "RESTARTING"]);
        const queryClient = renderCard();
        await screen.findByText("5/6 hoạt động");

        await queryClient.invalidateQueries({ queryKey: WORKERS_QUERY_KEY });

        expect(screen.getByText(/Đang khởi động lại/)).toBeInTheDocument();
        expect(screen.queryByText(/đã hoạt động trở lại/)).not.toBeInTheDocument();
    });

    it("không báo gì khi trang vừa mở và mọi worker đang hoạt động", async () => {
        respondWithLostWorkerStatuses(["ONLINE"]);
        renderCard();

        await screen.findByText("6/6 hoạt động");

        expect(screen.queryByText(/đã hoạt động trở lại/)).not.toBeInTheDocument();
    });
});
