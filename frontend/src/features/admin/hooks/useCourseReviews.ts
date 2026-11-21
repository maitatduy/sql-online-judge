import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { courseReviewApi } from "../course-reviews/api";
import type { CourseReviewAction, ReviewCourse } from "../course-reviews/types";

const COURSE_REVIEWS_KEY = "admin-course-reviews";

export function useCourseReviewQueue() {
    return useQuery({
        queryKey: [COURSE_REVIEWS_KEY, "queue"],
        queryFn: ({ signal }) => courseReviewApi.getQueue(signal),
    });
}

export function useReviewCourse(courseId: string) {
    return useQuery({
        queryKey: [COURSE_REVIEWS_KEY, "course", courseId],
        queryFn: ({ signal }) => courseReviewApi.getCourse(courseId, signal),
        retry: false,
    });
}

export function useReviewOutline(courseId: string) {
    return useQuery({
        queryKey: [COURSE_REVIEWS_KEY, "outline", courseId],
        queryFn: ({ signal }) => courseReviewApi.getOutline(courseId, signal),
        retry: false,
    });
}

interface CourseReviewRequest {
    action: CourseReviewAction;
    reason: string | null;
}

export function useCourseReviewDecision(
    courseId: string,
    onReviewed: (course: ReviewCourse, action: CourseReviewAction) => void,
) {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ action, reason }: CourseReviewRequest) =>
            courseReviewApi.review(courseId, action, reason),
        onSuccess: (course, { action }) => {
            onReviewed(course, action);
            return queryClient.invalidateQueries({ queryKey: [COURSE_REVIEWS_KEY] });
        },
    });
}
