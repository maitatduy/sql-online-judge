import { fireEvent, screen, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it, vi } from "vitest";

import { server } from "@/mocks/server";
import { renderWithProviders } from "@/test/render";

import { RecentSubmissionsTable } from "./RecentSubmissionsTable";

const NOW = new Date("2026-10-13T05:00:00Z");

function failNextRequestOnce() {
    server.use(
        http.get(
            "*/api/users/:username/submissions",
            () =>
                HttpResponse.json(
                    { status: 500, code: "INTERNAL_ERROR", message: "Đã có lỗi xảy ra" },
                    { status: 500 },
                ),
            { once: true },
        ),
    );
}

describe("RecentSubmissionsTable - xem thêm bị lỗi", () => {
    it("giữ các dòng đã tải và báo lỗi cạnh nút, không thay bảng bằng khối lỗi", async () => {
        renderWithProviders(<RecentSubmissionsTable username="minh.anh" now={NOW} />);
        const table = await screen.findByRole("table", { name: "Bài nộp gần đây" });
        failNextRequestOnce();

        fireEvent.click(screen.getByRole("button", { name: "Xem thêm" }));

        expect(await screen.findByRole("alert")).toHaveTextContent(
            "Không tải thêm được bài nộp, hãy thử lại",
        );
        expect(within(table).getAllByRole("row")).toHaveLength(6);
        expect(screen.queryByText("Không tải được bài nộp")).not.toBeInTheDocument();
    });

    it("bấm xem thêm lần nữa thì tải tiếp trang kế, không tải lại từ đầu", async () => {
        const requestedCursors: (string | null)[] = [];
        server.events.on("request:start", ({ request }) => {
            const url = new URL(request.url);
            if (url.pathname.endsWith("/submissions")) {
                requestedCursors.push(url.searchParams.get("cursor"));
            }
        });
        renderWithProviders(<RecentSubmissionsTable username="minh.anh" now={NOW} />);
        const table = await screen.findByRole("table", { name: "Bài nộp gần đây" });
        failNextRequestOnce();
        fireEvent.click(screen.getByRole("button", { name: "Xem thêm" }));
        await screen.findByRole("alert");

        fireEvent.click(screen.getByRole("button", { name: "Xem thêm" }));

        await vi.waitFor(() => expect(within(table).getAllByRole("row")).toHaveLength(11));
        expect(screen.queryByRole("alert")).not.toBeInTheDocument();
        expect(requestedCursors).toEqual([null, "5", "5"]);
        server.events.removeAllListeners();
    });
});
