import type { Difficulty } from "@/lib/difficulty";
import type { Page } from "@/lib/pagination";

export interface CourseReviewQueueItem {
    courseId: string;
    slug: string;
    title: string;
    difficulty: Difficulty;
    lessonCount: number;
    estimatedMinutes: number;
    authorId: string;
    submittedAt: string;
}

export type CourseReviewQueuePage = Page<CourseReviewQueueItem>;

export interface ReviewCourse {
    id: string;
    title: string;
    summary: string;
    difficulty: Difficulty;
    lessonCount: number;
    estimatedMinutes: number;
    revisionStatus: string;
}

export interface ReviewChapter {
    id: string;
    title: string;
    position: number;
    lessons: { id: string; title: string; estimatedMinutes: number }[];
}

export type CourseReviewAction = "approve" | "request-changes";
