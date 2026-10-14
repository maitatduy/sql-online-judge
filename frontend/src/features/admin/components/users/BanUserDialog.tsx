import { useToast } from "@/components/shared/ToastProvider";
import { useBanUser } from "@/features/admin/hooks/useBanUser";

import { ActionDialog } from "./ActionDialog";
import { BanForm } from "./BanForm";

export const BAN_DESCRIPTION =
    "Người bị khoá không thể đăng nhập, nộp bài hay bình luận. Bài nộp cũ được giữ nguyên.";

interface BanUserDialogProps {
    username: string;
    onClose: () => void;
}

export function BanUserDialog({ username, onClose }: BanUserDialogProps) {
    const showToast = useToast();
    const banUser = useBanUser();

    return (
        <ActionDialog title={`Khoá ${username}`} description={BAN_DESCRIPTION} onClose={onClose}>
            <BanForm
                isPending={banUser.isPending}
                submitBan={(values) =>
                    banUser.mutateAsync({ username, ...values }).then(() => {
                        showToast(`Đã khoá ${username}. Thao tác đã được ghi vào nhật ký.`);
                        onClose();
                    })
                }
                onFailure={(error) =>
                    showToast(`Chưa khoá được ${username}. ${error.message}`, "error")
                }
            />
        </ActionDialog>
    );
}
