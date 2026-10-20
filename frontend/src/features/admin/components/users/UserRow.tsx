import { memo, useId } from "react";

import { accountStateOf } from "@/features/admin/users/account-status";
import type { AdminUserSummary } from "@/features/admin/users/types";
import { formatDate } from "@/lib/format";

import { AccountStatusLabel } from "./AccountStatusLabel";
import { RoleBadge } from "./RoleBadge";
import { BODY_CELL_CLASS, CHECKBOX_CLASS, CHECKBOX_TARGET_CLASS } from "./table-cells";
import { UserAvatar } from "./UserAvatar";
import { UserRowActions, type UserRowHandlers } from "./UserRowActions";

interface UserRowProps {
    user: AdminUserSummary;
    isSelected: boolean;
    onToggleSelected: (username: string) => void;
    canDelete: boolean;
    handlers: UserRowHandlers;
}

export const UserRow = memo(function UserRow({
    user,
    isSelected,
    onToggleSelected,
    canDelete,
    handlers,
}: UserRowProps) {
    const usernameId = useId();

    return (
        <tr aria-labelledby={usernameId} className="hover:bg-surface">
            <td className={BODY_CELL_CLASS}>
                <label className={CHECKBOX_TARGET_CLASS}>
                    <input
                        type="checkbox"
                        aria-label={`Chọn ${user.username}`}
                        checked={isSelected}
                        onChange={() => onToggleSelected(user.username)}
                        className={CHECKBOX_CLASS}
                    />
                </label>
            </td>
            <th scope="row" className={`${BODY_CELL_CLASS} text-left font-normal`}>
                <div className="flex items-center gap-2.5">
                    <UserAvatar username={user.username} avatarUrl={user.avatarUrl} />
                    <div className="min-w-0">
                        <strong id={usernameId} className="block">
                            {user.username}
                        </strong>
                        <span className="text-xs text-muted-foreground">{user.email}</span>
                    </div>
                </div>
            </th>
            <td className={BODY_CELL_CLASS}>
                <RoleBadge role={user.role} />
            </td>
            <td className={BODY_CELL_CLASS}>
                <AccountStatusLabel state={accountStateOf(user)} />
                {user.deletedAt ? (
                    <span className="block text-xs text-muted-foreground">
                        {`Ngày ${formatDate(user.deletedAt)}`}
                    </span>
                ) : null}
            </td>
            <td className={`${BODY_CELL_CLASS} py-1`}>
                <UserRowActions
                    username={user.username}
                    isDeleted={user.deletedAt !== null}
                    canDelete={canDelete}
                    handlers={handlers}
                />
            </td>
        </tr>
    );
});
