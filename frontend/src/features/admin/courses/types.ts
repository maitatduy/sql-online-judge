import type { Difficulty } from "@/lib/difficulty";
import type { Page } from "@/lib/pagination";

export type AdminCourseStatus = "DRAFT" | "PUBLISHED" | "ARCHIVED";

export type AdminRevisionStatus = "DRAFT" | "IN_REVIEW" | "PUBLISHED" | "SUPERSEDED";

export interface AdminCourseItem {
    courseId: string;
    slug: string;
    title: string;
    difficulty: Difficulty;
    status: AdminCourseStatus;
    revisionStatus: AdminRevisionStatus;
    lessonCount: number;
    authorId: string;
    updatedAt: string;
    publishedAt: string | null;
}

export type AdminCoursePage = Page<AdminCourseItem>;

export type AdminCourseAction = "archive" | "restore";
