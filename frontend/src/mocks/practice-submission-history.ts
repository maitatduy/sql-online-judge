import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type {
    ProblemSubmissionSummary,
    ProblemSubmissionsResponse,
    Submission,
} from "@/features/practice/types";

import { apiError } from "./api-error";
import { listOwnSubmissions } from "./practice-submission-handlers";
import { findProblem } from "./practice-sql";
import { DEMO_SESSION_USER, readMockSession } from "./session";

const DEFAULT_PAGE_SIZE = 20;
const MAX_PAGE_SIZE = 100;
const HOUR_MS = 3_600_000;
const SEEDED_PROBLEM_ID = "0192f3b0-0000-7000-8000-0000000000b8";

interface HistorySeed {
    id: string;
    verdict: "WA" | "RE";
    runtimeMs: number | null;
    hoursAgo: number;
}

const HISTORY_SEEDS: HistorySeed[] = [
    { id: "0192f3c0-0000-7000-8000-0000000000a2", verdict: "WA", runtimeMs: 41, hoursAgo: 26 },
    { id: "0192f3c0-0000-7000-8000-0000000000a1", verdict: "RE", runtimeMs: null, hoursAgo: 27 },
];

function seededHistory(userId: string, problemId: string, now: Date): ProblemSubmissionSummary[] {
    if (userId !== DEMO_SESSION_USER.id || problemId !== SEEDED_PROBLEM_ID) {
        return [];
    }
    return HISTORY_SEEDS.map(({ id, verdict, runtimeMs, hoursAgo }) => ({
        id,
        status: "COMPLETED",
        verdict,
        dialect: "POSTGRESQL",
        runtimeMs,
        submittedAt: new Date(now.getTime() - hoursAgo * HOUR_MS).toISOString(),
    }));
}

function summaryOf(submission: Submission): ProblemSubmissionSummary {
    return {
        id: submission.id,
        status: submission.status,
        verdict: submission.verdict,
        dialect: submission.dialect,
        runtimeMs: submission.runtimeMs,
        submittedAt: submission.submittedAt,
    };
}

export const practiceSubmissionHistoryHandlers: RequestHandler[] = [
    http.get("*/api/problems/:problemId/submissions", async ({ params, request }) => {
        await delay();
        const user = readMockSession();
        if (!user) {
            return apiError(401, "UNAUTHENTICATED", "Bạn chưa đăng nhập");
        }
        const problemId = String(params.problemId);
        if (!findProblem(problemId)) {
            return apiError(404, "PROBLEM_NOT_FOUND", "Không tìm thấy bài tập");
        }
        const query = new URL(request.url).searchParams;
        const startIndex = Number(query.get("cursor") ?? 0);
        const pageSize = Math.min(Number(query.get("size") ?? DEFAULT_PAGE_SIZE), MAX_PAGE_SIZE);
        const history = [
            ...listOwnSubmissions(user.id, problemId).map(summaryOf),
            ...seededHistory(user.id, problemId, new Date()),
        ].sort((first, second) => Date.parse(second.submittedAt) - Date.parse(first.submittedAt));
        const endIndex = startIndex + pageSize;
        const response: ProblemSubmissionsResponse = {
            items: history.slice(startIndex, endIndex),
            nextCursor: endIndex < history.length ? String(endIndex) : null,
        };
        return HttpResponse.json(response);
    }),
];
