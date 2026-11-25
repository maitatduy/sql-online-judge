import { ADMIN_ROUTES } from "@/lib/admin-navigation";
import type { Dialect } from "@/lib/dialect";
import type { Verdict } from "@/lib/verdict";

import type { SubmissionSource } from "./types";

export const VERDICT_FILTERS = ["AC", "WA", "OM", "TLE", "RE", "FS", "SE"] as const;
export const PENDING_FILTER = "PENDING";
export const DIALECTS: readonly Dialect[] = ["POSTGRESQL", "MYSQL", "SQLITE"];
export const SOURCES: readonly SubmissionSource[] = ["PRACTICE", "CONTEST"];

export type ResultFilter = Verdict | typeof PENDING_FILTER;

const RESULTS: readonly ResultFilter[] = [...VERDICT_FILTERS, PENDING_FILTER];
const FIRST_PAGE = 1;

export interface SubmissionFilters {
    result: ResultFilter | null;
    dialect: Dialect | null;
    source: SubmissionSource | null;
    username: string;
    problem: string;
    page: number;
}

export const NO_SUBMISSION_FILTERS: SubmissionFilters = {
    result: null,
    dialect: null,
    source: null,
    username: "",
    problem: "",
    page: FIRST_PAGE,
};

function knownValue<T extends string>(knownValues: readonly T[], value: string | null): T | null {
    return knownValues.find((knownValue) => knownValue === value) ?? null;
}

function readPage(value: string | null): number {
    const page = Number(value);
    return Number.isInteger(page) && page >= FIRST_PAGE ? page : FIRST_PAGE;
}

export function readSubmissionFilters(params: Pick<URLSearchParams, "get">): SubmissionFilters {
    return {
        result: knownValue(RESULTS, params.get("result")),
        dialect: knownValue(DIALECTS, params.get("dialect")),
        source: knownValue(SOURCES, params.get("source")),
        username: params.get("username")?.trim() ?? "",
        problem: params.get("problem")?.trim() ?? "",
        page: readPage(params.get("page")),
    };
}

export function hasActiveSubmissionFilter(filters: SubmissionFilters): boolean {
    return Boolean(
        filters.result || filters.dialect || filters.source || filters.username || filters.problem,
    );
}

export function submissionsHref(filters: SubmissionFilters): string {
    const query = new URLSearchParams();
    const entries: [string, string | null][] = [
        ["result", filters.result],
        ["dialect", filters.dialect],
        ["source", filters.source],
        ["username", filters.username || null],
        ["problem", filters.problem || null],
        ["page", filters.page > FIRST_PAGE ? String(filters.page) : null],
    ];
    entries.forEach(([key, value]) => {
        if (value) {
            query.set(key, value);
        }
    });
    const search = query.toString();
    return `${ADMIN_ROUTES.submissions}${search ? `?${search}` : ""}`;
}

export function submissionsQueryOf(filters: SubmissionFilters) {
    const isPending = filters.result === PENDING_FILTER;
    return {
        verdict: isPending ? undefined : (filters.result ?? undefined),
        status: isPending ? PENDING_FILTER : undefined,
        dialect: filters.dialect ?? undefined,
        source: filters.source ?? undefined,
        username: filters.username || undefined,
        problem: filters.problem || undefined,
        page: filters.page,
    };
}
