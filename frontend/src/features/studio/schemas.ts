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
