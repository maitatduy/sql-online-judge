import { fireEvent, screen, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { server } from "@/mocks/server";
import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";
import { readStudioProblems, writeStudioProblems } from "@/mocks/studio-problems";
import { renderWithProviders } from "@/test/render";
import { visit } from "@/test/url-search-params";

import { AdminProblemReviews } from "./AdminProblemReviews";

vi.mock("next/navigation", async () => ({
    useSearchParams: (await import("@/test/url-search-params")).useUrlSearchParams,
}));

const PENDING_ID = "0192f3d0-0000-7000-8000-000000000004";
const PENDING_TITLE = "Tỉ lệ giữ chân theo cohort tháng";

function useLatestCheck(isCurrent: boolean) {
    server.use(
        http.get("*/api/studio/problems/:problemId/answer-checks/latest", () =>
            HttpResponse.json({
                id: "0192f3f0-0000-7000-8000-000000000009",
                status: "PASSED",
                testcaseCount: 2,
                checkedCount: 2,
                isCurrent,
                createdAt: "2026-11-10T08:00:00Z",
                completedAt: "2026-11-10T08:00:05Z",
                testcases: [
                    { testcaseId: "a", position: 1, isPublic: true, isPassed: true },
                    { testcaseId: "b", position: 2, isPublic: false, isPassed: true },
                ],
            }),
        ),
    );
}

function recordReviewBodies(): unknown[] {
    const bodies: unknown[] = [];
    server.events.on("request:start", async ({ request }) => {
        if (request.method === "POST" && request.url.includes("/api/admin/problems/")) {
            const text = await request.clone().text();
            bodies.push(text ? JSON.parse(text) : null);
        }
    });
    return bodies;
}

async function renderReviews(search = "") {
    visit(`/admin/problem-reviews${search}`);
    renderWithProviders(
        <ToastProvider>
            <AdminProblemReviews />
        </ToastProvider>,
    );
    return within(await screen.findByRole("region", { name: "Hàng đợi duyệt" }));
}

async function openPendingProblem() {
    const queue = await renderReviews();
    fireEvent.click(await queue.findByRole("button", { name: new RegExp(PENDING_TITLE) }));
    return within(await screen.findByRole("region", { name: "Chi tiết bài" }));
}

describe("trang duyệt bài tập", () => {
    beforeEach(() => {
        writeMockSession(ADMIN_SESSION_USER);
        server.events.removeAllListeners();
        writeStudioProblems(
            readStudioProblems().map((problem) =>
                problem.id === PENDING_ID
                    ? { ...problem, statementMarkdown: "Tính **tỉ lệ giữ chân** theo tháng." }
                    : problem,
            ),
        );
    });

    it("chọn bài trong hàng đợi thì xem đề bài và kết quả kiểm tra đáp án", async () => {
        useLatestCheck(true);
        const detail = await openPendingProblem();

        expect(
            await detail.findByRole("heading", { level: 2, name: PENDING_TITLE }),
        ).toBeInTheDocument();
        expect(
            await detail.findByText("Đạt: 2/2 testcase khớp trên mọi dialect."),
        ).toBeInTheDocument();
        expect(detail.getByText("tỉ lệ giữ chân")).toBeInTheDocument();
        expect(detail.getByRole("link", { name: "Mở trong Studio" })).toHaveAttribute(
            "href",
            `/studio/problems/${PENDING_ID}/publish`,
        );
    });

    it("duyệt và xuất bản thì bài rời khỏi hàng đợi chờ duyệt", async () => {
        useLatestCheck(true);
        const detail = await openPendingProblem();

        const approve = await detail.findByRole("button", { name: "Duyệt và xuất bản" });
        await vi.waitFor(() => expect(approve).toBeEnabled());
        fireEvent.click(approve);

        expect(await screen.findByText(`Đã xuất bản bài ${PENDING_TITLE}.`)).toBeInTheDocument();
        const queue = within(screen.getByRole("region", { name: "Hàng đợi duyệt" }));
        expect(await queue.findByText("Không có bài nào ở trạng thái này.")).toBeInTheDocument();
    });

    it("yêu cầu sửa và từ chối cần ghi lý do", async () => {
        useLatestCheck(true);
        const bodies = recordReviewBodies();
        const detail = await openPendingProblem();

        fireEvent.click(await detail.findByRole("button", { name: "Yêu cầu sửa" }));
        expect(await detail.findByText("Cần ghi lý do")).toBeInTheDocument();
        expect(bodies).toEqual([]);

        fireEvent.change(detail.getByRole("textbox", { name: "Lý do gửi người ra đề" }), {
            target: { value: "Thêm testcase có NULL." },
        });
        fireEvent.click(detail.getByRole("button", { name: "Yêu cầu sửa" }));

        expect(
            await screen.findByText(`Đã trả bài ${PENDING_TITLE} về cho người ra đề.`),
        ).toBeInTheDocument();
        expect(bodies).toEqual([{ reason: "Thêm testcase có NULL." }]);
    });

    it("đáp án đã sửa sau lần kiểm tra gần nhất thì chưa duyệt được", async () => {
        useLatestCheck(false);
        const detail = await openPendingProblem();

        expect(
            await detail.findByText("Đáp án đã sửa sau lần kiểm tra gần nhất."),
        ).toBeInTheDocument();
        expect(detail.getByRole("button", { name: "Duyệt và xuất bản" })).toBeDisabled();
        expect(
            detail.getByText("Chỉ duyệt được khi lần kiểm tra đáp án gần nhất đạt."),
        ).toBeInTheDocument();
    });

    it("lọc theo trạng thái Đã xuất bản thì chỉ xem, không có nút quyết định", async () => {
        const queue = await renderReviews("?status=PUBLISHED");

        fireEvent.click(await queue.findByRole("button", { name: /Đơn hàng giao trễ theo vùng/ }));
        const detail = within(await screen.findByRole("region", { name: "Chi tiết bài" }));

        expect(
            await detail.findByRole("heading", { level: 2, name: "Đơn hàng giao trễ theo vùng" }),
        ).toBeInTheDocument();
        expect(detail.queryByRole("button", { name: "Duyệt và xuất bản" })).not.toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Đã xuất bản" })).toHaveAttribute(
            "aria-pressed",
            "true",
        );
    });
});
