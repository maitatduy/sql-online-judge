import { formatDate, formatTime } from "@/lib/format";

import type { ContestPhase } from "./types";

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
