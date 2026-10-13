const RESTARTS_STORAGE_KEY = "sqljudge:mock:worker-restarts";

export function readWorkerRestarts(): Record<string, number> {
    const storedRestarts = sessionStorage.getItem(RESTARTS_STORAGE_KEY);
    return storedRestarts ? (JSON.parse(storedRestarts) as Record<string, number>) : {};
}

export function recordWorkerRestart(workerName: string, startedAtMs: number): void {
    sessionStorage.setItem(
        RESTARTS_STORAGE_KEY,
        JSON.stringify({ ...readWorkerRestarts(), [workerName]: startedAtMs }),
    );
}
