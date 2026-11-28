import { useEffect, useRef } from "react";

import type { ContestPhase } from "@/lib/contest";

const SERVER_CLOCK_MARGIN_MS = 1_000;
const PHASE_RETRY_MS = 2_000;

interface ContestTiming {
    phase: ContestPhase;
    startAt: string;
    endAt: string;
}

export function useRefreshWhenDue(dueAt: string | undefined, onDue: () => void): void {
    const latestOnDue = useRef(onDue);

    useEffect(() => {
        latestOnDue.current = onDue;
    }, [onDue]);

    useEffect(() => {
        if (!dueAt) {
            return;
        }
        const delay = Math.max(new Date(dueAt).getTime() - Date.now(), 0);
        const timer = setTimeout(() => latestOnDue.current(), delay + SERVER_CLOCK_MARGIN_MS);
        return () => clearTimeout(timer);
    }, [dueAt]);
}

export function nextPhaseChangeOf(contest: ContestTiming): string | undefined {
    if (contest.phase === "UPCOMING") {
        return contest.startAt;
    }
    return contest.phase === "RUNNING" ? contest.endAt : undefined;
}

export function contestRefetchInterval(contest: ContestTiming, now: Date): number | false {
    const change = nextPhaseChangeOf(contest);
    return change && now.getTime() >= new Date(change).getTime() ? PHASE_RETRY_MS : false;
}
