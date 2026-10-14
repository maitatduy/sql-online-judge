import { useState } from "react";

import { Button } from "@/components/ui/button";
import { useUserContestHistory } from "@/features/admin/hooks/useUserContestHistory";
import { pageRangeLabel } from "@/features/admin/users/pagination";

import { BlockEmpty, BlockError, BlockLoading } from "./BlockStates";
import { ContestTable } from "./ContestTable";
import { PageCard } from "./PageCard";

const PAGE_SIZE = 10;

interface ContestHistoryTabProps {
    username: string;
}

export function ContestHistoryTab({ username }: ContestHistoryTabProps) {
    const [page, setPage] = useState(1);
    const { data, error, isPending, refetch } = useUserContestHistory(username, {
        page,
        size: PAGE_SIZE,
    });
    const hasResults = data ? data.items.length > 0 : false;

    return (
        <PageCard title="Lịch sử contest">
            {isPending ? <BlockLoading label="Đang tải lịch sử contest" rows={5} /> : null}
            {error && !data ? <BlockError error={error} onRetry={() => void refetch()} /> : null}
            {data && hasResults ? (
                <>
                    <ContestTable results={data.items} />
                    <div className="mt-3 flex flex-wrap items-center justify-between gap-2 text-[13px]">
                        <span aria-live="polite" className="text-muted-foreground">
                            {pageRangeLabel(data)}
                        </span>
                        <div className="flex gap-1">
                            <Button
                                type="button"
                                size="sm"
                                variant="outline"
                                disabled={page <= 1}
                                onClick={() => setPage(page - 1)}
                            >
                                Trang trước
                            </Button>
                            <Button
                                type="button"
                                size="sm"
                                variant="outline"
                                disabled={page >= data.totalPages}
                                onClick={() => setPage(page + 1)}
                            >
                                Trang sau
                            </Button>
                        </div>
                    </div>
                </>
            ) : null}
            {data && !hasResults ? <BlockEmpty>Chưa tham gia contest nào.</BlockEmpty> : null}
        </PageCard>
    );
}
