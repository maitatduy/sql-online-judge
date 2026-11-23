import { useMutation, useQueryClient } from "@tanstack/react-query";

export function useProblemBankMutation<TVariables, TResult = unknown>(
    mutationFn: (variables: TVariables) => Promise<TResult>,
) {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn,
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin", "problems"] }),
    });
}
