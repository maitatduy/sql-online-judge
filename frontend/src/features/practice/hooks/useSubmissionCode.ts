import { useQuery } from "@tanstack/react-query";

import { practiceApi } from "../api";

export function useSubmissionCode(submissionId: string) {
    return useQuery({
        queryKey: ["submission-code", submissionId],
        queryFn: ({ signal }) => practiceApi.getSubmissionCode(submissionId, signal),
        staleTime: Infinity,
        retry: false,
    });
}
