import { z } from "zod";

const WEBSITE_PATTERN = /^https?:\/\/\S+$/;
const MIN_PASSWORD_LENGTH = 8;

export const accountProfileSchema = z.object({
    displayName: z
        .string()
        .trim()
        .min(1, { message: "Vui lòng nhập tên hiển thị" })
        .max(100, { message: "Tối đa 100 ký tự" }),
    bio: z.string().max(300, { message: "Tối đa 300 ký tự" }),
    school: z.string().max(120, { message: "Tối đa 120 ký tự" }),
    location: z.string().max(120, { message: "Tối đa 120 ký tự" }),
    websiteUrl: z
        .string()
        .trim()
        .max(200, { message: "Tối đa 200 ký tự" })
        .refine((url) => url === "" || WEBSITE_PATTERN.test(url), {
            message: "Đường dẫn phải bắt đầu bằng http:// hoặc https://",
        }),
});

export type AccountProfileValues = z.infer<typeof accountProfileSchema>;

export const ACCOUNT_PROFILE_FIELDS = [
    "displayName",
    "bio",
    "school",
    "location",
    "websiteUrl",
] as const satisfies readonly (keyof AccountProfileValues)[];

export const changePasswordSchema = z
    .object({
        currentPassword: z.string(),
        newPassword: z
            .string()
            .min(MIN_PASSWORD_LENGTH, { message: "Mật khẩu cần ít nhất 8 ký tự" }),
        confirmPassword: z.string(),
    })
    .refine((values) => values.newPassword === values.confirmPassword, {
        message: "Mật khẩu nhập lại không khớp",
        path: ["confirmPassword"],
    });

export type ChangePasswordValues = z.infer<typeof changePasswordSchema>;

export const PASSWORD_FIELDS = [
    "currentPassword",
    "newPassword",
] as const satisfies readonly (keyof ChangePasswordValues)[];
