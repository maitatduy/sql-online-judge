"use client";

import { useState } from "react";

import { useToast } from "@/components/shared/ToastProvider";
import { ApiRequestError } from "@/lib/api-client";
import { useUnsavedChangesWarning } from "@/lib/useUnsavedChangesWarning";

import { useSaveDataset } from "../hooks/useStudioDataset";
import { schemaErrors } from "../schema-editing";
import type { DatasetTable, StudioDataset, UpdateDatasetRequest } from "../types";
import { DatasetInfoForm, type DatasetInfoValues } from "./DatasetInfoForm";
import { SaveBar } from "./SaveBar";
import { SchemaEditor } from "./SchemaEditor";

const MAX_NAME_LENGTH = 120;
const MAX_DESCRIPTION_LENGTH = 500;

interface DatasetEditorProps {
    dataset: StudioDataset;
}

function infoErrors({ name, description }: DatasetInfoValues): Record<string, string> {
    return {
        ...(name.trim() === "" ? { name: "Không được để trống" } : {}),
        ...(name.length > MAX_NAME_LENGTH ? { name: "Tối đa 120 ký tự" } : {}),
        ...(description.length > MAX_DESCRIPTION_LENGTH ? { description: "Tối đa 500 ký tự" } : {}),
    };
}

function infoChanges(values: DatasetInfoValues, dataset: StudioDataset): UpdateDatasetRequest {
    const name = values.name.trim();
    const description = values.description.trim();
    return {
        ...(name !== dataset.name ? { name } : {}),
        ...(description !== dataset.description ? { description } : {}),
    };
}

function serverErrorsOf(error: Error | null): Record<string, string> {
    return error instanceof ApiRequestError
        ? Object.fromEntries(
              error.errors.map((fieldError) => [fieldError.field, fieldError.message]),
          )
        : {};
}

export function DatasetEditor({ dataset }: DatasetEditorProps) {
    const showToast = useToast();
    const [info, setInfo] = useState<DatasetInfoValues>({
        name: dataset.name,
        description: dataset.description,
    });
    const [tables, setTables] = useState<DatasetTable[]>(dataset.schema.tables);
    const save = useSaveDataset(dataset.id, (error) => {
        if (!(error instanceof ApiRequestError && error.code === "VALIDATION_FAILED")) {
            showToast(`Chưa lưu được thay đổi. ${error.message}`, "error");
        }
    });
    const changedInfo = infoChanges(info, dataset);
    const hasInfoChanges = Object.keys(changedInfo).length > 0;
    const hasSchemaChanges = JSON.stringify(tables) !== JSON.stringify(dataset.schema.tables);
    const hasChanges = hasInfoChanges || hasSchemaChanges;
    const clientErrors = { ...infoErrors(info), ...schemaErrors(tables) };
    const serverErrors = serverErrorsOf(save.error);
    useUnsavedChangesWarning(hasChanges);

    function errorOf(path: string): string | undefined {
        return clientErrors[path] ?? serverErrors[path];
    }

    function saveChanges() {
        save.save({
            info: hasInfoChanges ? changedInfo : null,
            tables: hasSchemaChanges ? tables : null,
        });
    }

    return (
        <>
            <DatasetInfoForm values={info} errorOf={errorOf} onChange={setInfo} />
            <SchemaEditor tables={tables} errorOf={errorOf} onChange={setTables} />
            <div className="sticky bottom-0 rounded-lg border bg-card px-4 py-3">
                <SaveBar
                    hasChanges={hasChanges}
                    canSave={hasChanges && Object.keys(clientErrors).length === 0}
                    isSaving={save.isSaving}
                    savedAt={save.savedAt}
                    onSave={saveChanges}
                />
            </div>
        </>
    );
}
