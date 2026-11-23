import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";

import { NativeSelect } from "@/components/shared/NativeSelect";
import { useToast } from "@/components/shared/ToastProvider";
import { DIFFICULTIES, DIFFICULTY_LABELS } from "@/lib/difficulty";

import { useBankTags } from "../../hooks/useProblemBank";
import { useProblemBankMutation } from "../../hooks/useProblemBankMutation";
import { problemBankApi } from "../../problem-bank/api";
import { quickEditSchema, type QuickEditValues } from "../../problem-bank/schemas";
import type { BankProblem } from "../../problem-bank/types";
import { fieldErrorOf } from "../../users/form-errors";
import { ActionDialog } from "../users/ActionDialog";
import { DialogActions } from "../users/DialogActions";
import { TextInputField } from "../users/TextInputField";

interface QuickEditProblemDialogProps {
    problem: BankProblem;
    onClose: () => void;
}

export function QuickEditProblemDialog({ problem, onClose }: QuickEditProblemDialogProps) {
    const showToast = useToast();
    const { data: tags } = useBankTags();
    const edit = useProblemBankMutation(problemBankApi.quickEdit);
    const {
        register,
        handleSubmit,
        setError,
        formState: { errors },
    } = useForm<QuickEditValues>({
        resolver: zodResolver(quickEditSchema),
        defaultValues: {
            title: problem.title,
            difficulty: problem.difficulty,
            tagSlugs: problem.tags.map((tag) => tag.slug),
        },
    });

    const submitEdit = (values: QuickEditValues) =>
        edit
            .mutateAsync({ problemId: problem.id, values })
            .then(() => {
                showToast("Đã lưu thay đổi.");
                onClose();
            })
            .catch((error: Error) => {
                const titleError = fieldErrorOf(error, "title");
                if (titleError) {
                    setError("title", { message: titleError });
                    return;
                }
                showToast(`Chưa lưu được. ${error.message}`, "error");
            });

    return (
        <ActionDialog
            title={`Sửa nhanh bài ${problem.number ?? ""}`}
            description="Đề, dữ liệu và đáp án giữ nguyên."
            onClose={onClose}
        >
            <form noValidate onSubmit={handleSubmit(submitEdit)} className="flex flex-col gap-4">
                <TextInputField
                    label="Tên bài"
                    error={errors.title?.message}
                    registration={register("title")}
                />
                <label className="flex flex-col gap-1.5 text-[13px] font-semibold">
                    Độ khó
                    <NativeSelect {...register("difficulty")}>
                        {DIFFICULTIES.map((difficulty) => (
                            <option key={difficulty} value={difficulty}>
                                {DIFFICULTY_LABELS[difficulty]}
                            </option>
                        ))}
                    </NativeSelect>
                </label>
                <fieldset className="flex flex-col gap-1.5">
                    <legend className="mb-1.5 text-[13px] font-semibold">Chủ đề</legend>
                    <div className="flex flex-wrap gap-x-4 gap-y-2">
                        {(tags?.items ?? []).map((tag) => (
                            <label key={tag.slug} className="flex items-center gap-2 text-sm">
                                <input
                                    type="checkbox"
                                    value={tag.slug}
                                    className="size-4 accent-brand"
                                    {...register("tagSlugs")}
                                />
                                {tag.name}
                            </label>
                        ))}
                    </div>
                    {errors.tagSlugs?.message ? (
                        <p className="text-xs text-destructive">{errors.tagSlugs.message}</p>
                    ) : null}
                </fieldset>
                <DialogActions submitLabel="Lưu" isSubmitDisabled={edit.isPending} />
            </form>
        </ActionDialog>
    );
}
