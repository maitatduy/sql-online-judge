import { z } from "zod";

import { DIFFICULTIES } from "@/lib/difficulty";

export const MAX_TITLE_LENGTH = 200;
export const MAX_TAGS = 10;

export const quickEditSchema = z.object({
    title: z
        .string()
        .trim()
        .min(1, "Không được để trống")
        .max(MAX_TITLE_LENGTH, "Tối đa 200 ký tự"),
    difficulty: z.enum(DIFFICULTIES as [string, ...string[]]),
    tagSlugs: z.array(z.string()).max(MAX_TAGS, "Tối đa 10 chủ đề"),
});

export type QuickEditValues = z.infer<typeof quickEditSchema>;

export const transferOwnershipSchema = z.object({
    username: z.string().trim().min(1, "Nhập username người nhận").max(50, "Tối đa 50 ký tự"),
});

export type TransferOwnershipValues = z.infer<typeof transferOwnershipSchema>;
