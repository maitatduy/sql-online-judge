import { z } from "zod";

import { POST_CATEGORIES } from "./labels";

const TAG_PATTERN = /^[a-z0-9]+(-[a-z0-9]+)*$/;
const MAX_TAGS = 5;

export function parseTags(text: string): string[] {
    return [
        ...new Set(
            text
                .split(",")
                .map((tag) => tag.trim().toLowerCase())
                .filter(Boolean),
        ),
    ];
}

export const postSchema = z.object({
    category: z.enum(POST_CATEGORIES),
    title: z.string().trim().min(5, "Từ 5 đến 200 ký tự").max(200, "Từ 5 đến 200 ký tự"),
    bodyMarkdown: z.string().trim().min(1, "Không được để trống").max(51_200, "Tối đa 50 KB"),
    tags: z
        .string()
        .refine((text) => parseTags(text).length <= MAX_TAGS, "Tối đa 5 tag")
        .refine(
            (text) => parseTags(text).every((tag) => TAG_PATTERN.test(tag) && tag.length <= 40),
            "Tag chỉ gồm chữ thường, số và dấu gạch nối",
        ),
});

export type PostValues = z.infer<typeof postSchema>;
