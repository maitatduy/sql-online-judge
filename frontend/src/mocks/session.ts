import type { CurrentUser } from "@/lib/current-user";

const SESSION_STORAGE_KEY = "sqljudge:mock:current-user";

export const DEMO_SESSION_USER: CurrentUser = {
    id: "0192f3a0-0000-7000-8000-000000000001",
    username: "minh.anh",
    displayName: "Nguyễn Minh Anh",
    role: "LEARNER",
    currentStreakDays: 12,
};

export const ADMIN_SESSION_USER: CurrentUser = {
    id: "0192f3a0-0000-7000-8000-0000000000ad",
    username: "admin.huy",
    displayName: "Admin Huy",
    role: "ADMIN",
    currentStreakDays: 0,
};

export function readMockSession(): CurrentUser | null {
    const storedUser = sessionStorage.getItem(SESSION_STORAGE_KEY);
    return storedUser ? (JSON.parse(storedUser) as CurrentUser) : null;
}

export function writeMockSession(user: CurrentUser): void {
    sessionStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(user));
}

export function clearMockSession(): void {
    sessionStorage.removeItem(SESSION_STORAGE_KEY);
}
