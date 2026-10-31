"use client";

import Link from "next/link";

import { ArrowLeft } from "lucide-react";

import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import { ApiRequestError } from "@/lib/api-client";
import { formatTime } from "@/lib/format";
import { ROUTES } from "@/lib/routes";

import {
    useCreateDatasetDraft,
    useDatasetSchemaAutosave,
    useStudioDataset,
} from "../hooks/useStudioDataset";
import { SchemaEditor } from "./SchemaEditor";

interface DatasetStudioProps {
    datasetId: string;
}

function isNotFound(error: Error | null): boolean {
    return error instanceof ApiRequestError && error.status === 404;
}

export function DatasetStudio({ datasetId }: DatasetStudioProps) {
    const showToast = useToast();
    const { data: dataset, error, isPending, refetch } = useStudioDataset(datasetId);
    const createDraft = useCreateDatasetDraft(datasetId);
    const autosave = useDatasetSchemaAutosave(datasetId, (saveError) => {
        if (!(saveError instanceof ApiRequestError && saveError.code === "VALIDATION_FAILED")) {
            showToast(`Chưa lưu được schema. ${saveError.message}`, "error");
        }
    });

    function startDraft() {
        createDraft.mutate(undefined, {
            onError: (draftError) =>
                showToast(`Chưa tạo được bản nháp. ${draftError.message}`, "error"),
        });
    }

    return (
        <div className="mx-auto flex max-w-[1280px] flex-col gap-5 px-6 pt-7 pb-16">
            <Link
                href={ROUTES.studioDatasets}
                className="inline-flex w-fit items-center gap-1.5 text-[13px] font-semibold text-muted-foreground hover:text-foreground"
            >
                <ArrowLeft aria-hidden="true" className="size-4" />
                Quay lại Dataset của tôi
            </Link>
            {isPending ? (
                <div role="status" aria-label="Đang tải dataset" className="flex flex-col gap-3">
                    <Skeleton className="h-9 w-80" />
                    <Skeleton className="h-64" />
                </div>
            ) : null}
            {isNotFound(error) ? (
                <p className="rounded-lg border bg-card p-6 text-center font-bold">
                    Không tìm thấy dataset này.
                </p>
            ) : null}
            {error && !isNotFound(error) ? (
                <ErrorState
                    title="Không tải được dataset"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                />
            ) : null}
            {dataset ? (
                <>
                    <div className="flex flex-wrap items-end justify-between gap-3">
                        <div>
                            <h1 className="text-[22px] font-extrabold tracking-[-0.02em]">
                                {dataset.name}
                            </h1>
                            {dataset.description ? (
                                <p className="mt-1 text-muted-foreground">{dataset.description}</p>
                            ) : null}
                        </div>
                        <div className="flex items-center gap-3 text-[13px] text-muted-foreground">
                            <span className="rounded-full bg-surface-2 px-2.5 py-0.5 text-xs font-semibold text-foreground">
                                Phiên bản {dataset.schema.versionNumber}
                                {dataset.schema.isDraft ? " · Nháp" : ""}
                            </span>
                            <span role="status">
                                {autosave.isSaving ? "Đang lưu…" : null}
                                {!autosave.isSaving && autosave.savedAt
                                    ? `Bản nháp · lưu ${formatTime(autosave.savedAt)}`
                                    : null}
                            </span>
                        </div>
                    </div>
                    {dataset.schema.isDraft ? null : (
                        <div className="flex flex-wrap items-center justify-between gap-3 rounded-md bg-surface-2 px-3 py-2 text-[13px]">
                            <p>
                                Phiên bản {dataset.schema.versionNumber} đã chốt nên không sửa được.
                            </p>
                            <Button
                                type="button"
                                size="sm"
                                disabled={createDraft.isPending}
                                onClick={startDraft}
                            >
                                Tạo bản nháp mới
                            </Button>
                        </div>
                    )}
                    <SchemaEditor
                        key={dataset.schema.versionNumber}
                        initialTables={dataset.schema.tables}
                        disabled={!dataset.schema.isDraft}
                        saveError={autosave.error}
                        onValidChange={autosave.queueSave}
                        onInvalidChange={autosave.cancelSave}
                    />
                </>
            ) : null}
        </div>
    );
}
