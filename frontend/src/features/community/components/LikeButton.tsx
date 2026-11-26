"use client";

import { Heart } from "lucide-react";

import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import { formatNumber } from "@/lib/format";
import { cn } from "@/lib/utils";

import { communityApi } from "../api";
import { useCommunityMutation } from "../hooks/useCommunity";

interface LikeButtonProps {
    postId: string;
    likeCount: number;
    isLiked: boolean;
    isSignedIn: boolean;
}

export function LikeButton({ postId, likeCount, isLiked, isSignedIn }: LikeButtonProps) {
    const showToast = useToast();
    const vote = useCommunityMutation(communityApi.vote);

    const toggle = () => {
        if (!isSignedIn) {
            showToast("Đăng nhập để thả tim.", "error");
            return;
        }
        vote.mutate(
            { targetType: "POST", targetId: postId, value: isLiked ? 0 : 1 },
            { onError: (error) => showToast(error.message, "error") },
        );
    };

    return (
        <div className="flex flex-col items-center gap-1">
            <Button
                type="button"
                variant="outline"
                size="icon"
                aria-label={`Thả tim bài viết, ${formatNumber(likeCount)} tim`}
                aria-pressed={isLiked}
                disabled={vote.isPending}
                onClick={toggle}
                className={cn(isLiked ? "border-like bg-like/12 text-like hover:text-like" : null)}
            >
                <Heart aria-hidden="true" className={cn(isLiked ? "fill-current" : null)} />
            </Button>
            <strong className="text-lg">{formatNumber(likeCount)}</strong>
        </div>
    );
}
