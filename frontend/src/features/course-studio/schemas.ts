import { z } from "zod";

const MAX_SLUG_LENGTH = 160;
const MAX_TITLE_LENGTH = 200;
const MAX_SUMMARY_LENGTH = 500;
const SLUG_PATTERN = /^[a-z0-9]+(-[a-z0-9]+)*$/;
const REQUIRED_MESSAGE = "Không được để trống";

export const courseInfoSchema = z.object({
    title: z.string().trim().min(1, REQUIRED_MESSAGE).max(MAX_TITLE_LENGTH, "Tối đa 200 ký tự"),
    summary: z.string().trim().min(1, REQUIRED_MESSAGE).max(MAX_SUMMARY_LENGTH, "Tối đa 500 ký tự"),
    difficulty: z.enum(["EASY", "MEDIUM", "HARD"], { message: "Chưa chọn độ khó" }),
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

const MAX_CHAPTER_TITLE_LENGTH = 200;
const MIN_LESSON_MINUTES = 1;
const MAX_LESSON_MINUTES = 600;
const LESSON_MINUTES_MESSAGE = "Từ 1 đến 600 phút";

export const chapterSchema = z.object({
    title: z
        .string()
        .trim()
        .min(1, REQUIRED_MESSAGE)
        .max(MAX_CHAPTER_TITLE_LENGTH, "Tối đa 200 ký tự"),
});

export const newLessonSchema = z.object({
    title: z.string().trim().min(1, REQUIRED_MESSAGE).max(MAX_TITLE_LENGTH, "Tối đa 200 ký tự"),
    slug: createCourseSchema.shape.slug,
    estimatedMinutes: z
        .number({ message: LESSON_MINUTES_MESSAGE })
        .int(LESSON_MINUTES_MESSAGE)
        .min(MIN_LESSON_MINUTES, LESSON_MINUTES_MESSAGE)
        .max(MAX_LESSON_MINUTES, LESSON_MINUTES_MESSAGE),
});

export type ChapterValues = z.infer<typeof chapterSchema>;
export type NewLessonValues = z.infer<typeof newLessonSchema>;

export const MAX_LESSON_BODY_LENGTH = 51_200;

export const lessonSchema = newLessonSchema.extend({
    body: z.string().max(MAX_LESSON_BODY_LENGTH, "Tối đa 50 KB"),
});

export type LessonValues = z.infer<typeof lessonSchema>;
