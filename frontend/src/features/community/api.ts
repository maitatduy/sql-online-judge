import { apiClient } from "@/lib/api-client";
import type { Dialect } from "@/lib/dialect";
import type { Page } from "@/lib/pagination";

import type { PostFilters } from "./search-params";
import type {
    CommentPage,
    CommentSort,
    CommentThread,
    CodeRun,
    CommunityOverview,
    Contributor,
    LinkedProblem,
    PostDetail,
    PostPage,
    ProblemOption,
    ReportReason,
    ReportTarget,
    SavePostInput,
    VoteResult,
    VoteTarget,
} from "./types";

export const POST_PAGE_SIZE = 20;
const COMMENT_PAGE_SIZE = 50;
const PROBLEM_OPTION_COUNT = 8;

function postPath(postId: string): string {
    return `/api/community/posts/${encodeURIComponent(postId)}`;
}

export interface NewComment extends CommentThread {
    parentId?: string;
    body: string;
}

export interface NewReport {
    targetType: ReportTarget;
    targetId: string;
    reason: ReportReason;
    detail?: string;
}

export const communityApi = {
    posts: (filters: PostFilters, signal?: AbortSignal) =>
        apiClient.get<PostPage>("/api/community/posts", {
            params: {
                category: filters.category ?? undefined,
                tab: filters.tab,
                q: filters.q || undefined,
                page: filters.page,
                size: POST_PAGE_SIZE,
            },
            signal,
        }),
    post: (postId: string, signal?: AbortSignal) =>
        apiClient.get<PostDetail>(postPath(postId), { signal }),
    createPost: (post: SavePostInput) => apiClient.post<PostDetail>("/api/community/posts", post),
    updatePost: ({ postId, post }: { postId: string; post: SavePostInput }) =>
        apiClient.put<PostDetail>(postPath(postId), post),
    deletePost: (postId: string) => apiClient.delete<void>(postPath(postId)),
    searchProblems: (q: string, signal?: AbortSignal) =>
        apiClient.get<Page<ProblemOption>>("/api/problems", {
            params: { q: q || undefined, size: PROBLEM_OPTION_COUNT },
            signal,
        }),
    problem: (slug: string, signal?: AbortSignal) =>
        apiClient.get<LinkedProblem>(`/api/problems/${encodeURIComponent(slug)}`, { signal }),
    startRun: ({
        problemId,
        dialect,
        code,
    }: {
        problemId: string;
        dialect: Dialect;
        code: string;
    }) =>
        apiClient.post<{ runId: string }>(`/api/problems/${encodeURIComponent(problemId)}/runs`, {
            dialect,
            code,
        }),
    run: (runId: string, signal?: AbortSignal) =>
        apiClient.get<CodeRun>(`/api/runs/${encodeURIComponent(runId)}`, { signal }),
    overview: (signal?: AbortSignal) =>
        apiClient.get<CommunityOverview>("/api/community/overview", { signal }),
    contributors: (signal?: AbortSignal) =>
        apiClient.get<Contributor[]>("/api/community/contributors", { signal }),
    comments: (thread: CommentThread, sort: CommentSort, signal?: AbortSignal) =>
        apiClient.get<CommentPage>("/api/community/comments", {
            params: { ...thread, sort, page: 1, size: COMMENT_PAGE_SIZE },
            signal,
        }),
    writeComment: (comment: NewComment) => apiClient.post<void>("/api/community/comments", comment),
    deleteComment: (commentId: string) =>
        apiClient.delete<void>(`/api/community/comments/${encodeURIComponent(commentId)}`),
    vote: (vote: { targetType: VoteTarget; targetId: string; value: number }) =>
        apiClient.put<VoteResult>("/api/community/votes", vote),
    bookmark: ({ postId, isSaved }: { postId: string; isSaved: boolean }) =>
        isSaved
            ? apiClient.put<void>(`${postPath(postId)}/bookmark`, undefined)
            : apiClient.delete<void>(`${postPath(postId)}/bookmark`),
    report: (report: NewReport) => apiClient.post<void>("/api/community/reports", report),
};
