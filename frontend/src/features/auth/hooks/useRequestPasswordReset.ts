import { useMutation } from "@tanstack/react-query";

import { authApi } from "../api";

export function useRequestPasswordReset() {
    return useMutation({ mutationFn: authApi.requestPasswordReset });
}
