import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type { RecentSubmission, SubmissionStats } from "@/features/learning/types";
import { shiftDateKey, vietnamDateKey } from "@/lib/activity-calendar";

import { apiError } from "./api-error";
import { readMockSession } from "./session";

export const MOCK_SUBMISSION_STATS: SubmissionStats = {
    submissionCount: 20,
    acceptedCount: 12,
    solvedCount: 9,
    solvedThisWeek: 2,
};

function recentSubmissions(now: Date): RecentSubmission[] {
    const minutesAgo = (minutes: number) =>
        new Date(now.getTime() - minutes * 60_000).toISOString();
    return [
        {
            id: "recent-1",
            status: "COMPLETED",
            verdict: "AC",
            dialect: "POSTGRESQL",
            runtimeMs: 48,
            createdAt: minutesAgo(5),
            problemId: "problem-1",
            problemNumber: 1,
            problemSlug: "tong-luong-moi-phong-ban",
            problemTitle: "Tổng lương mỗi phòng ban",
        },
        {
            id: "recent-2",
            status: "COMPLETED",
            verdict: "WA",
            dialect: "POSTGRESQL",
            runtimeMs: 41,
            createdAt: minutesAgo(12),
            problemId: "problem-1",
            problemNumber: 1,
            problemSlug: "tong-luong-moi-phong-ban",
            problemTitle: "Tổng lương mỗi phòng ban",
        },
    ];
}

function withSession(respond: () => Response): Response {
    return readMockSession() ? respond() : apiError(401, "UNAUTHENTICATED", "Bạn chưa đăng nhập");
}

export const learningDashboardHandlers: RequestHandler[] = [
    http.get("*/api/submissions/summary", async () => {
        await delay(100);
        return withSession(() => HttpResponse.json(MOCK_SUBMISSION_STATS));
    }),
    http.get("*/api/submissions/activity", async () => {
        await delay(100);
        const today = vietnamDateKey(new Date());
        return withSession(() =>
            HttpResponse.json([
                { date: shiftDateKey(today, -2), count: 3 },
                { date: shiftDateKey(today, -1), count: 2 },
            ]),
        );
    }),
    http.get("*/api/submissions/recent", async () => {
        await delay(100);
        return withSession(() => HttpResponse.json(recentSubmissions(new Date())));
    }),
];
