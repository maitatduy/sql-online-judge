import type { Page } from "@/lib/pagination";

export type ReportStatus = "OPEN" | "RESOLVED" | "DISMISSED";
export type ReportTarget = "POST" | "COMMENT" | "USER";
export type ReportReason = "SPAM" | "OFFENSIVE" | "CONTEST_SPOILER" | "PLAGIARISM" | "OTHER";
export type ReportAction = "DISMISS" | "REMOVE";

export interface ReportCase {
    targetType: ReportTarget;
    targetId: string;
    authorId: string | null;
    authorUsername: string | null;
    preview: string;
    reportCount: number;
    reasons: Partial<Record<ReportReason, number>>;
    latestReportAt: string;
}

export type ReportCasePage = Page<ReportCase>;

export interface ReportItem {
    id: string;
    reporterId: string;
    reporterUsername: string | null;
    reason: ReportReason;
    detail: string | null;
    status: ReportStatus;
    createdAt: string;
}

export interface ReportCaseDetail {
    targetType: ReportTarget;
    targetId: string;
    authorId: string | null;
    authorUsername: string | null;
    priorViolationCount: number;
    content: {
        title: string | null;
        body: string | null;
        postId: string | null;
        problemId: string | null;
        isHidden: boolean;
        isDeleted: boolean;
    };
    reports: ReportItem[];
}

export interface ReportCaseKey {
    targetType: ReportTarget;
    targetId: string;
}

export interface ResolveReport extends ReportCaseKey {
    action: ReportAction;
    note?: string;
}
