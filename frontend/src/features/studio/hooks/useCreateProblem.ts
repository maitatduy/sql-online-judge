import { useMutation, useQueryClient } from "@tanstack/react-query";

import { studioApi } from "../api";

export function useCreateProblem() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: studioApi.createProblem,
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ["studio-problems"] }),
    });
}
