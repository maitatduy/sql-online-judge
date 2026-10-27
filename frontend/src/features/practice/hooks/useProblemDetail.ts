import { useQuery } from "@tanstack/react-query";

import { ApiRequestError } from "@/lib/api-client";

import { practiceApi } from "../api";

const PROBLEM_STALE_TIME_MS = 10 * 60_000;
const MAX_RETRIES = 1;
const NOT_FOUND_STATUS = 404;

export function isProblemNotFound(error: unknown): boolean {
    return error instanceof ApiRequestError && error.status === NOT_FOUND_STATUS;
}

export function useProblemDetail(slug: string) {
    return useQuery({
        queryKey: ["problem", slug],
        queryFn: ({ signal }) => practiceApi.getProblem(slug, signal),
        staleTime: PROBLEM_STALE_TIME_MS,
        retry: (failureCount, error) => !isProblemNotFound(error) && failureCount < MAX_RETRIES,
    });
}
