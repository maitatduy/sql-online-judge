"use client";

import Link from "next/link";
import { useState } from "react";

import { NativeSelect } from "@/components/shared/NativeSelect";
import { useToast } from "@/components/shared/ToastProvider";
import { Label } from "@/components/ui/label";
import { ApiRequestError } from "@/lib/api-client";
import { ROUTES } from "@/lib/routes";
import { useUnsavedChangesWarning } from "@/lib/useUnsavedChangesWarning";

import { useChooseDataset, useDatasetOptions } from "../hooks/useProblemDataset";
import type { StudioProblem } from "../types";
import { DatasetSchemaPreview } from "./DatasetSchemaPreview";
import { SaveBar } from "./SaveBar";

const NOT_READY_CODE = "DATASET_NOT_READY";

interface DatasetStepProps {
    problem: StudioProblem;
    disabled: boolean;
}

function isNotReady(error: Error | null): error is ApiRequestError {
    return error instanceof ApiRequestError && error.code === NOT_READY_CODE;
}

export function DatasetStep({ problem, disabled }: DatasetStepProps) {
    const showToast = useToast();
    const [datasetId, setDatasetId] = useState(problem.dataset?.id ?? "");
    const { data: options, isPending: isLoadingOptions } = useDatasetOptions();
    const chosen = useChooseDataset(problem.id, (error) => {
        if (!isNotReady(error)) {
            showToast(`Chưa lưu được database. ${error.message}`, "error");
        }
    });
    const hasChanges = datasetId !== (problem.dataset?.id ?? "");
    useUnsavedChangesWarning(hasChanges);
    const hasNoDatasets = options !== undefined && options.items.length === 0;

    function handleChange(nextDatasetId: string) {
        chosen.reset();
        setDatasetId(nextDatasetId);
    }

    return (
        <section aria-labelledby="dataset-step-title" className="rounded-lg border bg-card p-6">
            <h2 id="dataset-step-title" className="text-base font-bold">
                Database
            </h2>
            <p className="mt-1 text-[13px] text-muted-foreground">
                Bài dùng database tại thời điểm bấm Lưu. Sửa database sau đó thì bấm Lưu lại để bài
                dùng dữ liệu mới.
            </p>
            <div className="mt-4 flex flex-col gap-1.5">
                <Label htmlFor="problem-dataset">Database của bài</Label>
                <NativeSelect
                    id="problem-dataset"
                    className="w-full max-w-sm"
                    value={datasetId}
                    disabled={disabled || isLoadingOptions}
                    aria-invalid={isNotReady(chosen.error) ? true : undefined}
                    aria-describedby={isNotReady(chosen.error) ? "dataset-error" : undefined}
                    onChange={(event) => handleChange(event.target.value)}
                >
                    <option value="" disabled>
                        Chưa chọn database
                    </option>
                    {options?.items.map((option) => (
                        <option key={option.id} value={option.id}>
                            {option.name}
                        </option>
                    ))}
                </NativeSelect>
                {isNotReady(chosen.error) ? (
                    <p id="dataset-error" role="alert" className="text-[13px] text-destructive">
                        {chosen.error.message}
                    </p>
                ) : null}
                {hasNoDatasets ? (
                    <p className="text-[13px] text-muted-foreground">
                        Bạn chưa có database nào.{" "}
                        <Link
                            href={ROUTES.studioDatasets}
                            className="font-semibold text-brand hover:underline"
                        >
                            Tạo database
                        </Link>
                    </p>
                ) : null}
            </div>
            {datasetId ? (
                <div className="mt-4">
                    <DatasetSchemaPreview datasetId={datasetId} />
                </div>
            ) : null}
            {disabled ? null : (
                <div className="mt-5 border-t pt-4">
                    <SaveBar
                        hasChanges={hasChanges}
                        canSave={datasetId !== ""}
                        isSaving={chosen.isSaving}
                        savedAt={chosen.savedAt}
                        onSave={() => chosen.choose(datasetId)}
                    />
                </div>
            )}
        </section>
    );
}
