"use client";

import { useState } from "react";

import { RotateCcw } from "lucide-react";

import { LazySqlEditor } from "@/components/shared/LazySqlEditor";
import { Button } from "@/components/ui/button";
import { DIALECT_LABELS } from "@/lib/dialect";
import { formatTime } from "@/lib/format";

import { useEditorDraft } from "../hooks/useEditorDraft";
import type { ProblemDetail } from "../types";
import { ResetCodeDialog } from "./ResetCodeDialog";

interface EditorPanelProps {
    problem: ProblemDetail;
}

export function EditorPanel({ problem }: EditorPanelProps) {
    const { code, savedAt, changeCode, resetToStarter } = useEditorDraft(
        problem.id,
        problem.starterCode,
    );
    const [isResetDialogOpen, setIsResetDialogOpen] = useState(false);
    const [dialect] = problem.dialects;

    return (
        <section aria-label="Editor" className="flex min-h-0 flex-1 flex-col">
            <div className="flex h-10 shrink-0 items-center gap-2 border-b bg-card px-3">
                <span className="rounded-md border px-2 py-0.5 text-[13px]">
                    {problem.engineVersions[dialect] ?? DIALECT_LABELS[dialect]}
                </span>
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
                onChange={changeCode}
                ariaLabel="Mã SQL của bạn"
                className="flex-1 bg-background"
            />
            <ResetCodeDialog
                open={isResetDialogOpen}
                onOpenChange={setIsResetDialogOpen}
                onConfirm={resetToStarter}
            />
        </section>
    );
}
