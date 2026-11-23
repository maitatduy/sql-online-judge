import { cn } from "@/lib/utils";

import { VISIBILITY_CLASSES, VISIBILITY_LABELS } from "../../problem-bank/labels";
import type { ProblemVisibility } from "../../problem-reviews/types";

interface VisibilityLabelProps {
    visibility: ProblemVisibility;
}

export function VisibilityLabel({ visibility }: VisibilityLabelProps) {
    return (
        <span
            className={cn(
                "inline-flex h-6 items-center rounded-full px-2.5 text-xs font-semibold whitespace-nowrap",
                VISIBILITY_CLASSES[visibility],
            )}
        >
            {VISIBILITY_LABELS[visibility]}
        </span>
    );
}
