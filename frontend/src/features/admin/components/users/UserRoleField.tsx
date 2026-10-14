import { useId, useState } from "react";

import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import { useChangeUserRole } from "@/features/admin/hooks/useChangeUserRole";
import { ROLE_LABELS, ROLES } from "@/features/admin/users/role-labels";
import type { AdminUserDetail } from "@/features/admin/users/types";
import type { Role } from "@/lib/current-user";
import { useCurrentUser } from "@/lib/useCurrentUser";

import { NativeSelect } from "./NativeSelect";

interface UserRoleFieldProps {
    user: Pick<AdminUserDetail, "username" | "role">;
}

export function UserRoleField({ user }: UserRoleFieldProps) {
    const selectId = useId();
    const showToast = useToast();
    const { data: currentUser } = useCurrentUser();
    const changeRole = useChangeUserRole();
    const [draftRole, setDraftRole] = useState<Role | null>(null);
    const selectedRole = draftRole ?? user.role;
    const isSelf = currentUser?.username === user.username;

    function handleSave() {
        changeRole.mutate(
            { username: user.username, role: selectedRole },
            {
                onSuccess: () => {
                    setDraftRole(null);
                    showToast(
                        `Đã đổi vai trò của ${user.username} thành ${ROLE_LABELS[selectedRole]}.`,
                    );
                },
                onError: (error) =>
                    showToast(
                        `Chưa đổi được vai trò của ${user.username}. ${error.message}`,
                        "error",
                    ),
            },
        );
    }

    return (
        <div>
            <label htmlFor={selectId} className="mb-2 block text-[13px] font-semibold">
                Vai trò
            </label>
            <div className="flex gap-2">
                <NativeSelect
                    id={selectId}
                    value={selectedRole}
                    disabled={!currentUser || isSelf}
                    onChange={(event) => setDraftRole(event.target.value as Role)}
                    className="min-w-0 flex-1"
                >
                    {ROLES.map((role) => (
                        <option key={role} value={role}>
                            {ROLE_LABELS[role]}
                        </option>
                    ))}
                </NativeSelect>
                {selectedRole !== user.role ? (
                    <Button type="button" disabled={changeRole.isPending} onClick={handleSave}>
                        Lưu
                    </Button>
                ) : null}
            </div>
            {isSelf ? (
                <p className="mt-1.5 text-xs text-muted-foreground">
                    Bạn không thể tự đổi vai trò hoặc khoá tài khoản của chính mình.
                </p>
            ) : null}
        </div>
    );
}
