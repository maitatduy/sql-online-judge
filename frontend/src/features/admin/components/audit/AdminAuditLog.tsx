"use client";

import { ScrollText } from "lucide-react";
import dynamic from "next/dynamic";
import { useState } from "react";

import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Pagination } from "@/components/shared/Pagination";
import { Skeleton } from "@/components/shared/Skeleton";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { StaleDataNotice } from "@/features/admin/components/StaleDataNotice";
import { auditHref } from "@/features/admin/audit/search-params";
import { useAuditFilters } from "@/features/admin/hooks/useAuditFilters";
import { useAuditLog } from "@/features/admin/hooks/useAuditLog";
import { ApiRequestError } from "@/lib/api-client";
import { cn } from "@/lib/utils";

import { AuditFilterBar } from "./AuditFilterBar";
import { AuditLogHeader } from "./AuditLogHeader";
import { AuditTable } from "./AuditTable";

const AuditEntryDialog = dynamic(
    () => import("./AuditEntryDialog").then((dialogModule) => dialogModule.AuditEntryDialog),
    { ssr: false },
);

const SKELETON_ROW_COUNT = 8;

export function AdminAuditLog() {
    const { filters, changeFilters } = useAuditFilters();
    const { data, dataUpdatedAt, error, isPending, isPlaceholderData, isRefetchError, refetch } =
        useAuditLog(filters);
    const [openEntryId, setOpenEntryId] = useState<string | null>(null);
    const [mountedAt] = useState(() => new Date());
    const openEntry = data?.items.find((entry) => entry.id === openEntryId) ?? null;

    return (
        <main id={MAIN_CONTENT_ID} className="flex flex-col gap-4 px-4 pt-6 pb-10 md:px-7">
            <AuditLogHeader filters={filters} />
            {isRefetchError ? <StaleDataNotice updatedAtMs={dataUpdatedAt} /> : null}
            <AuditFilterBar filters={filters} onChange={changeFilters} />
            <section
                aria-label="Bảng nhật ký"
                aria-busy={isPlaceholderData}
                className={cn(
                    "overflow-hidden rounded-lg border bg-card transition-opacity",
                    isPlaceholderData && "opacity-60",
                )}
            >
                {isPending ? (
                    <div
                        role="status"
                        aria-busy="true"
                        aria-label="Đang tải nhật ký"
                        className="flex flex-col gap-3 p-3.5"
                    >
                        {Array.from({ length: SKELETON_ROW_COUNT }, (_, index) => (
                            <Skeleton key={index} className="h-5" />
                        ))}
                    </div>
                ) : null}
                {!data && error ? (
                    <ErrorState
                        title="Chưa tải được nhật ký"
                        message={error.message}
                        traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                        onRetry={() => void refetch()}
                        className="border-0"
                    />
                ) : null}
                {data && data.items.length > 0 ? (
                    <>
                        <div className="overflow-x-auto">
                            <AuditTable
                                entries={data.items}
                                onView={setOpenEntryId}
                                now={mountedAt}
                            />
                        </div>
                        <Pagination
                            result={data}
                            hrefOf={(page) => auditHref({ ...filters, page })}
                            unit="thao tác"
                        />
                    </>
                ) : null}
                {data && data.items.length === 0 ? (
                    <EmptyState
                        title="Không có thao tác nào"
                        description="Thử bỏ bớt bộ lọc hoặc chọn khoảng thời gian dài hơn."
                        icon={<ScrollText />}
                        className="border-0"
                    />
                ) : null}
            </section>
            {openEntry ? (
                <AuditEntryDialog entry={openEntry} onClose={() => setOpenEntryId(null)} />
            ) : null}
        </main>
    );
}
