import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { communityApi } from "../api";
import type { PostFilters } from "../search-params";
import type { CommentSort, CommentThread } from "../types";

export function useCommunityPosts(filters: PostFilters) {
    return useQuery({
        queryKey: ["community", "posts", filters],
        queryFn: ({ signal }) => communityApi.posts(filters, signal),
        placeholderData: keepPreviousData,
        retry: false,
    });
}

export function useCommunityOverview() {
    return useQuery({
        queryKey: ["community", "overview"],
        queryFn: ({ signal }) => communityApi.overview(signal),
    });
}

export function useContributors() {
    return useQuery({
        queryKey: ["community", "contributors"],
        queryFn: ({ signal }) => communityApi.contributors(signal),
    });
}

export function usePost(postId: string) {
    return useQuery({
        queryKey: ["community", "post", postId],
        queryFn: ({ signal }) => communityApi.post(postId, signal),
        retry: false,
    });
}

export function useComments(thread: CommentThread, sort: CommentSort) {
    return useQuery({
        queryKey: ["community", "comments", thread, sort],
        queryFn: ({ signal }) => communityApi.comments(thread, sort, signal),
        retry: false,
    });
}

export function useCommunityMutation<TVariables, TResult = unknown>(
    mutationFn: (variables: TVariables) => Promise<TResult>,
) {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn,
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ["community"] }),
    });
}

export function useLinkedProblem(slug: string | null) {
    return useQuery({
        queryKey: ["community", "problem", slug],
        queryFn: ({ signal }) => communityApi.problem(slug ?? "", signal),
        enabled: slug !== null,
        staleTime: 5 * 60_000,
        retry: false,
    });
}

export function useProblemOptions(query: string) {
    return useQuery({
        queryKey: ["community", "problem-options", query],
        queryFn: ({ signal }) => communityApi.searchProblems(query, signal),
        placeholderData: keepPreviousData,
    });
}

export function useProblemSolutions(problemId: string | null) {
    return useQuery({
        queryKey: ["community", "problem-solutions", problemId],
        queryFn: ({ signal }) => communityApi.problemSolutions(problemId ?? "", signal),
        enabled: problemId !== null,
    });
}

export function useEditorial(slug: string) {
    return useQuery({
        queryKey: ["community", "editorial", slug],
        queryFn: ({ signal }) => communityApi.editorial(slug, signal),
        retry: false,
    });
}
