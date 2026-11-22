import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type {
    AdminContest,
    AdminContestSummary,
    ContestPhase,
    CreateContestRequest,
} from "@/features/admin/contests/types";

import { apiError } from "./api-error";
import { readMockSession } from "./session";

const MS_PER_MINUTE = 60_000;

function contest(
    id: string,
    slug: string,
    title: string,
    startAt: string,
    problems: AdminContest["problems"],
): AdminContest {
    return {
        id,
        slug,
        title,
        description: "Thi SQL 90 phút, chấm theo luật ICPC.",
        startAt,
        endAt: new Date(new Date(startAt).getTime() + 90 * MS_PER_MINUTE).toISOString(),
        durationMinutes: 90,
        freezeMinutes: 30,
        penaltyMinutes: 20,
        isRated: true,
        phase: "UPCOMING",
        problems,
    };
}

function seedContests(): AdminContest[] {
    return [
        contest(
            "0193a000-0000-7000-8000-000000000001",
            "sql-cup-1",
            "SQL Cup 1",
            "2026-10-11T12:00:00Z",
            [
                {
                    letter: "A",
                    problemId: "0193a100-0000-7000-8000-000000000001",
                    slug: "ghep-phong-ban",
                    title: "Ghép phòng ban",
                    difficulty: "EASY",
                },
                {
                    letter: "B",
                    problemId: "0193a100-0000-7000-8000-000000000002",
                    slug: "xep-hang-luong",
                    title: "Xếp hạng lương",
                    difficulty: "MEDIUM",
                },
            ],
        ),
        contest(
            "0193a000-0000-7000-8000-000000000002",
            "sql-cup-2",
            "SQL Cup 2",
            "2099-01-10T12:00:00Z",
            [],
        ),
    ];
}

let contests = seedContests();

export function resetAdminContests() {
    contests = seedContests();
}

function phaseAt(item: AdminContestSummary, now: Date): ContestPhase {
    if (now < new Date(item.startAt)) {
        return "UPCOMING";
    }
    return now < new Date(item.endAt) ? "RUNNING" : "FINISHED";
}

function withPhase(item: AdminContest): AdminContest {
    return { ...item, phase: phaseAt(item, new Date()) };
}

function toSummary(item: AdminContest): AdminContestSummary {
    return {
        id: item.id,
        slug: item.slug,
        title: item.title,
        startAt: item.startAt,
        endAt: item.endAt,
        durationMinutes: item.durationMinutes,
        isRated: item.isRated,
        phase: item.phase,
    };
}

function adminOnly(): Response | null {
    const user = readMockSession();
    if (!user) {
        return apiError(401, "UNAUTHORIZED", "Bạn cần đăng nhập");
    }
    return user.role === "ADMIN"
        ? null
        : apiError(403, "FORBIDDEN", "Bạn không có quyền thực hiện thao tác này");
}

export const adminContestHandlers: RequestHandler[] = [
    http.get("*/api/admin/contests", async () => {
        await delay();
        const denied = adminOnly();
        if (denied) {
            return denied;
        }
        const items = contests
            .map(withPhase)
            .sort((left, right) => right.startAt.localeCompare(left.startAt))
            .map(toSummary);
        return HttpResponse.json({
            items,
            page: 1,
            size: 100,
            totalItems: items.length,
            totalPages: 1,
        });
    }),
    http.get("*/api/admin/contests/:contestId", async ({ params }) => {
        await delay();
        const denied = adminOnly();
        if (denied) {
            return denied;
        }
        const found = contests.find((item) => item.id === params.contestId);
        return found
            ? HttpResponse.json(withPhase(found))
            : apiError(404, "CONTEST_NOT_FOUND", "Không tìm thấy contest");
    }),
    http.post("*/api/admin/contests", async ({ request }) => {
        await delay();
        const denied = adminOnly();
        if (denied) {
            return denied;
        }
        const body = (await request.json()) as CreateContestRequest;
        if (contests.some((item) => item.slug === body.slug)) {
            return apiError(409, "SLUG_TAKEN", "Slug đã được contest khác dùng");
        }
        const created: AdminContest = {
            ...body,
            id: crypto.randomUUID(),
            endAt: new Date(
                new Date(body.startAt).getTime() + body.durationMinutes * MS_PER_MINUTE,
            ).toISOString(),
            phase: "UPCOMING",
            problems: [],
        };
        contests = [...contests, created];
        return HttpResponse.json(withPhase(created), { status: 201 });
    }),
];
