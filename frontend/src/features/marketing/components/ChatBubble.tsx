import { cn } from "@/lib/utils";

type ChatSender = "user" | "assistant";

interface ChatBubbleProps {
    sender: ChatSender;
    hintLabel?: string;
    children: React.ReactNode;
}

const SENDER_NAMES: Record<ChatSender, string> = {
    user: "Bạn:",
    assistant: "Trợ lý AI:",
};

const SENDER_CLASSES: Record<ChatSender, string> = {
    user: "max-w-[80%] self-end rounded-br-sm bg-brand px-3.5 py-2.5 text-white",
    assistant: "rounded-bl-sm border border-ai/30 bg-ai-soft px-3.5 py-3 text-white",
};

export function ChatBubble({ sender, hintLabel, children }: ChatBubbleProps) {
    return (
        <div className={cn("rounded-lg", SENDER_CLASSES[sender])}>
            <span className="sr-only">{SENDER_NAMES[sender]}</span>
            {hintLabel && <p className="mb-1.5 text-xs font-semibold text-ai">{hintLabel}</p>}
            {children}
        </div>
    );
}
