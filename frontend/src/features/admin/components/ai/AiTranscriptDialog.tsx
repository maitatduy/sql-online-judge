"use client";

import { Markdown } from "@/components/shared/Markdown";
import { Skeleton } from "@/components/shared/Skeleton";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";
import { cn } from "@/lib/utils";

import { useAiTranscript } from "../../hooks/useAdminAi";

interface AiTranscriptDialogProps {
    conversationId: string;
    onClose: () => void;
}

export function AiTranscriptDialog({ conversationId, onClose }: AiTranscriptDialogProps) {
    const { data, isPending, error } = useAiTranscript(conversationId);

    return (
        <Dialog open onOpenChange={(isOpen) => (isOpen ? null : onClose())}>
            <DialogContent className="max-h-[85vh] max-w-2xl overflow-y-auto">
                <div>
                    <DialogTitle>{data?.conversation.title ?? "Hội thoại"}</DialogTitle>
                    <DialogDescription>
                        Toàn bộ tin nhắn, câu bị chặn được đánh dấu.
                    </DialogDescription>
                </div>
                {isPending ? <Skeleton className="h-40" /> : null}
                {error ? (
                    <p role="alert" className="text-destructive">
                        {error.message}
                    </p>
                ) : null}
                {data ? (
                    <ul aria-label="Tin nhắn" className="flex flex-col gap-3">
                        {data.messages.map((message) => (
                            <li
                                key={message.id}
                                className={cn(
                                    "rounded-lg px-4 py-2.5",
                                    message.role === "USER" ? "bg-surface-2" : "bg-ai-soft",
                                    message.isBlocked && "ring-2 ring-ai",
                                )}
                            >
                                <p className="mb-1 text-sm font-semibold text-muted-foreground">
                                    {message.role === "USER" ? "Người hỏi" : "Trợ lý"}
                                    {message.isBlocked ? " - bị chặn" : null}
                                </p>
                                <Markdown className="text-[15px]">{message.content}</Markdown>
                            </li>
                        ))}
                    </ul>
                ) : null}
            </DialogContent>
        </Dialog>
    );
}
