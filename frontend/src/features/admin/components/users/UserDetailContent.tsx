import { useState } from "react";

import { accountStateOf } from "@/features/admin/users/account-status";
import type { AdminUserDetail } from "@/features/admin/users/types";
import { formatDate } from "@/lib/format";

import { AccountStatusLabel } from "./AccountStatusLabel";
import { DetailAvatar } from "./DetailAvatar";
import { RoleBadge } from "./RoleBadge";
import { UserDetailActions } from "./UserDetailActions";
import { UserFacts } from "./UserFacts";
import { UserStats } from "./UserStats";
import { ViolationList } from "./ViolationList";

const UNNAMED_LABEL = "Chưa đặt tên";
const PERMANENT_BAN_LABEL = "vĩnh viễn";
const SECTION_LABEL_CLASS = "mb-2 text-[13px] font-semibold";

interface UserDetailContentProps {
    user: AdminUserDetail;
}

export function UserDetailContent({ user }: UserDetailContentProps) {
    const [mountedAt] = useState(() => new Date());

    return (
        <>
            <div className="flex items-center gap-3 pr-9">
                <DetailAvatar username={user.username} avatarUrl={user.avatarUrl} />
                <div className="min-w-0">
                    <strong className="block text-base break-all">{user.username}</strong>
                    <p className="text-xs text-muted-foreground">
                        {user.displayName ?? UNNAMED_LABEL}
                    </p>
                </div>
            </div>
            <p className="flex flex-wrap items-center gap-1.5 text-[13px]">
                <RoleBadge role={user.role} />
                <AccountStatusLabel state={accountStateOf(user)} />
                {user.status === "BANNED" ? (
                    <span className="text-muted-foreground">
                        {user.bannedUntil
                            ? `đến ${formatDate(user.bannedUntil)}`
                            : PERMANENT_BAN_LABEL}
                    </span>
                ) : null}
            </p>
            <UserStats user={user} />
            <section>
                <h2 className={SECTION_LABEL_CLASS}>Lịch sử vi phạm</h2>
                <ViolationList violations={user.violations} />
            </section>
            <UserFacts user={user} now={mountedAt} />
            <UserDetailActions user={user} />
        </>
    );
}
