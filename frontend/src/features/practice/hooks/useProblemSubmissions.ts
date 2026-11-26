import { keepPreviousData, useQuery } from "@tanstack/react-query";

import { practiceApi } from "../api";

export const HISTORY_PAGE_SIZE = 10;

export function problemSubmissionsQueryKey(problemId: string) {
    return ["problem-submissions", problemId] as const;
}

export function useProblemSubmissions(problemId: string, page: number) {
    return useQuery({
        queryKey: [...problemSubmissionsQueryKey(problemId), { page }],
        queryFn: ({ signal }) =>
            practiceApi.listProblemSubmissions(
                problemId,
                { page, size: HISTORY_PAGE_SIZE },
                signal,
            ),
        placeholderData: keepPreviousData,
        retry: false,
    });
}
