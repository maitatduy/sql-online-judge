import type { AdminNavigationCounts } from "@/lib/admin-navigation";
import { apiClient } from "@/lib/api-client";

import type {
    AdminOverviewResponse,
    JudgeWorkersResponse,
    RestartWorkerRequest,
    RestartWorkerResponse,
} from "./types";

export const adminApi = {
    getNavigationCounts: (signal?: AbortSignal) =>
        apiClient.get<AdminNavigationCounts>("/api/admin/navigation-counts", { signal }),
    getOverview: (signal?: AbortSignal) =>
        apiClient.get<AdminOverviewResponse>("/api/admin/overview", { signal }),
    getJudgeWorkers: (signal?: AbortSignal) =>
        apiClient.get<JudgeWorkersResponse>("/api/admin/judge/workers", { signal }),
    restartWorker: ({ name, reason }: RestartWorkerRequest) =>
        apiClient.post<RestartWorkerResponse>(
            `/api/admin/judge/workers/${encodeURIComponent(name)}/restart`,
            { reason },
        ),
};
