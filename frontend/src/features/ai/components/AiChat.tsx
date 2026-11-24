"use client";

import { Lock, SendHorizontal, Sparkles } from "lucide-react";
import { useEffect, useId, useRef, useState } from "react";

import { Skeleton } from "@/components/shared/Skeleton";
import { Button } from "@/components/ui/button";
import { ApiRequestError } from "@/lib/api-client";
import { formatNumber } from "@/lib/format";
import { cn } from "@/lib/utils";

import { useAiQuota, useAskAssistant, useConversation } from "../hooks/useAssistant";
import { AiMessageBubble } from "./AiMessageBubble";

const MAX_QUESTION_LENGTH = 4000;
const SECONDS_PER_MINUTE = 60;

interface AiChatProps {
    conversationId: string | null;
    problemId: string | null;
    readCode?: () => string | null;
    suggestions: readonly string[];
    onConversationStarted: (conversationId: string) => void;
    className?: string;
}

function waitText(seconds: number | undefined): string {
    if (!seconds) {
        return "Bạn đã dùng hết lượt hỏi hôm nay.";
    }
    const hours = Math.ceil(seconds / SECONDS_PER_MINUTE / SECONDS_PER_MINUTE);
    return `Bạn đã dùng hết lượt hỏi hôm nay. Thử lại sau khoảng ${formatNumber(hours)} giờ.`;
}

function errorText(error: Error): string {
    if (error instanceof ApiRequestError && error.status === 429) {
        return waitText(error.retryAfterSeconds);
    }
    return error.message;
}

function isContestLock(error: Error | null): boolean {
    return error instanceof ApiRequestError && error.code === "AI_DISABLED_DURING_CONTEST";
}

export function AiChat({
    conversationId,
    problemId,
    readCode,
    suggestions,
    onConversationStarted,
    className,
}: AiChatProps) {
    const fieldId = useId();
    const [question, setQuestion] = useState("");
    const [includeCode, setIncludeCode] = useState(true);
    const endRef = useRef<HTMLLIElement>(null);
    const conversation = useConversation(conversationId);
    const quota = useAiQuota(true);
    const ask = useAskAssistant(onConversationStarted);
    const messages = conversation.data?.messages ?? [];
    const isLoadingConversation = conversationId !== null && conversation.isPending;
    const isLocked = isContestLock(ask.error);
    const remaining = quota.data ? Math.max(0, quota.data.limit - quota.data.used) : null;

    useEffect(() => {
        endRef.current?.scrollIntoView?.({ block: "end" });
    }, [messages.length, ask.isPending]);

    const send = (content: string) => {
        const trimmed = content.trim();
        if (!trimmed || ask.isPending) {
            return;
        }
        const code = readCode && includeCode ? (readCode() ?? undefined) : undefined;
        ask.mutate(
            { conversationId, problemId, content: trimmed, code },
            { onSuccess: () => setQuestion("") },
        );
    };

    return (
        <section aria-label="Trợ lý AI" className={cn("flex min-h-0 flex-col", className)}>
            <ul
                aria-label="Tin nhắn"
                className="flex min-h-0 flex-1 flex-col gap-4 overflow-y-auto p-4"
            >
                {isLoadingConversation ? (
                    <li role="status" aria-label="Đang tải hội thoại">
                        <Skeleton className="h-20" />
                    </li>
                ) : null}
                {messages.length === 0 && !isLoadingConversation && !ask.isPending ? (
                    <EmptyChat suggestions={suggestions} onPick={send} />
                ) : null}
                {messages.map((message) => (
                    <AiMessageBubble
                        key={message.id}
                        role={message.role}
                        content={message.content}
                    />
                ))}
                {ask.isPending ? (
                    <>
                        <AiMessageBubble role="USER" content={ask.variables.content} isPending />
                        <li role="status" className="text-sm text-ai">
                            Trợ lý đang trả lời…
                        </li>
                    </>
                ) : null}
                <li ref={endRef} aria-hidden="true" />
            </ul>
            {isLocked ? (
                <p
                    role="status"
                    className="mx-4 mb-4 flex items-center gap-2 rounded-lg border bg-surface-2 px-4 py-3 text-[15px]"
                >
                    <Lock aria-hidden="true" className="size-4 shrink-0" />
                    Trợ lý AI tạm khoá khi bạn đang thi contest tính rating.
                </p>
            ) : (
                <form
                    className="flex flex-col gap-2 border-t p-4"
                    onSubmit={(event) => {
                        event.preventDefault();
                        send(question);
                    }}
                >
                    {ask.error ? (
                        <p role="alert" className="text-sm text-destructive">
                            {errorText(ask.error)}
                        </p>
                    ) : null}
                    <label htmlFor={fieldId} className="sr-only">
                        Câu hỏi cho trợ lý AI
                    </label>
                    <textarea
                        id={fieldId}
                        rows={2}
                        maxLength={MAX_QUESTION_LENGTH}
                        placeholder="Hỏi về SQL, ví dụ: vì sao câu này sai?"
                        value={question}
                        onChange={(event) => setQuestion(event.target.value)}
                        onKeyDown={(event) => {
                            if (event.key === "Enter" && !event.shiftKey) {
                                event.preventDefault();
                                send(question);
                            }
                        }}
                        className="resize-none rounded-md border bg-transparent px-3 py-2 text-[15px]"
                    />
                    <div className="flex flex-wrap items-center justify-between gap-2 text-sm text-muted-foreground">
                        <div className="flex items-center gap-3">
                            {readCode ? (
                                <label className="flex items-center gap-1.5">
                                    <input
                                        type="checkbox"
                                        checked={includeCode}
                                        onChange={(event) => setIncludeCode(event.target.checked)}
                                        className="size-4 accent-ai"
                                    />
                                    Gửi kèm code
                                </label>
                            ) : null}
                            {remaining !== null && quota.data ? (
                                <span>{`Còn ${formatNumber(remaining)}/${formatNumber(quota.data.limit)} lượt hôm nay`}</span>
                            ) : null}
                        </div>
                        <Button
                            type="submit"
                            size="sm"
                            disabled={!question.trim() || ask.isPending}
                            className="bg-ai text-white hover:bg-ai/90"
                        >
                            <SendHorizontal aria-hidden="true" />
                            Gửi
                        </Button>
                    </div>
                </form>
            )}
        </section>
    );
}

interface EmptyChatProps {
    suggestions: readonly string[];
    onPick: (question: string) => void;
}

function EmptyChat({ suggestions, onPick }: EmptyChatProps) {
    return (
        <li className="flex flex-col items-center gap-3 py-6 text-center">
            <span className="flex size-10 items-center justify-center rounded-full bg-ai-soft text-ai">
                <Sparkles aria-hidden="true" className="size-5" />
            </span>
            <p className="text-[15px] text-muted-foreground">
                Trợ lý gợi ý từng bước, không giải hộ.
            </p>
            <div className="flex flex-wrap justify-center gap-2">
                {suggestions.map((suggestion) => (
                    <button
                        key={suggestion}
                        type="button"
                        onClick={() => onPick(suggestion)}
                        className="rounded-full border border-ai/40 px-3 py-1.5 text-sm text-ai hover:bg-ai-soft"
                    >
                        {suggestion}
                    </button>
                ))}
            </div>
        </li>
    );
}
