import { useMutation, useQueryClient } from "@tanstack/react-query";

import type { BulkUserActionResponse } from "../users/types";

export function useBulkUserMutation<TRequest>(
    mutationFn: (request: TRequest) => Promise<BulkUserActionResponse>,
) {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn,
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin", "users"] }),
    });
}
