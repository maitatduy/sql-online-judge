"use client";

import { useState } from "react";

import { RotateCcw } from "lucide-react";

import { LazySqlEditor } from "@/components/shared/LazySqlEditor";
import { NativeSelect } from "@/components/shared/NativeSelect";
import { Button } from "@/components/ui/button";
import { DIALECT_LABELS, type Dialect } from "@/lib/dialect";
import { formatTime } from "@/lib/format";
import type { CompletionSchema } from "@/lib/sql-completion";

import { ResetCodeDialog } from "./ResetCodeDialog";

interface EditorPanelProps {
    dialects: Dialect[];
    dialect: Dialect;
    engineVersions: Partial<Record<Dialect, string>>;
    onDialectChange: (dialect: Dialect) => void;
    code: string;
    schema: CompletionSchema;
    savedAt: Date | null;
    onChange: (code: string) => void;
    onReset: () => void;
}

export function EditorPanel({
    dialects,
    dialect,
    engineVersions,
    onDialectChange,
    code,
    schema,
    savedAt,
    onChange,
    onReset,
}: EditorPanelProps) {
    const [isResetDialogOpen, setIsResetDialogOpen] = useState(false);

    return (
        <section aria-label="Editor" className="flex min-h-0 flex-1 flex-col">
            <div className="flex h-10 shrink-0 items-center gap-2 border-b bg-card px-3">
                <NativeSelect
                    aria-label="Dialect"
                    className="h-8 w-auto text-[13px]"
                    value={dialect}
                    onChange={(event) => onDialectChange(event.target.value as Dialect)}
                >
                    {dialects.map((option) => (
                        <option key={option} value={option}>
                            {engineVersions[option] ?? DIALECT_LABELS[option]}
                        </option>
                    ))}
                </NativeSelect>
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
                        {`Đã lưu nháp lúc ${formatTime(savedAt)}`}
                    </span>
                ) : null}
            </div>
            <LazySqlEditor
                value={code}
                onChange={onChange}
                ariaLabel="Mã SQL của bạn"
                schema={schema}
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
