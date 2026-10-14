import { useToast } from "@/components/shared/ToastProvider";
import { useBulkBanUsers } from "@/features/admin/hooks/useBulkBanUsers";
import { accountCountLabel, affectedSummary } from "@/features/admin/users/bulk-result";

import { ActionDialog } from "./ActionDialog";
import { BanForm } from "./BanForm";
import { BAN_DESCRIPTION } from "./BanUserDialog";
import { SelfSkipNotice } from "./SelfSkipNotice";

interface BulkBanDialogProps {
    usernames: string[];
    skippedSelf: string | null;
    onClose: () => void;
    onDone: () => void;
}

export function BulkBanDialog({ usernames, skippedSelf, onClose, onDone }: BulkBanDialogProps) {
    const showToast = useToast();
    const bulkBan = useBulkBanUsers();
    const targetLabel = accountCountLabel(usernames.length);

    return (
        <ActionDialog title={`Khoá ${targetLabel}`} description={BAN_DESCRIPTION} onClose={onClose}>
            {skippedSelf ? <SelfSkipNotice username={skippedSelf} /> : null}
            <BanForm
                isPending={bulkBan.isPending}
                hasTargets={usernames.length > 0}
                submitBan={(values) =>
                    bulkBan.mutateAsync({ usernames, ...values }).then((result) => {
                        showToast(
                            `Đã khoá ${affectedSummary(result)}. Thao tác đã được ghi vào nhật ký.`,
                        );
                        onDone();
                    })
                }
                onFailure={(error) =>
                    showToast(`Chưa khoá được ${targetLabel}. ${error.message}`, "error")
                }
            />
        </ActionDialog>
    );
}
