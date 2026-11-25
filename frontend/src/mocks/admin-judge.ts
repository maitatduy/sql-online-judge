import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type { JudgeQueue } from "@/features/admin/judge/types";
import type { AdminSubmission } from "@/features/admin/submissions/types";

import { buildAdminSubmissions, toAdminSubmissionRow } from "./admin-submissions";
import { apiError } from "./api-error";
import { readMockSession } from "./session";

function initialAttention(now: Date): AdminSubmission[] {
    return buildAdminSubmissions(now)
        .filter((submission) => submission.status !== "COMPLETED" || submission.verdict === "SE")
        .map(toAdminSubmissionRow);
}

let attention = initialAttention(new Date());
let requeuedCount = 0;

export function resetAdminJudge(): void {
    attention = initialAttention(new Date());
    requeuedCount = 0;
}

function queueOf(): JudgeQueue {
    return {
        waiting: 4 + requeuedCount,
        judging: 2,
        stuck: attention.filter((submission) => submission.status !== "COMPLETED").length,
        systemErrorsLastDay: attention.filter((submission) => submission.verdict === "SE").length,
        judgedLastHour: 318,
        stuckAfterMinutes: 2,
    };
}

function isAdmin(): boolean {
    return readMockSession()?.role === "ADMIN";
}

function forbidden() {
    return apiError(403, "FORBIDDEN", "Bạn không có quyền thực hiện thao tác này");
}

export const adminJudgeHandlers: RequestHandler[] = [
    http.get("*/api/admin/judge/queue", async () => {
        await delay();
        return isAdmin() ? HttpResponse.json(queueOf()) : forbidden();
    }),

    http.get("*/api/admin/judge/attention", async () => {
        await delay();
        return isAdmin() ? HttpResponse.json({ items: attention }) : forbidden();
    }),

    http.post("*/api/admin/submissions/:submissionId/rejudge", async ({ params }) => {
        await delay();
        if (!isAdmin()) {
            return forbidden();
        }
        if (!attention.some((submission) => submission.id === params.submissionId)) {
            return apiError(
                422,
                "SUBMISSION_NOT_REJUDGEABLE",
                "Chỉ chấm lại được bài lỗi hệ thống chấm hoặc bài bị kẹt trong hàng đợi",
            );
        }
        attention = attention.filter((submission) => submission.id !== params.submissionId);
        requeuedCount += 1;
        return new HttpResponse(null, { status: 202 });
    }),
];
