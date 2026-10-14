import { useUserRestrictions } from "@/features/admin/hooks/useUserRestrictions";
import type { AdminUserDetail } from "@/features/admin/users/types";
import { useCurrentUser } from "@/lib/useCurrentUser";

import { BlockError, BlockLoading } from "./BlockStates";
import { PageCard } from "./PageCard";
import { RestrictionsForm } from "./RestrictionsForm";

interface RestrictionsCardProps {
    user: Pick<AdminUserDetail, "username" | "role" | "deletedAt">;
}

export function RestrictionsCard({ user }: RestrictionsCardProps) {
    const { data: currentUser } = useCurrentUser();
    const { data: restrictions, error, isPending, refetch } = useUserRestrictions(user.username);
    const isSelf = currentUser?.username === user.username;

    return (
        <PageCard title="Quyền và trạng thái">
            {isPending || !currentUser ? (
                <BlockLoading label="Đang tải quyền và trạng thái" />
            ) : null}
            {error && !restrictions ? (
                <BlockError error={error} onRetry={() => void refetch()} />
            ) : null}
            {restrictions && currentUser ? (
                <RestrictionsForm
                    key={`${user.role}-${restrictions.aiDailyQuota}-${restrictions.isBannedFromRatedContests}`}
                    user={user}
                    restrictions={restrictions}
                    isLocked={isSelf || user.deletedAt !== null}
                />
            ) : null}
            {isSelf ? (
                <p className="mt-2 text-xs text-muted-foreground">
                    Bạn không thể tự đổi quyền hoặc hạn chế của chính mình.
                </p>
            ) : null}
        </PageCard>
    );
}
