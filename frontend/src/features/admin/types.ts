import type { Dialect } from "@/lib/dialect";
import type { Verdict } from "@/lib/verdict";

export interface MetricSummary {
    value: number;
    changePercent: number;
    changeNote: string;
}

export interface JudgeQueueSummary {
    size: number;
    averageWaitMs: number;
    isDegraded: boolean;
}

export interface VerdictShare {
    verdict: Verdict;
    count: number;
    percent: number;
}

export interface SubmissionTotals {
    allTime: number;
    today: number;
    last7Days: number;
}

export interface MonthlyCount {
    month: number;
    count: number;
}

export interface MonthlySubmissions {
    year: number;
    months: MonthlyCount[];
}

export interface AdminOverviewResponse {
    users: MetricSummary;
    submissions: MetricSummary;
    aiMessages: MetricSummary;
    queue: JudgeQueueSummary;
    submissionTotals: SubmissionTotals;
    monthlySubmissions: MonthlySubmissions;
    verdictDistribution: VerdictShare[];
}

export type WorkerStatus = "ONLINE" | "BUSY" | "DISCONNECTED" | "RESTARTING";

export interface JudgeWorker {
    name: string;
    dialect: Dialect;
    status: WorkerStatus;
    judgedPerHour: number;
    cpuPercent: number | null;
    disconnectedAt: string | null;
}

export interface JudgeWorkersResponse {
    items: JudgeWorker[];
}

export interface RestartWorkerRequest {
    name: string;
    reason: string;
}

export interface RestartWorkerResponse {
    name: string;
    status: WorkerStatus;
}
