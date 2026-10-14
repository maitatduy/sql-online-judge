import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { adminApi } from "../api";

const sessionsKey = (username: string) => ["admin", "users", "sessions", username];

export function useUserSessions(username: string) {
    return useQuery({
        queryKey: sessionsKey(username),
        queryFn: ({ signal }) => adminApi.getUserSessions(username, signal),
        select: (response) => response.items,
        retry: false,
    });
}

export function useRevokeSession(username: string) {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (sessionId: string) => adminApi.revokeUserSession({ username, sessionId }),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: sessionsKey(username) }),
    });
}

export function useRevokeAllSessions(username: string) {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: () => adminApi.revokeAllUserSessions(username),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: sessionsKey(username) }),
    });
}
