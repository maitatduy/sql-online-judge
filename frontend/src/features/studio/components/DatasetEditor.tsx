"use client";

import { useState } from "react";

import { FileUp } from "lucide-react";

import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ApiRequestError } from "@/lib/api-client";
import { useUnsavedChangesWarning } from "@/lib/useUnsavedChangesWarning";

import { useSaveDataset } from "../hooks/useStudioDataset";
import { schemaErrors } from "../schema-editing";
import type {
    DatasetTable,
    PendingImport,
    SqlImportPreview,
    StudioDataset,
    UpdateDatasetRequest,
} from "../types";
import { liveDrafts, type VariantDrafts } from "../variant-drafts";
import { DatasetInfoForm, type DatasetInfoValues } from "./DatasetInfoForm";
import { DatasetVariants } from "./DatasetVariants";
import { SaveBar } from "./SaveBar";
import { SchemaEditor } from "./SchemaEditor";
import { SqlImportDialog } from "./SqlImportDialog";

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
    const [pendingImport, setPendingImport] = useState<PendingImport | null>(null);
    const [isImportOpen, setIsImportOpen] = useState(false);
    const [variantDrafts, setVariantDrafts] = useState<VariantDrafts>({});
    const save = useSaveDataset(dataset, (error) => {
        if (!(error instanceof ApiRequestError && error.code === "VALIDATION_FAILED")) {
            showToast(`Chưa lưu được thay đổi. ${error.message}`, "error");
        }
    });
    const changedInfo = infoChanges(info, dataset);
    const hasInfoChanges = Object.keys(changedInfo).length > 0;
    const hasSchemaChanges = JSON.stringify(tables) !== JSON.stringify(dataset.schema.tables);
    const hasChanges = hasInfoChanges || hasSchemaChanges || pendingImport !== null;
    const clientErrors = { ...infoErrors(info), ...schemaErrors(tables) };
    const serverErrors = serverErrorsOf(save.error);
    useUnsavedChangesWarning(hasChanges || liveDrafts(variantDrafts, dataset).length > 0);

    function errorOf(path: string): string | undefined {
        return clientErrors[path] ?? serverErrors[path];
    }

    function saveChanges() {
        save.save(
            {
                info: hasInfoChanges ? changedInfo : null,
                tables: hasSchemaChanges ? tables : null,
                pendingImport,
            },
            { onSuccess: () => setPendingImport(null) },
        );
    }

    function applyImport(preview: SqlImportPreview, imported: PendingImport | null) {
        if (preview.tables.length > 0) {
            setTables(preview.tables);
        }
        setPendingImport(imported);
    }

    function cancelImport() {
        setTables(dataset.schema.tables);
        setPendingImport(null);
    }

    return (
        <Tabs defaultValue="schema" className="gap-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
                <TabsList className="rounded-lg border">
                    <TabsTrigger value="schema">Thông tin và schema</TabsTrigger>
                    <TabsTrigger value="variants">Bộ dữ liệu</TabsTrigger>
                </TabsList>
                <Button type="button" variant="outline" onClick={() => setIsImportOpen(true)}>
                    <FileUp aria-hidden="true" />
                    Nhập từ file SQL
                </Button>
            </div>
            <SqlImportDialog
                open={isImportOpen}
                dataset={dataset}
                onOpenChange={setIsImportOpen}
                onApply={applyImport}
            />
            <TabsContent value="schema" className="flex flex-col gap-5">
                {pendingImport ? (
                    <div className="flex flex-wrap items-center justify-between gap-3 rounded-md bg-brand-soft px-3 py-2 text-[13px]">
                        <p>
                            Dữ liệu của {pendingImport.data.length} bảng sẽ được lưu vào bộ{" "}
                            {pendingImport.variantName} khi bấm Lưu.
                        </p>
                        <Button type="button" variant="outline" size="sm" onClick={cancelImport}>
                            Huỷ nhập
                        </Button>
                    </div>
                ) : null}
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
            </TabsContent>
            <TabsContent value="variants">
                <DatasetVariants
                    dataset={dataset}
                    drafts={variantDrafts}
                    onDraftsChange={setVariantDrafts}
                />
            </TabsContent>
        </Tabs>
    );
}
