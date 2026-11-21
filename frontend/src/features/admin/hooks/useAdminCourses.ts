import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { adminCourseApi } from "../courses/api";
import type { AdminCourseAction, AdminCourseStatus } from "../courses/types";

const ADMIN_COURSES_KEY = "admin-courses";

export function useAdminCourses(status: AdminCourseStatus | null) {
    return useQuery({
        queryKey: [ADMIN_COURSES_KEY, status],
        queryFn: ({ signal }) => adminCourseApi.getCatalog(status, signal),
        placeholderData: keepPreviousData,
    });
}

export function useCourseVisibility() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ courseId, action }: { courseId: string; action: AdminCourseAction }) =>
            adminCourseApi.changeVisibility(courseId, action),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: [ADMIN_COURSES_KEY] }),
    });
}
