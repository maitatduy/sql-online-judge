import { apiClient } from "@/lib/api-client";

import type { PostFilters } from "./search-params";
import type {
    CommentPage,
    CommentSort,
    CommentThread,
    CommunityOverview,
    Contributor,
    PostDetail,
    PostPage,
    ReportReason,
    ReportTarget,
    VoteResult,
    VoteTarget,
} from "./types";

export const POST_PAGE_SIZE = 20;
const COMMENT_PAGE_SIZE = 50;

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
