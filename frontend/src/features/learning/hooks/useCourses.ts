import { keepPreviousData, useQuery } from "@tanstack/react-query";

import { learningApi } from "../api";
import { COURSES_PAGE_SIZE, type CourseFilters } from "./useCourseFilters";

export function useCourses({ difficulty, page }: CourseFilters) {
    return useQuery({
        queryKey: ["courses", { difficulty, page }],
        queryFn: ({ signal }) =>
            learningApi.listCourses({ difficulty, page, size: COURSES_PAGE_SIZE }, signal),
        placeholderData: keepPreviousData,
    });
}
