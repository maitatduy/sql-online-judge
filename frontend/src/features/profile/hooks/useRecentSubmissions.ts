import { useInfiniteQuery } from "@tanstack/react-query";
import { useMemo } from "react";

import { profileApi } from "../api";

export function useRecentSubmissions(userId: string) {
    const query = useInfiniteQuery({
        queryKey: ["profile", userId, "submissions"],
        queryFn: ({ pageParam, signal }) =>
            profileApi.getRecentSubmissions(userId, pageParam, signal),
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
