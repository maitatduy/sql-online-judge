import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import {
    changePassword,
    getAccountProfile,
    removeAvatar,
    replaceAvatar,
    updateAccountProfile,
} from "../api";

const ACCOUNT_PROFILE_KEY = ["account-profile"] as const;

function useRefreshAccount() {
    const queryClient = useQueryClient();
    return () =>
        Promise.all([
            queryClient.invalidateQueries({ queryKey: ACCOUNT_PROFILE_KEY }),
            queryClient.invalidateQueries({ queryKey: ["current-user"] }),
            queryClient.invalidateQueries({ queryKey: ["profile"] }),
        ]);
}

export function useAccountProfile() {
    return useQuery({
        queryKey: ACCOUNT_PROFILE_KEY,
        queryFn: ({ signal }) => getAccountProfile(signal),
    });
}

export function useUpdateAccountProfile() {
    const refreshAccount = useRefreshAccount();
    return useMutation({ mutationFn: updateAccountProfile, onSuccess: refreshAccount });
}

export function useReplaceAvatar() {
    const refreshAccount = useRefreshAccount();
    return useMutation({ mutationFn: replaceAvatar, onSuccess: refreshAccount });
}

export function useRemoveAvatar() {
    const refreshAccount = useRefreshAccount();
    return useMutation({ mutationFn: removeAvatar, onSuccess: refreshAccount });
}

export function useChangePassword() {
    const refreshAccount = useRefreshAccount();
    return useMutation({ mutationFn: changePassword, onSuccess: refreshAccount });
}
