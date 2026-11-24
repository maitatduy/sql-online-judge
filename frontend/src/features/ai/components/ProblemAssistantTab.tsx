"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState } from "react";

import { Skeleton } from "@/components/shared/Skeleton";
import { ROUTES } from "@/lib/routes";
import { useCurrentUser } from "@/lib/useCurrentUser";
import { readWorkspaceCode } from "@/lib/workspace-storage";

import { useAssistantProblem, useConversations } from "../hooks/useAssistant";
import { AiChat } from "./AiChat";

const SUGGESTIONS = [
    "Gợi ý bước đầu tiên",
    "Code của mình sai ở đâu?",
    "Bài này cần khái niệm gì?",
] as const;

interface ProblemAssistantTabProps {
    problemSlug: string;
}

export function ProblemAssistantTab({ problemSlug }: ProblemAssistantTabProps) {
    const pathname = usePathname();
    const { data: currentUser, isPending: isUserPending } = useCurrentUser();
    const problem = useAssistantProblem(problemSlug);
    const conversations = useConversations(Boolean(currentUser));
    const [startedId, setStartedId] = useState<string | null>(null);

    if (isUserPending) {
        return <Skeleton className="m-5 h-40" />;
    }
    if (!currentUser) {
        return (
            <p className="p-5 text-[15px] text-muted-foreground">
                <Link href={ROUTES.loginWithNext(pathname)} className="font-semibold text-brand">
                    Đăng nhập
                </Link>{" "}
                để hỏi trợ lý AI.
            </p>
        );
    }
    if (!problem.data || conversations.isPending) {
        return <Skeleton className="m-5 h-40" />;
    }

    const problemId = problem.data.id;
    const existingId =
        conversations.data?.items.find((conversation) => conversation.problemId === problemId)
            ?.id ?? null;

    return (
        <AiChat
            conversationId={startedId ?? existingId}
            problemId={problemId}
            readCode={() => readWorkspaceCode(problemId)}
            suggestions={SUGGESTIONS}
            onConversationStarted={setStartedId}
            className="h-full"
        />
    );
}
