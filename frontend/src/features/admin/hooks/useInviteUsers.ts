import { useMutation } from "@tanstack/react-query";

import { adminApi } from "../api";

export function useInviteUsers() {
    return useMutation({ mutationFn: adminApi.inviteUsers });
}
