"use client";

import Link from "next/link";

import { ArrowLeft } from "lucide-react";

import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { useToast } from "@/components/shared/ToastProvider";
import { ApiRequestError } from "@/lib/api-client";
import { formatTime } from "@/lib/format";
import { ROUTES } from "@/lib/routes";

import {
    useDatasetInfoAutosave,
    useDatasetSchemaAutosave,
    useStudioDataset,
} from "../hooks/useStudioDataset";
import { DatasetInfoForm } from "./DatasetInfoForm";
import { SchemaEditor } from "./SchemaEditor";

interface DatasetStudioProps {
    datasetId: string;
}

function isNotFound(error: Error | null): boolean {
    return error instanceof ApiRequestError && error.status === 404;
}

function latestOf(first: string | null, second: string | null): string | null {
    if (!first || !second) {
        return first ?? second;
    }
    return first > second ? first : second;
}

export function DatasetStudio({ datasetId }: DatasetStudioProps) {
    const showToast = useToast();
    const { data: dataset, error, isPending, refetch } = useStudioDataset(datasetId);
    const reportSaveError = (saveError: Error) => {
        if (!(saveError instanceof ApiRequestError && saveError.code === "VALIDATION_FAILED")) {
            showToast(`Chưa lưu được thay đổi. ${saveError.message}`, "error");
        }
    };
    const schemaAutosave = useDatasetSchemaAutosave(datasetId, reportSaveError);
    const infoAutosave = useDatasetInfoAutosave(datasetId, reportSaveError);
    const isSaving = schemaAutosave.isSaving || infoAutosave.isSaving;
    const savedAt = latestOf(schemaAutosave.savedAt, infoAutosave.savedAt);

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
                    <div className="flex flex-wrap items-end justify-between gap-3">
                        <h1 className="text-[22px] font-extrabold tracking-[-0.02em]">
                            {dataset.name}
                        </h1>
                        <span role="status" className="text-[13px] text-muted-foreground">
                            {isSaving ? "Đang lưu…" : null}
                            {!isSaving && savedAt ? `Đã lưu ${formatTime(savedAt)}` : null}
                        </span>
                    </div>
                    <DatasetInfoForm
                        key={`info-${dataset.id}`}
                        dataset={dataset}
                        saveError={infoAutosave.error}
                        onChange={infoAutosave.queueSave}
                    />
                    <SchemaEditor
                        key={`schema-${dataset.id}`}
                        initialTables={dataset.schema.tables}
                        saveError={schemaAutosave.error}
                        onValidChange={schemaAutosave.queueSave}
                        onInvalidChange={schemaAutosave.cancelSave}
                    />
                </>
            ) : null}
        </div>
    );
}
