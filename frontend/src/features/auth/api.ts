import { apiClient } from "@/lib/api-client";

import type {
    AuthResponse,
    LoginRequest,
    OAuthProvider,
    RegisterRequest,
    RegisterResponse,
    UsernameAvailability,
} from "./types";

export const authApi = {
    login: (body: LoginRequest) => apiClient.post<AuthResponse>("/api/auth/login", body),
    loginWithOAuth: (provider: OAuthProvider) =>
        apiClient.post<AuthResponse>(`/api/auth/oauth/${provider}`),
    register: (body: RegisterRequest) =>
        apiClient.post<RegisterResponse>("/api/auth/register", body),
    checkUsername: (username: string, signal?: AbortSignal) =>
        apiClient.get<UsernameAvailability>("/api/users/availability", {
            params: { username },
            signal,
        }),
};
