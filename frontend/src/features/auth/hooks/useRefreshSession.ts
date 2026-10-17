import { useMutation } from "@tanstack/react-query";

import { authApi } from "../api";

import { useCompleteSignIn } from "./useCompleteSignIn";

export function useRefreshSession() {
    const completeSignIn = useCompleteSignIn();
    return useMutation({ mutationFn: authApi.refreshSession, onSuccess: completeSignIn });
}
