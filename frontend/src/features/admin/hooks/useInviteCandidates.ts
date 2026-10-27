import { keepPreviousData, useQuery } from "@tanstack/react-query";

import { adminApi } from "../api";

const MIN_QUERY_LENGTH = 2;

export function useInviteCandidates(query: string) {
    const trimmed = query.trim();
    return useQuery({
        queryKey: ["admin", "invite-candidates", trimmed],
        queryFn: ({ signal }) => adminApi.searchInviteCandidates(trimmed, signal),
        enabled: trimmed.length >= MIN_QUERY_LENGTH,
        placeholderData: keepPreviousData,
        retry: false,
    });
}
