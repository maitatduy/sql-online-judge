import { zodResolver } from "@hookform/resolvers/zod";
import { useId, useState } from "react";
import { useForm, useWatch } from "react-hook-form";

import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useUpdateUser } from "@/features/admin/hooks/useUpdateUser";
import { useUpdateRestrictions } from "@/features/admin/hooks/useUserRestrictions";
import { fieldErrorOf } from "@/features/admin/users/form-errors";
import { ROLE_LABELS, ROLES } from "@/features/admin/users/role-labels";
import { restrictionsSchema, type RestrictionsValues } from "@/features/admin/users/schemas";
import type { AdminUserDetail, UserRestrictions } from "@/features/admin/users/types";

import { AdminGrantConfirmDialog } from "./AdminGrantConfirmDialog";
import { NativeSelect } from "./NativeSelect";
import { TextAreaField } from "./TextAreaField";

const LABEL_CLASS = "mb-1.5 block text-[13px] font-semibold";

interface RestrictionsFormProps {
    user: Pick<AdminUserDetail, "username" | "role">;
    restrictions: UserRestrictions;
    isLocked: boolean;
}

function restrictionChangesOf(current: UserRestrictions, values: RestrictionsValues) {
    return {
        ...(values.aiDailyQuota === current.aiDailyQuota
            ? {}
            : { aiDailyQuota: values.aiDailyQuota }),
        ...(values.isBannedFromRatedContests === current.isBannedFromRatedContests
            ? {}
            : { isBannedFromRatedContests: values.isBannedFromRatedContests }),
    };
}

export function RestrictionsForm({ user, restrictions, isLocked }: RestrictionsFormProps) {
    const roleId = useId();
    const quotaId = useId();
    const quotaHintId = useId();
    const quotaErrorId = useId();
    const showToast = useToast();
    const updateUser = useUpdateUser();
    const updateRestrictions = useUpdateRestrictions(user.username);
    const [pendingAdminGrant, setPendingAdminGrant] = useState<RestrictionsValues | null>(null);
    const {
        control,
        register,
        handleSubmit,
        reset,
        setError,
        formState: { errors, isDirty, isSubmitting },
    } = useForm<RestrictionsValues>({
        resolver: zodResolver(restrictionsSchema),
        defaultValues: { role: user.role, ...restrictions, reason: "" },
    });
    const reason = useWatch({ control, name: "reason" });

    async function saveChanges(values: RestrictionsValues) {
        const restrictionChanges = restrictionChangesOf(restrictions, values);
        try {
            if (values.role !== user.role) {
                await updateUser.mutateAsync({
                    username: user.username,
                    role: values.role,
                    reason: values.reason,
                });
            }
            if (Object.keys(restrictionChanges).length > 0) {
                await updateRestrictions.mutateAsync({
                    username: user.username,
                    ...restrictionChanges,
                    reason: values.reason,
                });
            }
            showToast(`Đã lưu quyền và trạng thái của ${user.username}.`);
            reset({ ...values, reason: "" });
        } catch (error) {
            const quotaError = fieldErrorOf(error, "aiDailyQuota");
            if (quotaError) {
                setError("aiDailyQuota", { message: quotaError });
                return;
            }
            showToast(
                `Chưa lưu được quyền và trạng thái của ${user.username}. ${(error as Error).message}`,
                "error",
            );
        }
    }

    function requestSave(values: RestrictionsValues) {
        if (values.role === "ADMIN" && user.role !== "ADMIN") {
            setPendingAdminGrant(values);
            return;
        }
        return saveChanges(values);
    }

    return (
        <form noValidate onSubmit={handleSubmit(requestSave)} className="flex flex-col gap-3">
            <div>
                <label htmlFor={roleId} className={LABEL_CLASS}>
                    Vai trò
                </label>
                <NativeSelect
                    id={roleId}
                    disabled={isLocked}
                    className="w-full"
                    {...register("role")}
                >
                    {ROLES.map((role) => (
                        <option key={role} value={role}>
                            {ROLE_LABELS[role]}
                        </option>
                    ))}
                </NativeSelect>
            </div>
            <div>
                <label htmlFor={quotaId} className={LABEL_CLASS}>
                    Hạn mức AI mỗi ngày
                </label>
                <Input
                    id={quotaId}
                    type="number"
                    inputMode="numeric"
                    disabled={isLocked}
                    aria-invalid={errors.aiDailyQuota ? true : undefined}
                    aria-describedby={
                        errors.aiDailyQuota ? `${quotaHintId} ${quotaErrorId}` : quotaHintId
                    }
                    className="shadow-none"
                    {...register("aiDailyQuota", { valueAsNumber: true })}
                />
                <p id={quotaHintId} className="mt-1 text-xs text-muted-foreground">
                    0 là tắt trợ lý AI với người này.
                </p>
                {errors.aiDailyQuota ? (
                    <p id={quotaErrorId} className="mt-1 text-[13px] text-destructive">
                        {errors.aiDailyQuota.message}
                    </p>
                ) : null}
            </div>
            <label className="flex items-center gap-2 text-[13px]">
                <input
                    type="checkbox"
                    disabled={isLocked}
                    className="size-4 accent-brand"
                    {...register("isBannedFromRatedContests")}
                />
                Cấm tham gia contest có xếp hạng
            </label>
            {isDirty && !isLocked ? (
                <TextAreaField
                    label="Lý do thay đổi"
                    hint="Lý do được ghi vào nhật ký thao tác."
                    rows={2}
                    registration={register("reason")}
                />
            ) : null}
            {isLocked ? null : (
                <Button
                    type="submit"
                    size="sm"
                    disabled={!isDirty || reason.trim() === "" || isSubmitting}
                >
                    Lưu thay đổi
                </Button>
            )}
            {pendingAdminGrant ? (
                <AdminGrantConfirmDialog
                    username={user.username}
                    onCancel={() => setPendingAdminGrant(null)}
                    onConfirm={() => {
                        const values = pendingAdminGrant;
                        setPendingAdminGrant(null);
                        void saveChanges(values);
                    }}
                />
            ) : null}
        </form>
    );
}
