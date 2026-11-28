import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type { LeaderboardEntry } from "@/features/leaderboard/api";

import { DEMO_SESSION_USER } from "./session";

const PLAYERS: [string, string, number, number][] = [
    ["an.bui", "Bùi An", 1820, 6],
    ["minh.anh", "Nguyễn Minh Anh", 1640, 3],
    ["khoa.tran", "Trần Khoa", 1588, 4],
    ["linh.pham", "Phạm Linh", 1512, 1],
];

const EXTRA_PLAYERS = 22;

function userIdOf(username: string, index: number): string {
    if (username === DEMO_SESSION_USER.username) {
        return DEMO_SESSION_USER.id;
    }
    return `0192f3a0-0000-7000-8800-${String(index).padStart(12, "0")}`;
}

const ENTRIES: LeaderboardEntry[] = [
    ...PLAYERS,
    ...Array.from({ length: EXTRA_PLAYERS }, (_, index): [string, string, number, number] => [
        `hoc.vien${index + 1}`,
        `Học viên ${index + 1}`,
        1500 - index * 7,
        1 + (index % 3),
    ]),
].map(([username, displayName, rating, contestsPlayed], index) => ({
    rank: index + 1,
    userId: userIdOf(username, index),
    username,
    displayName,
    rating,
    contestsPlayed,
}));

export const leaderboardHandlers: RequestHandler[] = [
    http.get("*/api/leaderboard", async ({ request }) => {
        await delay(100);
        const url = new URL(request.url);
        const page = Number(url.searchParams.get("page") ?? 1);
        const size = Number(url.searchParams.get("size") ?? 20);
        return HttpResponse.json({
            items: ENTRIES.slice((page - 1) * size, page * size),
            page,
            size,
            totalItems: ENTRIES.length,
            totalPages: Math.max(1, Math.ceil(ENTRIES.length / size)),
        });
    }),
];
