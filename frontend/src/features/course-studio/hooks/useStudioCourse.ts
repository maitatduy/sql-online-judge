import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { courseStudioApi } from "../api";
import type { CourseInfo, CreateCourseRequest } from "../types";
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
            queryClient.setQueryData(courseKey(courseId), course);
            return queryClient.invalidateQueries({ queryKey: [...STUDIO_COURSES_KEY, "list"] });
        },
    });
}
