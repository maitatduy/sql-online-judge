import { lastActiveLabel } from "@/features/admin/users/activity";
import type { AdminUserDetail } from "@/features/admin/users/types";
import { joinedLabel } from "@/features/admin/users/user-meta";

const ROW_CLASS = "flex justify-between gap-3";

interface UserFactsProps {
    user: AdminUserDetail;
    now: Date;
}

export function UserFacts({ user, now }: UserFactsProps) {
    return (
        <dl className="flex flex-col gap-1.5 border-t pt-3 text-xs">
            <div className={ROW_CLASS}>
                <dt className="text-muted-foreground">Email</dt>
                <dd className="min-w-0 break-all">{user.email}</dd>
            </div>
            <div className={ROW_CLASS}>
                <dt className="text-muted-foreground">Xác minh email</dt>
                <dd>{user.isEmailVerified ? "Đã xác minh" : "Chưa xác minh"}</dd>
            </div>
            <div className={ROW_CLASS}>
                <dt className="text-muted-foreground">Đăng nhập gần nhất</dt>
                <dd>{lastActiveLabel(user, now)}</dd>
            </div>
            <div className={ROW_CLASS}>
                <dt className="text-muted-foreground">Tham gia</dt>
                <dd>{joinedLabel(user)}</dd>
            </div>
        </dl>
    );
}
