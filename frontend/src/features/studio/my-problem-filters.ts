import { DIFFICULTIES, type Difficulty } from "@/lib/difficulty";
import { ROUTES } from "@/lib/routes";

import {
    DEFAULT_PAGE_SIZE,
    DEFAULT_SORT,
    FIRST_PAGE,
    listParams,
    PROBLEM_SORTS,
    readPage,
    readPageSize,
    readSort,
} from "./list-options";

export interface MyProblemFilters {
    difficulty: Difficulty | null;
    q: string;
    sort: string;
    size: number;
    page: number;
}

export const NO_MY_PROBLEM_FILTERS: MyProblemFilters = {
    difficulty: null,
    q: "",
    sort: DEFAULT_SORT,
    size: DEFAULT_PAGE_SIZE,
    page: FIRST_PAGE,
};

function readDifficulty(value: string | null): Difficulty | null {
    return DIFFICULTIES.find((difficulty) => difficulty === value) ?? null;
}

export function readMyProblemFilters(searchParams: Pick<URLSearchParams, "get">): MyProblemFilters {
    return {
        difficulty: readDifficulty(searchParams.get("difficulty")),
        q: searchParams.get("q")?.trim() ?? "",
        sort: readSort(searchParams.get("sort"), PROBLEM_SORTS),
        size: readPageSize(searchParams.get("size")),
        page: readPage(searchParams.get("page")),
    };
}

function activeFilterParams({ difficulty, q }: MyProblemFilters) {
    return {
        ...(difficulty ? { difficulty } : {}),
        ...(q ? { q } : {}),
    };
}

export function myProblemsQueryOf(filters: MyProblemFilters) {
    return {
        ...activeFilterParams(filters),
        sort: filters.sort,
        page: filters.page,
        size: filters.size,
    };
}

export function myProblemsHref(filters: MyProblemFilters): string {
    const query = new URLSearchParams({
        ...activeFilterParams(filters),
        ...listParams(filters.page, filters.size, filters.sort),
    });
    const queryString = query.toString();
    return queryString ? `${ROUTES.studioProblems}?${queryString}` : ROUTES.studioProblems;
}
