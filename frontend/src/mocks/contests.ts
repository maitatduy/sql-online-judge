import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type { AdminContest } from "@/features/admin/contests/types";
import type { StartRunRequest, SubmitSolutionRequest } from "@/features/practice/types";

import { mockContests } from "./admin-contests";
import { apiError } from "./api-error";
import { acceptMockRun } from "./practice-run-handlers";
import { findProblem } from "./practice-sql";
import { acceptMockSubmission } from "./practice-submission-handlers";
import { readMockSession } from "./session";

let registrations = new Map<string, Map<string, string>>();

export function resetContestRegistrations() {
    registrations = new Map();
}

export function registerForMockContest(slug: string, userId: string) {
    const contest = findContest(slug);
    if (contest) {
        registrantsOf(contest.id).set(userId, new Date().toISOString());
    }
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

function enteredProblem(slug: unknown, letter: unknown, userId: string) {
    const contest = findContest(slug);
    if (!contest) {
        return notFound();
    }
    if (contest.phase === "UPCOMING") {
        return apiError(403, "CONTEST_NOT_STARTED", "Contest chưa bắt đầu, chưa xem được đề");
    }
    if (contest.phase === "RUNNING" && !registrantsOf(contest.id).has(userId)) {
        return apiError(403, "CONTEST_NOT_REGISTERED", "Bạn cần đăng ký contest để xem đề");
    }
    const entry = contest.problems.find((problem) => problem.letter === letter);
    const problem = entry ? findProblem(entry.problemId) : null;
    return problem && entry
        ? { contest, letter: entry.letter, problem }
        : apiError(404, "PROBLEM_NOT_FOUND", "Contest không có bài này");
}

const MOCK_STANDINGS = [
    {
        username: "quang.le",
        displayName: "Lê Quang",
        acceptedMinute: 12,
        wrongAttempts: 0,
        pendingAttempts: 0,
    },
    {
        username: "trang.hoang",
        displayName: "Hoàng Trang",
        acceptedMinute: 31,
        wrongAttempts: 1,
        pendingAttempts: 0,
    },
    {
        username: "minh.anh",
        displayName: "Minh Anh",
        acceptedMinute: null,
        wrongAttempts: 2,
        pendingAttempts: 1,
    },
];

function cellsFor(
    contest: AdminContest,
    first: Omit<(typeof MOCK_STANDINGS)[number], "username" | "displayName">,
) {
    return contest.problems.map((problem, index) =>
        index === 0
            ? { letter: problem.letter, ...first }
            : {
                  letter: problem.letter,
                  acceptedMinute: null,
                  wrongAttempts: 0,
                  pendingAttempts: 0,
              },
    );
}

const MOCK_RATING_CHANGES = [
    { username: "quang.le", rank: 1, oldRating: 1500, newRating: 1520, delta: 20 },
    { username: "trang.hoang", rank: 2, oldRating: 1500, newRating: 1505, delta: 5 },
    { username: "minh.anh", rank: 3, oldRating: 1500, newRating: 1488, delta: -12 },
];

export const contestHandlers: RequestHandler[] = [
    http.get("*/api/contests/:slug/rating-changes", async ({ params }) => {
        await delay();
        const contest = findContest(params.slug);
        if (!contest) {
            return notFound();
        }
        return HttpResponse.json(contest.phase === "FINISHED" ? MOCK_RATING_CHANGES : []);
    }),
    http.get(
        "*/api/contests/:slug/standings/events",
        () =>
            new HttpResponse(new ReadableStream({ start() {} }), {
                headers: { "Content-Type": "text/event-stream" },
            }),
    ),
    http.get("*/api/contests/:slug/standings/me", async ({ params }) => {
        await delay();
        const user = readMockSession();
        const contest = findContest(params.slug);
        if (!user) {
            return unauthenticated();
        }
        if (!contest || !registrantsOf(contest.id).has(user.id)) {
            return apiError(404, "CONTEST_NOT_REGISTERED", "Bạn chưa đăng ký contest này");
        }
        return HttpResponse.json({
            solved: 1,
            penaltyMinutes: 65,
            cells: cellsFor(contest, { acceptedMinute: 65, wrongAttempts: 0, pendingAttempts: 0 }),
        });
    }),
    http.get("*/api/contests/:slug/standings", async ({ params }) => {
        await delay();
        const contest = findContest(params.slug);
        if (!contest) {
            return notFound();
        }
        const items = MOCK_STANDINGS.map(({ username, displayName, ...first }, index) => ({
            rank: index + 1,
            username,
            displayName,
            solved: first.acceptedMinute === null ? 0 : 1,
            penaltyMinutes:
                first.acceptedMinute === null ? 0 : first.acceptedMinute + first.wrongAttempts * 20,
            cells: cellsFor(contest, first),
        }));
        return HttpResponse.json({
            problems: contest.problems.map((problem) => problem.letter),
            isFrozen: contest.phase !== "UPCOMING",
            items,
            page: 1,
            size: 50,
            totalItems: items.length,
            totalPages: 1,
        });
    }),
    http.get("*/api/contests/:slug/problems/:letter", async ({ params }) => {
        await delay();
        const user = readMockSession();
        if (!user) {
            return unauthenticated();
        }
        const entered = enteredProblem(params.slug, params.letter, user.id);
        if (entered instanceof Response) {
            return entered;
        }
        const { problem } = entered;
        return HttpResponse.json({
            letter: entered.letter,
            title: problem.title,
            difficulty: problem.difficulty,
            statementMarkdown: problem.statementMarkdown,
            dialects: problem.dialects,
            engineVersions: problem.engineVersions,
            starterCode: problem.starterCode,
            schema: problem.schema,
            examples: problem.examples,
            requiresOrder: problem.requiresOrder,
            allowedStatements: problem.allowedStatements,
            timeLimitMs: problem.timeLimitMs,
            hiddenTestcaseCount: problem.hiddenTestcaseCount,
        });
    }),
    http.post("*/api/contests/:slug/problems/:letter/runs", async ({ params, request }) => {
        await delay();
        const user = readMockSession();
        if (!user) {
            return unauthenticated();
        }
        const entered = enteredProblem(params.slug, params.letter, user.id);
        if (entered instanceof Response) {
            return entered;
        }
        return acceptMockRun(user.id, entered.problem, (await request.json()) as StartRunRequest);
    }),
    http.post("*/api/contests/:slug/problems/:letter/submissions", async ({ params, request }) => {
        await delay();
        const user = readMockSession();
        if (!user) {
            return unauthenticated();
        }
        const entered = enteredProblem(params.slug, params.letter, user.id);
        if (entered instanceof Response) {
            return entered;
        }
        return acceptMockSubmission({
            userId: user.id,
            problem: entered.problem,
            body: (await request.json()) as SubmitSolutionRequest,
            idempotencyKey: request.headers.get("Idempotency-Key"),
            isInContest: true,
        });
    }),
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
