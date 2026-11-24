import { Sparkles } from "lucide-react";

import { Markdown } from "@/components/shared/Markdown";
import { cn } from "@/lib/utils";

const ASSISTANT_BODY =
    "text-[15px] [&_pre]:overflow-x-auto [&_pre]:rounded-md [&_pre]:border [&_pre]:bg-card [&_pre]:p-3 [&_pre_code]:bg-transparent [&_pre_code]:p-0";

interface AiMessageBubbleProps {
    role: "USER" | "ASSISTANT";
    content: string;
    isPending?: boolean;
}

export function AiMessageBubble({ role, content, isPending = false }: AiMessageBubbleProps) {
    if (role === "USER") {
        return (
            <li className="flex justify-end">
                <div
                    className={cn(
                        "max-w-[85%] rounded-lg bg-surface-2 px-4 py-2.5 text-[15px] whitespace-pre-wrap",
                        isPending && "opacity-70",
                    )}
                >
                    {content}
                </div>
            </li>
        );
    }
    return (
        <li className="flex gap-2.5">
            <span
                aria-hidden="true"
                className="mt-0.5 flex size-7 shrink-0 items-center justify-center rounded-full bg-ai text-white"
            >
                <Sparkles className="size-4" />
            </span>
            <div className="min-w-0 flex-1 rounded-lg bg-ai-soft px-4 py-2.5">
                <Markdown className={ASSISTANT_BODY}>{content}</Markdown>
            </div>
        </li>
    );
}
