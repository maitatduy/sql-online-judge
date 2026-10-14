import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type { ContestHistoryResponse, ContestResult } from "@/features/admin/users/types";

import { buildAdminUserDetail } from "./admin-user-details";
import { userNotFound } from "./admin-user-errors";
import { findAnyAdminUser } from "./admin-user-store";

const LATEST_WEEKLY = 37;
const LATEST_FINISHED_MS = Date.parse("2026-10-11T14:00:00Z");
const WEEK_MS = 7 * 24 * 60 * 60_000;
const DEFAULT_PAGE_SIZE = 10;
const MAX_PAGE_SIZE = 50;

const NAMED_RESULTS: Record<string, Partial<ContestResult>[]> = {
    "viet.luu": [
        { contestTitle: "SQL Weekly #37", rank: 212, solvedCount: 2, ratingDelta: null },
        { contestTitle: "SQL Weekly #36", rank: 604, solvedCount: 1, ratingDelta: -18 },
        { contestTitle: "Window Function Cup", rank: 488, solvedCount: 2, ratingDelta: 9 },
    ],
};

function generatedResult(username: string, index: number): ContestResult {
    const weekly = LATEST_WEEKLY - index;
    return {
        contestSlug: `weekly-${weekly}`,
        contestTitle: `SQL Weekly #${weekly}`,
        rank: 40 + ((username.length * 37 + index * 53) % 600),
        solvedCount: 1 + ((index + username.length) % 4),
        ratingDelta: ((index * 7 + username.length) % 41) - 20,
        finishedAt: new Date(LATEST_FINISHED_MS - index * WEEK_MS).toISOString(),
    };
}

export function buildContestHistory(username: string, contestCount: number): ContestResult[] {
    const named = NAMED_RESULTS[username] ?? [];
    return Array.from({ length: contestCount }, (_, index) => ({
        ...generatedResult(username, index),
        ...named[index],
    }));
}

export const adminUserContestHandlers: RequestHandler[] = [
    http.get("*/api/admin/users/:username/contest-history", async ({ params, request }) => {
        await delay();
        const user = findAnyAdminUser(String(params.username));
        if (!user) {
            return userNotFound();
        }
        const query = new URL(request.url).searchParams;
        const page = Math.max(1, Number(query.get("page") ?? 1));
        const size = Math.min(MAX_PAGE_SIZE, Number(query.get("size") ?? DEFAULT_PAGE_SIZE));
        const results = buildContestHistory(user.username, buildAdminUserDetail(user).contestCount);
        const firstIndex = (page - 1) * size;
        const response: ContestHistoryResponse = {
            items: results.slice(firstIndex, firstIndex + size),
            page,
            size,
            totalItems: results.length,
            totalPages: Math.ceil(results.length / size),
        };
        return HttpResponse.json(response);
    }),
];
