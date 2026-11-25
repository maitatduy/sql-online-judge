import { apiClient } from "@/lib/api-client";

import type { JudgeAttention, JudgeQueue } from "./types";

export const adminJudgeApi = {
    queue: (signal?: AbortSignal) =>
        apiClient.get<JudgeQueue>("/api/admin/judge/queue", { signal }),
    attention: (signal?: AbortSignal) =>
        apiClient.get<JudgeAttention>("/api/admin/judge/attention", { signal }),
    rejudge: (submissionId: string) =>
        apiClient.post<void>(`/api/admin/submissions/${encodeURIComponent(submissionId)}/rejudge`),
};
