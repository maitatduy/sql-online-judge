"use client";

import { useState } from "react";

import { FileUp } from "lucide-react";

import { NativeSelect } from "@/components/shared/NativeSelect";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ApiRequestError } from "@/lib/api-client";

import { useParseSql } from "../hooks/useParseSql";
import type { PendingImport, SqlImportPreview, StudioDataset } from "../types";
import { SqlImportPreviewList } from "./SqlImportPreviewList";

const NEW_VARIANT = "__new__";
const FIRST_VARIANT_NAME = "Mẫu";
const IMPORTED_VARIANT_NAME = "Từ file SQL";
const DUPLICATE_NAME_MESSAGE = "Đã có bộ dữ liệu tên này";

interface SqlImportDialogProps {
    open: boolean;
    dataset: StudioDataset;
    onOpenChange: (open: boolean) => void;
    onApply: (preview: SqlImportPreview, pendingImport: PendingImport | null) => void;
}

interface SqlImportFormProps {
    dataset: StudioDataset;
    onApply: (preview: SqlImportPreview, pendingImport: PendingImport | null) => void;
    onClose: () => void;
}

function errorMessageOf(error: Error | null): string | null {
    if (!error) {
        return null;
    }
    return error instanceof ApiRequestError && error.errors.length > 0
        ? error.errors[0].message
        : error.message;
}

function hasVariantNamed(dataset: StudioDataset, name: string): boolean {
    return dataset.variants.some((variant) => variant.name === name.trim());
}

function defaultNewName(dataset: StudioDataset): string {
    if (!hasVariantNamed(dataset, FIRST_VARIANT_NAME)) {
        return FIRST_VARIANT_NAME;
    }
    let suffix = 1;
    let name = IMPORTED_VARIANT_NAME;
    while (hasVariantNamed(dataset, name)) {
        suffix += 1;
        name = `${IMPORTED_VARIANT_NAME} ${suffix}`;
    }
    return name;
}

function SqlImportForm({ dataset, onApply, onClose }: SqlImportFormProps) {
    const parse = useParseSql();
    const [sql, setSql] = useState("");
    const [target, setTarget] = useState(dataset.variants[0]?.id ?? NEW_VARIANT);
    const [newName, setNewName] = useState(() => defaultNewName(dataset));
    const preview = parse.data;
    const isNewVariant = target === NEW_VARIANT;
    const createsVariant = isNewVariant && preview !== undefined && preview.data.length > 0;
    const nameError = createsVariant && hasVariantNamed(dataset, newName);

    async function loadFile(file: File | undefined) {
        if (file) {
            setSql(await file.text());
            parse.reset();
        }
    }

    function apply() {
        if (!preview) {
            return;
        }
        const variantName = isNewVariant
            ? newName.trim()
            : (dataset.variants.find((variant) => variant.id === target)?.name ?? "");
        onApply(
            preview,
            preview.data.length > 0
                ? { data: preview.data, variantName, createsVariant: isNewVariant }
                : null,
        );
        onClose();
    }

    return (
        <>
            <label className="inline-flex h-9 w-fit cursor-pointer items-center gap-1.5 rounded-md border px-3 text-sm font-medium hover:bg-surface-2 focus-within:ring-2 focus-within:ring-ring">
                <FileUp aria-hidden="true" className="size-4" />
                Chọn file .sql
                <input
                    type="file"
                    accept=".sql,text/plain,application/sql"
                    className="sr-only"
                    onChange={(event) => void loadFile(event.target.files?.[0])}
                />
            </label>
            <textarea
                aria-label="Nội dung SQL"
                value={sql}
                rows={8}
                spellCheck={false}
                placeholder="Hoặc dán nội dung SQL vào đây"
                onChange={(event) => {
                    setSql(event.target.value);
                    parse.reset();
                }}
                className="rounded-md border border-input bg-transparent px-3 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50"
            />
            {errorMessageOf(parse.error) ? (
                <p role="alert" className="text-[13px] text-destructive">
                    {errorMessageOf(parse.error)}
                </p>
            ) : null}
            {preview ? <SqlImportPreviewList preview={preview} /> : null}
            {preview && preview.data.length > 0 ? (
                <div className="grid gap-3 sm:grid-cols-2">
                    <div className="flex flex-col gap-1.5">
                        <Label htmlFor="sql-import-target">Đưa dữ liệu vào bộ</Label>
                        <NativeSelect
                            id="sql-import-target"
                            value={target}
                            onChange={(event) => setTarget(event.target.value)}
                        >
                            {dataset.variants.map((variant) => (
                                <option key={variant.id} value={variant.id}>
                                    {variant.name}
                                </option>
                            ))}
                            <option value={NEW_VARIANT}>Tạo bộ dữ liệu mới</option>
                        </NativeSelect>
                    </div>
                    {isNewVariant ? (
                        <div className="flex flex-col gap-1.5">
                            <Label htmlFor="sql-import-new-name">Tên bộ dữ liệu mới</Label>
                            <Input
                                id="sql-import-new-name"
                                value={newName}
                                autoComplete="off"
                                aria-invalid={nameError ? true : undefined}
                                aria-describedby={
                                    nameError ? "sql-import-new-name-error" : undefined
                                }
                                onChange={(event) => setNewName(event.target.value)}
                            />
                            {nameError ? (
                                <p
                                    id="sql-import-new-name-error"
                                    className="text-[13px] text-destructive"
                                >
                                    {DUPLICATE_NAME_MESSAGE}
                                </p>
                            ) : null}
                        </div>
                    ) : null}
                </div>
            ) : null}
            <div className="flex justify-end gap-2">
                <Button type="button" variant="outline" onClick={onClose}>
                    Huỷ
                </Button>
                {preview ? (
                    <Button
                        type="button"
                        disabled={createsVariant && (newName.trim() === "" || nameError)}
                        onClick={apply}
                    >
                        Áp dụng
                    </Button>
                ) : (
                    <Button
                        type="button"
                        disabled={sql.trim() === "" || parse.isPending}
                        onClick={() => parse.mutate(sql)}
                    >
                        Đọc file
                    </Button>
                )}
            </div>
        </>
    );
}

export function SqlImportDialog({ open, dataset, onOpenChange, onApply }: SqlImportDialogProps) {
    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="sm:max-w-2xl">
                <DialogTitle>Nhập từ file SQL</DialogTitle>
                <DialogDescription>
                    Chỉ nhận câu CREATE TABLE và INSERT. File chỉ được đọc để xem trước, chưa có gì
                    được lưu cho tới khi bạn bấm Lưu.
                </DialogDescription>
                <SqlImportForm
                    dataset={dataset}
                    onApply={onApply}
                    onClose={() => onOpenChange(false)}
                />
            </DialogContent>
        </Dialog>
    );
}
