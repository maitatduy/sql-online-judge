import { CONTEST_PHASE_LABELS, type ContestPhase } from "@/lib/contest";
import { cn } from "@/lib/utils";

const PHASE_CLASSES: Record<ContestPhase, string> = {
    RUNNING: "bg-contest-live/12 text-contest-live",
    UPCOMING: "bg-brand/12 text-brand",
    FINISHED: "bg-surface-2 text-muted-foreground",
};

interface AdminContestPhaseProps {
    phase: ContestPhase;
}

export function AdminContestPhase({ phase }: AdminContestPhaseProps) {
    return (
        <span
            className={cn(
                "inline-flex h-7 w-fit items-center rounded-full px-3 text-sm font-semibold",
                PHASE_CLASSES[phase],
            )}
        >
            {CONTEST_PHASE_LABELS[phase]}
        </span>
    );
}
