"use client";

import { Bookmark, Flag } from "lucide-react";
import Link from "next/link";
import { useState } from "react";

import { Avatar } from "@/components/shared/Avatar";
import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import { ApiRequestError } from "@/lib/api-client";
import { formatNumber, formatRelativeTime } from "@/lib/format";
import { ROUTES } from "@/lib/routes";
import { useCurrentUser } from "@/lib/useCurrentUser";
import { useNow } from "@/lib/useNow";
import { cn } from "@/lib/utils";

import { communityApi } from "../api";
import { useCommunityMutation, usePost } from "../hooks/useCommunity";
import { POST_CATEGORY_LABELS } from "../labels";
import type { PostDetail } from "../types";
import { CategoryPill } from "./CategoryPill";
import { CommentSection } from "./CommentSection";
import { DiscussionSummary } from "./DiscussionSummary";
import { PostAuthorActions } from "./PostAuthorActions";
import { PostBody } from "./PostBody";
import { ReportDialog } from "./ReportDialog";
import { VoteControls } from "./VoteControls";

const NOT_FOUND_STATUS = 404;

interface PostDetailViewProps {
    postId: string;
}

export function PostDetailView({ postId }: PostDetailViewProps) {
    const { data: post, error, isPending, refetch } = usePost(postId);

    if (isPending) {
        return (
            <div role="status" aria-label="Đang tải bài viết" className="flex flex-col gap-4">
                <Skeleton className="h-10 w-2/3" />
                <Skeleton className="h-64" />
            </div>
        );
    }
    if (error instanceof ApiRequestError && error.status === NOT_FOUND_STATUS) {
        return (
            <EmptyState
                title="Không tìm thấy bài viết"
                action={
                    <Button asChild variant="outline" size="sm">
                        <Link href={ROUTES.community}>Về cộng đồng</Link>
                    </Button>
                }
            />
        );
    }
    if (!post) {
        return (
            <ErrorState
                title="Chưa tải được bài viết"
                message={error?.message}
                traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                onRetry={() => void refetch()}
            />
        );
    }
    return <PostArticle post={post} />;
}

function PostArticle({ post }: { post: PostDetail }) {
    const showToast = useToast();
    const now = useNow();
    const { data: currentUser } = useCurrentUser();
    const bookmark = useCommunityMutation(communityApi.bookmark);
    const [isReporting, setIsReporting] = useState(false);
    const isSignedIn = Boolean(currentUser);
    const isAuthor = currentUser?.id === post.authorId;
    const author = post.authorUsername ?? "Người dùng";

    const toggleBookmark = () => {
        if (!isSignedIn) {
            showToast("Đăng nhập để lưu bài.", "error");
            return;
        }
        bookmark.mutate(
            { postId: post.id, isSaved: !post.isBookmarked },
            { onError: (error) => showToast(error.message, "error") },
        );
    };

    return (
        <div className="mx-auto flex w-full max-w-[880px] flex-col gap-6">
            <nav aria-label="Đường dẫn" className="text-[15px] text-muted-foreground">
                <Link href={ROUTES.community} className="hover:text-foreground">
                    Cộng đồng
                </Link>
                {" / "}
                <span>{POST_CATEGORY_LABELS[post.category]}</span>
            </nav>
            <article className="flex gap-5 rounded-lg border bg-card p-5 sm:p-6">
                <div className="flex flex-col items-center gap-3">
                    <VoteControls
                        targetType="POST"
                        targetId={post.id}
                        score={post.voteScore}
                        myVote={post.myVote}
                        isSignedIn={isSignedIn}
                    />
                    <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        aria-label={post.isBookmarked ? "Bỏ lưu bài" : "Lưu bài"}
                        aria-pressed={post.isBookmarked}
                        disabled={bookmark.isPending}
                        onClick={toggleBookmark}
                        className={cn(post.isBookmarked ? "text-brand" : null)}
                    >
                        <Bookmark
                            aria-hidden="true"
                            className={cn(post.isBookmarked ? "fill-current" : null)}
                        />
                    </Button>
                    {isSignedIn && !isAuthor ? (
                        <Button
                            type="button"
                            variant="ghost"
                            size="icon"
                            aria-label="Báo cáo bài viết"
                            onClick={() => setIsReporting(true)}
                        >
                            <Flag aria-hidden="true" />
                        </Button>
                    ) : null}
                </div>
                <div className="flex min-w-0 flex-1 flex-col gap-4">
                    <div className="flex flex-wrap items-center gap-2">
                        <CategoryPill category={post.category} />
                        {post.status !== "PUBLISHED" ? (
                            <span className="rounded-full bg-surface-2 px-2.5 py-0.5 text-sm font-semibold">
                                {post.status === "DRAFT" ? "Bản nháp" : "Đang ẩn"}
                            </span>
                        ) : null}
                        {post.problemSlug ? (
                            <Link
                                href={ROUTES.problem(post.problemSlug)}
                                className="rounded-full bg-brand-soft px-2.5 py-0.5 text-sm font-semibold text-brand hover:underline"
                            >
                                {post.problemTitle}
                            </Link>
                        ) : null}
                        {post.tags.map((tag) => (
                            <span
                                key={tag}
                                className="rounded-full bg-surface-2 px-2.5 py-0.5 text-sm text-muted-foreground"
                            >
                                {tag}
                            </span>
                        ))}
                    </div>
                    <h1 className="text-[28px] leading-tight font-extrabold">{post.title}</h1>
                    <div className="flex flex-wrap items-center gap-2.5 text-[15px] text-muted-foreground">
                        <Avatar name={author} />
                        <span className="flex-1">
                            <strong className="text-foreground">{author}</strong>
                            {post.publishedAt
                                ? ` - ${formatRelativeTime(post.publishedAt, now)}`
                                : null}
                            {` - ${formatNumber(post.viewCount)} lượt xem`}
                        </span>
                        {isAuthor ? <PostAuthorActions postId={post.id} /> : null}
                    </div>
                    <PostBody markdown={post.bodyMarkdown} problemSlug={post.problemSlug} />
                </div>
            </article>
            {isSignedIn && post.commentCount > 0 ? <DiscussionSummary postId={post.id} /> : null}
            <CommentSection thread={{ postId: post.id }} />
            {isReporting ? (
                <ReportDialog
                    targetType="POST"
                    targetId={post.id}
                    onClose={() => setIsReporting(false)}
                />
            ) : null}
        </div>
    );
}
