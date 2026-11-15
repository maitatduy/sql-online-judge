import { useQuery } from "@tanstack/react-query";

import { learningApi } from "../api";

export function courseProgressKey(courseSlug: string) {
    return ["course-progress", courseSlug] as const;
}

export function useCourseProgress(courseSlug: string, isSignedIn: boolean) {
    return useQuery({
        queryKey: courseProgressKey(courseSlug),
        queryFn: ({ signal }) => learningApi.getProgress(courseSlug, signal),
        enabled: isSignedIn,
    });
}
