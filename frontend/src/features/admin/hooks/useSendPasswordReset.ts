import { useMutation } from "@tanstack/react-query";

import { adminApi } from "../api";

export function useSendPasswordReset() {
    return useMutation({ mutationFn: adminApi.sendPasswordResetEmail });
}
