import { useMutation } from "@tanstack/react-query";

import { authApi } from "../api";

export function useResendVerification() {
    return useMutation({ mutationFn: authApi.resendVerification });
}
