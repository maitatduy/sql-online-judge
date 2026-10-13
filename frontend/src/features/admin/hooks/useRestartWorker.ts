import { useMutation, useQueryClient } from "@tanstack/react-query";

import { adminApi } from "../api";

export function useRestartWorker() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: adminApi.restartWorker,
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin", "judge-workers"] }),
    });
}
