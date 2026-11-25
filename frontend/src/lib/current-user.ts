export type Role = "LEARNER" | "SETTER" | "MODERATOR" | "ADMIN";

export interface CurrentUser {
    id: string;
    username: string;
    displayName: string;
    avatarUrl: string | null;
    role: Role;
    currentStreakDays: number;
}
