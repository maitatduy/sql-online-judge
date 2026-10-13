import type { AdminNavigationCounts } from "@/lib/admin-navigation";
import { apiClient } from "@/lib/api-client";

import type { AdminOverviewResponse } from "./types";

export const adminApi = {
    getNavigationCounts: (signal?: AbortSignal) =>
        apiClient.get<AdminNavigationCounts>("/api/admin/navigation-counts", { signal }),
    getOverview: (signal?: AbortSignal) =>
        apiClient.get<AdminOverviewResponse>("/api/admin/overview", { signal }),
};
