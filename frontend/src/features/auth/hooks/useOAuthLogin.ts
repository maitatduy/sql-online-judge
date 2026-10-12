import { useMutation } from "@tanstack/react-query";

import { authApi } from "../api";

export function useOAuthLogin() {
    return useMutation({ mutationFn: authApi.loginWithOAuth });
}
