"use client";

import { ListChecks, Search } from "lucide-react";
import { useState } from "react";

import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Pagination } from "@/components/shared/Pagination";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { Button } from "@/components/ui/button";
import { ApiRequestError } from "@/lib/api-client";

import { useAdminSubmissions, useSubmissionFilters } from "../../hooks/useAdminSubmissions";
import { hasActiveSubmissionFilter, submissionsHref } from "../../submissions/search-params";
import { StaleDataNotice } from "../StaleDataNotice";
import { BlockLoading } from "../users/BlockStates";
import { SubmissionDetailDialog } from "./SubmissionDetailDialog";
import { SubmissionFiltersBar } from "./SubmissionFiltersBar";
import { SubmissionTable } from "./SubmissionTable";

export function AdminSubmissions() {
    const { filters, changeFilters, clearFilters } = useSubmissionFilters();
    const { data, dataUpdatedAt, error, isPending, isRefetchError, refetch } =
        useAdminSubmissions(filters);
    const [openedId, setOpenedId] = useState<string | null>(null);
    const hasSubmissions = data ? data.items.length > 0 : false;
    const isFiltered = hasActiveSubmissionFilter(filters);

    return (
        <main id={MAIN_CONTENT_ID} className="flex flex-col gap-4 px-4 pt-6 pb-10 md:px-7">
            <h1 className="text-[26px] font-extrabold">Bài nộp</h1>
            {isRefetchError ? <StaleDataNotice updatedAtMs={dataUpdatedAt} /> : null}
            <section
                aria-label="Danh sách bài nộp"
                className="overflow-hidden rounded-lg border bg-card"
            >
                <SubmissionFiltersBar
                    filters={filters}
                    onChange={changeFilters}
                    onClear={clearFilters}
                />
                {isPending ? (
                    <div className="p-4">
                        <BlockLoading label="Đang tải bài nộp" rows={8} />
                    </div>
                ) : null}
                {!data && error ? (
                    <ErrorState
                        title="Chưa tải được bài nộp"
                        message={error.message}
                        traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                        onRetry={() => void refetch()}
                        className="border-0"
                    />
                ) : null}
                {data && hasSubmissions ? (
                    <>
                        <SubmissionTable submissions={data.items} onOpen={setOpenedId} />
                        <Pagination
                            result={data}
                            hrefOf={(page) => submissionsHref({ ...filters, page })}
                            unit="bài nộp"
                        />
                    </>
                ) : null}
                {data && !hasSubmissions ? (
                    <EmptyState
                        title={isFiltered ? "Không có bài nộp khớp bộ lọc" : "Chưa có bài nộp nào"}
                        icon={isFiltered ? <Search /> : <ListChecks />}
                        action={
                            isFiltered ? (
                                <Button
                                    type="button"
                                    size="sm"
                                    variant="outline"
                                    onClick={clearFilters}
                                >
                                    Xoá lọc
                                </Button>
                            ) : undefined
                        }
                        className="border-0"
                    />
                ) : null}
            </section>
            {openedId ? (
                <SubmissionDetailDialog submissionId={openedId} onClose={() => setOpenedId(null)} />
            ) : null}
        </main>
    );
}
