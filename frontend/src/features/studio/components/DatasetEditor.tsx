"use client";

import { useState } from "react";

import { FileUp } from "lucide-react";

import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ApiRequestError } from "@/lib/api-client";
import { useUnsavedChangesWarning } from "@/lib/useUnsavedChangesWarning";

import { useSaveDataset } from "../hooks/useStudioDataset";
import { useSaveVariantDrafts, variantSaveErrorMessageOf } from "../hooks/useVariantCsv";
import { schemaErrors } from "../schema-editing";
import type {
    DatasetTable,
    PendingImport,
    SqlImportPreview,
    StudioDataset,
    UpdateDatasetRequest,
} from "../types";
import { draftKey, liveDrafts, type VariantDrafts, withoutDraft } from "../variant-drafts";
import { DatasetInfoForm, type DatasetInfoValues } from "./DatasetInfoForm";
import { DatasetVariants } from "./DatasetVariants";
import { PendingImportNotice } from "./PendingImportNotice";
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

function latestOf(first: string | null, second: string | null): string | null {
    return (
        [first, second]
            .filter((value) => value !== null)
            .sort()
            .at(-1) ?? null
    );
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
    const saveDrafts = useSaveVariantDrafts(dataset.id, (draft) =>
        setVariantDrafts((current) =>
            withoutDraft(current, draftKey(draft.variantId, draft.tableName)),
        ),
    );
    const changedInfo = infoChanges(info, dataset);
    const hasInfoChanges = Object.keys(changedInfo).length > 0;
    const hasSchemaChanges = JSON.stringify(tables) !== JSON.stringify(dataset.schema.tables);
    const hasDatasetChanges = hasInfoChanges || hasSchemaChanges || pendingImport !== null;
    const pendingDrafts = liveDrafts(variantDrafts, {
        ...dataset,
        schema: { ...dataset.schema, tables },
    });
    const hasChanges = hasDatasetChanges || pendingDrafts.length > 0;
    const clientErrors = { ...infoErrors(info), ...schemaErrors(tables) };
    const serverErrors = serverErrorsOf(save.error);
    const draftErrorMessage = variantSaveErrorMessageOf(saveDrafts.error, dataset);
    useUnsavedChangesWarning(hasChanges);

    function errorOf(path: string): string | undefined {
        return clientErrors[path] ?? serverErrors[path];
    }

    function saveVariantDrafts() {
        if (pendingDrafts.length > 0) {
            saveDrafts.save(pendingDrafts);
        }
    }

    function saveChanges() {
        if (!hasDatasetChanges) {
            saveVariantDrafts();
            return;
        }
        save.save(
            {
                info: hasInfoChanges ? changedInfo : null,
                tables: hasSchemaChanges ? tables : null,
                pendingImport,
            },
            {
                onSuccess: () => {
                    setPendingImport(null);
                    saveVariantDrafts();
                },
            },
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

    const importNotice = pendingImport ? (
        <PendingImportNotice pendingImport={pendingImport} onCancel={cancelImport} />
    ) : null;
    const saveBar = (
        <div className="sticky bottom-0 flex flex-col gap-2 rounded-lg border bg-card px-4 py-3">
            {draftErrorMessage ? (
                <p role="alert" className="text-[13px] text-destructive">
                    {draftErrorMessage}
                </p>
            ) : null}
            <SaveBar
                hasChanges={hasChanges}
                canSave={hasChanges && Object.keys(clientErrors).length === 0}
                isSaving={save.isSaving || saveDrafts.isSaving}
                savedAt={latestOf(save.savedAt, saveDrafts.savedAt)}
                onSave={saveChanges}
            />
        </div>
    );

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
                {importNotice}
                <DatasetInfoForm values={info} errorOf={errorOf} onChange={setInfo} />
                <SchemaEditor tables={tables} errorOf={errorOf} onChange={setTables} />
                {saveBar}
            </TabsContent>
            <TabsContent value="variants" className="flex flex-col gap-4">
                {importNotice}
                <DatasetVariants
                    dataset={dataset}
                    drafts={variantDrafts}
                    onDraftsChange={setVariantDrafts}
                />
                {saveBar}
            </TabsContent>
        </Tabs>
    );
}
