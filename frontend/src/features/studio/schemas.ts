import { z } from "zod";

const MAX_TITLE_LENGTH = 200;

export const createProblemSchema = z.object({
    title: z
        .string()
        .trim()
        .min(1, "Không được để trống")
        .max(MAX_TITLE_LENGTH, "Tối đa 200 ký tự"),
});

export type CreateProblemValues = z.infer<typeof createProblemSchema>;

const MAX_SLUG_LENGTH = 160;
export const MAX_TAGS = 5;

export const problemInfoSchema = z.object({
    title: z
        .string()
        .trim()
        .min(1, "Không được để trống")
        .max(MAX_TITLE_LENGTH, "Tối đa 200 ký tự"),
    slug: z
        .string()
        .max(MAX_SLUG_LENGTH, "Tối đa 160 ký tự")
        .regex(/^[a-z0-9]+(-[a-z0-9]+)*$/, "Chỉ gồm chữ thường không dấu, số và dấu gạch nối"),
    difficulty: z.enum(["EASY", "MEDIUM", "HARD"]),
    tagSlugs: z.array(z.string()).max(MAX_TAGS, "Tối đa 5 chủ đề"),
});

export type ProblemInfoValues = z.infer<typeof problemInfoSchema>;
