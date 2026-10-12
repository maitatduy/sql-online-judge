import { useMutation } from "@tanstack/react-query";

import { authApi } from "../api";

export function useLogin() {
    return useMutation({ mutationFn: authApi.login });
}
