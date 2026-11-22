"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";

import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";
import { ApiRequestError } from "@/lib/api-client";
import { ADMIN_ROUTES } from "@/lib/admin-navigation";

import {
    createContestSchema,
    toCreateContestRequest,
    type CreateContestValues,
} from "../../contests/schemas";
import { useCreateContest } from "../../hooks/useAdminContests";
import { TextAreaField } from "../users/TextAreaField";
import { TextInputField } from "../users/TextInputField";

const FIELDS = [
    "slug",
    "title",
    "description",
    "startAt",
    "durationMinutes",
    "freezeMinutes",
    "penaltyMinutes",
] as const;

const DEFAULT_VALUES: CreateContestValues = {
    slug: "",
    title: "",
    description: "",
    startAt: "",
    durationMinutes: 90,
    freezeMinutes: 30,
    penaltyMinutes: 20,
    isRated: true,
};

interface CreateContestDialogProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
}

function serverFieldError(error: unknown, field: string): string | undefined {
    if (!(error instanceof ApiRequestError)) {
        return undefined;
    }
    if (field === "slug" && error.code === "SLUG_TAKEN") {
        return error.message;
    }
    return error.errors.find((fieldError) => fieldError.field === field)?.message;
}

export function CreateContestDialog({ open, onOpenChange }: CreateContestDialogProps) {
    const router = useRouter();
    const showToast = useToast();
    const createContest = useCreateContest();
    const {
        register,
        handleSubmit,
        reset,
        setError,
        formState: { errors },
    } = useForm<CreateContestValues>({
        resolver: zodResolver(createContestSchema),
        defaultValues: DEFAULT_VALUES,
    });

    function changeOpen(nextOpen: boolean) {
        if (!nextOpen) {
            reset();
        }
        onOpenChange(nextOpen);
    }

    const submitCreate = (values: CreateContestValues) =>
        createContest
            .mutateAsync(toCreateContestRequest(values))
            .then((created) => {
                showToast(`Đã tạo contest ${created.title}.`);
                router.push(ADMIN_ROUTES.contest(created.id));
            })
            .catch((error: Error) => {
                const fieldErrors = FIELDS.flatMap((field) => {
                    const message = serverFieldError(error, field);
                    return message ? [{ field, message }] : [];
                });
                fieldErrors.forEach(({ field, message }) => setError(field, { message }));
                if (fieldErrors.length === 0) {
                    showToast(`Chưa tạo được contest. ${error.message}`, "error");
                }
            });

    const minutesOptions = { valueAsNumber: true } as const;

    return (
        <Dialog open={open} onOpenChange={changeOpen}>
            <DialogContent className="sm:max-w-lg">
                <DialogTitle>Tạo contest</DialogTitle>
                <DialogDescription className="text-base">
                    Chấm theo luật ICPC. Gắn bài sau khi tạo.
                </DialogDescription>
                <form
                    noValidate
                    onSubmit={handleSubmit(submitCreate)}
                    className="flex flex-col gap-4"
                >
                    <TextInputField
                        label="Tên contest"
                        registration={register("title")}
                        error={errors.title?.message}
                    />
                    <TextInputField
                        label="Slug"
                        placeholder="sql-cup-1"
                        registration={register("slug")}
                        error={errors.slug?.message}
                    />
                    <TextInputField
                        label="Bắt đầu lúc"
                        type="datetime-local"
                        registration={register("startAt")}
                        error={errors.startAt?.message}
                    />
                    <div className="grid gap-4 sm:grid-cols-3">
                        <TextInputField
                            label="Thời lượng (phút)"
                            type="number"
                            registration={register("durationMinutes", minutesOptions)}
                            error={errors.durationMinutes?.message}
                        />
                        <TextInputField
                            label="Đóng băng cuối (phút)"
                            type="number"
                            registration={register("freezeMinutes", minutesOptions)}
                            error={errors.freezeMinutes?.message}
                        />
                        <TextInputField
                            label="Phạt mỗi lần sai (phút)"
                            type="number"
                            registration={register("penaltyMinutes", minutesOptions)}
                            error={errors.penaltyMinutes?.message}
                        />
                    </div>
                    <TextAreaField
                        label="Mô tả"
                        rows={3}
                        registration={register("description")}
                        error={errors.description?.message}
                    />
                    <label className="flex min-h-11 items-center gap-2">
                        <input type="checkbox" className="size-4" {...register("isRated")} />
                        Tính rating
                    </label>
                    <div className="flex justify-end gap-2">
                        <Button type="button" variant="outline" onClick={() => changeOpen(false)}>
                            Huỷ
                        </Button>
                        <Button type="submit" disabled={createContest.isPending}>
                            Tạo contest
                        </Button>
                    </div>
                </form>
            </DialogContent>
        </Dialog>
    );
}
