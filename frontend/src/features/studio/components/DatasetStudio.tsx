"use client";

import Link from "next/link";

import { ArrowLeft } from "lucide-react";

import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { ApiRequestError } from "@/lib/api-client";
import { ROUTES } from "@/lib/routes";

import { useStudioDataset } from "../hooks/useStudioDataset";
import { DatasetEditor } from "./DatasetEditor";

interface DatasetStudioProps {
    datasetId: string;
}

function isNotFound(error: Error | null): boolean {
    return error instanceof ApiRequestError && error.status === 404;
}

export function DatasetStudio({ datasetId }: DatasetStudioProps) {
    const { data: dataset, error, isPending, refetch } = useStudioDataset(datasetId);

    return (
        <div className="mx-auto flex max-w-[1280px] flex-col gap-5 px-6 pt-7 pb-16">
            <Link
                href={ROUTES.studioDatasets}
                className="inline-flex w-fit items-center gap-1.5 text-[13px] font-semibold text-muted-foreground hover:text-foreground"
            >
                <ArrowLeft aria-hidden="true" className="size-4" />
                Quay lại Database của tôi
            </Link>
            {isPending ? (
                <div role="status" aria-label="Đang tải database" className="flex flex-col gap-3">
                    <Skeleton className="h-9 w-80" />
                    <Skeleton className="h-64" />
                </div>
            ) : null}
            {isNotFound(error) ? (
                <p className="rounded-lg border bg-card p-6 text-center font-bold">
                    Không tìm thấy database này.
                </p>
            ) : null}
            {error && !isNotFound(error) ? (
                <ErrorState
                    title="Không tải được database"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                />
            ) : null}
            {dataset ? (
                <>
                    <h1 className="text-[22px] font-extrabold tracking-[-0.02em]">
                        {dataset.name}
                    </h1>
                    <DatasetEditor key={dataset.id} dataset={dataset} />
                </>
            ) : null}
        </div>
    );
}
