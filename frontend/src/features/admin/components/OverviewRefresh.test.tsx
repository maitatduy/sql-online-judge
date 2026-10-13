import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { server } from "@/mocks/server";
import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";

import { AdminOverview } from "./AdminOverview";

function failEveryAdminRequest() {
    server.use(
        http.get("*/api/admin/*", () =>
            HttpResponse.json(
                { status: 500, code: "INTERNAL_ERROR", message: "Đã có lỗi xảy ra" },
                { status: 500 },
            ),
        ),
    );
}

function renderOverview() {
    writeMockSession(ADMIN_SESSION_USER);
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
        <QueryClientProvider client={queryClient}>
            <ToastProvider>
                <AdminOverview />
            </ToastProvider>
        </QueryClientProvider>,
    );
    return queryClient;
}

describe("làm mới số liệu tổng quan", () => {
    it("giữ nguyên dữ liệu đang hiển thị khi làm mới nền bị lỗi", async () => {
        const queryClient = renderOverview();
        await screen.findByText("48.260");
        await screen.findByText("5/6 hoạt động");
        await screen.findByRole("link", { name: /^Bài chờ duyệt/ });
        failEveryAdminRequest();

        await queryClient.invalidateQueries({ queryKey: ["admin"] });
        await waitFor(() => expect(queryClient.isFetching()).toBe(0));

        expect(screen.getByText("48.260")).toBeInTheDocument();
        expect(screen.getByText("5/6 hoạt động")).toBeInTheDocument();
        expect(screen.getByRole("link", { name: /^Bài chờ duyệt/ })).toBeInTheDocument();
        expect(screen.queryByRole("button", { name: "Thử lại" })).not.toBeInTheDocument();
    });
});
