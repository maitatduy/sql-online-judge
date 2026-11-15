import type { Difficulty } from "@/lib/difficulty";

export interface CourseSummary {
    id: string;
    slug: string;
    title: string;
    summary: string;
    difficulty: Difficulty;
    lessonCount: number;
    estimatedMinutes: number;
}

export interface LessonOutline {
    lessonKey: string;
    slug: string;
    title: string;
}

export interface ChapterOutline {
    id: string;
    title: string;
    lessons: LessonOutline[];
}

export interface CourseDetail extends CourseSummary {
    publishedAt: string;
    chapters: ChapterOutline[];
}

export interface Lesson extends LessonOutline {
    body: string;
    chapterTitle: string;
    courseSlug: string;
    courseTitle: string;
}

export interface CourseProgress {
    enrolled: boolean;
    completedLessonKeys: string[];
    lessonCount: number;
    nextLessonSlug: string | null;
}
