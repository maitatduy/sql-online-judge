import { useMutation } from "@tanstack/react-query";

import { authApi } from "../api";

export function useConfirmEmail() {
    return useMutation({ mutationFn: authApi.confirmEmail });
}
