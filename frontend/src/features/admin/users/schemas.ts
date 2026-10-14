import { z } from "zod";

import type { Role } from "@/lib/current-user";
import { USERNAME_PATTERN } from "@/lib/username";

import { invalidEmailsOf, parseEmailList } from "./email-list";
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

export const bulkRoleSchema = z.object({
    role: z.enum(ROLES as [Role, ...Role[]]),
    reason,
});

export type BulkRoleValues = z.infer<typeof bulkRoleSchema>;

export const INVITE_ROLES = ["SETTER", "MODERATOR"] as const;
export const MAX_INVITE_EMAILS = 50;
export const MAX_INVITE_MESSAGE_LENGTH = 300;

export const inviteUsersSchema = z.object({
    emails: z.string().superRefine((text, context) => {
        const emails = parseEmailList(text);
        const invalidEmails = invalidEmailsOf(emails);
        if (emails.length === 0) {
            context.addIssue({ code: "custom", message: "Vui lòng nhập ít nhất một email" });
        } else if (invalidEmails.length > 0) {
            context.addIssue({
                code: "custom",
                message: `Email không hợp lệ: ${invalidEmails.join(", ")}`,
            });
        } else if (emails.length > MAX_INVITE_EMAILS) {
            context.addIssue({
                code: "custom",
                message: `Tối đa ${MAX_INVITE_EMAILS} email mỗi lần`,
            });
        }
    }),
    role: z.enum(INVITE_ROLES),
    message: z
        .string()
        .trim()
        .max(MAX_INVITE_MESSAGE_LENGTH, `Lời nhắn tối đa ${MAX_INVITE_MESSAGE_LENGTH} ký tự`),
});

export type InviteUsersValues = z.infer<typeof inviteUsersSchema>;

export const MAX_DISPLAY_NAME_LENGTH = 60;

export const userFormSchema = z.object({
    username: z
        .string()
        .trim()
        .regex(
            USERNAME_PATTERN,
            "Tên người dùng gồm 3-30 ký tự: chữ thường, số, dấu chấm hoặc gạch dưới",
        ),
    email: z.string().trim().pipe(z.email("Email không hợp lệ")),
    displayName: z
        .string()
        .trim()
        .max(MAX_DISPLAY_NAME_LENGTH, `Tên hiển thị tối đa ${MAX_DISPLAY_NAME_LENGTH} ký tự`),
    role: z.enum(ROLES as [Role, ...Role[]]),
});

export type UserFormValues = z.infer<typeof userFormSchema>;
