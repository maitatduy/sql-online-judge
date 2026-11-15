import { useQuery } from "@tanstack/react-query";

import { learningApi } from "../api";

export function useEnrollments() {
    return useQuery({
        queryKey: ["enrollments"],
        queryFn: ({ signal }) => learningApi.listEnrollments(signal),
    });
}
