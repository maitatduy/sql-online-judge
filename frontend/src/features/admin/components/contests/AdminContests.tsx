"use client";

import { useState } from "react";

import { Plus, Trophy } from "lucide-react";

import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { Button } from "@/components/ui/button";
import { ApiRequestError } from "@/lib/api-client";
import { formatNumber } from "@/lib/format";

import { useAdminContests } from "../../hooks/useAdminContests";
import { AdminContestRow } from "./AdminContestRow";
import { CreateContestDialog } from "./CreateContestDialog";

const HEADER_CLASS = "border-b px-4 py-2.5 text-left text-sm font-semibold text-muted-foreground";
const HEADERS = ["Contest", "Giai đoạn", "Bắt đầu", "Thời lượng", "Tính rating"];
const SKELETON_ROW_COUNT = 4;

export function AdminContests() {
    const { data, error, isPending, refetch } = useAdminContests();
    const [isCreating, setIsCreating] = useState(false);

    return (
        <main id={MAIN_CONTENT_ID} className="flex flex-col gap-4 px-4 pt-6 pb-10 md:px-7">
            <div className="flex flex-wrap items-end justify-between gap-3">
                <div>
                    <h1 className="text-[24px] font-extrabold tracking-[-0.02em]">Contest</h1>
                    {data ? (
                        <p className="mt-1 text-muted-foreground">
                            {formatNumber(data.totalItems)} contest
                        </p>
                    ) : null}
                </div>
                <Button type="button" onClick={() => setIsCreating(true)}>
                    <Plus aria-hidden="true" />
                    Tạo contest
                </Button>
            </div>
            {isPending ? (
                <div role="status" aria-label="Đang tải contest" className="flex flex-col gap-3">
                    {Array.from({ length: SKELETON_ROW_COUNT }, (_, index) => (
                        <Skeleton key={index} className="h-14" />
                    ))}
                </div>
            ) : null}
            {error && !data ? (
                <ErrorState
                    title="Chưa tải được contest"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                />
            ) : null}
            {data && data.items.length === 0 ? (
                <EmptyState title="Chưa có contest nào." icon={<Trophy />} />
            ) : null}
            {data && data.items.length > 0 ? (
                <div className="overflow-x-auto rounded-lg border bg-card">
                    <table aria-label="Contest" className="w-full min-w-[720px] border-collapse">
                        <thead>
                            <tr>
                                {HEADERS.map((header) => (
                                    <th key={header} scope="col" className={HEADER_CLASS}>
                                        {header}
                                    </th>
                                ))}
                            </tr>
                        </thead>
                        <tbody>
                            {data.items.map((contest) => (
                                <AdminContestRow key={contest.id} contest={contest} />
                            ))}
                        </tbody>
                    </table>
                </div>
            ) : null}
            <CreateContestDialog open={isCreating} onOpenChange={setIsCreating} />
        </main>
    );
}
