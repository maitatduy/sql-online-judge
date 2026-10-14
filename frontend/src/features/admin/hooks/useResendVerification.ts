import { useMutation } from "@tanstack/react-query";

import { adminApi } from "../api";

export function useResendVerification() {
    return useMutation({ mutationFn: adminApi.resendVerificationEmail });
}
