import { apiClient } from "@/lib/api-client";

import type {
    AuthResponse,
    ConfirmEmailRequest,
    LoginRequest,
    OAuthProvider,
    RegisterRequest,
    RegisterResponse,
    ResendVerificationRequest,
    UsernameAvailability,
} from "./types";

export const authApi = {
    login: (body: LoginRequest) => apiClient.post<AuthResponse>("/api/auth/login", body),
    loginWithOAuth: (provider: OAuthProvider) =>
        apiClient.post<AuthResponse>(`/api/auth/oauth/${provider}`),
    register: (body: RegisterRequest) =>
        apiClient.post<RegisterResponse>("/api/auth/register", body),
    confirmEmail: (body: ConfirmEmailRequest) =>
        apiClient.post<AuthResponse>("/api/auth/email-verification/confirm", body),
    resendVerification: (body: ResendVerificationRequest) =>
        apiClient.post<undefined>("/api/auth/email-verification", body),
    checkUsername: (username: string, signal?: AbortSignal) =>
        apiClient.get<UsernameAvailability>("/api/users/availability", {
            params: { username },
            signal,
        }),
};
