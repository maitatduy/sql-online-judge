import { useMemo } from "react";

import { useInfiniteQuery } from "@tanstack/react-query";

import { practiceApi } from "../api";

export function problemSubmissionsQueryKey(problemId: string) {
    return ["problem-submissions", problemId] as const;
}

export function useProblemSubmissions(problemId: string) {
    const query = useInfiniteQuery({
        queryKey: problemSubmissionsQueryKey(problemId),
        queryFn: ({ pageParam, signal }) =>
            practiceApi.listProblemSubmissions(problemId, pageParam, signal),
        initialPageParam: undefined as string | undefined,
        getNextPageParam: (lastPage) => lastPage.nextCursor ?? undefined,
        retry: false,
    });
    const submissions = useMemo(
        () => query.data?.pages.flatMap((page) => page.items) ?? [],
        [query.data],
    );

    return { ...query, submissions };
}
