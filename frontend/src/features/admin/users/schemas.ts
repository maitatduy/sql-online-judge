import { z } from "zod";

import type { Role } from "@/lib/current-user";
import { isReservedUsername, RESERVED_USERNAME_MESSAGE, USERNAME_PATTERN } from "@/lib/username";

import { ROLES } from "./role-labels";

export const MAX_REASON_LENGTH = 500;
export const BAN_DURATIONS = ["DAYS_7", "DAYS_30", "PERMANENT"] as const;

const reason = z
    .string()
    .trim()
    .min(1, "Vui lòng nhập lý do")
    .max(MAX_REASON_LENGTH, `Lý do tối đa ${MAX_REASON_LENGTH} ký tự`);

export const banUserSchema = z.object({
    duration: z.enum(BAN_DURATIONS),
    reason,
    notifyByEmail: z.boolean(),
});

export const unbanUserSchema = z.object({ reason });
export const deleteUserSchema = z.object({ reason });

export type BanUserValues = z.infer<typeof banUserSchema>;
export type UnbanUserValues = z.infer<typeof unbanUserSchema>;
export type DeleteUserValues = z.infer<typeof deleteUserSchema>;

export const BULK_ROLES = ROLES.filter((role) => role !== "ADMIN");

export const bulkRoleSchema = z.object({
    role: z.enum(BULK_ROLES as [Role, ...Role[]]),
    reason,
});

export type BulkRoleValues = z.infer<typeof bulkRoleSchema>;

export const MAX_DISPLAY_NAME_LENGTH = 60;

export const userFormSchema = z.object({
    username: z
        .string()
        .trim()
        .regex(
            USERNAME_PATTERN,
            "Tên người dùng gồm 3-30 ký tự: chữ thường, số, dấu chấm hoặc gạch dưới",
        )
        .refine((username) => !isReservedUsername(username), RESERVED_USERNAME_MESSAGE),
    email: z.string().trim().pipe(z.email("Email không hợp lệ")),
    displayName: z
        .string()
        .trim()
        .max(MAX_DISPLAY_NAME_LENGTH, `Tên hiển thị tối đa ${MAX_DISPLAY_NAME_LENGTH} ký tự`),
    role: z.enum(ROLES as [Role, ...Role[]]),
    reason: z.string().trim(),
});

export type UserFormValues = z.infer<typeof userFormSchema>;
