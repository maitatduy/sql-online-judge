import type { Difficulty } from "@/lib/difficulty";

export type CourseStatus = "DRAFT" | "PUBLISHED" | "ARCHIVED";

export type CourseRevisionStatus = "DRAFT" | "IN_REVIEW" | "PUBLISHED" | "SUPERSEDED";

export interface StudioCourse {
    id: string;
    slug: string;
    status: CourseStatus;
    revisionId: string;
    revisionStatus: CourseRevisionStatus;
    title: string;
    summary: string;
    difficulty: Difficulty;
    estimatedMinutes: number;
    lessonCount: number;
    updatedAt: string;
}

export interface CourseInfo {
    title: string;
    summary: string;
    difficulty: Difficulty;
}

export interface CreateCourseRequest extends CourseInfo {
    slug: string;
}

export interface StudioLessonOutline {
    id: string;
    slug: string;
    title: string;
    estimatedMinutes: number;
}

export interface StudioChapter {
    id: string;
    title: string;
    position: number;
    lessons: StudioLessonOutline[];
}

export interface LessonRequest {
    slug: string;
    title: string;
    body: string;
    estimatedMinutes: number;
}

export interface StudioLesson extends LessonRequest {
    id: string;
    chapterId: string;
    position: number;
}
