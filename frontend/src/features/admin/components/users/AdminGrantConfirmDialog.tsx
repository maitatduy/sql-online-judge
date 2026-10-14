import { ActionDialog } from "./ActionDialog";
import { DialogActions } from "./DialogActions";

interface AdminGrantConfirmDialogProps {
    username: string;
    onConfirm: () => void;
    onCancel: () => void;
}

export function AdminGrantConfirmDialog({
    username,
    onConfirm,
    onCancel,
}: AdminGrantConfirmDialogProps) {
    return (
        <ActionDialog
            title={`Cấp quyền Quản trị cho ${username}?`}
            description="Quản trị viên có toàn quyền với tài khoản, nội dung và cấu hình hệ thống, kể cả khoá hay xoá quản trị viên khác."
            onClose={onCancel}
        >
            <form
                onSubmit={(event) => {
                    event.preventDefault();
                    onConfirm();
                }}
            >
                <DialogActions
                    submitLabel="Cấp quyền Quản trị"
                    submitVariant="destructive"
                    isSubmitDisabled={false}
                />
            </form>
        </ActionDialog>
    );
}
