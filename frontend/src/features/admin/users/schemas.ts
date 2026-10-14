import { z } from "zod";

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

export type BanUserValues = z.infer<typeof banUserSchema>;
export type UnbanUserValues = z.infer<typeof unbanUserSchema>;
