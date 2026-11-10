import { cn } from "@/lib/utils";

import { REVIEW_STATUS_LABELS } from "../../problem-reviews/labels";
import { REVIEW_STATUSES } from "../../problem-reviews/search-params";
import type { ReviewProblemStatus } from "../../problem-reviews/types";

interface ReviewStatusFilterProps {
    status: ReviewProblemStatus;
    onChange: (status: ReviewProblemStatus) => void;
}

export function ReviewStatusFilter({ status, onChange }: ReviewStatusFilterProps) {
    return (
        <div
            role="group"
            aria-label="Lọc theo trạng thái"
            className="flex w-fit flex-wrap gap-1 rounded-lg border bg-card p-1"
        >
            {REVIEW_STATUSES.map((candidate) => (
                <button
                    key={candidate}
                    type="button"
                    aria-pressed={candidate === status}
                    onClick={() => onChange(candidate)}
                    className={cn(
                        "min-h-9 rounded-md px-3 text-[13px] font-semibold",
                        candidate === status
                            ? "bg-brand-soft text-brand"
                            : "text-muted-foreground hover:bg-surface-2",
                    )}
                >
                    {REVIEW_STATUS_LABELS[candidate]}
                </button>
            ))}
        </div>
    );
}
