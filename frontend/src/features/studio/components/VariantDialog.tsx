"use client";

import { useEffect } from "react";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";

import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ApiRequestError } from "@/lib/api-client";

import { datasetVariantSchema, type DatasetVariantValues } from "../schemas";

interface VariantDialogProps {
    open: boolean;
    title: string;
    submitLabel: string;
    initialValues: DatasetVariantValues;
    isPending: boolean;
    onOpenChange: (open: boolean) => void;
    onSubmit: (values: DatasetVariantValues) => Promise<unknown>;
}

function nameErrorOf(error: unknown): string | undefined {
    if (!(error instanceof ApiRequestError)) {
        return undefined;
    }
    if (error.code === "DATASET_VARIANT_NAME_TAKEN") {
        return error.message;
    }
    return error.errors.find((fieldError) => fieldError.field === "name")?.message;
}

export function VariantDialog({
    open,
    title,
    submitLabel,
    initialValues,
    isPending,
    onOpenChange,
    onSubmit,
}: VariantDialogProps) {
    const {
        register,
        handleSubmit,
        reset,
        setError,
        formState: { errors },
    } = useForm<DatasetVariantValues>({
        resolver: zodResolver(datasetVariantSchema),
        defaultValues: initialValues,
    });

    useEffect(() => {
        if (open) {
            reset(initialValues);
        }
    }, [open, initialValues, reset]);

    const submit = (values: DatasetVariantValues) =>
        onSubmit(values)
            .then(() => onOpenChange(false))
            .catch((error: unknown) => {
                setError("name", {
                    message: nameErrorOf(error) ?? "Chưa lưu được bộ dữ liệu, bạn thử lại nhé",
                });
            });

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent>
                <DialogTitle>{title}</DialogTitle>
                <DialogDescription>
                    Bộ công khai hiện trong đề và dùng khi chạy thử. Bộ ẩn chỉ dùng khi chấm.
                </DialogDescription>
                <form noValidate onSubmit={handleSubmit(submit)} className="flex flex-col gap-4">
                    <div className="flex flex-col gap-1.5">
                        <Label htmlFor="variant-name">Tên bộ dữ liệu</Label>
                        <Input
                            id="variant-name"
                            autoComplete="off"
                            placeholder="Ví dụ: Có NULL"
                            aria-invalid={errors.name ? true : undefined}
                            aria-describedby={errors.name ? "variant-name-error" : undefined}
                            {...register("name")}
                        />
                        {errors.name ? (
                            <p id="variant-name-error" className="text-[13px] text-destructive">
                                {errors.name.message}
                            </p>
                        ) : null}
                    </div>
                    <label className="flex items-center gap-2 text-sm">
                        <input type="checkbox" {...register("isPublic")} />
                        Công khai cho học viên
                    </label>
                    <div className="flex justify-end gap-2">
                        <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                            Huỷ
                        </Button>
                        <Button type="submit" disabled={isPending}>
                            {submitLabel}
                        </Button>
                    </div>
                </form>
            </DialogContent>
        </Dialog>
    );
}
