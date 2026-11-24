"use client";

import { useMutation } from "@tanstack/react-query";
import { Sparkles } from "lucide-react";

import { Markdown } from "@/components/shared/Markdown";
import { Button } from "@/components/ui/button";

import { aiErrorText } from "../ai-errors";
import { communityApi } from "../api";

interface DiscussionSummaryProps {
    postId: string;
}

export function DiscussionSummary({ postId }: DiscussionSummaryProps) {
    const summarize = useMutation({ mutationFn: communityApi.summarizeDiscussion });

    if (summarize.data) {
        return (
            <section
                aria-labelledby="discussion-summary"
                className="flex flex-col gap-2 rounded-lg bg-ai-soft p-5"
            >
                <h2 id="discussion-summary" className="flex items-center gap-2 font-bold text-ai">
                    <Sparkles aria-hidden="true" className="size-4" />
                    Tóm tắt thảo luận
                </h2>
                <Markdown className="text-[15px]">{summarize.data.text}</Markdown>
            </section>
        );
    }
    return (
        <div className="flex flex-wrap items-center gap-3">
            <Button
                type="button"
                variant="outline"
                disabled={summarize.isPending}
                onClick={() => summarize.mutate(postId)}
                className="border-ai/40 text-ai hover:bg-ai-soft hover:text-ai"
            >
                <Sparkles aria-hidden="true" />
                {summarize.isPending ? "Đang tóm tắt…" : "Tóm tắt thảo luận"}
            </Button>
            {summarize.error ? (
                <p role="alert" className="text-sm text-destructive">
                    {aiErrorText(summarize.error)}
                </p>
            ) : null}
        </div>
    );
}
