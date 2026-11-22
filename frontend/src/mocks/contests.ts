import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type { AdminContest } from "@/features/admin/contests/types";

import { mockContests } from "./admin-contests";
import { apiError } from "./api-error";
import { readMockSession } from "./session";

let registrations = new Map<string, Map<string, string>>();

export function resetContestRegistrations() {
    registrations = new Map();
}

function registrantsOf(contestId: string): Map<string, string> {
    const existing = registrations.get(contestId);
    if (existing) {
        return existing;
    }
    const created = new Map<string, string>();
    registrations.set(contestId, created);
    return created;
}

function findContest(slug: unknown): AdminContest | undefined {
    return mockContests().find((contest) => contest.slug === slug);
}

function notFound() {
    return apiError(404, "CONTEST_NOT_FOUND", "Không tìm thấy contest");
}

function unauthenticated() {
    return apiError(401, "UNAUTHORIZED", "Bạn cần đăng nhập");
}

function statusOf(contest: AdminContest, userId: string) {
    const registeredAt = registrantsOf(contest.id).get(userId) ?? null;
    return { isRegistered: registeredAt !== null, registeredAt };
}

export const contestHandlers: RequestHandler[] = [
    http.get("*/api/contests", async ({ request }) => {
        await delay();
        const phase = new URL(request.url).searchParams.get("phase") ?? "UPCOMING";
        const items = mockContests()
            .filter((contest) => contest.phase === phase)
            .map(({ id, slug, title, startAt, endAt, durationMinutes, isRated }) => ({
                id,
                slug,
                title,
                startAt,
                endAt,
                durationMinutes,
                isRated,
                phase,
            }));
        return HttpResponse.json({
            items,
            page: 1,
            size: 20,
            totalItems: items.length,
            totalPages: 1,
        });
    }),
    http.get("*/api/contests/:slug", async ({ params }) => {
        await delay();
        const contest = findContest(params.slug);
        if (!contest) {
            return notFound();
        }
        return HttpResponse.json({
            id: contest.id,
            slug: contest.slug,
            title: contest.title,
            description: contest.description,
            startAt: contest.startAt,
            endAt: contest.endAt,
            durationMinutes: contest.durationMinutes,
            freezeMinutes: contest.freezeMinutes,
            penaltyMinutes: contest.penaltyMinutes,
            isRated: contest.isRated,
            phase: contest.phase,
            registrantCount: registrantsOf(contest.id).size,
        });
    }),
    http.get("*/api/contests/:slug/problems", async ({ params }) => {
        await delay();
        const user = readMockSession();
        const contest = findContest(params.slug);
        if (!user) {
            return unauthenticated();
        }
        if (!contest) {
            return notFound();
        }
        if (contest.phase === "UPCOMING") {
            return apiError(403, "CONTEST_NOT_STARTED", "Contest chưa bắt đầu, chưa xem được đề");
        }
        if (contest.phase === "RUNNING" && !registrantsOf(contest.id).has(user.id)) {
            return apiError(403, "CONTEST_NOT_REGISTERED", "Bạn cần đăng ký contest để xem đề");
        }
        return HttpResponse.json(
            contest.problems.map(({ letter, title, difficulty }) => ({
                letter,
                title,
                difficulty,
            })),
        );
    }),
    http.get("*/api/contests/:slug/registration", async ({ params }) => {
        await delay();
        const user = readMockSession();
        const contest = findContest(params.slug);
        if (!user) {
            return unauthenticated();
        }
        return contest ? HttpResponse.json(statusOf(contest, user.id)) : notFound();
    }),
    http.post("*/api/contests/:slug/registration", async ({ params }) => {
        await delay();
        const user = readMockSession();
        const contest = findContest(params.slug);
        if (!user) {
            return unauthenticated();
        }
        if (!contest) {
            return notFound();
        }
        if (contest.phase === "FINISHED") {
            return apiError(
                422,
                "CONTEST_ALREADY_FINISHED",
                "Contest đã kết thúc, không đăng ký được",
            );
        }
        const registrants = registrantsOf(contest.id);
        const isCreated = !registrants.has(user.id);
        if (isCreated) {
            registrants.set(user.id, new Date().toISOString());
        }
        return HttpResponse.json(statusOf(contest, user.id), { status: isCreated ? 201 : 200 });
    }),
    http.delete("*/api/contests/:slug/registration", async ({ params }) => {
        await delay();
        const user = readMockSession();
        const contest = findContest(params.slug);
        if (!user) {
            return unauthenticated();
        }
        if (!contest) {
            return notFound();
        }
        if (contest.phase !== "UPCOMING") {
            return apiError(
                409,
                "CONTEST_ALREADY_STARTED",
                "Contest đã bắt đầu, không huỷ đăng ký được nữa",
            );
        }
        registrantsOf(contest.id).delete(user.id);
        return new HttpResponse(null, { status: 204 });
    }),
];
