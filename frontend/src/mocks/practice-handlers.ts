import { delay, http, HttpResponse, type RequestHandler } from "msw";

import { PROBLEM_PROGRESSES } from "@/features/practice/problem-filters";
import type { ProblemSummary, TagsResponse } from "@/features/practice/types";
import type { ApiFieldError } from "@/lib/api-client";
import { DIFFICULTIES } from "@/lib/difficulty";
import type { Page } from "@/lib/pagination";

import { validationFailed } from "./admin-user-errors";
import { apiError } from "./api-error";
import { buildProblemSummaries, PRACTICE_TAGS } from "./practice-fixtures";
import { buildProblemDetail } from "./practice-problem-details";
import { readMockSession } from "./session";

const DEFAULT_PAGE_SIZE = 20;
const MAX_PAGE_SIZE = 100;
const SEARCH_LOCALE = "vi";

function integerParam(value: string | null, fallback: number): number {
    return value === null ? fallback : Number(value);
}

function isKnown(knownValues: readonly string[], value: string | null): boolean {
    return value === null || knownValues.includes(value);
}

function invalidParam(query: URLSearchParams): ApiFieldError | null {
    if (!isKnown(DIFFICULTIES, query.get("difficulty"))) {
        return { field: "difficulty", message: "Độ khó không hợp lệ" };
    }
    if (!isKnown(PROBLEM_PROGRESSES, query.get("progress"))) {
        return { field: "progress", message: "Tiến độ không hợp lệ" };
    }
    const page = integerParam(query.get("page"), 1);
    if (!Number.isInteger(page) || page < 1) {
        return { field: "page", message: "Trang bắt đầu từ 1" };
    }
    const size = integerParam(query.get("size"), DEFAULT_PAGE_SIZE);
    if (!Number.isInteger(size) || size < 1 || size > MAX_PAGE_SIZE) {
        return { field: "size", message: `Kích thước trang từ 1 đến ${MAX_PAGE_SIZE}` };
    }
    return null;
}

function matchesQuery(problem: ProblemSummary, query: URLSearchParams): boolean {
    const keyword = query.get("q")?.trim().toLocaleLowerCase(SEARCH_LOCALE) ?? "";
    const difficulty = query.get("difficulty");
    const tag = query.get("tag");
    const progress = query.get("progress");
    return (
        (problem.title.toLocaleLowerCase(SEARCH_LOCALE).includes(keyword) ||
            String(problem.number) === keyword) &&
        (difficulty === null || problem.difficulty === difficulty) &&
        (tag === null || problem.tags.some((problemTag) => problemTag.slug === tag)) &&
        (progress === null || problem.progress === progress)
    );
}

export const practiceHandlers: RequestHandler[] = [
    http.get("*/api/tags", async () => {
        await delay();
        const response: TagsResponse = { items: PRACTICE_TAGS };
        return HttpResponse.json(response);
    }),

    http.get("*/api/problems", async ({ request }) => {
        await delay();
        const query = new URL(request.url).searchParams;
        const invalid = invalidParam(query);
        if (invalid) {
            return validationFailed(invalid);
        }
        const page = integerParam(query.get("page"), 1);
        const size = integerParam(query.get("size"), DEFAULT_PAGE_SIZE);
        const problems = buildProblemSummaries(readMockSession() !== null)
            .filter((problem) => matchesQuery(problem, query))
            .sort((first, second) => first.number - second.number);
        const firstIndex = (page - 1) * size;
        const response: Page<ProblemSummary> = {
            items: problems.slice(firstIndex, firstIndex + size),
            page,
            size,
            totalItems: problems.length,
            totalPages: Math.ceil(problems.length / size),
        };
        return HttpResponse.json(response);
    }),

    http.get("*/api/problems/:slug", async ({ params }) => {
        await delay();
        const summary = buildProblemSummaries(readMockSession() !== null).find(
            (problem) => problem.slug === params.slug,
        );
        if (!summary) {
            return apiError(404, "PROBLEM_NOT_FOUND", "Không tìm thấy bài tập");
        }
        return HttpResponse.json(buildProblemDetail(summary));
    }),
];
