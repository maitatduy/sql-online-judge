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
    updatedAt: string;
}

export interface CourseInfo {
    title: string;
    summary: string;
    difficulty: Difficulty;
    estimatedMinutes: number;
}

export interface CreateCourseRequest extends CourseInfo {
    slug: string;
}
