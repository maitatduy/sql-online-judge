import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type {
    ProblemVisibility,
    ReviewDecision,
    ReviewProblemStatus,
} from "@/features/admin/problem-reviews/types";

import { validationFailed } from "./admin-user-errors";
import { apiError } from "./api-error";
import {
    readStudioProblems,
    type StoredProblem,
    toStudioProblemDetail,
    writeStudioProblems,
} from "./studio-problems";

interface Decision {
    decision: ReviewDecision;
    status: ReviewProblemStatus;
    visibility?: ProblemVisibility;
}

const DECISIONS: Record<string, Decision> = {
    approve: { decision: "APPROVED", status: "PUBLISHED", visibility: "PUBLIC" },
    "approve-for-contest": {
        decision: "APPROVED",
        status: "PUBLISHED",
        visibility: "CONTEST_ONLY",
    },
    "request-changes": { decision: "CHANGES_REQUESTED", status: "CHANGES_REQUESTED" },
    reject: { decision: "REJECTED", status: "REJECTED" },
};

function nextNumber(problems: StoredProblem[]): number {
    return Math.max(0, ...problems.map((problem) => problem.number ?? 0)) + 1;
}

async function reasonOf(request: Request): Promise<string> {
    const text = await request.text();
    return text ? ((JSON.parse(text) as { reason?: string }).reason ?? "").trim() : "";
}

export const adminProblemReviewHandlers: RequestHandler[] = [
    http.post("*/api/admin/problems/:problemId/:action", async ({ params, request }) => {
        await delay();
        const outcome = DECISIONS[String(params.action)];
        const problems = readStudioProblems();
        const problem = problems.find(
            (candidate) => candidate.id === params.problemId && !candidate.deletedAt,
        );
        if (!outcome || !problem) {
            return apiError(404, "PROBLEM_NOT_FOUND", "Không tìm thấy bài tập");
        }
        if (problem.status !== "PENDING_REVIEW") {
            return apiError(422, "PROBLEM_NOT_PENDING_REVIEW", "Bài không ở trạng thái chờ duyệt");
        }
        const reason = outcome.decision === "APPROVED" ? null : await reasonOf(request);
        const isPublic = outcome.visibility === "PUBLIC";
        if (reason === "") {
            return validationFailed({ field: "reason", message: "Cần ghi lý do" });
        }
        const now = new Date().toISOString();
        const reviewed: StoredProblem = {
            ...problem,
            status: outcome.status,
            number: isPublic ? (problem.number ?? nextNumber(problems)) : problem.number,
            visibility: outcome.visibility ?? problem.visibility,
            latestReview: { decision: outcome.decision, reason, reviewedAt: now },
            updatedAt: now,
        };
        writeStudioProblems(
            problems.map((candidate) => (candidate.id === problem.id ? reviewed : candidate)),
        );
        return HttpResponse.json(toStudioProblemDetail(reviewed));
    }),
];
