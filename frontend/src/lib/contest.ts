import { formatDate, formatTime } from "@/lib/format";

export type ContestPhase = "UPCOMING" | "RUNNING" | "FINISHED";

export const CONTEST_PHASE_LABELS: Record<ContestPhase, string> = {
    UPCOMING: "Sắp diễn ra",
    RUNNING: "Đang diễn ra",
    FINISHED: "Đã kết thúc",
};

export function formatContestMoment(date: string): string {
    return `${formatTime(date)} ${formatDate(date)}`;
}

export function formatMinutes(minutes: number): string {
    return `${minutes} phút`;
}

const MS_PER_SECOND = 1000;
const SECONDS_PER_MINUTE = 60;
const SECONDS_PER_HOUR = 3_600;

export function formatRemaining(target: string, now: Date): string {
    const seconds = Math.max(
        0,
        Math.floor((new Date(target).getTime() - now.getTime()) / MS_PER_SECOND),
    );
    return [
        Math.floor(seconds / SECONDS_PER_HOUR),
        Math.floor((seconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE),
        seconds % SECONDS_PER_MINUTE,
    ]
        .map((part) => String(part).padStart(2, "0"))
        .join(":");
}

export type ContestProblemStatus = "ACCEPTED" | "WRONG" | "PENDING";

export interface ContestProblemAttempts {
    acceptedMinute: number | null;
    wrongAttempts: number;
    pendingAttempts: number;
}

export function contestProblemStatusOf(
    attempts: ContestProblemAttempts | undefined,
): ContestProblemStatus | null {
    if (!attempts) {
        return null;
    }
    if (attempts.acceptedMinute !== null) {
        return "ACCEPTED";
    }
    if (attempts.pendingAttempts > 0) {
        return "PENDING";
    }
    return attempts.wrongAttempts > 0 ? "WRONG" : null;
}
