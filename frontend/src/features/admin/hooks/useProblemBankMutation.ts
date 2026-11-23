import { useMutation, useQueryClient } from "@tanstack/react-query";

export function useProblemBankMutation<TVariables>(
    mutationFn: (variables: TVariables) => Promise<unknown>,
) {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn,
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin", "problems"] }),
    });
}
