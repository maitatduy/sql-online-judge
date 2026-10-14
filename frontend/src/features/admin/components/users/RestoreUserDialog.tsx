import { useToast } from "@/components/shared/ToastProvider";
import { useRestoreUser } from "@/features/admin/hooks/useRestoreUser";

import { ActionDialog } from "./ActionDialog";
import { DialogActions } from "./DialogActions";

interface RestoreUserDialogProps {
    username: string;
    onClose: () => void;
}

export function RestoreUserDialog({ username, onClose }: RestoreUserDialogProps) {
    const showToast = useToast();
    const restoreUser = useRestoreUser();

    function handleSubmit(event: React.FormEvent) {
        event.preventDefault();
        restoreUser.mutate(username, {
            onSuccess: () => {
                showToast(`Đã khôi phục ${username}.`);
                onClose();
            },
            onError: (error) =>
                showToast(`Chưa khôi phục được ${username}. ${error.message}`, "error"),
        });
    }

    return (
        <ActionDialog
            title={`Khôi phục ${username}?`}
            description="Tài khoản hiện lại trong danh sách và đăng nhập được như trước khi bị xoá."
            onClose={onClose}
        >
            <form onSubmit={handleSubmit}>
                <DialogActions submitLabel="Khôi phục" isSubmitDisabled={restoreUser.isPending} />
            </form>
        </ActionDialog>
    );
}
