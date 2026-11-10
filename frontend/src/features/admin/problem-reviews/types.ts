import type { Difficulty } from "@/lib/difficulty";
import type { Page } from "@/lib/pagination";

export type ReviewProblemStatus =
    "DRAFT" | "PENDING_REVIEW" | "CHANGES_REQUESTED" | "REJECTED" | "PUBLISHED";

export type ReviewDecision = "APPROVED" | "CHANGES_REQUESTED" | "REJECTED";

export interface ReviewProblemSummary {
    id: string;
    number: number | null;
    slug: string;
    title: string;
    status: ReviewProblemStatus;
    difficulty: Difficulty;
    authorId: string;
    updatedAt: string;
}

export type ReviewProblemPage = Page<ReviewProblemSummary>;

export interface ReviewProblemDetail {
    id: string;
    number: number | null;
    slug: string;
    title: string;
    status: ReviewProblemStatus;
    difficulty: Difficulty;
    tags: { slug: string; name: string }[];
    statementMarkdown: string;
    dataset: { id: string; name: string } | null;
    latestReview: { decision: ReviewDecision; reason: string | null; reviewedAt: string } | null;
    updatedAt: string;
}

export interface ReviewAnswerCheck {
    status: "PENDING" | "PASSED" | "FAILED";
    testcaseCount: number;
    isCurrent: boolean;
    completedAt: string | null;
    testcases: { isPassed: boolean }[];
}

export type ReviewAction = "approve" | "request-changes" | "reject";
