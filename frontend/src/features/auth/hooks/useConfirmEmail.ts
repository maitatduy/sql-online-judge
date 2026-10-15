import { useMutation } from "@tanstack/react-query";

import { authApi } from "../api";

import { useCompleteSignIn } from "./useCompleteSignIn";

export function useConfirmEmail() {
    const completeSignIn = useCompleteSignIn();
    return useMutation({ mutationFn: authApi.confirmEmail, onSuccess: completeSignIn });
}
