import { formatDate } from "@/lib/format";

import type { AdminUserDetail, SignupProvider } from "./types";

const SEPARATOR = " · ";

export const SIGNUP_PROVIDER_LABELS: Record<SignupProvider, string> = {
    EMAIL: "email",
    GOOGLE: "Google",
    GITHUB: "GitHub",
};

export function joinedLabel({ joinedAt, signupProvider }: AdminUserDetail): string {
    return `${formatDate(joinedAt)} qua ${SIGNUP_PROVIDER_LABELS[signupProvider]}`;
}

export function userMetaLine(user: AdminUserDetail): string {
    return [
        user.displayName,
        user.school,
        user.email,
        user.isEmailVerified ? "Đã xác minh" : "Chưa xác minh",
        `Tham gia ${joinedLabel(user)}`,
    ]
        .filter(Boolean)
        .join(SEPARATOR);
}
