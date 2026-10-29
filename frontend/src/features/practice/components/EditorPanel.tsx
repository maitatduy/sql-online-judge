"use client";

import { useState } from "react";

import { RotateCcw } from "lucide-react";

import { LazySqlEditor } from "@/components/shared/LazySqlEditor";
import { Button } from "@/components/ui/button";
import { formatTime } from "@/lib/format";

import { ResetCodeDialog } from "./ResetCodeDialog";

interface EditorPanelProps {
    engineLabel: string;
    code: string;
    savedAt: Date | null;
    onChange: (code: string) => void;
    onReset: () => void;
    onRun: () => void;
    onSubmit: () => void;
}

export function EditorPanel({
    engineLabel,
    code,
    savedAt,
    onChange,
    onReset,
    onRun,
    onSubmit,
}: EditorPanelProps) {
    const [isResetDialogOpen, setIsResetDialogOpen] = useState(false);

    return (
        <section aria-label="Editor" className="flex min-h-0 flex-1 flex-col">
            <div className="flex h-10 shrink-0 items-center gap-2 border-b bg-card px-3">
                <span className="rounded-md border px-2 py-0.5 text-[13px]">{engineLabel}</span>
                <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={() => setIsResetDialogOpen(true)}
                >
                    <RotateCcw aria-hidden="true" />
                    Đặt lại mã
                </Button>
                {savedAt ? (
                    <span className="ml-auto text-xs text-muted-foreground">
                        {`Đã lưu nháp · ${formatTime(savedAt)}`}
                    </span>
                ) : null}
            </div>
            <LazySqlEditor
                value={code}
                onChange={onChange}
                onRun={onRun}
                onSubmit={onSubmit}
                ariaLabel="Mã SQL của bạn"
                className="flex-1 bg-background"
            />
            <ResetCodeDialog
                open={isResetDialogOpen}
                onOpenChange={setIsResetDialogOpen}
                onConfirm={onReset}
            />
        </section>
    );
}
