import { ROUTES } from "@/lib/routes";

import { PROBLEM_STATUS_LABELS } from "./status-labels";
import type { ProblemStatus } from "./types";

const FIRST_PAGE = 1;
export const MY_PROBLEMS_PAGE_SIZE = 20;

export interface MyProblemFilters {
    status: ProblemStatus | null;
    q: string;
    page: number;
}

export const NO_MY_PROBLEM_FILTERS: MyProblemFilters = { status: null, q: "", page: FIRST_PAGE };

function readStatus(value: string | null): ProblemStatus | null {
    return value !== null && value in PROBLEM_STATUS_LABELS ? (value as ProblemStatus) : null;
}

function readPage(value: string | null): number {
    const page = Number(value);
    return Number.isInteger(page) && page >= FIRST_PAGE ? page : FIRST_PAGE;
}

export function readMyProblemFilters(searchParams: Pick<URLSearchParams, "get">): MyProblemFilters {
    return {
        status: readStatus(searchParams.get("status")),
        q: searchParams.get("q")?.trim() ?? "",
        page: readPage(searchParams.get("page")),
    };
}

function activeFilterParams({ status, q }: MyProblemFilters) {
    return { ...(status ? { status } : {}), ...(q ? { q } : {}) };
}

export function myProblemsQueryOf(filters: MyProblemFilters) {
    return { ...activeFilterParams(filters), page: filters.page, size: MY_PROBLEMS_PAGE_SIZE };
}

export function myProblemsHref(filters: MyProblemFilters): string {
    const query = new URLSearchParams(activeFilterParams(filters));
    if (filters.page > FIRST_PAGE) {
        query.set("page", String(filters.page));
    }
    const queryString = query.toString();
    return queryString ? `${ROUTES.studioProblems}?${queryString}` : ROUTES.studioProblems;
}
