import { useState } from "react";

import type { AdminUserDetail } from "@/features/admin/users/types";

import { AccountStatusLine } from "./AccountStatusLine";
import { DetailAvatar } from "./DetailAvatar";
import { UserDetailActions } from "./UserDetailActions";
import { UserFacts } from "./UserFacts";
import { UserStats } from "./UserStats";
import { ViolationList } from "./ViolationList";

const UNNAMED_LABEL = "Chưa đặt tên";
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
            <AccountStatusLine user={user} />
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
