import type { AdminSubmission } from "../submissions/types";

export interface JudgeQueue {
    waiting: number;
    judging: number;
    stuck: number;
    systemErrorsLastDay: number;
    judgedLastHour: number;
    stuckAfterMinutes: number;
}

export interface JudgeAttention {
    items: AdminSubmission[];
}
