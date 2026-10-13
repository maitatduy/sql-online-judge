"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useId, useState } from "react";
import { useForm, useWatch } from "react-hook-form";

import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import {
    Dialog,
    DialogClose,
    DialogContent,
    DialogDescription,
    DialogTitle,
} from "@/components/ui/dialog";
import { ApiRequestError } from "@/lib/api-client";

import { useRestartWorker } from "../hooks/useRestartWorker";
import { restartWorkerSchema, type RestartWorkerValues } from "../schemas";

const REASON_FIELD = "reason";

interface RestartWorkerDialogProps {
    workerName: string;
    onClose: () => void;
}

function reasonErrorFrom(error: unknown): string | undefined {
    return error instanceof ApiRequestError
        ? error.errors.find((fieldError) => fieldError.field === REASON_FIELD)?.message
        : undefined;
}

export function RestartWorkerDialog({ workerName, onClose }: RestartWorkerDialogProps) {
    const reasonId = useId();
    const reasonErrorId = useId();
    const [opener] = useState(() => document.activeElement);
    const showToast = useToast();
    const restartWorker = useRestartWorker();
    const {
        control,
        register,
        handleSubmit,
        setError,
        formState: { errors },
    } = useForm<RestartWorkerValues>({
        resolver: zodResolver(restartWorkerSchema),
        defaultValues: { reason: "" },
    });
    const reason = useWatch({ control, name: REASON_FIELD });
    const reasonError = errors.reason?.message;

    const submitRestart = (values: RestartWorkerValues) => {
        restartWorker.mutate(
            { name: workerName, reason: values.reason },
            {
                onSuccess: onClose,
                onError: (error) => {
                    const serverReasonError = reasonErrorFrom(error);
                    if (serverReasonError) {
                        setError(REASON_FIELD, { message: serverReasonError });
                        return;
                    }
                    showToast(`Chưa khởi động lại được ${workerName}. ${error.message}`, "error");
                },
            },
        );
    };

    return (
        <Dialog
            open
            onOpenChange={(isOpen) => {
                if (!isOpen) {
                    onClose();
                }
            }}
        >
            <DialogContent
                onCloseAutoFocus={(event) => {
                    event.preventDefault();
                    if (opener instanceof HTMLElement && opener.isConnected) {
                        opener.focus();
                    }
                }}
            >
                <div>
                    <DialogTitle>Khởi động lại {workerName}?</DialogTitle>
                    <DialogDescription>
                        Bài đang chờ sẽ chuyển sang worker khác, không bài nào bị mất.
                    </DialogDescription>
                </div>
                <form
                    noValidate
                    onSubmit={handleSubmit(submitRestart)}
                    className="flex flex-col gap-4"
                >
                    <div>
                        <label
                            htmlFor={reasonId}
                            className="mb-1.5 block text-[13px] font-semibold"
                        >
                            Lý do (ghi vào nhật ký thao tác)
                        </label>
                        <textarea
                            id={reasonId}
                            rows={3}
                            aria-invalid={reasonError ? true : undefined}
                            aria-describedby={reasonError ? reasonErrorId : undefined}
                            className="w-full rounded-md border border-input bg-transparent px-2.5 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 aria-invalid:border-destructive"
                            {...register(REASON_FIELD)}
                        />
                        {reasonError ? (
                            <p id={reasonErrorId} className="mt-1 text-[13px] text-destructive">
                                {reasonError}
                            </p>
                        ) : null}
                    </div>
                    <div className="flex justify-end gap-2">
                        <DialogClose asChild>
                            <Button type="button" variant="outline">
                                Huỷ
                            </Button>
                        </DialogClose>
                        <Button
                            type="submit"
                            variant="destructive"
                            disabled={reason.trim() === "" || restartWorker.isPending}
                        >
                            Khởi động lại
                        </Button>
                    </div>
                </form>
            </DialogContent>
        </Dialog>
    );
}
