"use client";

import { useState } from "react";

import { Server } from "lucide-react";
import dynamic from "next/dynamic";
import Link from "next/link";

import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { ADMIN_ROUTES } from "@/lib/admin-navigation";
import { ApiRequestError } from "@/lib/api-client";
import { cn } from "@/lib/utils";

import { formatVietnamTime } from "../format";
import { useJudgeWorkers } from "../hooks/useJudgeWorkers";
import { useWorkerRecoveryToast } from "../hooks/useWorkerRecoveryToast";
import type { JudgeWorker } from "../types";
import { countActiveWorkers } from "../worker-status";
import { JudgeWorkerAlerts } from "./JudgeWorkerAlerts";
import { StaleDataNotice } from "./StaleDataNotice";
import { WorkerTable } from "./WorkerTable";

const RestartWorkerDialog = dynamic(
    () => import("./RestartWorkerDialog").then((dialogModule) => dialogModule.RestartWorkerDialog),
    { ssr: false },
);

const SKELETON_ROW_COUNT = 6;
const FLUSH_STATE_CLASS = "rounded-none border-x-0 border-b-0";

interface ActiveWorkersPillProps {
    workers: JudgeWorker[];
}

function ActiveWorkersPill({ workers }: ActiveWorkersPillProps) {
    const activeCount = countActiveWorkers(workers.map((worker) => worker.status));
    const isAllActive = activeCount === workers.length;

    return (
        <span
            className={cn(
                "inline-flex h-6 items-center rounded-full px-2.5 text-xs font-semibold",
                isAllActive
                    ? "bg-status-ok/12 text-status-ok"
                    : "bg-status-down/12 text-status-down",
            )}
        >
            {activeCount}/{workers.length} hoạt động
        </span>
    );
}

function WorkersSkeleton() {
    return (
        <div role="status" aria-busy="true" aria-label="Đang tải trạng thái máy chấm">
            {Array.from({ length: SKELETON_ROW_COUNT }, (_, index) => (
                <div key={index} className="border-t px-4.5 py-3">
                    <Skeleton className="h-3.5 w-full" />
                </div>
            ))}
        </div>
    );
}

export function JudgeWorkersCard() {
    const {
        data: workers,
        dataUpdatedAt,
        error,
        isPending,
        isRefetchError,
        refetch,
    } = useJudgeWorkers();
    const [workerToRestart, setWorkerToRestart] = useState<string | null>(null);
    useWorkerRecoveryToast(workers);

    return (
        <section className="min-w-0 flex-[3_1_520px] overflow-hidden rounded-lg border bg-card">
            <div className="flex items-center justify-between gap-3 px-4.5 py-4">
                <div className="flex flex-wrap items-center gap-2.5">
                    <h2 className="text-base font-bold">Máy chấm</h2>
                    {workers && workers.length > 0 ? <ActiveWorkersPill workers={workers} /> : null}
                    {workers ? (
                        <p className="text-xs text-muted-foreground">
                            {`Cập nhật lúc ${formatVietnamTime(new Date(dataUpdatedAt))}`}
                        </p>
                    ) : null}
                </div>
                <Link
                    href={ADMIN_ROUTES.judge}
                    aria-label="Chi tiết máy chấm"
                    className="inline-flex min-h-11 items-center text-[13px] font-semibold text-brand md:min-h-0"
                >
                    Chi tiết
                </Link>
            </div>
            {isPending ? <WorkersSkeleton /> : null}
            {isRefetchError ? (
                <StaleDataNotice updatedAtMs={dataUpdatedAt} className="mx-4.5 mb-1" />
            ) : null}
            {!workers && error ? (
                <ErrorState
                    title="Chưa tải được trạng thái máy chấm"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                    className={FLUSH_STATE_CLASS}
                />
            ) : null}
            {workers?.length === 0 ? (
                <EmptyState
                    icon={<Server />}
                    title="Chưa có worker nào đăng ký"
                    className={cn(FLUSH_STATE_CLASS, "border-solid")}
                />
            ) : null}
            {workers && workers.length > 0 ? (
                <>
                    <JudgeWorkerAlerts workers={workers} onRestart={setWorkerToRestart} />
                    <WorkerTable workers={workers} />
                </>
            ) : null}
            {workerToRestart ? (
                <RestartWorkerDialog
                    workerName={workerToRestart}
                    onClose={() => setWorkerToRestart(null)}
                />
            ) : null}
        </section>
    );
}
