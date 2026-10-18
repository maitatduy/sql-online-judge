import { useQueryClient } from "@tanstack/react-query";

import { setAccessToken } from "@/lib/auth-session";

import type { AuthResponse } from "../types";

export function useCompleteSignIn() {
    const queryClient = useQueryClient();

    return (response: AuthResponse) => {
        setAccessToken(response.accessToken);
        void queryClient.invalidateQueries({ queryKey: ["current-user"] });
    };
}
