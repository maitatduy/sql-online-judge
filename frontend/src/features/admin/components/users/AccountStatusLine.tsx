import { accountStateOf } from "@/features/admin/users/account-status";
import type { AdminUserDetail } from "@/features/admin/users/types";
import { formatDate } from "@/lib/format";

import { AccountStatusLabel } from "./AccountStatusLabel";
import { RoleBadge } from "./RoleBadge";

const PERMANENT_BAN_LABEL = "vĩnh viễn";

interface AccountStatusLineProps {
    user: Pick<
        AdminUserDetail,
        "role" | "status" | "bannedUntil" | "isEmailVerified" | "deletedAt"
    >;
}

export function AccountStatusLine({ user }: AccountStatusLineProps) {
    const state = accountStateOf(user);

    return (
        <p className="flex flex-wrap items-center gap-1.5 text-[13px]">
            <RoleBadge role={user.role} />
            <AccountStatusLabel state={state} />
            {state === "BANNED" ? (
                <span className="text-muted-foreground">
                    {user.bannedUntil ? `đến ${formatDate(user.bannedUntil)}` : PERMANENT_BAN_LABEL}
                </span>
            ) : null}
        </p>
    );
}
