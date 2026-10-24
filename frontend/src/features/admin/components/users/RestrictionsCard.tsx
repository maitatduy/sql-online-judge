import { useUserRestrictions } from "@/features/admin/hooks/useUserRestrictions";
import { isOtherAdmin } from "@/features/admin/users/moderation";
import type { AdminUserDetail } from "@/features/admin/users/types";
import { useCurrentUser } from "@/lib/useCurrentUser";

import { BlockError, BlockLoading } from "./BlockStates";
import { PageCard } from "./PageCard";
import { RestrictionsForm } from "./RestrictionsForm";

interface RestrictionsCardProps {
    user: Pick<AdminUserDetail, "username" | "role" | "deletedAt">;
}

function lockedNoteOf(currentUsername: string | undefined, user: RestrictionsCardProps["user"]) {
    if (currentUsername === user.username) {
        return "Bạn không thể tự đổi quyền hoặc hạn chế của chính mình.";
    }
    return isOtherAdmin(currentUsername, user)
        ? "Không thể đổi quyền hoặc hạn chế của quản trị viên khác."
        : null;
}

export function RestrictionsCard({ user }: RestrictionsCardProps) {
    const { data: currentUser } = useCurrentUser();
    const { data: restrictions, error, isPending, refetch } = useUserRestrictions(user.username);
    const lockedNote = lockedNoteOf(currentUser?.username, user);

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
                    isLocked={lockedNote !== null || user.deletedAt !== null}
                />
            ) : null}
            {currentUser && lockedNote ? (
                <p className="mt-2 text-xs text-muted-foreground">{lockedNote}</p>
            ) : null}
        </PageCard>
    );
}
