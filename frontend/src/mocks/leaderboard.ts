import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type { LeaderboardEntry } from "@/features/leaderboard/api";

const PLAYERS: [string, string, number, number][] = [
    ["an.bui", "Bùi An", 1820, 6],
    ["minh.anh", "Nguyễn Minh Anh", 1640, 3],
    ["khoa.tran", "Trần Khoa", 1588, 4],
    ["linh.pham", "Phạm Linh", 1512, 1],
];

const ENTRIES: LeaderboardEntry[] = PLAYERS.map(
    ([username, displayName, rating, contestsPlayed], index) => ({
        rank: index + 1,
        userId: `0192f3a0-0000-7000-8800-00000000000${index}`,
        username,
        displayName,
        rating,
        contestsPlayed,
    }),
);

export const leaderboardHandlers: RequestHandler[] = [
    http.get("*/api/leaderboard", async ({ request }) => {
        await delay(100);
        const url = new URL(request.url);
        const page = Number(url.searchParams.get("page") ?? 1);
        const size = Number(url.searchParams.get("size") ?? 50);
        return HttpResponse.json({
            items: ENTRIES.slice((page - 1) * size, page * size),
            page,
            size,
            totalItems: ENTRIES.length,
            totalPages: Math.max(1, Math.ceil(ENTRIES.length / size)),
        });
    }),
];
