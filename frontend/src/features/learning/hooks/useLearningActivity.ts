import { useQuery } from "@tanstack/react-query";

import { learningActivityQuery } from "@/lib/useLearningStreak";

export function useLearningActivity(from: string) {
    return useQuery(learningActivityQuery(from));
}
