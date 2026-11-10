import { fireEvent, screen, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import type { AnswerCheck } from "@/features/studio/types";
import { server } from "@/mocks/server";
import { SETTER_SESSION_USER, writeMockSession } from "@/mocks/session";
import {
    readStudioProblems,
    toStudioProblemDetail,
    writeStudioProblems,
} from "@/mocks/studio-problems";
import { renderWithProviders } from "@/test/render";

import { ProblemStudio } from "./ProblemStudio";

const EMPTY_DRAFT_ID = "0192f3d0-0000-7000-8000-000000000001";
const CHANGES_REQUESTED_ID = "0192f3d0-0000-7000-8000-000000000002";
const READY_DRAFT_ID = "0192f3d0-0000-7000-8000-000000000003";
const PUBLISHED_ID = "0192f3d0-0000-7000-8000-000000000005";
const SAMPLE_VARIANT_ID = "0192f3e1-0000-7000-8000-000000000001";

function prepareReadyDraft() {
    writeStudioProblems(
        readStudioProblems().map((problem) =>
            problem.id === READY_DRAFT_ID
                ? {
                      ...problem,
                      statementMarkdown: "## Yêu cầu\n\nIn ra **lương trung vị** mỗi phòng ban.",
                      answer: {
                          solutions: [{ dialect: "POSTGRESQL", sql: "SELECT 1" }],
                          testcaseVariantIds: [SAMPLE_VARIANT_ID],
                          requiresOrder: false,
                          compareColumnNames: false,
                          floatTolerance: 0.000001,
                      },
                  }
                : problem,
        ),
    );
}

function useLatestCheck(check: Partial<AnswerCheck>) {
    server.use(
        http.get("*/api/studio/problems/:problemId/answer-checks/latest", () =>
            HttpResponse.json({
                id: "0192f3f0-0000-7000-8000-000000000001",
                status: "PASSED",
                testcaseCount: 1,
                checkedCount: 1,
                isCurrent: true,
                createdAt: "2026-11-10T08:00:00Z",
                completedAt: "2026-11-10T08:00:05Z",
                testcases: [],
                ...check,
            }),
        ),
    );
}

function renderPublishStep(problemId: string) {
    renderWithProviders(
        <ToastProvider>
            <ProblemStudio problemId={problemId} step="publish" />
        </ToastProvider>,
    );
}

async function findCondition(name: string) {
    const list = await screen.findByRole("list", { name: "Điều kiện gửi duyệt" });
    return within(list).getByText(name).closest("li") as HTMLElement;
}

describe("ProblemStudio - bước Xem trước và xuất bản", () => {
    beforeEach(() => {
        writeMockSession(SETTER_SESSION_USER);
    });

    it("liệt kê mục còn thiếu kèm đường dẫn tới bước cần làm và khoá nút Gửi duyệt", async () => {
        renderPublishStep(EMPTY_DRAFT_ID);

        const statement = await findCondition("Đề bài");
        expect(within(statement).getByText("Chưa có đề bài")).toBeInTheDocument();
        expect(within(statement).getByRole("link", { name: "Soạn đề bài" })).toHaveAttribute(
            "href",
            `/studio/problems/${EMPTY_DRAFT_ID}/statement`,
        );
        const database = await findCondition("Database");
        expect(within(database).getByText("Chưa chọn database")).toBeInTheDocument();
        const check = await findCondition("Kiểm tra đáp án");
        expect(await within(check).findByText("Chưa kiểm tra đáp án")).toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Gửi duyệt" })).toBeDisabled();
    });

    it("đủ điều kiện thì xem trước đề bài, gửi duyệt và bài chuyển sang chờ duyệt", async () => {
        prepareReadyDraft();
        useLatestCheck({});
        server.use(
            http.post("*/api/studio/problems/:problemId/submit-for-review", () => {
                const problem = readStudioProblems().find(({ id }) => id === READY_DRAFT_ID);
                return HttpResponse.json(
                    toStudioProblemDetail({ ...problem!, status: "PENDING_REVIEW" }),
                );
            }),
        );
        renderPublishStep(READY_DRAFT_ID);

        const preview = await screen.findByRole("region", { name: "Xem trước đề bài" });
        expect(within(preview).getByText("lương trung vị")).toBeInTheDocument();
        const check = await findCondition("Kiểm tra đáp án");
        expect(await within(check).findByText("Đạt trên mọi dialect")).toBeInTheDocument();
        const answer = await findCondition("Đáp án và testcase");
        expect(await within(answer).findByText("1 đáp án, 1 testcase")).toBeInTheDocument();

        const submit = screen.getByRole("button", { name: "Gửi duyệt" });
        expect(submit).toBeEnabled();
        fireEvent.click(submit);

        expect(await screen.findByText("Đã gửi bài cho admin duyệt.")).toBeInTheDocument();
        expect(await screen.findByText("Bài đang chờ admin duyệt.")).toBeInTheDocument();
        expect(screen.queryByRole("button", { name: "Gửi duyệt" })).not.toBeInTheDocument();
    });

    it("đã sửa đáp án sau lần kiểm tra gần nhất thì yêu cầu kiểm tra lại", async () => {
        prepareReadyDraft();
        useLatestCheck({ isCurrent: false });
        renderPublishStep(READY_DRAFT_ID);

        const check = await findCondition("Kiểm tra đáp án");
        expect(
            await within(check).findByText("Đáp án đã sửa sau lần kiểm tra gần nhất"),
        ).toBeInTheDocument();
        expect(within(check).getByRole("link", { name: "Chạy kiểm tra" })).toHaveAttribute(
            "href",
            `/studio/problems/${READY_DRAFT_ID}/answer`,
        );
        expect(screen.getByRole("button", { name: "Gửi duyệt" })).toBeDisabled();
    });

    it("bài được yêu cầu sửa thì hiện lý do của admin", async () => {
        writeStudioProblems(
            readStudioProblems().map((problem) =>
                problem.id === CHANGES_REQUESTED_ID
                    ? {
                          ...problem,
                          latestReview: {
                              decision: "CHANGES_REQUESTED",
                              reason: "Cần thêm testcase có NULL.",
                              reviewedAt: "2026-11-09T10:00:00Z",
                          },
                      }
                    : problem,
            ),
        );
        renderPublishStep(CHANGES_REQUESTED_ID);

        expect(await screen.findByText("Admin yêu cầu sửa")).toBeInTheDocument();
        expect(screen.getByText("Cần thêm testcase có NULL.")).toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Gửi duyệt" })).toBeInTheDocument();
    });

    it("bài đã xuất bản thì có đường dẫn tới trang bài công khai", async () => {
        renderPublishStep(PUBLISHED_ID);

        expect(await screen.findByText("Bài đã xuất bản với số 260.")).toBeInTheDocument();
        expect(screen.getByRole("link", { name: "Xem trang bài" })).toHaveAttribute(
            "href",
            "/problems/late-orders-by-region",
        );
        expect(screen.queryByRole("button", { name: "Gửi duyệt" })).not.toBeInTheDocument();
    });
});
