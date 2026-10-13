import { fireEvent, screen, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it, vi } from "vitest";

import { server } from "@/mocks/server";
import { DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { RecentSubmissionsTable } from "./RecentSubmissionsTable";

const NOW = new Date("2026-10-13T05:00:00Z");

function respondWithOneSubmission() {
    server.use(
        http.get("*/api/users/:username/submissions", () =>
            HttpResponse.json({
                items: [
                    {
                        id: "0192f3a0-0000-7000-8000-00000000a001",
                        verdict: "WA",
                        problemNumber: 1184,
                        problemSlug: "highest-salary-per-department",
                        problemTitle: "Lương cao nhất mỗi phòng ban",
                        dialect: "POSTGRESQL",
                        submittedAt: "2026-10-13T04:55:00Z",
                    },
                ],
                nextCursor: null,
            }),
        ),
    );
}

describe("RecentSubmissionsTable", () => {
    it("báo đang tải rồi hiện verdict, bài, dialect và thời gian tương đối", async () => {
        respondWithOneSubmission();
        renderWithProviders(<RecentSubmissionsTable username="minh.anh" now={NOW} />);

        expect(screen.getByRole("status", { name: "Đang tải bài nộp" })).toHaveAttribute(
            "aria-busy",
            "true",
        );

        const table = await screen.findByRole("table", { name: "Bài nộp gần đây" });
        const row = within(table).getAllByRole("row")[1];

        expect(within(row).getByText("WA")).toBeInTheDocument();
        expect(row).toHaveTextContent("#1.184 · Lương cao nhất mỗi phòng ban");
        expect(row).toHaveTextContent("PostgreSQL");
        expect(within(row).getByText("5 phút trước")).toHaveAttribute(
            "datetime",
            "2026-10-13T04:55:00Z",
        );
        expect(screen.queryByRole("button", { name: "Xem thêm" })).not.toBeInTheDocument();
    });

    it("không dẫn tới chi tiết bài nộp của người khác", async () => {
        respondWithOneSubmission();
        renderWithProviders(<RecentSubmissionsTable username="minh.anh" now={NOW} />);

        await screen.findByRole("table", { name: "Bài nộp gần đây" });

        expect(screen.queryByRole("link")).not.toBeInTheDocument();
    });

    it("nút xem thêm tải trang kế và biến mất khi hết", async () => {
        renderWithProviders(<RecentSubmissionsTable username="minh.anh" now={NOW} />);
        const table = await screen.findByRole("table", { name: "Bài nộp gần đây" });
        expect(within(table).getAllByRole("row")).toHaveLength(6);

        fireEvent.click(screen.getByRole("button", { name: "Xem thêm" }));
        await vi.waitFor(() => expect(within(table).getAllByRole("row")).toHaveLength(11));

        fireEvent.click(screen.getByRole("button", { name: "Xem thêm" }));
        await vi.waitFor(() => expect(within(table).getAllByRole("row")).toHaveLength(13));

        expect(screen.queryByRole("button", { name: "Xem thêm" })).not.toBeInTheDocument();
    });

    it("người mới thấy trạng thái trống, khách không được mời giải bài", async () => {
        renderWithProviders(<RecentSubmissionsTable username="newbie" now={NOW} />);

        expect(await screen.findByText("Chưa có bài nộp nào")).toBeInTheDocument();
        expect(screen.queryByRole("table")).not.toBeInTheDocument();
        expect(screen.queryByRole("link", { name: "Giải bài đầu tiên" })).not.toBeInTheDocument();
    });

    it("chủ hồ sơ chưa nộp bài được mời giải bài đầu tiên", async () => {
        writeMockSession({ ...DEMO_SESSION_USER, username: "newbie" });
        renderWithProviders(<RecentSubmissionsTable username="newbie" now={NOW} />);

        expect(await screen.findByRole("link", { name: "Giải bài đầu tiên" })).toHaveAttribute(
            "href",
            "/problems",
        );
    });

    it("lỗi thì báo riêng trong thẻ kèm mã lỗi", async () => {
        renderWithProviders(<RecentSubmissionsTable username="error" now={NOW} />);

        const alert = await screen.findByRole("alert");

        expect(alert).toHaveTextContent("Không tải được bài nộp");
        expect(alert).toHaveTextContent("Mã lỗi: 4bf92f3577b34da6");
    });
});
