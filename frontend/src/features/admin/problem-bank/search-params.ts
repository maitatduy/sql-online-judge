import { DIFFICULTIES, type Difficulty } from "@/lib/difficulty";
import { ROUTES } from "@/lib/routes";

import type { ProblemVisibility } from "../problem-reviews/types";
import { VISIBILITIES } from "./labels";

const QUERY_PARAM = "q";
const VISIBILITY_PARAM = "visibility";
const DIFFICULTY_PARAM = "difficulty";
const TAG_PARAM = "tag";
const PAGE_PARAM = "page";
const FIRST_PAGE = 1;

export interface ProblemBankFilters {
    q: string;
    visibility: ProblemVisibility | null;
    difficulty: Difficulty | null;
    tag: string | null;
    page: number;
}

export const NO_BANK_FILTERS: ProblemBankFilters = {
    q: "",
    visibility: null,
    difficulty: null,
    tag: null,
    page: FIRST_PAGE,
};

function knownValue<T extends string>(knownValues: readonly T[], value: string | null): T | null {
    return knownValues.find((knownValue) => knownValue === value) ?? null;
}

function readPage(value: string | null): number {
    const page = Number(value);
    return Number.isInteger(page) && page >= FIRST_PAGE ? page : FIRST_PAGE;
}

export function readBankFilters(searchParams: Pick<URLSearchParams, "get">): ProblemBankFilters {
    return {
        q: searchParams.get(QUERY_PARAM)?.trim() ?? "",
        visibility: knownValue(VISIBILITIES, searchParams.get(VISIBILITY_PARAM)),
        difficulty: knownValue(DIFFICULTIES, searchParams.get(DIFFICULTY_PARAM)),
        tag: searchParams.get(TAG_PARAM) || null,
        page: readPage(searchParams.get(PAGE_PARAM)),
    };
}

export function bankHref({ q, visibility, difficulty, tag, page }: ProblemBankFilters): string {
    const query = new URLSearchParams();
    if (q) {
        query.set(QUERY_PARAM, q);
    }
    if (visibility) {
        query.set(VISIBILITY_PARAM, visibility);
    }
    if (difficulty) {
        query.set(DIFFICULTY_PARAM, difficulty);
    }
    if (tag) {
        query.set(TAG_PARAM, tag);
    }
    if (page > FIRST_PAGE) {
        query.set(PAGE_PARAM, String(page));
    }
    const queryString = query.toString();
    return `${ROUTES.adminProblems}${queryString ? `?${queryString}` : ""}`;
}

export function hasActiveBankFilter({ q, visibility, difficulty, tag }: ProblemBankFilters) {
    return Boolean(q || visibility || difficulty || tag);
}
