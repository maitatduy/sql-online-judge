import { useMutation } from "@tanstack/react-query";

import { authApi } from "../api";

import { useCompleteSignIn } from "./useCompleteSignIn";

export function useLogin() {
    const completeSignIn = useCompleteSignIn();
    return useMutation({ mutationFn: authApi.login, onSuccess: completeSignIn });
}
