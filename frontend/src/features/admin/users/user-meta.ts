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

export function userMetaLines(user: AdminUserDetail): string[] {
    return [
        user.displayName,
        user.school,
        user.email,
        user.isEmailVerified ? "Đã xác minh" : "Chưa xác minh",
        `Tham gia ${joinedLabel(user)}`,
    ].filter((line): line is string => Boolean(line));
}
