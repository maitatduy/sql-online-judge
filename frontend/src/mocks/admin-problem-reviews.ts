import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type {
    ReviewDecision,
    ReviewProblemStatus,
    ReviewProblemSummary,
} from "@/features/admin/problem-reviews/types";

import { validationFailed } from "./admin-user-errors";
import { apiError } from "./api-error";
import {
    readStudioProblems,
    type StoredProblem,
    toStudioProblemDetail,
    writeStudioProblems,
} from "./studio-problems";

const DEFAULT_PAGE_SIZE = 20;

const DECISIONS: Record<string, { decision: ReviewDecision; status: ReviewProblemStatus }> = {
    approve: { decision: "APPROVED", status: "PUBLISHED" },
    "request-changes": { decision: "CHANGES_REQUESTED", status: "CHANGES_REQUESTED" },
    reject: { decision: "REJECTED", status: "REJECTED" },
};

function toSummary(problem: StoredProblem): ReviewProblemSummary {
    return {
        id: problem.id,
        number: problem.number,
        slug: problem.slug,
        title: problem.title,
        status: problem.status,
        difficulty: problem.difficulty,
        authorId: problem.authorId,
        updatedAt: problem.updatedAt,
    };
}

function nextNumber(problems: StoredProblem[]): number {
    return Math.max(0, ...problems.map((problem) => problem.number ?? 0)) + 1;
}

async function reasonOf(request: Request): Promise<string> {
    const text = await request.text();
    return text ? ((JSON.parse(text) as { reason?: string }).reason ?? "").trim() : "";
}

export const adminProblemReviewHandlers: RequestHandler[] = [
    http.get("*/api/admin/problems", async ({ request }) => {
        await delay();
        const params = new URL(request.url).searchParams;
        const status = params.get("status") ?? "PENDING_REVIEW";
        const page = Number(params.get("page") ?? 1);
        const size = Number(params.get("size") ?? DEFAULT_PAGE_SIZE);
        const matching = readStudioProblems()
            .filter((problem) => !problem.deletedAt && problem.status === status)
            .sort((first, second) => first.updatedAt.localeCompare(second.updatedAt));
        return HttpResponse.json({
            items: matching.slice((page - 1) * size, page * size).map(toSummary),
            page,
            size,
            totalItems: matching.length,
            totalPages: Math.ceil(matching.length / size),
        });
    }),

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
        if (reason === "") {
            return validationFailed({ field: "reason", message: "Cần ghi lý do" });
        }
        const now = new Date().toISOString();
        const reviewed: StoredProblem = {
            ...problem,
            status: outcome.status,
            number:
                outcome.status === "PUBLISHED"
                    ? (problem.number ?? nextNumber(problems))
                    : problem.number,
            latestReview: { decision: outcome.decision, reason, reviewedAt: now },
            updatedAt: now,
        };
        writeStudioProblems(
            problems.map((candidate) => (candidate.id === problem.id ? reviewed : candidate)),
        );
        return HttpResponse.json(toStudioProblemDetail(reviewed));
    }),
];
