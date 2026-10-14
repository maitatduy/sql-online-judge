import { useUserContestHistory } from "@/features/admin/hooks/useUserContestHistory";

import { BlockEmpty, BlockError, BlockLoading } from "./BlockStates";
import { ContestTable } from "./ContestTable";
import { PageCard } from "./PageCard";

const RECENT_COUNT = 3;

interface RecentContestsCardProps {
    username: string;
    onShowAll: () => void;
}

export function RecentContestsCard({ username, onShowAll }: RecentContestsCardProps) {
    const { data, error, isPending, refetch } = useUserContestHistory(username, {
        page: 1,
        size: RECENT_COUNT,
    });

    return (
        <PageCard
            title="Contest gần đây"
            action={
                <button
                    type="button"
                    onClick={onShowAll}
                    className="text-[13px] font-semibold text-brand hover:underline"
                >
                    Xem tất cả
                </button>
            }
        >
            {isPending ? <BlockLoading label="Đang tải contest gần đây" /> : null}
            {error && !data ? <BlockError error={error} onRetry={() => void refetch()} /> : null}
            {data && data.items.length > 0 ? <ContestTable results={data.items} /> : null}
            {data && data.items.length === 0 ? (
                <BlockEmpty>Chưa tham gia contest nào.</BlockEmpty>
            ) : null}
        </PageCard>
    );
}
