import { useId } from "react";

import { accountStateOf } from "@/features/admin/users/account-status";
import { lastActiveLabel } from "@/features/admin/users/activity";
import type { AdminUserSummary } from "@/features/admin/users/types";
import { formatDate, formatNumber } from "@/lib/format";
import { cn } from "@/lib/utils";

import { AccountStatusLabel } from "./AccountStatusLabel";
import { RoleBadge } from "./RoleBadge";
import { BODY_CELL_CLASS, CHECKBOX_CLASS } from "./table-cells";
import { UserAvatar } from "./UserAvatar";

const UNRATED_LABEL = "-";
const MUTED_CELL_CLASS = `${BODY_CELL_CLASS} whitespace-nowrap text-muted-foreground`;
const NUMBER_CELL_CLASS = `${BODY_CELL_CLASS} text-right font-mono`;

interface UserRowProps {
    user: AdminUserSummary;
    now: Date;
    isOpen: boolean;
    isSelected: boolean;
    onOpen: (username: string) => void;
    onToggleSelected: (username: string) => void;
}

export function UserRow({ user, now, isOpen, isSelected, onOpen, onToggleSelected }: UserRowProps) {
    const usernameId = useId();

    return (
        <tr
            aria-labelledby={usernameId}
            aria-current={isOpen ? "true" : undefined}
            onClick={() => onOpen(user.username)}
            className={cn(
                "cursor-pointer hover:bg-surface",
                isOpen ? "bg-brand/5 shadow-[inset_3px_0_0_var(--brand)] hover:bg-brand/5" : null,
            )}
        >
            <td className={BODY_CELL_CLASS} onClick={(event) => event.stopPropagation()}>
                <input
                    type="checkbox"
                    aria-label={`Chọn ${user.username}`}
                    checked={isSelected}
                    onChange={() => onToggleSelected(user.username)}
                    className={CHECKBOX_CLASS}
                />
            </td>
            <th scope="row" className={`${BODY_CELL_CLASS} text-left font-normal`}>
                <div className="flex items-center gap-2.5">
                    <UserAvatar username={user.username} />
                    <div className="min-w-0">
                        <button type="button" className="block text-left">
                            <strong id={usernameId}>{user.username}</strong>
                        </button>
                        <span className="text-xs text-muted-foreground">{user.email}</span>
                    </div>
                </div>
            </th>
            <td className={BODY_CELL_CLASS}>
                <RoleBadge role={user.role} />
            </td>
            <td className={NUMBER_CELL_CLASS}>
                {user.rating > 0 ? formatNumber(user.rating) : UNRATED_LABEL}
            </td>
            <td className={NUMBER_CELL_CLASS}>{formatNumber(user.acceptedCount)}</td>
            <td className={MUTED_CELL_CLASS}>{formatDate(user.joinedAt)}</td>
            <td className={MUTED_CELL_CLASS}>{lastActiveLabel(user, now)}</td>
            <td className={BODY_CELL_CLASS}>
                <AccountStatusLabel state={accountStateOf(user)} />
            </td>
        </tr>
    );
}
