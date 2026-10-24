import { Check } from "lucide-react";

import { PROGRESS_LABELS } from "../problem-filters";
import type { ProblemProgress } from "../types";

interface ProblemProgressMarkProps {
    progress: ProblemProgress;
}

export function ProblemProgressMark({ progress }: ProblemProgressMarkProps) {
    const label = PROGRESS_LABELS[progress];

    return (
        <span title={label} className="inline-flex size-5 items-center justify-center">
            {progress === "SOLVED" ? (
                <Check aria-hidden="true" strokeWidth={3} className="size-4 text-verdict-ac" />
            ) : null}
            {progress === "ATTEMPTED" ? (
                <span aria-hidden="true" className="size-2 rounded-full bg-muted-foreground" />
            ) : null}
            <span className="sr-only">{label}</span>
        </span>
    );
}
