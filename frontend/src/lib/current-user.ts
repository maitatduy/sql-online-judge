export type Role = "LEARNER" | "SETTER" | "MODERATOR" | "ADMIN";

export interface CurrentUser {
    id: string;
    username: string;
    displayName: string;
    role: Role;
    currentStreakDays: number;
}
