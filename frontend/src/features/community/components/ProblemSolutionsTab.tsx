"use client";

import { Heart, Lock, MessageSquare, PenLine } from "lucide-react";
import Link from "next/link";

import { EmptyState } from "@/components/shared/EmptyState";
import { Skeleton } from "@/components/shared/Skeleton";
import { Button } from "@/components/ui/button";
import { formatNumber } from "@/lib/format";
import { ROUTES } from "@/lib/routes";

import { useEditorial, useLinkedProblem, useProblemSolutions } from "../hooks/useCommunity";
import { PostBody } from "./PostBody";

interface ProblemSolutionsTabProps {
    problemSlug: string;
}

export function ProblemSolutionsTab({ problemSlug }: ProblemSolutionsTabProps) {
    return (
        <div className="flex flex-col gap-6 p-5">
            <OfficialSolution problemSlug={problemSlug} />
            <CommunitySolutions problemSlug={problemSlug} />
        </div>
    );
}

function OfficialSolution({ problemSlug }: ProblemSolutionsTabProps) {
    const { data } = useEditorial(problemSlug);

    if (!data || data.status === "NONE") {
        return null;
    }
    return (
        <section aria-labelledby="official-solution" className="flex flex-col gap-3">
            <h2 id="official-solution" className="text-lg font-bold">
                Lời giải chính thức
            </h2>
            {data.status === "AVAILABLE" && data.editorialMarkdown ? (
                <PostBody markdown={data.editorialMarkdown} problemSlug={problemSlug} />
            ) : (
                <p className="flex items-center gap-2 rounded-lg border bg-surface-2 px-4 py-3 text-[15px] text-muted-foreground">
                    <Lock aria-hidden="true" className="size-4" />
                    AC bài này để mở lời giải chính thức.
                </p>
            )}
        </section>
    );
}

function CommunitySolutions({ problemSlug }: ProblemSolutionsTabProps) {
    const { data: problem } = useLinkedProblem(problemSlug);
    const { data, isPending } = useProblemSolutions(problem?.id ?? null);

    return (
        <section aria-labelledby="community-solutions" className="flex flex-col gap-3">
            <div className="flex items-center justify-between gap-3">
                <h2 id="community-solutions" className="text-lg font-bold">
                    Lời giải từ cộng đồng
                </h2>
                <Button asChild size="sm" variant="outline">
                    <Link href={ROUTES.communityNewForProblem(problemSlug)}>
                        <PenLine aria-hidden="true" />
                        Chia sẻ lời giải
                    </Link>
                </Button>
            </div>
            {isPending ? <Skeleton className="h-16" /> : null}
            {data && data.items.length === 0 ? (
                <EmptyState title="Chưa có ai chia sẻ lời giải" />
            ) : null}
            {data && data.items.length > 0 ? (
                <ul
                    aria-label="Lời giải từ cộng đồng"
                    className="flex flex-col divide-y rounded-lg border"
                >
                    {data.items.map((post) => (
                        <li key={post.id} className="flex flex-col gap-1 px-4 py-3">
                            <Link
                                href={ROUTES.communityPost(post.id)}
                                className="text-[15px] font-semibold hover:text-brand"
                            >
                                {post.title}
                            </Link>
                            <p className="flex items-center gap-3 text-sm text-muted-foreground">
                                <span>{post.authorUsername ?? "Người dùng"}</span>
                                <span className="inline-flex items-center gap-1">
                                    <Heart aria-hidden="true" className="size-4 text-like" />
                                    {`${formatNumber(post.voteScore)} tim`}
                                </span>
                                <span className="inline-flex items-center gap-1">
                                    <MessageSquare aria-hidden="true" className="size-4" />
                                    {`${formatNumber(post.commentCount)} bình luận`}
                                </span>
                            </p>
                        </li>
                    ))}
                </ul>
            ) : null}
        </section>
    );
}
