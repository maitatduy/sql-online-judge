"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";

import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ApiRequestError } from "@/lib/api-client";

import { useCreateDataset } from "../hooks/useMyDatasets";
import { createDatasetSchema, type CreateDatasetValues } from "../schemas";

type DatasetField = keyof CreateDatasetValues;

const FIELDS: readonly DatasetField[] = ["name", "description"];

interface CreateDatasetDialogProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
}

export function CreateDatasetDialog({ open, onOpenChange }: CreateDatasetDialogProps) {
    const showToast = useToast();
    const createDataset = useCreateDataset();
    const {
        register,
        handleSubmit,
        reset,
        setError,
        formState: { errors },
    } = useForm<CreateDatasetValues>({
        resolver: zodResolver(createDatasetSchema),
        defaultValues: { name: "", description: "" },
    });

    function changeOpen(nextOpen: boolean) {
        if (!nextOpen) {
            reset();
        }
        onOpenChange(nextOpen);
    }

    function applyFieldErrors(error: Error): boolean {
        if (!(error instanceof ApiRequestError)) {
            return false;
        }
        const fieldErrors = error.errors.filter((fieldError) =>
            FIELDS.includes(fieldError.field as DatasetField),
        );
        fieldErrors.forEach((fieldError) =>
            setError(fieldError.field as DatasetField, { message: fieldError.message }),
        );
        return fieldErrors.length > 0;
    }

    const submitCreate = (values: CreateDatasetValues) =>
        createDataset
            .mutateAsync(values)
            .then((created) => {
                showToast(`Đã tạo dataset ${created.name}.`);
                changeOpen(false);
            })
            .catch((error: Error) => {
                if (!applyFieldErrors(error)) {
                    showToast(`Chưa tạo được dataset. ${error.message}`, "error");
                }
            });

    function describedBy(field: DatasetField) {
        return errors[field]
            ? { "aria-invalid": true, "aria-describedby": `dataset-${field}-error` }
            : {};
    }

    function fieldError(field: DatasetField) {
        return errors[field] ? (
            <p id={`dataset-${field}-error`} className="text-[13px] text-destructive">
                {errors[field].message}
            </p>
        ) : null;
    }

    return (
        <Dialog open={open} onOpenChange={changeOpen}>
            <DialogContent>
                <DialogTitle>Tạo dataset</DialogTitle>
                <DialogDescription>
                    Dataset gồm schema và các bộ dữ liệu, dùng chung cho nhiều bài.
                </DialogDescription>
                <form
                    noValidate
                    onSubmit={handleSubmit(submitCreate)}
                    className="flex flex-col gap-4"
                >
                    <div className="flex flex-col gap-1.5">
                        <Label htmlFor="dataset-name">Tên dataset</Label>
                        <Input
                            id="dataset-name"
                            autoComplete="off"
                            placeholder="Ví dụ: Công ty"
                            {...describedBy("name")}
                            {...register("name")}
                        />
                        {fieldError("name")}
                    </div>
                    <div className="flex flex-col gap-1.5">
                        <Label htmlFor="dataset-description">Mô tả</Label>
                        <textarea
                            id="dataset-description"
                            rows={3}
                            placeholder="Ví dụ: Phòng ban và nhân viên kèm lương"
                            className="rounded-md border border-input bg-transparent px-3 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50"
                            {...describedBy("description")}
                            {...register("description")}
                        />
                        {fieldError("description")}
                    </div>
                    <div className="flex justify-end gap-2">
                        <Button type="button" variant="outline" onClick={() => changeOpen(false)}>
                            Huỷ
                        </Button>
                        <Button type="submit" disabled={createDataset.isPending}>
                            Tạo dataset
                        </Button>
                    </div>
                </form>
            </DialogContent>
        </Dialog>
    );
}
