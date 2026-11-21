import type { Dialect } from "@/lib/dialect";
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
    submittedAt: string | null;
    updatedAt: string;
    latestReview: CourseReview | null;
}

export type CourseReviewDecision = "APPROVED" | "CHANGES_REQUESTED";

export interface CourseReview {
    decision: CourseReviewDecision;
    reason: string | null;
    reviewedAt: string;
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

export interface StudioCell {
    id: string;
    position: number;
    dialect: Dialect;
    starterSql: string;
    datasetVariantId: string;
}

export interface CellRequest {
    datasetVariantId: string;
    dialect: Dialect;
    starterSql: string;
}

export interface DatasetOption {
    id: string;
    name: string;
}

export interface DatasetChoice {
    id: string;
    name: string;
    schema: { isDraft: boolean };
    variants: { id: string; name: string; isPublic: boolean }[];
}

export interface QuizOptionDraft {
    label: string;
    correct: boolean;
}

export interface QuizQuestionRequest {
    prompt: string;
    multiple: boolean;
    options: QuizOptionDraft[];
}

export interface StudioQuizQuestion extends QuizQuestionRequest {
    id: string;
    position: number;
}
