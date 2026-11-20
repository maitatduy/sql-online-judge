"use client";

import { NativeSelect } from "@/components/shared/NativeSelect";
import { Label } from "@/components/ui/label";

import { useDatasetChoice, useDatasetOptions } from "../hooks/useLessonCells";

const SELECT_CLASS = "h-10 w-full bg-card text-base";

interface DatasetVariantPickerProps {
    idPrefix: string;
    datasetId: string;
    variantId: string;
    onDatasetChange: (datasetId: string) => void;
    onVariantChange: (variantId: string) => void;
}

export function DatasetVariantPicker({
    idPrefix,
    datasetId,
    variantId,
    onDatasetChange,
    onVariantChange,
}: DatasetVariantPickerProps) {
    const datasets = useDatasetOptions(true);
    const choice = useDatasetChoice(datasetId);
    const publicVariants = choice.data?.variants.filter((variant) => variant.isPublic) ?? [];

    return (
        <div className="grid gap-3 sm:grid-cols-2">
            <div className="flex flex-col gap-1.5">
                <Label htmlFor={`${idPrefix}-dataset`} className="text-sm">
                    Database
                </Label>
                <NativeSelect
                    id={`${idPrefix}-dataset`}
                    value={datasetId}
                    disabled={!datasets.data}
                    onChange={(event) => onDatasetChange(event.target.value)}
                    className={SELECT_CLASS}
                >
                    <option value="">Chọn database</option>
                    {datasets.data?.items.map((dataset) => (
                        <option key={dataset.id} value={dataset.id}>
                            {dataset.name}
                        </option>
                    ))}
                </NativeSelect>
            </div>
            {choice.data?.schema.isDraft ? (
                <p className="self-end text-sm text-destructive">
                    Database còn bản nháp chưa chốt. Hãy chốt trước khi gắn.
                </p>
            ) : null}
            {choice.data && !choice.data.schema.isDraft && publicVariants.length === 0 ? (
                <p className="self-end text-sm text-muted-foreground">
                    Database chưa có bộ dữ liệu công khai.
                </p>
            ) : null}
            {choice.data && !choice.data.schema.isDraft && publicVariants.length > 0 ? (
                <div className="flex flex-col gap-1.5">
                    <Label htmlFor={`${idPrefix}-variant`} className="text-sm">
                        Bộ dữ liệu
                    </Label>
                    <NativeSelect
                        id={`${idPrefix}-variant`}
                        value={variantId}
                        onChange={(event) => onVariantChange(event.target.value)}
                        className={SELECT_CLASS}
                    >
                        <option value="">Chọn bộ dữ liệu</option>
                        {publicVariants.map((variant) => (
                            <option key={variant.id} value={variant.id}>
                                {variant.name}
                            </option>
                        ))}
                    </NativeSelect>
                </div>
            ) : null}
        </div>
    );
}
