import { TriangleAlert } from "lucide-react";

import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

interface ErrorStateProps {
    title: string;
    message?: string;
    traceId?: string;
    onRetry: () => void;
    className?: string;
}

export function ErrorState({ title, message, traceId, onRetry, className }: ErrorStateProps) {
    return (
        <div role="alert" className={cn("rounded-lg border bg-card p-6 text-center", className)}>
            <TriangleAlert aria-hidden="true" className="mx-auto size-9 text-destructive" />
            <p className="mt-2.5 font-bold">{title}</p>
            {message ? <p className="mt-1 text-[13px] text-muted-foreground">{message}</p> : null}
            {traceId ? (
                <p className="mt-1 text-[13px] text-muted-foreground">
                    Mã lỗi: <span className="font-mono">{traceId}</span>
                </p>
            ) : null}
            <Button type="button" size="sm" className="mt-3.5" onClick={onRetry}>
                Thử lại
            </Button>
        </div>
    );
}
