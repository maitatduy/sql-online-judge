import { useQueries } from "@tanstack/react-query";

import type { AdminNavigationCounts, AdminServiceCounts } from "@/lib/admin-navigation";

import { adminApi } from "../api";

const NAVIGATION_COUNTS_KEY = ["admin", "navigation-counts"] as const;
const NAVIGATION_COUNTS_REFRESH_MS = 60_000;
const NO_SERVICE_COUNTS: AdminServiceCounts = {
    abuseReports: 0,
    supportMessages: 0,
    hasLiveContest: false,
    hasJudgeIncident: false,
};

export function useAdminNavigationCounts(): { data: AdminNavigationCounts | undefined } {
    const [services, problemReviews, courseReviews] = useQueries({
        queries: [
            {
                queryKey: [...NAVIGATION_COUNTS_KEY, "services"],
                queryFn: ({ signal }: { signal: AbortSignal }) => adminApi.getServiceCounts(signal),
                refetchInterval: NAVIGATION_COUNTS_REFRESH_MS,
                retry: false,
            },
            {
                queryKey: [...NAVIGATION_COUNTS_KEY, "problem-reviews"],
                queryFn: ({ signal }: { signal: AbortSignal }) =>
                    adminApi.getPendingProblemCount(signal),
                refetchInterval: NAVIGATION_COUNTS_REFRESH_MS,
                retry: false,
            },
            {
                queryKey: [...NAVIGATION_COUNTS_KEY, "course-reviews"],
                queryFn: ({ signal }: { signal: AbortSignal }) =>
                    adminApi.getPendingCourseCount(signal),
                refetchInterval: NAVIGATION_COUNTS_REFRESH_MS,
                retry: false,
            },
        ],
    });
    const isSettled = [services, problemReviews, courseReviews].every((query) => !query.isPending);
    if (!isSettled) {
        return { data: undefined };
    }
    return {
        data: {
            ...NO_SERVICE_COUNTS,
            ...services.data,
            problemReviews: problemReviews.data ?? 0,
            courseReviews: courseReviews.data ?? 0,
        },
    };
}
