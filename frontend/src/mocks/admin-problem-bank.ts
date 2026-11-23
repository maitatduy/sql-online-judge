import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type { BankProblem, ProblemBankSummary } from "@/features/admin/problem-bank/types";

import { PRACTICE_TAGS } from "./practice-fixtures";
import { SETTER_SESSION_USER } from "./session";
import { readStudioProblems, type StoredProblem } from "./studio-problems";

const DEFAULT_PAGE_SIZE = 20;
const LOW_ACCEPTANCE_MIN_SUBMISSIONS = 50;
const LOW_ACCEPTANCE_MAX_PERCENT = 10;
const PERCENT_TENTHS = 1000;
const TENTHS = 10;

const SEEDED_TAGS: Record<string, string[]> = {
    "late-orders-by-region": ["group-by", "date-time"],
    "classmate-pairs": ["join"],
    "employees-earning-more-than-managers": ["join", "subquery"],
    "booking-gaps": ["window-function"],
    "best-sellers-per-quarter": ["window-function", "cte"],
};

const SEEDED_STATS: Record<string, [submissions: number, accepted: number]> = {
    "late-orders-by-region": [1204, 555],
    "classmate-pairs": [21385, 9431],
    "employees-earning-more-than-managers": [35240, 24104],
    "booking-gaps": [612, 57],
    "best-sellers-per-quarter": [20, 1],
};

function acceptanceRate(submissions: number, accepted: number): number {
    return submissions === 0 ? 0 : Math.round((accepted * PERCENT_TENTHS) / submissions) / TENTHS;
}

function visibilityOf(problem: StoredProblem) {
    return problem.status === "PUBLISHED" ? (problem.visibility ?? "PUBLIC") : null;
}

function toBankProblem(problem: StoredProblem): BankProblem {
    const tagSlugs =
        problem.tagSlugs.length > 0 ? problem.tagSlugs : (SEEDED_TAGS[problem.slug] ?? []);
    const [submissionCount, acceptedCount] = SEEDED_STATS[problem.slug] ?? [0, 0];
    return {
        id: problem.id,
        number: problem.number,
        slug: problem.slug,
        title: problem.title,
        status: problem.status,
        visibility: visibilityOf(problem),
        difficulty: problem.difficulty,
        authorId: problem.authorId,
        authorUsername:
            problem.authorId === SETTER_SESSION_USER.id ? SETTER_SESSION_USER.username : null,
        updatedAt: problem.updatedAt,
        tags: PRACTICE_TAGS.filter((tag) => tagSlugs.includes(tag.slug)),
        submissionCount,
        acceptedCount,
        acceptanceRate: acceptanceRate(submissionCount, acceptedCount),
    };
}

function matchesKeyword(problem: BankProblem, keyword: string): boolean {
    if (!keyword) {
        return true;
    }
    return (
        problem.title.toLowerCase().includes(keyword.toLowerCase()) ||
        String(problem.number) === keyword
    );
}

function publishedProblems(): BankProblem[] {
    return readStudioProblems()
        .filter((problem) => !problem.deletedAt && problem.status === "PUBLISHED")
        .map(toBankProblem);
}

function isLowAcceptance(problem: BankProblem): boolean {
    return (
        problem.submissionCount >= LOW_ACCEPTANCE_MIN_SUBMISSIONS &&
        problem.acceptedCount * 100 < problem.submissionCount * LOW_ACCEPTANCE_MAX_PERCENT
    );
}

export const adminProblemBankHandlers: RequestHandler[] = [
    http.get("*/api/admin/problems/summary", async () => {
        await delay();
        const problems = publishedProblems();
        const countOf = (visibility: string) =>
            problems.filter((problem) => problem.visibility === visibility).length;
        const summary: ProblemBankSummary = {
            publicCount: countOf("PUBLIC"),
            contestOnlyCount: countOf("CONTEST_ONLY"),
            hiddenCount: countOf("HIDDEN"),
            lowAcceptanceCount: problems.filter(isLowAcceptance).length,
        };
        return HttpResponse.json(summary);
    }),

    http.get("*/api/admin/problems", async ({ request }) => {
        await delay();
        const params = new URL(request.url).searchParams;
        const status = params.get("status") ?? "PENDING_REVIEW";
        const visibility = params.get("visibility");
        const difficulty = params.get("difficulty");
        const tag = params.get("tag");
        const keyword = params.get("q")?.trim() ?? "";
        const page = Number(params.get("page") ?? 1);
        const size = Number(params.get("size") ?? DEFAULT_PAGE_SIZE);
        const byNumber = params.get("sort") === "number";
        const matching = readStudioProblems()
            .filter((problem) => !problem.deletedAt && problem.status === status)
            .map(toBankProblem)
            .filter((problem) => !visibility || problem.visibility === visibility)
            .filter((problem) => !difficulty || problem.difficulty === difficulty)
            .filter((problem) => !tag || problem.tags.some((candidate) => candidate.slug === tag))
            .filter((problem) => matchesKeyword(problem, keyword))
            .sort((first, second) =>
                byNumber
                    ? (first.number ?? 0) - (second.number ?? 0)
                    : first.updatedAt.localeCompare(second.updatedAt),
            );
        return HttpResponse.json({
            items: matching.slice((page - 1) * size, page * size),
            page,
            size,
            totalItems: matching.length,
            totalPages: Math.ceil(matching.length / size),
        });
    }),
];
