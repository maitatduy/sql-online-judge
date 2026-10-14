import { zodResolver } from "@hookform/resolvers/zod";
import { useForm, useWatch } from "react-hook-form";

import { fieldErrorOf } from "@/features/admin/users/form-errors";
import { BAN_DURATIONS, banUserSchema, type BanUserValues } from "@/features/admin/users/schemas";
import type { BanDuration } from "@/features/admin/users/types";

import { DialogActions } from "./DialogActions";
import { TextAreaField } from "./TextAreaField";

const DURATION_LABELS: Record<BanDuration, string> = {
    DAYS_7: "7 ngày",
    DAYS_30: "30 ngày",
    PERMANENT: "Vĩnh viễn",
};

interface BanFormProps {
    submitBan: (values: BanUserValues) => Promise<unknown>;
    onFailure: (error: Error) => void;
    isPending: boolean;
    hasTargets?: boolean;
    initialDuration?: BanDuration;
}

export function BanForm({
    submitBan,
    onFailure,
    isPending,
    hasTargets = true,
    initialDuration = "DAYS_7",
}: BanFormProps) {
    const {
        control,
        register,
        handleSubmit,
        setError,
        formState: { errors },
    } = useForm<BanUserValues>({
        resolver: zodResolver(banUserSchema),
        defaultValues: { duration: initialDuration, reason: "", notifyByEmail: true },
    });
    const reason = useWatch({ control, name: "reason" });

    const handleBan = (values: BanUserValues) =>
        submitBan(values).catch((error: Error) => {
            const reasonError = fieldErrorOf(error, "reason");
            if (reasonError) {
                setError("reason", { message: reasonError });
                return;
            }
            onFailure(error);
        });

    return (
        <form noValidate onSubmit={handleSubmit(handleBan)} className="flex flex-col gap-4">
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
            <TextAreaField
                label="Lý do"
                hint="Người dùng sẽ thấy lý do này."
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
                isSubmitDisabled={!hasTargets || reason.trim() === "" || isPending}
            />
        </form>
    );
}
