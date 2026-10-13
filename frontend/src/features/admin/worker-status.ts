import type { WorkerStatus } from "./types";

const HIGH_CPU_PERCENT = 60;
const CRITICAL_CPU_PERCENT = 85;
const IDLE_REFRESH_MS = 30_000;
const RESTARTING_REFRESH_MS = 2000;
const ACTIVE_STATUSES: WorkerStatus[] = ["ONLINE", "BUSY"];

interface WorkerSnapshot {
    name: string;
    status: WorkerStatus;
}

export type CpuLevel = "normal" | "high" | "critical";

export function cpuLevel(cpuPercent: number): CpuLevel {
    if (cpuPercent >= CRITICAL_CPU_PERCENT) {
        return "critical";
    }
    return cpuPercent >= HIGH_CPU_PERCENT ? "high" : "normal";
}

export function countActiveWorkers(statuses: WorkerStatus[]): number {
    return statuses.filter((status) => ACTIVE_STATUSES.includes(status)).length;
}

export function findRecoveredWorkers(
    previousWorkers: WorkerSnapshot[],
    currentWorkers: WorkerSnapshot[],
): string[] {
    const restartingNames = previousWorkers
        .filter((worker) => worker.status === "RESTARTING")
        .map((worker) => worker.name);
    return currentWorkers
        .filter(
            (worker) =>
                restartingNames.includes(worker.name) && ACTIVE_STATUSES.includes(worker.status),
        )
        .map((worker) => worker.name);
}

export function workersRefreshMs(statuses: WorkerStatus[]): number {
    return statuses.includes("RESTARTING") ? RESTARTING_REFRESH_MS : IDLE_REFRESH_MS;
}
