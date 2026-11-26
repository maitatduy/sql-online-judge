"use client";

import { Heart } from "lucide-react";
import { useState } from "react";

import { Avatar } from "@/components/shared/Avatar";
import { Markdown } from "@/components/shared/Markdown";
import { useToast } from "@/components/shared/ToastProvider";
import { formatNumber, formatRelativeTime } from "@/lib/format";
import { cn } from "@/lib/utils";

import { communityApi } from "../api";
import { useCommunityMutation } from "../hooks/useCommunity";
import type { CommentItem, CommentThread } from "../types";
import { CommentForm } from "./CommentForm";
import { ReportDialog } from "./ReportDialog";

const ACTION_CLASS = "min-h-8 text-sm font-semibold text-muted-foreground hover:text-foreground";

interface CommentViewProps {
    comment: CommentItem;
    thread: CommentThread;
    viewerId: string | null;
    now: Date;
}

export function CommentView({ comment, thread, viewerId, now }: CommentViewProps) {
    const showToast = useToast();
    const vote = useCommunityMutation(communityApi.vote);
    const remove = useCommunityMutation(communityApi.deleteComment);
    const [isReplying, setIsReplying] = useState(false);
    const [isReporting, setIsReporting] = useState(false);
    const author = comment.authorUsername ?? "Người dùng";
    const isReply = comment.parentId !== null;

    const isLiked = comment.myVote === 1;

    const toggleLike = () => {
        if (!viewerId) {
            showToast("Đăng nhập để thả tim.", "error");
            return;
        }
        vote.mutate(
            { targetType: "COMMENT", targetId: comment.id, value: isLiked ? 0 : 1 },
            { onError: (error) => showToast(error.message, "error") },
        );
    };

    return (
        <li className="flex gap-3">
            <Avatar name={author} className={isReply ? "size-7 text-[11px]" : undefined} />
            <div className="flex min-w-0 flex-1 flex-col gap-1">
                <p className="text-sm">
                    <strong>{author}</strong>
                    <span className="text-muted-foreground">
                        {` - ${formatRelativeTime(comment.createdAt, now)}`}
                    </span>
                </p>
                <Markdown className="text-[15px]">{comment.body}</Markdown>
                <div className="flex flex-wrap items-center gap-4">
                    <button
                        type="button"
                        aria-label={`Thả tim bình luận, ${formatNumber(comment.voteScore)} tim`}
                        aria-pressed={isLiked}
                        onClick={toggleLike}
                        className={cn(
                            ACTION_CLASS,
                            "inline-flex items-center gap-1",
                            isLiked ? "text-like hover:text-like" : null,
                        )}
                    >
                        <Heart
                            aria-hidden="true"
                            className={cn("size-4", isLiked ? "fill-current" : null)}
                        />
                        {formatNumber(comment.voteScore)}
                    </button>
                    {viewerId ? (
                        <button
                            type="button"
                            className={ACTION_CLASS}
                            onClick={() => setIsReplying(true)}
                        >
                            Trả lời
                        </button>
                    ) : null}
                    {viewerId && viewerId !== comment.authorId ? (
                        <button
                            type="button"
                            className={ACTION_CLASS}
                            onClick={() => setIsReporting(true)}
                        >
                            Báo cáo
                        </button>
                    ) : null}
                    {viewerId === comment.authorId ? (
                        <button
                            type="button"
                            className={ACTION_CLASS}
                            disabled={remove.isPending}
                            onClick={() =>
                                remove.mutate(comment.id, {
                                    onError: (error) => showToast(error.message, "error"),
                                })
                            }
                        >
                            Xoá
                        </button>
                    ) : null}
                </div>
                {isReplying ? (
                    <CommentForm
                        thread={thread}
                        parentId={comment.id}
                        initialBody={isReply ? `@${author} ` : undefined}
                        placeholder={`Trả lời ${author}`}
                        onDone={() => setIsReplying(false)}
                    />
                ) : null}
                {comment.replies.length > 0 ? (
                    <ul className="mt-2 flex flex-col gap-4 border-l-2 pl-4">
                        {comment.replies.map((reply) => (
                            <CommentView
                                key={reply.id}
                                comment={reply}
                                thread={thread}
                                viewerId={viewerId}
                                now={now}
                            />
                        ))}
                    </ul>
                ) : null}
            </div>
            {isReporting ? (
                <ReportDialog
                    targetType="COMMENT"
                    targetId={comment.id}
                    onClose={() => setIsReporting(false)}
                />
            ) : null}
        </li>
    );
}
