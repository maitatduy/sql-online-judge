"use client";

import { ChevronDown, ChevronUp } from "lucide-react";

import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import { formatNumber } from "@/lib/format";
import { cn } from "@/lib/utils";

import { communityApi } from "../api";
import { useCommunityMutation } from "../hooks/useCommunity";
import type { VoteTarget } from "../types";

interface VoteControlsProps {
    targetType: VoteTarget;
    targetId: string;
    score: number;
    myVote: number;
    isSignedIn: boolean;
}

export function VoteControls({
    targetType,
    targetId,
    score,
    myVote,
    isSignedIn,
}: VoteControlsProps) {
    const showToast = useToast();
    const vote = useCommunityMutation(communityApi.vote);

    const cast = (value: number) => {
        if (!isSignedIn) {
            showToast("Đăng nhập để vote.", "error");
            return;
        }
        vote.mutate(
            { targetType, targetId, value: myVote === value ? 0 : value },
            { onError: (error) => showToast(error.message, "error") },
        );
    };

    return (
        <div className="flex flex-col items-center gap-1">
            <Button
                type="button"
                variant="outline"
                size="icon"
                aria-label="Vote lên"
                aria-pressed={myVote === 1}
                disabled={vote.isPending}
                onClick={() => cast(1)}
                className={cn(myVote === 1 ? "border-brand bg-brand-soft text-brand" : null)}
            >
                <ChevronUp aria-hidden="true" />
            </Button>
            <strong className="text-lg">{formatNumber(score)}</strong>
            <Button
                type="button"
                variant="outline"
                size="icon"
                aria-label="Vote xuống"
                aria-pressed={myVote === -1}
                disabled={vote.isPending}
                onClick={() => cast(-1)}
                className={cn(myVote === -1 ? "border-brand bg-brand-soft text-brand" : null)}
            >
                <ChevronDown aria-hidden="true" />
            </Button>
        </div>
    );
}
