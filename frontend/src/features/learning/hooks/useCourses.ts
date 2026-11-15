import { keepPreviousData, useQuery } from "@tanstack/react-query";

import type { Difficulty } from "@/lib/difficulty";

import { learningApi } from "../api";

export function useCourses(difficulty: Difficulty | undefined) {
    return useQuery({
        queryKey: ["courses", { difficulty }],
        queryFn: ({ signal }) => learningApi.listCourses(difficulty, signal),
        placeholderData: keepPreviousData,
    });
}
