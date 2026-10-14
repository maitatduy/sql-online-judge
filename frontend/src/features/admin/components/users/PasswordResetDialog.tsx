import { useToast } from "@/components/shared/ToastProvider";
import { useSendPasswordReset } from "@/features/admin/hooks/useSendPasswordReset";

import { ActionDialog } from "./ActionDialog";
import { DialogActions } from "./DialogActions";

interface PasswordResetDialogProps {
    username: string;
    email: string;
    onClose: () => void;
}

export function PasswordResetDialog({ username, email, onClose }: PasswordResetDialogProps) {
    const showToast = useToast();
    const sendPasswordReset = useSendPasswordReset();

    function handleSubmit(event: React.FormEvent) {
        event.preventDefault();
        sendPasswordReset.mutate(username, {
            onSuccess: () => {
                showToast(`Đã gửi email đặt lại mật khẩu tới ${email}.`);
                onClose();
            },
            onError: (error) =>
                showToast(
                    `Chưa gửi được email đặt lại mật khẩu tới ${email}. ${error.message}`,
                    "error",
                ),
        });
    }

    return (
        <ActionDialog
            title="Gửi email đặt lại mật khẩu?"
            description={
                <>
                    Liên kết có hiệu lực 30 phút sẽ được gửi tới{" "}
                    <strong className="break-all text-foreground">{email}</strong>. Mật khẩu hiện
                    tại vẫn dùng được cho tới khi người dùng đổi.
                </>
            }
            onClose={onClose}
        >
            <form onSubmit={handleSubmit}>
                <DialogActions
                    submitLabel="Gửi email"
                    isSubmitDisabled={sendPasswordReset.isPending}
                />
            </form>
        </ActionDialog>
    );
}
