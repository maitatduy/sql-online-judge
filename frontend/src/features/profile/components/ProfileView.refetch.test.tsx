import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, render, screen } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeAll, describe, expect, it, vi } from "vitest";

import { server } from "@/mocks/server";

import { ProfileView } from "./ProfileView";

vi.mock("next/navigation", () => ({
    notFound: vi.fn(),
    usePathname: () => "/u/minh.anh",
    useRouter: () => ({ replace: vi.fn() }),
    useSearchParams: () => new URLSearchParams(),
}));

const NOTIFICATION_SETTLE_MS = 50;

async function preloadChartModule(): Promise<void> {
    await import("./RatingChart");
}

function waitForQueryNotifications(): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, NOTIFICATION_SETTLE_MS));
}

function failEveryProfileRequest() {
    const serverError = () =>
        HttpResponse.json(
            { status: 500, code: "INTERNAL_ERROR", message: "Đã có lỗi xảy ra" },
            { status: 500 },
        );
    server.use(
        http.get("*/api/users/:username", serverError),
        http.get("*/api/users/:username/:resource", serverError),
    );
}

describe("ProfileView - tải lại nền thất bại", () => {
    beforeAll(preloadChartModule);

    it("giữ nguyên dữ liệu đang hiển thị, không chồng thêm khối báo lỗi", async () => {
        const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
        render(
            <QueryClientProvider client={queryClient}>
                <ProfileView username="minh.anh" />
            </QueryClientProvider>,
        );
        await screen.findByRole("heading", { level: 1, name: "Nguyễn Minh Anh" });
        await screen.findByRole("heading", { level: 2, name: "Rating contest" });
        await screen.findByRole("heading", { level: 2, name: /bài nộp trong năm qua$/ });
        await screen.findByRole("table", { name: "Bài nộp gần đây" });

        failEveryProfileRequest();
        await act(() => queryClient.refetchQueries({ queryKey: ["profile"] }));
        await act(waitForQueryNotifications);

        expect(screen.queryAllByRole("alert")).toHaveLength(0);
        expect(screen.getByRole("heading", { level: 1, name: "Nguyễn Minh Anh" })).toBeVisible();
        expect(screen.getByRole("heading", { level: 2, name: "Rating contest" })).toBeVisible();
        expect(screen.getByRole("table", { name: "Bài nộp gần đây" })).toBeVisible();
    });
});
