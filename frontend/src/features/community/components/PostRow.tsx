import { MessageSquare, Pin } from "lucide-react";
import Link from "next/link";

import { Avatar } from "@/components/shared/Avatar";
import { formatNumber, formatRelativeTime } from "@/lib/format";
import { ROUTES } from "@/lib/routes";
import { cn } from "@/lib/utils";

import type { PostSummary } from "../types";
import { CategoryPill } from "./CategoryPill";

interface PostRowProps {
    post: PostSummary;
    now: Date;
}

export function PostRow({ post, now }: PostRowProps) {
    const author = post.authorUsername ?? "Người dùng";

    return (
        <li className="border-b last:border-b-0">
            <Link
                href={ROUTES.communityPost(post.id)}
                className="flex gap-4 px-1 py-4 hover:bg-surface/60"
            >
                <div className="flex w-14 shrink-0 flex-col items-center gap-1.5 pt-0.5">
                    <span className="text-lg font-extrabold">{formatNumber(post.voteScore)}</span>
                    <span className="text-xs text-muted-foreground">vote</span>
                    <span
                        className={cn(
                            "inline-flex items-center gap-1 rounded-sm px-1.5 py-0.5 text-xs font-semibold",
                            post.commentCount > 0
                                ? "bg-brand-soft text-brand"
                                : "border text-muted-foreground",
                        )}
                    >
                        <MessageSquare aria-hidden="true" className="size-3" />
                        {formatNumber(post.commentCount)}
                    </span>
                </div>
                <div className="flex min-w-0 flex-1 flex-col gap-1.5">
                    <div className="flex items-center gap-2">
                        <CategoryPill category={post.category} />
                        {post.isPinned ? (
                            <span className="inline-flex items-center gap-1 text-xs font-semibold text-muted-foreground">
                                <Pin aria-hidden="true" className="size-3" />
                                Đã ghim
                            </span>
                        ) : null}
                    </div>
                    <strong className="text-[17px] leading-snug">{post.title}</strong>
                    {post.excerpt ? (
                        <p className="line-clamp-2 text-[15px] text-muted-foreground">
                            {post.excerpt}
                        </p>
                    ) : null}
                    <div className="flex flex-wrap items-center gap-1.5 text-sm">
                        {post.tags.map((tag) => (
                            <span
                                key={tag}
                                className="rounded-sm bg-surface-2 px-1.5 py-0.5 text-xs"
                            >
                                {tag}
                            </span>
                        ))}
                        <span className="flex-1" />
                        <Avatar name={author} className="size-6 text-[10px]" />
                        <span className="font-semibold">{author}</span>
                        <span className="text-muted-foreground">
                            {formatRelativeTime(post.publishedAt, now)}
                        </span>
                    </div>
                </div>
            </Link>
        </li>
    );
}
