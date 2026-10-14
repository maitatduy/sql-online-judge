import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { useToast } from "@/components/shared/ToastProvider";
import { useAdminUser } from "@/features/admin/hooks/useAdminUser";
import { useUpdateUser } from "@/features/admin/hooks/useUpdateUser";
import { useRemoveAvatar, useUploadAvatar } from "@/features/admin/hooks/useUserAvatar";
import type { UserFormValues } from "@/features/admin/users/schemas";
import type { AdminUserDetail, UpdateUserRequest } from "@/features/admin/users/types";
import { ApiRequestError } from "@/lib/api-client";
import { useCurrentUser } from "@/lib/useCurrentUser";

import { ActionDialog } from "./ActionDialog";
import { UserForm, type UserFormSubmission } from "./UserForm";

interface EditUserDialogProps {
    username: string;
    onClose: () => void;
}

function profileChangesOf(user: AdminUserDetail, values: UserFormValues): UpdateUserRequest {
    const displayName = values.displayName || null;
    return {
        username: user.username,
        ...(displayName === user.displayName ? {} : { displayName }),
        ...(values.role === user.role ? {} : { role: values.role, reason: values.reason }),
    };
}

export function EditUserDialog({ username, onClose }: EditUserDialogProps) {
    const showToast = useToast();
    const { data: user, error, refetch } = useAdminUser(username);
    const { data: currentUser } = useCurrentUser();
    const updateUser = useUpdateUser();
    const uploadAvatar = useUploadAvatar();
    const removeAvatar = useRemoveAvatar();

    async function submitEdit(loadedUser: AdminUserDetail, { values, avatar }: UserFormSubmission) {
        const changes = profileChangesOf(loadedUser, values);
        if (Object.keys(changes).length > 1) {
            await updateUser.mutateAsync(changes);
        }
        if (avatar.kind === "replace") {
            await uploadAvatar.mutateAsync({ username, file: avatar.file });
        }
        if (avatar.kind === "remove") {
            await removeAvatar.mutateAsync(username);
        }
        showToast(`Đã cập nhật ${username}.`);
        onClose();
    }

    return (
        <ActionDialog
            title={`Sửa ${username}`}
            description="Đổi vai trò có hiệu lực ở lần tải trang tiếp theo của người dùng."
            onClose={onClose}
            className="max-w-[600px]"
        >
            {user && currentUser ? (
                <UserForm
                    mode="edit"
                    defaultValues={{
                        username: user.username,
                        email: user.email,
                        displayName: user.displayName ?? "",
                        role: user.role,
                        reason: "",
                    }}
                    currentAvatarUrl={user.avatarUrl}
                    isSelf={currentUser.username === username}
                    submitLabel="Lưu thay đổi"
                    onSubmit={(submission) => submitEdit(user, submission)}
                    onFailure={(failure) =>
                        showToast(`Chưa cập nhật được ${username}. ${failure.message}`, "error")
                    }
                />
            ) : null}
            {!user && error ? (
                <ErrorState
                    title="Chưa tải được thông tin người dùng"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                    className="border-0 p-0"
                />
            ) : null}
            {!error && !(user && currentUser) ? (
                <div
                    role="status"
                    aria-busy="true"
                    aria-label="Đang tải thông tin người dùng"
                    className="flex flex-col gap-3"
                >
                    <Skeleton className="size-16 rounded-full" />
                    <Skeleton className="h-9" />
                    <Skeleton className="h-9" />
                </div>
            ) : null}
        </ActionDialog>
    );
}
