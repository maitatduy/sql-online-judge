import { DIFFICULTIES, type Difficulty } from "@/lib/difficulty";
import { ROUTES } from "@/lib/routes";

import type { ProblemProgress } from "./types";

export const PROBLEM_PROGRESSES: readonly ProblemProgress[] = [
    "SOLVED",
    "ATTEMPTED",
    "NOT_STARTED",
];

export const PROGRESS_LABELS: Record<ProblemProgress, string> = {
    SOLVED: "Đã AC",
    ATTEMPTED: "Đã thử",
    NOT_STARTED: "Chưa làm",
};

const PROBLEMS_PAGE_SIZE = 20;
const FIRST_PAGE = 1;

export interface ProblemFilters {
    q: string;
    difficulty: Difficulty | null;
    tag: string | null;
    progress: ProblemProgress | null;
    page: number;
}

export const NO_PROBLEM_FILTERS: ProblemFilters = {
    q: "",
    difficulty: null,
    tag: null,
    progress: null,
    page: FIRST_PAGE,
};

function knownValue<T extends string>(knownValues: readonly T[], value: string | null): T | null {
    return knownValues.find((knownValue) => knownValue === value) ?? null;
}

function readPage(value: string | null): number {
    const page = Number(value);
    return Number.isInteger(page) && page >= FIRST_PAGE ? page : FIRST_PAGE;
}

export function readProblemFilters(searchParams: Pick<URLSearchParams, "get">): ProblemFilters {
    return {
        q: searchParams.get("q")?.trim() ?? "",
        difficulty: knownValue(DIFFICULTIES, searchParams.get("difficulty")),
        tag: searchParams.get("tag") || null,
        progress: knownValue(PROBLEM_PROGRESSES, searchParams.get("progress")),
        page: readPage(searchParams.get("page")),
    };
}

function activeFilterParams({ q, difficulty, tag, progress }: ProblemFilters) {
    return {
        ...(q ? { q } : {}),
        ...(difficulty ? { difficulty } : {}),
        ...(tag ? { tag } : {}),
        ...(progress ? { progress } : {}),
    };
}

export function problemsQueryOf(filters: ProblemFilters) {
    return { ...activeFilterParams(filters), page: filters.page, size: PROBLEMS_PAGE_SIZE };
}

export function problemsHref(filters: ProblemFilters): string {
    const query = new URLSearchParams(activeFilterParams(filters));
    if (filters.page > FIRST_PAGE) {
        query.set("page", String(filters.page));
    }
    const queryString = query.toString();
    return queryString ? `${ROUTES.problems}?${queryString}` : ROUTES.problems;
}

export function hasActiveProblemFilter({ q, difficulty, tag, progress }: ProblemFilters): boolean {
    return q !== "" || difficulty !== null || tag !== null || progress !== null;
}
