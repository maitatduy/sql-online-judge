import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type { RecentSubmission, SubmissionStats } from "@/features/learning/types";
import { shiftDateKey, vietnamDateKey } from "@/lib/activity-calendar";

import { apiError } from "./api-error";
import { buildProblemSummaries } from "./practice-fixtures";
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

const DEFAULT_DAILY_CHALLENGES = 4;

function dailyChallenges(size: number) {
    const unsolved = buildProblemSummaries(true).filter((problem) => problem.progress !== "SOLVED");
    const offset = Number(vietnamDateKey(new Date()).replaceAll("-", "")) % unsolved.length;
    return [...unsolved.slice(offset), ...unsolved.slice(0, offset)].slice(0, size);
}

function topicProgress() {
    const topics = new Map<string, { slug: string; name: string; solved: number; total: number }>();
    for (const problem of buildProblemSummaries(true)) {
        for (const tag of problem.tags) {
            const topic = topics.get(tag.slug) ?? { ...tag, solved: 0, total: 0 };
            topic.total += 1;
            topic.solved += problem.progress === "SOLVED" ? 1 : 0;
            topics.set(tag.slug, topic);
        }
    }
    return [...topics.values()].sort(
        (first, second) => second.total - first.total || first.name.localeCompare(second.name),
    );
}

export const learningDashboardHandlers: RequestHandler[] = [
    http.get("*/api/problems/daily-challenges", async ({ request }) => {
        await delay(100);
        const size = Number(
            new URL(request.url).searchParams.get("size") ?? DEFAULT_DAILY_CHALLENGES,
        );
        return withSession(() => HttpResponse.json({ items: dailyChallenges(size) }));
    }),
    http.get("*/api/solved-summaries/:userId/topics", async ({ params }) => {
        await delay(100);
        return HttpResponse.json({ userId: String(params.userId), topics: topicProgress() });
    }),
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
