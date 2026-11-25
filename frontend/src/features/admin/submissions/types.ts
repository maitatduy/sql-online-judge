import type { Dialect } from "@/lib/dialect";
import type { Page } from "@/lib/pagination";
import type { Verdict } from "@/lib/verdict";

export type SubmissionStatus = "PENDING" | "JUDGING" | "COMPLETED";
export type SubmissionSource = "PRACTICE" | "CONTEST";

export interface AdminSubmission {
    id: string;
    userId: string;
    username: string | null;
    displayName: string | null;
    problemId: string;
    problemNumber: number | null;
    problemSlug: string | null;
    problemTitle: string | null;
    contestId: string | null;
    dialect: Dialect;
    status: SubmissionStatus;
    verdict: Verdict | null;
    runtimeMs: number | null;
    createdAt: string;
}

export type AdminSubmissionPage = Page<AdminSubmission>;

export interface AdminSubmissionDetail extends AdminSubmission {
    code: string;
    errorMessage: string | null;
    firstFailedIndex: number | null;
    judgedTestcases: number;
    totalTestcases: number;
    attempts: number;
    judgedAt: string | null;
}
