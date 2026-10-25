"use client";

import { ScrollText } from "lucide-react";
import { useState } from "react";

import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Pagination } from "@/components/shared/Pagination";
import { Skeleton } from "@/components/shared/Skeleton";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { StaleDataNotice } from "@/features/admin/components/StaleDataNotice";
import { auditHref } from "@/features/admin/audit/search-params";
import type { AuditLogEntry } from "@/features/admin/audit/types";
import { useAuditFilters } from "@/features/admin/hooks/useAuditFilters";
import { useAuditLog } from "@/features/admin/hooks/useAuditLog";
import { ApiRequestError } from "@/lib/api-client";

import { AuditEntryDetail } from "./AuditEntryDetail";
import { AuditFilterBar } from "./AuditFilterBar";
import { AuditTable } from "./AuditTable";

const SKELETON_ROW_COUNT = 8;

export function AdminAuditLog() {
    const { filters, changeFilters, clearFilters } = useAuditFilters();
    const { data, dataUpdatedAt, error, isPending, isRefetchError, refetch } = useAuditLog(filters);
    const [selected, setSelected] = useState<AuditLogEntry | null>(null);

    return (
        <main id={MAIN_CONTENT_ID} className="flex flex-col gap-4 px-4 pt-6 pb-10 md:px-7">
            <div>
                <h1 className="text-[26px] font-extrabold">Nhật ký thao tác</h1>
                <p className="mt-0.5 text-[13px] text-muted-foreground">
                    Mọi thay đổi của quản trị viên trên tài khoản người dùng, chỉ đọc
                </p>
            </div>
            {isRefetchError ? <StaleDataNotice updatedAtMs={dataUpdatedAt} /> : null}
            <div className="flex flex-col items-start gap-4 lg:flex-row">
                <section
                    aria-label="Bảng nhật ký"
                    className="w-full min-w-0 flex-1 overflow-hidden rounded-lg border bg-card"
                >
                    <AuditFilterBar
                        filters={filters}
                        onChange={changeFilters}
                        onClear={clearFilters}
                    />
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
                                    selectedId={selected?.id ?? null}
                                    onSelect={setSelected}
                                />
                            </div>
                            <Pagination
                                result={data}
                                hrefOf={(page) => auditHref({ ...filters, page })}
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
                {selected ? <AuditEntryDetail entry={selected} /> : null}
            </div>
        </main>
    );
}
