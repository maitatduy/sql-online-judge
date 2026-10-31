import { Play, Send } from "lucide-react";

import { Button } from "@/components/ui/button";

interface WorkspaceActionsProps {
    isRunning: boolean;
    isJudging: boolean;
    onRun: () => void;
    onSubmit: () => void;
}

export function WorkspaceActions({ isRunning, isJudging, onRun, onSubmit }: WorkspaceActionsProps) {
    return (
        <div className="flex items-center gap-2 max-lg:fixed max-lg:inset-x-0 max-lg:bottom-0 max-lg:z-20 max-lg:gap-2.5 max-lg:border-t max-lg:bg-card max-lg:px-4 max-lg:py-3 max-lg:[&>button]:h-11 max-lg:[&>button]:flex-1">
            <Button type="button" variant="outline" size="sm" disabled={isRunning} onClick={onRun}>
                <Play aria-hidden="true" />
                Run
            </Button>
            <Button type="button" size="sm" disabled={isJudging} onClick={onSubmit}>
                <Send aria-hidden="true" />
                Submit
            </Button>
        </div>
    );
}
