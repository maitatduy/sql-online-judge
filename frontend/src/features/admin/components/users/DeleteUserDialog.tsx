import { zodResolver } from "@hookform/resolvers/zod";
import { useForm, useWatch } from "react-hook-form";

import { useToast } from "@/components/shared/ToastProvider";
import { useDeleteUser } from "@/features/admin/hooks/useDeleteUser";
import { fieldErrorOf } from "@/features/admin/users/form-errors";
import { deleteUserSchema, type DeleteUserValues } from "@/features/admin/users/schemas";

import { ActionDialog } from "./ActionDialog";
import { DialogActions } from "./DialogActions";
import { TextAreaField } from "./TextAreaField";

interface DeleteUserDialogProps {
    username: string;
    onClose: () => void;
    onDeleted: (username: string) => void;
}

export function DeleteUserDialog({ username, onClose, onDeleted }: DeleteUserDialogProps) {
    const showToast = useToast();
    const deleteUser = useDeleteUser();
    const {
        control,
        register,
        handleSubmit,
        setError,
        formState: { errors },
    } = useForm<DeleteUserValues>({
        resolver: zodResolver(deleteUserSchema),
        defaultValues: { reason: "" },
    });
    const reason = useWatch({ control, name: "reason" });

    const submitDelete = (values: DeleteUserValues) =>
        deleteUser
            .mutateAsync({ username, reason: values.reason })
            .then(() => {
                showToast(`Đã xoá ${username}.`);
                onDeleted(username);
            })
            .catch((error: Error) => {
                const reasonError = fieldErrorOf(error, "reason");
                if (reasonError) {
                    setError("reason", { message: reasonError });
                    return;
                }
                showToast(`Chưa xoá được ${username}. ${error.message}`, "error");
            });

    return (
        <ActionDialog
            title={`Xoá ${username}?`}
            description="Tài khoản sẽ bị ẩn khỏi danh sách và không đăng nhập được nữa. Bài nộp và lịch sử vẫn được giữ để đối soát."
            onClose={onClose}
        >
            <form noValidate onSubmit={handleSubmit(submitDelete)} className="flex flex-col gap-4">
                <TextAreaField
                    label="Lý do"
                    hint="Lý do được ghi vào nhật ký thao tác."
                    rows={2}
                    error={errors.reason?.message}
                    registration={register("reason")}
                />
                <DialogActions
                    submitLabel="Xoá tài khoản"
                    submitVariant="destructive"
                    isSubmitDisabled={reason.trim() === "" || deleteUser.isPending}
                />
            </form>
        </ActionDialog>
    );
}
