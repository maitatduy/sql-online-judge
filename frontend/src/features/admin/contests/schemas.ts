import { z } from "zod";

import type { CreateContestRequest } from "./types";

const REQUIRED_MESSAGE = "Không được để trống";
const SLUG_PATTERN = /^[a-z0-9]+(-[a-z0-9]+)*$/;
const VIETNAM_OFFSET = "+07:00";

const minutes = (min: number, max: number, message: string) =>
    z.number({ message: REQUIRED_MESSAGE }).int(message).min(min, message).max(max, message);

export const createContestSchema = z
    .object({
        slug: z
            .string()
            .trim()
            .min(1, REQUIRED_MESSAGE)
            .max(160, "Tối đa 160 ký tự")
            .regex(SLUG_PATTERN, "Chỉ gồm chữ thường không dấu, số và dấu gạch nối"),
        title: z.string().trim().min(1, REQUIRED_MESSAGE).max(200, "Tối đa 200 ký tự"),
        description: z.string().max(20000, "Tối đa 20.000 ký tự"),
        startAt: z.string().min(1, REQUIRED_MESSAGE),
        durationMinutes: minutes(30, 600, "Từ 30 đến 600 phút"),
        freezeMinutes: minutes(0, 600, "Không được âm"),
        penaltyMinutes: minutes(0, 60, "Từ 0 đến 60 phút"),
        isRated: z.boolean(),
    })
    .refine((values) => values.freezeMinutes < values.durationMinutes, {
        path: ["freezeMinutes"],
        message: "Thời gian đóng băng phải ngắn hơn thời lượng contest",
    });

export type CreateContestValues = z.infer<typeof createContestSchema>;

export function toCreateContestRequest(values: CreateContestValues): CreateContestRequest {
    return {
        ...values,
        startAt: new Date(`${values.startAt}:00${VIETNAM_OFFSET}`).toISOString(),
    };
}
