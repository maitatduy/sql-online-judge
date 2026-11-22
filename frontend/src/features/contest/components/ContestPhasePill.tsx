import { CONTEST_PHASE_LABELS, type ContestPhase } from "@/lib/contest";
import { cn } from "@/lib/utils";

const PHASE_CLASSES: Record<ContestPhase, string> = {
    UPCOMING: "bg-contest-hero-accent/15 text-contest-hero-accent",
    RUNNING: "bg-contest-live/15 text-contest-live",
    FINISHED: "bg-contest-hero-panel text-contest-hero-muted",
};

interface ContestPhasePillProps {
    phase: ContestPhase;
}

export function ContestPhasePill({ phase }: ContestPhasePillProps) {
    return (
        <span
            className={cn(
                "inline-flex h-7 items-center gap-2 rounded-full px-3 font-semibold",
                PHASE_CLASSES[phase],
            )}
        >
            {phase === "RUNNING" ? (
                <span aria-hidden="true" className="size-2 rounded-full bg-contest-live" />
            ) : null}
            {CONTEST_PHASE_LABELS[phase]}
        </span>
    );
}
