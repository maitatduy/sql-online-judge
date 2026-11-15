import type { Difficulty } from "@/lib/difficulty";
import type { ResultSet } from "@/lib/result-set";

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

export interface LessonCell {
    cellKey: string;
    starterSql: string;
    dialect: "POSTGRESQL" | "MYSQL" | "SQLITE";
}

export interface Lesson extends LessonOutline {
    body: string;
    chapterTitle: string;
    courseSlug: string;
    courseTitle: string;
    cells: LessonCell[];
    questions: QuizQuestion[];
}

export interface CourseProgress {
    enrolled: boolean;
    completedLessonKeys: string[];
    lessonCount: number;
    nextLessonSlug: string | null;
}

export type LessonRunOutcome = "OK" | "RE" | "TLE" | "FS" | "SE";

export interface LessonRun {
    id: string;
    status: "PENDING" | "COMPLETED";
    outcome: LessonRunOutcome | null;
    result: ResultSet | null;
    errorMessage: string | null;
    runtimeMs: number | null;
}

export interface QuizOption {
    optionKey: string;
    label: string;
}

export interface QuizQuestion {
    questionKey: string;
    prompt: string;
    multiple: boolean;
    options: QuizOption[];
}

export interface QuizAnswer {
    questionKey: string;
    optionKeys: string[];
}

export interface QuizResult {
    allCorrect: boolean;
    results: { questionKey: string; correct: boolean }[];
    progress: CourseProgress;
}

export interface EnrolledCourse {
    slug: string;
    title: string;
    difficulty: Difficulty;
    completedCount: number;
    lessonCount: number;
    nextLessonSlug: string | null;
}
