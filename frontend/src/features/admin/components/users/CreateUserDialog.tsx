import { useToast } from "@/components/shared/ToastProvider";
import { useCreateUser } from "@/features/admin/hooks/useCreateUser";
import { useUploadAvatar } from "@/features/admin/hooks/useUserAvatar";

import { ActionDialog } from "./ActionDialog";
import { UserForm, type UserFormSubmission } from "./UserForm";

const EMPTY_USER = {
    username: "",
    email: "",
    displayName: "",
    role: "LEARNER",
    reason: "",
} as const;

interface CreateUserDialogProps {
    onClose: () => void;
}

export function CreateUserDialog({ onClose }: CreateUserDialogProps) {
    const showToast = useToast();
    const createUser = useCreateUser();
    const uploadAvatar = useUploadAvatar();

    async function submitCreate({ values, avatar }: UserFormSubmission) {
        const { username, email, role, displayName } = values;
        const account = { username, email, role };
        await createUser.mutateAsync({ ...account, ...(displayName ? { displayName } : {}) });
        if (avatar.kind === "replace") {
            await uploadAvatar
                .mutateAsync({ username: account.username, file: avatar.file })
                .catch((error: Error) =>
                    showToast(
                        `Đã tạo ${account.username} nhưng chưa tải được ảnh đại diện. ${error.message}`,
                        "error",
                    ),
                );
        }
        showToast(
            `Đã tạo tài khoản ${account.username} và gửi email kích hoạt tới ${account.email}.`,
        );
        onClose();
    }

    return (
        <ActionDialog
            title="Tạo người dùng"
            description="Người dùng nhận email kích hoạt để tự đặt mật khẩu. Liên kết có hiệu lực 7 ngày."
            onClose={onClose}
            className="max-w-[600px]"
        >
            <UserForm
                mode="create"
                defaultValues={EMPTY_USER}
                currentAvatarUrl={null}
                isSelf={false}
                submitLabel="Tạo tài khoản"
                onSubmit={submitCreate}
                onFailure={(error, values) =>
                    showToast(
                        `Chưa tạo được tài khoản ${values.username}. ${error.message}`,
                        "error",
                    )
                }
            />
        </ActionDialog>
    );
}
