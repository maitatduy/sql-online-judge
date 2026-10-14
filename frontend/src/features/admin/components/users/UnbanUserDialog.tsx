import { zodResolver } from "@hookform/resolvers/zod";
import { useForm, useWatch } from "react-hook-form";

import { useToast } from "@/components/shared/ToastProvider";
import { useUnbanUser } from "@/features/admin/hooks/useUnbanUser";
import { fieldErrorOf } from "@/features/admin/users/form-errors";
import { unbanUserSchema, type UnbanUserValues } from "@/features/admin/users/schemas";

import { ActionDialog } from "./ActionDialog";
import { DialogActions } from "./DialogActions";
import { TextAreaField } from "./TextAreaField";

interface UnbanUserDialogProps {
    username: string;
    onClose: () => void;
}

export function UnbanUserDialog({ username, onClose }: UnbanUserDialogProps) {
    const showToast = useToast();
    const unbanUser = useUnbanUser();
    const {
        control,
        register,
        handleSubmit,
        setError,
        formState: { errors },
    } = useForm<UnbanUserValues>({
        resolver: zodResolver(unbanUserSchema),
        defaultValues: { reason: "" },
    });
    const reason = useWatch({ control, name: "reason" });

    const submitUnban = (values: UnbanUserValues) => {
        unbanUser.mutate(
            { username, reason: values.reason },
            {
                onSuccess: () => {
                    showToast(`Đã mở khoá ${username}.`);
                    onClose();
                },
                onError: (error) => {
                    const reasonError = fieldErrorOf(error, "reason");
                    if (reasonError) {
                        setError("reason", { message: reasonError });
                        return;
                    }
                    showToast(`Chưa mở khoá được ${username}. ${error.message}`, "error");
                },
            },
        );
    };

    return (
        <ActionDialog
            title={`Mở khoá ${username}?`}
            description="Người dùng có thể đăng nhập lại ngay."
            onClose={onClose}
        >
            <form noValidate onSubmit={handleSubmit(submitUnban)} className="flex flex-col gap-4">
                <TextAreaField
                    label="Lý do"
                    hint="Lý do được ghi vào nhật ký thao tác."
                    rows={2}
                    error={errors.reason?.message}
                    registration={register("reason")}
                />
                <DialogActions
                    submitLabel="Mở khoá"
                    isSubmitDisabled={reason.trim() === "" || unbanUser.isPending}
                />
            </form>
        </ActionDialog>
    );
}
