import { z } from "zod";

import { MIN_PASSWORD_LENGTH } from "./password-strength";

const emailSchema = z
    .string()
    .trim()
    .min(1, "Vui lòng nhập email")
    .pipe(z.email("Email không hợp lệ"));

export const loginSchema = z.object({
    email: emailSchema,
    password: z.string().min(1, "Vui lòng nhập mật khẩu"),
    rememberMe: z.boolean(),
});

export type LoginFormValues = z.infer<typeof loginSchema>;

export const USERNAME_PATTERN = /^[a-z0-9._]{3,30}$/;

export const registerSchema = z.object({
    username: z
        .string()
        .trim()
        .min(1, "Vui lòng nhập tên người dùng")
        .regex(
            USERNAME_PATTERN,
            "Tên người dùng gồm 3-30 ký tự: chữ thường, số, dấu chấm hoặc gạch dưới",
        ),
    email: emailSchema,
    password: z
        .string()
        .min(1, "Vui lòng nhập mật khẩu")
        .min(MIN_PASSWORD_LENGTH, `Mật khẩu cần ít nhất ${MIN_PASSWORD_LENGTH} ký tự`),
    acceptedTerms: z.boolean().refine((isAccepted) => isAccepted, {
        message: "Bạn cần đồng ý với điều khoản để tiếp tục",
    }),
});

export type RegisterFormValues = z.infer<typeof registerSchema>;
