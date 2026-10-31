"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";

import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ApiRequestError } from "@/lib/api-client";

import { useCreateProblem } from "../hooks/useCreateProblem";
import { createProblemSchema, type CreateProblemValues } from "../schemas";

const TITLE_FIELD = "title";

interface CreateProblemDialogProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
}

function titleErrorOf(error: unknown): string | undefined {
    return error instanceof ApiRequestError
        ? error.errors.find((fieldError) => fieldError.field === TITLE_FIELD)?.message
        : undefined;
}

export function CreateProblemDialog({ open, onOpenChange }: CreateProblemDialogProps) {
    const showToast = useToast();
    const createProblem = useCreateProblem();
    const {
        register,
        handleSubmit,
        reset,
        setError,
        formState: { errors },
    } = useForm<CreateProblemValues>({
        resolver: zodResolver(createProblemSchema),
        defaultValues: { title: "" },
    });

    function changeOpen(nextOpen: boolean) {
        if (!nextOpen) {
            reset();
        }
        onOpenChange(nextOpen);
    }

    const submitCreate = (values: CreateProblemValues) =>
        createProblem
            .mutateAsync(values)
            .then((created) => {
                showToast(`Đã tạo bài nháp ${created.title}.`);
                changeOpen(false);
            })
            .catch((error: Error) => {
                const titleError = titleErrorOf(error);
                if (titleError) {
                    setError(TITLE_FIELD, { message: titleError });
                    return;
                }
                showToast(`Chưa tạo được bài. ${error.message}`, "error");
            });

    return (
        <Dialog open={open} onOpenChange={changeOpen}>
            <DialogContent>
                <DialogTitle>Tạo bài mới</DialogTitle>
                <DialogDescription>
                    Bài được lưu ở trạng thái nháp. Bạn soạn đề, dataset và đáp án sau.
                </DialogDescription>
                <form
                    noValidate
                    onSubmit={handleSubmit(submitCreate)}
                    className="flex flex-col gap-4"
                >
                    <div className="flex flex-col gap-1.5">
                        <Label htmlFor="problem-title">Tên bài</Label>
                        <Input
                            id="problem-title"
                            autoComplete="off"
                            placeholder="Ví dụ: Lương trung vị mỗi phòng ban"
                            aria-invalid={errors.title ? true : undefined}
                            aria-describedby={errors.title ? "problem-title-error" : undefined}
                            {...register(TITLE_FIELD)}
                        />
                        {errors.title ? (
                            <p id="problem-title-error" className="text-[13px] text-destructive">
                                {errors.title.message}
                            </p>
                        ) : null}
                    </div>
                    <div className="flex justify-end gap-2">
                        <Button type="button" variant="outline" onClick={() => changeOpen(false)}>
                            Huỷ
                        </Button>
                        <Button type="submit" disabled={createProblem.isPending}>
                            Tạo bài nháp
                        </Button>
                    </div>
                </form>
            </DialogContent>
        </Dialog>
    );
}
