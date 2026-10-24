import { useQuery } from "@tanstack/react-query";

import { practiceApi } from "../api";

const TOPICS_STALE_TIME_MS = 60 * 60_000;

export function useTopics() {
    return useQuery({
        queryKey: ["problem-topics"],
        queryFn: ({ signal }) => practiceApi.listTopics(signal),
        staleTime: TOPICS_STALE_TIME_MS,
    });
}
