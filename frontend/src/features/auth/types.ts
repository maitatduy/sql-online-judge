import type { Role } from "@/lib/current-user";

export type AuthMode = "login" | "register";

export type OAuthProvider = "github" | "google";

export interface AuthUser {
    id: string;
    username: string;
    email: string;
    role: Role;
}

export interface AuthResponse {
    accessToken: string;
    user: AuthUser;
}

export interface LoginRequest {
    email: string;
    password: string;
    rememberMe: boolean;
}

export interface RegisterRequest {
    username: string;
    email: string;
    password: string;
    acceptedTerms: boolean;
}

export interface UsernameAvailability {
    available: boolean;
}
