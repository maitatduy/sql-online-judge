import { useMutation, useQueryClient } from "@tanstack/react-query";

import { studioApi } from "../api";

export function useDeleteProblem() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: studioApi.deleteProblem,
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ["studio-problems"] }),
    });
}
