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

interface SqlImportDialogProps {
    open: boolean;
    dataset: StudioDataset;
    onOpenChange: (open: boolean) => void;
    onApply: (preview: SqlImportPreview, pendingImport: PendingImport | null) => void;
}

function errorMessageOf(error: Error | null): string | null {
    if (!error) {
        return null;
    }
    return error instanceof ApiRequestError && error.errors.length > 0
        ? error.errors[0].message
        : error.message;
}

function defaultNewName(dataset: StudioDataset): string {
    return dataset.variants.some((variant) => variant.name === "Mẫu") ? "Từ file SQL" : "Mẫu";
}

export function SqlImportDialog({ open, dataset, onOpenChange, onApply }: SqlImportDialogProps) {
    const parse = useParseSql();
    const [sql, setSql] = useState("");
    const [target, setTarget] = useState(dataset.variants[0]?.id ?? NEW_VARIANT);
    const [newName, setNewName] = useState(() => defaultNewName(dataset));
    const preview = parse.data;
    const isNewVariant = target === NEW_VARIANT;

    function changeOpen(nextOpen: boolean) {
        if (!nextOpen) {
            setSql("");
            parse.reset();
        }
        onOpenChange(nextOpen);
    }

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
        changeOpen(false);
    }

    return (
        <Dialog open={open} onOpenChange={changeOpen}>
            <DialogContent className="sm:max-w-2xl">
                <DialogTitle>Nhập từ file SQL</DialogTitle>
                <DialogDescription>
                    Chỉ nhận câu CREATE TABLE và INSERT. File chỉ được đọc để xem trước, chưa có gì
                    được lưu cho tới khi bạn bấm Lưu.
                </DialogDescription>
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
                                    onChange={(event) => setNewName(event.target.value)}
                                />
                            </div>
                        ) : null}
                    </div>
                ) : null}
                <div className="flex justify-end gap-2">
                    <Button type="button" variant="outline" onClick={() => changeOpen(false)}>
                        Huỷ
                    </Button>
                    {preview ? (
                        <Button
                            type="button"
                            disabled={
                                isNewVariant && preview.data.length > 0 && newName.trim() === ""
                            }
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
            </DialogContent>
        </Dialog>
    );
}
