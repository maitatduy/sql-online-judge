"use client";

import { Plus } from "lucide-react";
import Link from "next/link";
import { usePathname, useSearchParams } from "next/navigation";

import { EmptyState } from "@/components/shared/EmptyState";
import { Skeleton } from "@/components/shared/Skeleton";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { Button } from "@/components/ui/button";
import { formatRelativeTime } from "@/lib/format";
import { ROUTES } from "@/lib/routes";
import { useCurrentUser } from "@/lib/useCurrentUser";
import { useNow } from "@/lib/useNow";
import { cn } from "@/lib/utils";

import { useConversations } from "../hooks/useAssistant";
import { AiChat } from "./AiChat";

const SUGGESTIONS = [
    "LEFT JOIN khác INNER JOIN thế nào?",
    "Khi nào dùng window function?",
    "Giải thích GROUP BY và HAVING",
] as const;

function select(conversationId: string | null): void {
    const query = conversationId ? `?c=${encodeURIComponent(conversationId)}` : "";
    window.history.replaceState(null, "", `${ROUTES.ai}${query}`);
}

export function AiPage() {
    const pathname = usePathname();
    const now = useNow();
    const selectedId = useSearchParams().get("c");
    const { data: currentUser, isPending: isUserPending } = useCurrentUser();
    const conversations = useConversations(Boolean(currentUser));

    if (isUserPending) {
        return <Skeleton className="m-6 h-96" />;
    }
    if (!currentUser) {
        return (
            <main id={MAIN_CONTENT_ID} className="px-4 pt-10 md:px-6">
                <EmptyState
                    title="Đăng nhập để hỏi trợ lý AI"
                    action={
                        <Button asChild size="sm">
                            <Link href={ROUTES.loginWithNext(pathname)}>Đăng nhập</Link>
                        </Button>
                    }
                />
            </main>
        );
    }

    return (
        <main
            id={MAIN_CONTENT_ID}
            className="mx-auto grid h-[calc(100vh-64px)] w-full max-w-6xl gap-5 px-4 py-6 md:px-6 lg:grid-cols-[280px_minmax(0,1fr)]"
        >
            <aside className="flex min-h-0 flex-col gap-3 max-lg:hidden">
                <h1 className="text-[26px] font-extrabold">Trợ lý AI</h1>
                <Button
                    type="button"
                    onClick={() => select(null)}
                    className="justify-start bg-ai text-white hover:bg-ai/90"
                >
                    <Plus aria-hidden="true" />
                    Hội thoại mới
                </Button>
                <nav aria-label="Hội thoại" className="min-h-0 overflow-y-auto">
                    <ul className="flex flex-col gap-1">
                        {conversations.data?.items.map((conversation) => (
                            <li key={conversation.id}>
                                <button
                                    type="button"
                                    aria-current={
                                        conversation.id === selectedId ? "true" : undefined
                                    }
                                    onClick={() => select(conversation.id)}
                                    className={cn(
                                        "flex w-full flex-col rounded-md px-3 py-2 text-left hover:bg-surface-2",
                                        conversation.id === selectedId && "bg-ai-soft",
                                    )}
                                >
                                    <span className="line-clamp-1 text-[15px] font-semibold">
                                        {conversation.title}
                                    </span>
                                    <span className="text-[13px] text-muted-foreground">
                                        {formatRelativeTime(conversation.lastMessageAt, now)}
                                    </span>
                                </button>
                            </li>
                        ))}
                    </ul>
                </nav>
            </aside>
            <div className="flex min-h-0 flex-col overflow-hidden rounded-lg border bg-card">
                <AiChat
                    key={selectedId ?? "new"}
                    conversationId={selectedId}
                    problemId={null}
                    suggestions={SUGGESTIONS}
                    onConversationStarted={select}
                    className="flex-1"
                />
            </div>
        </main>
    );
}
