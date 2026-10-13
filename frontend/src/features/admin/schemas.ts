import { z } from "zod";

export const MAX_RESTART_REASON_LENGTH = 500;

export const restartWorkerSchema = z.object({
    reason: z
        .string()
        .trim()
        .min(1, "Vui lòng nhập lý do")
        .max(MAX_RESTART_REASON_LENGTH, `Lý do tối đa ${MAX_RESTART_REASON_LENGTH} ký tự`),
});

export type RestartWorkerValues = z.infer<typeof restartWorkerSchema>;
