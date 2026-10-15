import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";

import { apiClient } from "@/lib/api-client";
import { clearAccessToken } from "@/lib/auth-session";
import { ROUTES } from "@/lib/routes";

export function useLogout() {
    const queryClient = useQueryClient();
    const router = useRouter();

    return useMutation({
        mutationFn: () => apiClient.post<undefined>("/api/auth/logout"),
        onSettled: () => {
            clearAccessToken();
            queryClient.clear();
            router.push(ROUTES.home);
        },
    });
}
