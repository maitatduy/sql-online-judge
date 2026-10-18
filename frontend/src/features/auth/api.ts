import { apiClient } from "@/lib/api-client";

import type {
    AuthResponse,
    ConfirmEmailRequest,
    LoginRequest,
    PasswordResetRequest,
    RegisterRequest,
    RegisterResponse,
    ResendVerificationRequest,
    ResetPasswordRequest,
    UsernameAvailability,
} from "./types";

export const authApi = {
    login: (body: LoginRequest) => apiClient.post<AuthResponse>("/api/auth/login", body),
    refreshSession: () => apiClient.post<AuthResponse>("/api/auth/refresh"),
    register: (body: RegisterRequest) =>
        apiClient.post<RegisterResponse>("/api/auth/register", body),
    confirmEmail: (body: ConfirmEmailRequest) =>
        apiClient.post<AuthResponse>("/api/auth/email-verification/confirm", body),
    resendVerification: (body: ResendVerificationRequest) =>
        apiClient.post<undefined>("/api/auth/email-verification", body),
    requestPasswordReset: (body: PasswordResetRequest) =>
        apiClient.post<undefined>("/api/auth/password-reset", body),
    resetPassword: (body: ResetPasswordRequest) =>
        apiClient.post<undefined>("/api/auth/password-reset/confirm", body),
    checkUsername: (username: string, signal?: AbortSignal) =>
        apiClient.get<UsernameAvailability>("/api/users/availability", {
            params: { username },
            signal,
        }),
};
