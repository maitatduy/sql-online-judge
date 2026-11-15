import { useQuery } from "@tanstack/react-query";

import { learningApi } from "../api";

export function useLearningActivity(from: string) {
    return useQuery({
        queryKey: ["learning-activity", from],
        queryFn: ({ signal }) => learningApi.getLearningActivity(from, signal),
    });
}
