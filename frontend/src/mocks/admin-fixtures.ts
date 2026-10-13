import type {
    AdminOverviewResponse,
    JudgeQueueSummary,
    JudgeWorker,
    MonthlySubmissions,
    WorkerStatus,
} from "@/features/admin/types";
import type { AdminNavigationCounts } from "@/lib/admin-navigation";
import type { Verdict } from "@/lib/verdict";

const MINUTE_MS = 60_000;
const DISTRIBUTION_VERDICTS: Verdict[] = ["AC", "WA", "OM", "TLE", "RE", "FS"];
const VERDICT_PERCENTS = [43, 35, 5, 7, 8, 2];
const VERDICT_COUNTS = [552348, 449586, 64227, 89917, 102762, 25690];
const HOUR_MS = 60 * MINUTE_MS;
const VIETNAM_UTC_OFFSET_MS = 7 * HOUR_MS;
const MONTHLY_COUNTS = [
    86420, 79310, 98210, 104560, 112340, 96780, 88150, 101920, 118470, 124630, 109870, 93540,
];
const WEEKLY_CHANGE_NOTE = "so với tuần trước";

const DISCONNECTED_FOR_MS = 8 * MINUTE_MS;
const RESTART_DURATION_MS = 2500;

function restartedWorker(worker: JudgeWorker, restartedForMs: number): JudgeWorker {
    return restartedForMs < RESTART_DURATION_MS
        ? {
              ...worker,
              status: "RESTARTING",
              judgedPerHour: 0,
              cpuPercent: null,
              disconnectedAt: null,
          }
        : { ...worker, status: "ONLINE", judgedPerHour: 12, cpuPercent: 35, disconnectedAt: null };
}

export function buildJudgeWorkers(
    now: Date,
    restartStartedAtByName: Record<string, number> = {},
): JudgeWorker[] {
    const disconnectedAt = new Date(now.getTime() - DISCONNECTED_FOR_MS).toISOString();
    const connected = { disconnectedAt: null };

    const workers: JudgeWorker[] = [
        {
            name: "judge-pg-01",
            dialect: "POSTGRESQL",
            status: "BUSY",
            judgedPerHour: 212,
            cpuPercent: 92,
            ...connected,
        },
        {
            name: "judge-pg-02",
            dialect: "POSTGRESQL",
            status: "BUSY",
            judgedPerHour: 205,
            cpuPercent: 88,
            ...connected,
        },
        {
            name: "judge-pg-03",
            dialect: "POSTGRESQL",
            status: "ONLINE",
            judgedPerHour: 198,
            cpuPercent: 61,
            ...connected,
        },
        {
            name: "judge-my-01",
            dialect: "MYSQL",
            status: "BUSY",
            judgedPerHour: 233,
            cpuPercent: 94,
            ...connected,
        },
        {
            name: "judge-my-02",
            dialect: "MYSQL",
            status: "DISCONNECTED",
            judgedPerHour: 0,
            cpuPercent: null,
            disconnectedAt,
        },
        {
            name: "judge-lite-01",
            dialect: "SQLITE",
            status: "ONLINE",
            judgedPerHour: 121,
            cpuPercent: 22,
            ...connected,
        },
    ];

    return workers.map((worker) => {
        const restartStartedAt = restartStartedAtByName[worker.name];
        return restartStartedAt === undefined
            ? worker
            : restartedWorker(worker, now.getTime() - restartStartedAt);
    });
}

function hasWorkerWithStatus(workers: JudgeWorker[], statuses: WorkerStatus[]): boolean {
    return workers.some((worker) => statuses.includes(worker.status));
}

export function buildNavigationCounts(workers: JudgeWorker[]): AdminNavigationCounts {
    return {
        abuseReports: 12,
        supportMessages: 3,
        problemReviews: 5,
        hasLiveContest: true,
        hasJudgeIncident: hasWorkerWithStatus(workers, ["DISCONNECTED"]),
    };
}

function buildQueueSummary(workers: JudgeWorker[]): JudgeQueueSummary {
    return hasWorkerWithStatus(workers, ["DISCONNECTED", "RESTARTING"])
        ? { size: 146, averageWaitMs: 3800, isDegraded: true }
        : { size: 107, averageWaitMs: 2100, isDegraded: false };
}

function buildMonthlySubmissions(now: Date): MonthlySubmissions {
    const vietnamNow = new Date(now.getTime() + VIETNAM_UTC_OFFSET_MS);
    const currentMonth = vietnamNow.getUTCMonth() + 1;

    return {
        year: vietnamNow.getUTCFullYear(),
        months: MONTHLY_COUNTS.slice(0, currentMonth).map((count, index) => ({
            month: index + 1,
            count,
        })),
    };
}

export function buildAdminOverview(
    workers: JudgeWorker[] = buildJudgeWorkers(new Date()),
    now: Date = new Date(),
): AdminOverviewResponse {
    return {
        users: { value: 48260, changePercent: 2, changeNote: WEEKLY_CHANGE_NOTE },
        submissions: { value: 1284530, changePercent: 4, changeNote: WEEKLY_CHANGE_NOTE },
        aiMessages: { value: 3905110, changePercent: 5, changeNote: WEEKLY_CHANGE_NOTE },
        queue: buildQueueSummary(workers),
        submissionTotals: { allTime: 1284530, today: 9807, last7Days: 44307 },
        monthlySubmissions: buildMonthlySubmissions(now),
        verdictDistribution: DISTRIBUTION_VERDICTS.map((verdict, index) => ({
            verdict,
            count: VERDICT_COUNTS[index],
            percent: VERDICT_PERCENTS[index],
        })),
    };
}
