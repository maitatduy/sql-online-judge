import { Play, Send } from "lucide-react";

import { Button } from "@/components/ui/button";

const SHORTCUT_CLASS =
    "rounded-sm bg-surface-2 px-1.5 font-mono text-[11px] text-muted-foreground max-lg:hidden";

interface WorkspaceActionsProps {
    isRunning: boolean;
    isJudging: boolean;
    onRun: () => void;
    onSubmit: () => void;
}

export function WorkspaceActions({ isRunning, isJudging, onRun, onSubmit }: WorkspaceActionsProps) {
    return (
        <div className="flex items-center gap-2 max-lg:fixed max-lg:inset-x-0 max-lg:bottom-0 max-lg:z-20 max-lg:gap-2.5 max-lg:border-t max-lg:bg-card max-lg:px-4 max-lg:py-3 max-lg:[&>button]:h-11 max-lg:[&>button]:flex-1">
            <Button
                type="button"
                variant="outline"
                size="sm"
                disabled={isRunning}
                aria-keyshortcuts="Control+Enter"
                onClick={onRun}
            >
                <Play aria-hidden="true" />
                Run
                <kbd aria-hidden="true" className={SHORTCUT_CLASS}>
                    Ctrl ↵
                </kbd>
            </Button>
            <Button
                type="button"
                size="sm"
                disabled={isJudging}
                aria-keyshortcuts="Control+Shift+Enter"
                onClick={onSubmit}
            >
                <Send aria-hidden="true" />
                Submit
            </Button>
        </div>
    );
}
