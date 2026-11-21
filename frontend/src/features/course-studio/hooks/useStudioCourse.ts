import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { courseStudioApi } from "../api";
import type { CourseInfo, CreateCourseRequest, StudioCourse } from "../types";
import { STUDIO_COURSES_KEY } from "./useMyCourses";

function courseKey(courseId: string) {
    return [...STUDIO_COURSES_KEY, "detail", courseId] as const;
}

export function useStudioCourse(courseId: string) {
    return useQuery({
        queryKey: courseKey(courseId),
        queryFn: ({ signal }) => courseStudioApi.getCourse(courseId, signal),
    });
}

export function useCreateCourse() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (request: CreateCourseRequest) => courseStudioApi.createCourse(request),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: STUDIO_COURSES_KEY }),
    });
}

export function useUpdateCourse(courseId: string) {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (info: CourseInfo) => courseStudioApi.updateCourse(courseId, info),
        onSuccess: (course) => {
            queryClient.setQueryData<StudioCourse>(courseKey(courseId), (previous) => ({
                ...course,
                latestReview: course.latestReview ?? previous?.latestReview ?? null,
            }));
            return queryClient.invalidateQueries({ queryKey: [...STUDIO_COURSES_KEY, "list"] });
        },
    });
}

export function useSubmitCourse(courseId: string) {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: () => courseStudioApi.submitCourse(courseId),
        onSuccess: (course) => {
            queryClient.setQueryData(courseKey(courseId), course);
            return queryClient.invalidateQueries({ queryKey: [...STUDIO_COURSES_KEY, "list"] });
        },
    });
}

export function useCreateCourseDraft(courseId: string) {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: () => courseStudioApi.createDraft(courseId),
        onSuccess: (course) => {
            queryClient.setQueryData(courseKey(courseId), course);
            return queryClient.invalidateQueries({
                queryKey: STUDIO_COURSES_KEY,
                predicate: (query) => query.queryKey[1] !== "detail",
            });
        },
    });
}
