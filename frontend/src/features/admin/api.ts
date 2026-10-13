import type { AdminNavigationCounts } from "@/lib/admin-navigation";
import { apiClient } from "@/lib/api-client";

export const adminApi = {
    getNavigationCounts: (signal?: AbortSignal) =>
        apiClient.get<AdminNavigationCounts>("/api/admin/navigation-counts", { signal }),
};
