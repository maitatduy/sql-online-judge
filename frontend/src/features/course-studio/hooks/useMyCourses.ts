import { useQuery } from "@tanstack/react-query";

import { courseStudioApi } from "../api";

export const STUDIO_COURSES_KEY = ["studio-courses"] as const;

export function useMyCourses() {
    return useQuery({
        queryKey: [...STUDIO_COURSES_KEY, "list"],
        queryFn: ({ signal }) => courseStudioApi.listMyCourses(signal),
    });
}
