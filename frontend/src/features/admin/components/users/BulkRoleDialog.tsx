import { zodResolver } from "@hookform/resolvers/zod";
import { useId } from "react";
import { useForm, useWatch } from "react-hook-form";

import { useToast } from "@/components/shared/ToastProvider";
import { useBulkChangeRole } from "@/features/admin/hooks/useBulkChangeRole";
import { accountCountLabel, affectedSummary } from "@/features/admin/users/bulk-result";
import { fieldErrorOf } from "@/features/admin/users/form-errors";
import { ROLE_LABELS } from "@/features/admin/users/role-labels";
import { BULK_ROLES, bulkRoleSchema, type BulkRoleValues } from "@/features/admin/users/schemas";

import { ActionDialog } from "./ActionDialog";
import { DialogActions } from "./DialogActions";
import { NativeSelect } from "./NativeSelect";
import { SelfSkipNotice } from "./SelfSkipNotice";
import { TextAreaField } from "./TextAreaField";

interface BulkRoleDialogProps {
    usernames: string[];
    skippedSelf: string | null;
    onClose: () => void;
    onDone: () => void;
}

export function BulkRoleDialog({ usernames, skippedSelf, onClose, onDone }: BulkRoleDialogProps) {
    const roleId = useId();
    const showToast = useToast();
    const bulkChangeRole = useBulkChangeRole();
    const targetLabel = accountCountLabel(usernames.length);
    const {
        control,
        register,
        handleSubmit,
        setError,
        formState: { errors },
    } = useForm<BulkRoleValues>({
        resolver: zodResolver(bulkRoleSchema),
        defaultValues: { role: "SETTER", reason: "" },
    });
    const reason = useWatch({ control, name: "reason" });

    const submitRoles = (values: BulkRoleValues) =>
        bulkChangeRole
            .mutateAsync({ usernames, ...values })
            .then((result) => {
                showToast(
                    `Đã đổi vai trò ${affectedSummary(result)} thành ${ROLE_LABELS[values.role]}.`,
                );
                onDone();
            })
            .catch((error: Error) => {
                const reasonError = fieldErrorOf(error, "reason");
                if (reasonError) {
                    setError("reason", { message: reasonError });
                    return;
                }
                showToast(`Chưa đổi được vai trò ${targetLabel}. ${error.message}`, "error");
            });

    return (
        <ActionDialog
            title={`Đổi vai trò ${targetLabel}`}
            description="Quyền mới có hiệu lực ở lần tải trang tiếp theo của người dùng."
            onClose={onClose}
        >
            {skippedSelf ? <SelfSkipNotice username={skippedSelf} /> : null}
            <form noValidate onSubmit={handleSubmit(submitRoles)} className="flex flex-col gap-4">
                <div>
                    <label htmlFor={roleId} className="mb-1.5 block text-[13px] font-semibold">
                        Vai trò mới
                    </label>
                    <NativeSelect id={roleId} className="w-full" {...register("role")}>
                        {BULK_ROLES.map((role) => (
                            <option key={role} value={role}>
                                {ROLE_LABELS[role]}
                            </option>
                        ))}
                    </NativeSelect>
                </div>
                <TextAreaField
                    label="Lý do"
                    hint="Lý do được ghi vào nhật ký thao tác."
                    rows={2}
                    error={errors.reason?.message}
                    registration={register("reason")}
                />
                <DialogActions
                    submitLabel="Đổi vai trò"
                    isSubmitDisabled={
                        usernames.length === 0 || reason.trim() === "" || bulkChangeRole.isPending
                    }
                />
            </form>
        </ActionDialog>
    );
}
