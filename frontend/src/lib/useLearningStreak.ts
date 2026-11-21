import { useState } from "react";

import { useQuery } from "@tanstack/react-query";

import {
    type ActivityDay,
    currentStreakDays,
    mergeActivity,
    shiftDateKey,
    vietnamDateKey,
} from "./activity-calendar";
import { apiClient } from "./api-client";

export const ACTIVITY_WINDOW_DAYS = 364;

export function activitySince(now: Date): string {
    return shiftDateKey(vietnamDateKey(now), -ACTIVITY_WINDOW_DAYS);
}

export function learningActivityQuery(from: string) {
    return {
        queryKey: ["learning-activity", from],
        queryFn: ({ signal }: { signal: AbortSignal }) =>
            apiClient.get<ActivityDay[]>("/api/learning/activity", { params: { from }, signal }),
    };
}

export function submissionActivityQuery(from: string) {
    return {
        queryKey: ["submission-activity", from],
        queryFn: ({ signal }: { signal: AbortSignal }) =>
            apiClient.get<ActivityDay[]>("/api/submissions/activity", { params: { from }, signal }),
    };
}

export function useLearningStreak(enabled: boolean): number | undefined {
    const [now] = useState(() => new Date());
    const from = activitySince(now);
    const lessons = useQuery({ ...learningActivityQuery(from), enabled });
    const submissions = useQuery({ ...submissionActivityQuery(from), enabled });
    if (!lessons.data || !submissions.data) {
        return undefined;
    }
    return currentStreakDays(mergeActivity(lessons.data, submissions.data), vietnamDateKey(now));
}
