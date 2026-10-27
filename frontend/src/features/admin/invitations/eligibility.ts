import { z } from "zod";

import type { AdminUserSummary } from "@/features/admin/users/types";

import type { InvitableRole } from "./types";

const emailSchema = z.string().trim().email();

const ROLE_HELD_REASONS: Record<InvitableRole, string> = {
    SETTER: "Đã là Người ra đề",
    MODERATOR: "Đã là Kiểm duyệt",
};

type Candidate = Pick<AdminUserSummary, "email" | "role" | "status">;

export function ineligibilityOf(
    candidate: Candidate,
    role: InvitableRole,
    pendingEmails: ReadonlySet<string>,
): string | null {
    if (candidate.role === "ADMIN") {
        return "Là quản trị viên";
    }
    if (candidate.role === role) {
        return ROLE_HELD_REASONS[role];
    }
    if (candidate.status === "BANNED") {
        return "Tài khoản đang bị khoá";
    }
    if (pendingEmails.has(candidate.email.toLowerCase())) {
        return "Đang chờ nhận lời mời";
    }
    return null;
}

export function isInvitableEmail(value: string): boolean {
    return emailSchema.safeParse(value).success && /\.[^.@]+$/.test(value.trim());
}

export function normalizeEmail(value: string): string {
    return value.trim().toLowerCase();
}
