import { useQuery } from "@tanstack/react-query";

import { learningApi } from "../api";

export function useLesson(courseSlug: string, lessonSlug: string) {
    return useQuery({
        queryKey: ["lesson", courseSlug, lessonSlug],
        queryFn: ({ signal }) => learningApi.getLesson(courseSlug, lessonSlug, signal),
    });
}
