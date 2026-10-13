"use client";

import { CircleCheck } from "lucide-react";

import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { ApiRequestError } from "@/lib/api-client";

import { usePendingTasks } from "../hooks/usePendingTasks";
import { PendingTaskRow } from "./PendingTaskRow";

const SKELETON_ROW_COUNT = 4;

function PendingTasksSkeleton() {
    return (
        <div role="status" aria-busy="true" aria-label="Đang tải việc cần xử lý">
            {Array.from({ length: SKELETON_ROW_COUNT }, (_, index) => (
                <div key={index} className="flex flex-col gap-2 border-t px-4.5 py-3.5">
                    <Skeleton className="h-3.5 w-1/2" />
                    <Skeleton className="h-2.5 w-4/5" />
                </div>
            ))}
        </div>
    );
}

function PendingTasksBody() {
    const { data: tasks, error, isPending, refetch } = usePendingTasks();

    if (isPending) {
        return <PendingTasksSkeleton />;
    }

    if (!tasks) {
        return (
            <ErrorState
                title="Chưa tải được việc cần xử lý"
                message={error.message}
                traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                onRetry={() => void refetch()}
                className="rounded-none border-x-0 border-b-0"
            />
        );
    }

    const openTasks = tasks.filter((task) => task.count > 0);

    if (openTasks.length === 0) {
        return (
            <EmptyState
                icon={<CircleCheck />}
                title="Không còn việc nào cần xử lý"
                className="rounded-none border-x-0 border-b-0 border-solid"
            />
        );
    }

    return (
        <ul>
            {openTasks.map((task) => (
                <li key={task.type}>
                    <PendingTaskRow task={task} />
                </li>
            ))}
        </ul>
    );
}

export function PendingTasksCard() {
    return (
        <section className="min-w-0 flex-[1_1_320px] overflow-hidden rounded-lg border bg-card">
            <h2 className="px-4.5 py-4 text-base font-bold">Cần xử lý</h2>
            <PendingTasksBody />
        </section>
    );
}
