import { formatDate } from "@/lib/format";

import type { AdminUserDetail, SignupProvider } from "./types";

export const SIGNUP_PROVIDER_LABELS: Record<SignupProvider, string> = {
    EMAIL: "email",
    GOOGLE: "Google",
    GITHUB: "GitHub",
};

export function joinedLabel({ joinedAt, signupProvider }: AdminUserDetail): string {
    return `${formatDate(joinedAt)} qua ${SIGNUP_PROVIDER_LABELS[signupProvider]}`;
}

export interface UserMetaLine {
    kind: "displayName" | "school" | "email" | "verification" | "joined";
    text: string;
}

export function userMetaLines(user: AdminUserDetail): UserMetaLine[] {
    const lines: { kind: UserMetaLine["kind"]; text: string | null }[] = [
        { kind: "displayName", text: user.displayName },
        { kind: "school", text: user.school },
        { kind: "email", text: user.email },
        { kind: "verification", text: user.isEmailVerified ? "Đã xác minh" : "Chưa xác minh" },
        { kind: "joined", text: `Tham gia ${joinedLabel(user)}` },
    ];
    return lines.filter((line): line is UserMetaLine => Boolean(line.text));
}
