import { useQuery } from "@tanstack/react-query";

import { practiceApi } from "../api";

const TAGS_STALE_TIME_MS = 60 * 60_000;

export function useTags() {
    return useQuery({
        queryKey: ["tags"],
        queryFn: ({ signal }) => practiceApi.listTags(signal),
        staleTime: TAGS_STALE_TIME_MS,
    });
}
