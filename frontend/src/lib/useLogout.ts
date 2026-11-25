import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";

import { apiClient } from "@/lib/api-client";
import { clearAccessToken } from "@/lib/auth-session";
import type { CurrentUser } from "@/lib/current-user";
import { ROUTES } from "@/lib/routes";
import { CURRENT_USER_KEY } from "@/lib/useCurrentUser";

export function useLogout() {
    const queryClient = useQueryClient();
    const router = useRouter();

    return useMutation({
        mutationFn: () => apiClient.post<undefined>("/api/auth/logout"),
        onSettled: () => {
            clearAccessToken();
            queryClient.setQueryData<CurrentUser | null>(CURRENT_USER_KEY, null);
            queryClient.removeQueries({
                predicate: (query) => query.queryKey[0] !== CURRENT_USER_KEY[0],
            });
            router.push(ROUTES.home);
        },
    });
}
