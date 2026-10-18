import { useToast } from "@/components/shared/ToastProvider";
import { useRevokeAllSessions } from "@/features/admin/hooks/useUserSessions";

import { ActionDialog } from "./ActionDialog";
import { DialogActions } from "./DialogActions";

const REVOKE_DESCRIPTION =
    "Mọi phiên đăng nhập bị thu hồi; người dùng sẽ bị đăng xuất trong vòng 15 phút.";
const SELF_WARNING = "Bạn cũng sẽ bị đăng xuất khỏi phiên đang dùng.";

interface RevokeAllSessionsDialogProps {
    username: string;
    isSelf: boolean;
    onClose: () => void;
}

export function RevokeAllSessionsDialog({
    username,
    isSelf,
    onClose,
}: RevokeAllSessionsDialogProps) {
    const showToast = useToast();
    const revokeAll = useRevokeAllSessions(username);

    function handleSubmit(event: React.FormEvent) {
        event.preventDefault();
        revokeAll.mutate(undefined, {
            onSuccess: () => {
                showToast(`Đã đăng xuất mọi thiết bị của ${username}.`);
                onClose();
            },
            onError: (error) =>
                showToast(
                    `Chưa đăng xuất được thiết bị của ${username}. ${error.message}`,
                    "error",
                ),
        });
    }

    return (
        <ActionDialog
            title={`Đăng xuất mọi thiết bị của ${username}?`}
            description={isSelf ? `${REVOKE_DESCRIPTION} ${SELF_WARNING}` : REVOKE_DESCRIPTION}
            onClose={onClose}
        >
            <form onSubmit={handleSubmit}>
                <DialogActions
                    submitLabel="Đăng xuất mọi thiết bị"
                    submitVariant="destructive"
                    isSubmitDisabled={revokeAll.isPending}
                />
            </form>
        </ActionDialog>
    );
}
