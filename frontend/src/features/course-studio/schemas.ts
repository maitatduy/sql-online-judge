import { z } from "zod";

const MAX_SLUG_LENGTH = 160;
const MAX_TITLE_LENGTH = 200;
const MAX_SUMMARY_LENGTH = 500;
const MIN_MINUTES = 1;
const MAX_MINUTES = 6000;
const SLUG_PATTERN = /^[a-z0-9]+(-[a-z0-9]+)*$/;
const MINUTES_MESSAGE = "Từ 1 đến 6000 phút";
const REQUIRED_MESSAGE = "Không được để trống";

export const courseInfoSchema = z.object({
    title: z.string().trim().min(1, REQUIRED_MESSAGE).max(MAX_TITLE_LENGTH, "Tối đa 200 ký tự"),
    summary: z.string().trim().min(1, REQUIRED_MESSAGE).max(MAX_SUMMARY_LENGTH, "Tối đa 500 ký tự"),
    difficulty: z.enum(["EASY", "MEDIUM", "HARD"], { message: "Chưa chọn độ khó" }),
    estimatedMinutes: z
        .number({ message: MINUTES_MESSAGE })
        .int(MINUTES_MESSAGE)
        .min(MIN_MINUTES, MINUTES_MESSAGE)
        .max(MAX_MINUTES, MINUTES_MESSAGE),
});

export const createCourseSchema = courseInfoSchema.extend({
    slug: z
        .string()
        .trim()
        .min(1, REQUIRED_MESSAGE)
        .max(MAX_SLUG_LENGTH, "Tối đa 160 ký tự")
        .regex(SLUG_PATTERN, "Chỉ gồm chữ thường không dấu, số và dấu gạch nối"),
});

export type CourseInfoValues = z.infer<typeof courseInfoSchema>;
export type CreateCourseValues = z.infer<typeof createCourseSchema>;
