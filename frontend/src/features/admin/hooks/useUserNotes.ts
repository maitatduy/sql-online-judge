import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { adminApi } from "../api";

const notesKey = (username: string) => ["admin", "users", "notes", username];

export function useUserNotes(username: string) {
    return useQuery({
        queryKey: notesKey(username),
        queryFn: ({ signal }) => adminApi.getUserNotes(username, signal),
        select: (response) => response.items,
        retry: false,
    });
}

export function useAddUserNote(username: string) {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (body: string) => adminApi.addUserNote({ username, body }),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: notesKey(username) }),
    });
}
