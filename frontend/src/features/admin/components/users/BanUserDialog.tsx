import { zodResolver } from "@hookform/resolvers/zod";
import { useForm, useWatch } from "react-hook-form";

import { useToast } from "@/components/shared/ToastProvider";
import { useBanUser } from "@/features/admin/hooks/useBanUser";
import { fieldErrorOf } from "@/features/admin/users/form-errors";
import { BAN_DURATIONS, banUserSchema, type BanUserValues } from "@/features/admin/users/schemas";
import type { BanDuration } from "@/features/admin/users/types";

import { ActionDialog } from "./ActionDialog";
import { DialogActions } from "./DialogActions";
import { ReasonField } from "./ReasonField";

const DURATION_LABELS: Record<BanDuration, string> = {
    DAYS_7: "7 ngày",
    DAYS_30: "30 ngày",
    PERMANENT: "Vĩnh viễn",
};

interface BanUserDialogProps {
    username: string;
    onClose: () => void;
}

export function BanUserDialog({ username, onClose }: BanUserDialogProps) {
    const showToast = useToast();
    const banUser = useBanUser();
    const {
        control,
        register,
        handleSubmit,
        setError,
        formState: { errors },
    } = useForm<BanUserValues>({
        resolver: zodResolver(banUserSchema),
        defaultValues: { duration: "DAYS_7", reason: "", notifyByEmail: true },
    });
    const reason = useWatch({ control, name: "reason" });

    const submitBan = (values: BanUserValues) => {
        banUser.mutate(
            { username, ...values },
            {
                onSuccess: () => {
                    showToast(`Đã khoá ${username}. Thao tác đã được ghi vào nhật ký.`);
                    onClose();
                },
                onError: (error) => {
                    const reasonError = fieldErrorOf(error, "reason");
                    if (reasonError) {
                        setError("reason", { message: reasonError });
                        return;
                    }
                    showToast(`Chưa khoá được ${username}. ${error.message}`, "error");
                },
            },
        );
    };

    return (
        <ActionDialog
            title={`Khoá ${username}`}
            description="Người bị khoá không thể đăng nhập, nộp bài hay bình luận. Bài nộp cũ được giữ nguyên."
            onClose={onClose}
        >
            <form noValidate onSubmit={handleSubmit(submitBan)} className="flex flex-col gap-4">
                <fieldset>
                    <legend className="mb-1.5 text-[13px] font-semibold">Thời hạn</legend>
                    <div className="flex gap-2">
                        {BAN_DURATIONS.map((duration) => (
                            <label key={duration} className="flex-1 cursor-pointer">
                                <input
                                    type="radio"
                                    value={duration}
                                    className="peer sr-only"
                                    {...register("duration")}
                                />
                                <span className="flex h-9.5 items-center justify-center rounded-md border-[1.5px] text-[13px] font-semibold peer-checked:border-brand peer-checked:bg-brand-soft peer-checked:text-brand peer-focus-visible:ring-[3px] peer-focus-visible:ring-ring/50 max-md:h-11">
                                    {DURATION_LABELS[duration]}
                                </span>
                            </label>
                        ))}
                    </div>
                </fieldset>
                <ReasonField
                    label="Lý do (người dùng sẽ thấy)"
                    placeholder="Ví dụ: đăng đáp án trong lúc contest đang diễn ra"
                    error={errors.reason?.message}
                    registration={register("reason")}
                />
                <label className="flex items-center gap-2 text-[13px]">
                    <input
                        type="checkbox"
                        className="size-4 accent-brand"
                        {...register("notifyByEmail")}
                    />
                    Gửi email thông báo cho người dùng
                </label>
                <DialogActions
                    submitLabel="Khoá tài khoản"
                    submitVariant="destructive"
                    isSubmitDisabled={reason.trim() === "" || banUser.isPending}
                />
            </form>
        </ActionDialog>
    );
}
