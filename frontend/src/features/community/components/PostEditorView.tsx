"use client";

import Link from "next/link";
import { usePathname, useSearchParams } from "next/navigation";

import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { Button } from "@/components/ui/button";
import { ApiRequestError } from "@/lib/api-client";
import { ROUTES } from "@/lib/routes";
import { useCurrentUser } from "@/lib/useCurrentUser";

import { useLinkedProblem, usePost } from "../hooks/useCommunity";
import { PostEditor } from "./PostEditor";

function Loading() {
    return (
        <div
            role="status"
            aria-label="Đang tải"
            className="mx-auto flex max-w-[880px] flex-col gap-4"
        >
            <Skeleton className="h-10 w-1/3" />
            <Skeleton className="h-96" />
        </div>
    );
}

function SignInRequired() {
    const pathname = usePathname();
    return (
        <EmptyState
            title="Đăng nhập để viết bài"
            action={
                <Button asChild size="sm">
                    <Link href={ROUTES.loginWithNext(pathname)}>Đăng nhập</Link>
                </Button>
            }
        />
    );
}

export function NewPostView() {
    const problemSlug = useSearchParams().get("problem");
    const { data: currentUser, isPending } = useCurrentUser();
    const problem = useLinkedProblem(problemSlug);

    if (isPending || (problemSlug !== null && problem.isPending)) {
        return <Loading />;
    }
    if (!currentUser) {
        return <SignInRequired />;
    }
    return <PostEditor initialProblem={problem.data ?? null} />;
}

interface EditPostViewProps {
    postId: string;
}

export function EditPostView({ postId }: EditPostViewProps) {
    const { data: currentUser, isPending: isUserPending } = useCurrentUser();
    const { data: post, error, isPending, refetch } = usePost(postId);

    if (isUserPending || isPending) {
        return <Loading />;
    }
    if (!currentUser) {
        return <SignInRequired />;
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
    if (post.authorId !== currentUser.id) {
        return <EmptyState title="Bạn chỉ sửa được bài viết của mình" />;
    }
    return <PostEditor post={post} />;
}
