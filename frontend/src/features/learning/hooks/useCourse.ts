import { useQuery } from "@tanstack/react-query";

import { learningApi } from "../api";

export function useCourse(slug: string) {
    return useQuery({
        queryKey: ["course", slug],
        queryFn: ({ signal }) => learningApi.getCourse(slug, signal),
    });
}
