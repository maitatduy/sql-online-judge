"use client";

import { useState } from "react";

import { Plus, Search } from "lucide-react";

import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Pagination } from "@/components/shared/Pagination";
import { Skeleton } from "@/components/shared/Skeleton";
import { Button } from "@/components/ui/button";
import { ApiRequestError } from "@/lib/api-client";
import { formatNumber } from "@/lib/format";
import { useDebouncedSearch } from "@/lib/useDebouncedSearch";

import { useMyDatasetFilters, useMyDatasets } from "../hooks/useMyDatasets";
import { myDatasetsHref } from "../my-dataset-filters";
import { CreateDatasetDialog } from "./CreateDatasetDialog";
import { MyDatasetsTable } from "./MyDatasetsTable";

const SKELETON_ROW_COUNT = 4;

export function MyDatasets() {
    const { filters, changeFilters } = useMyDatasetFilters();
    const { data, error, isPending, refetch } = useMyDatasets(filters);
    const { searchText, changeSearchText } = useDebouncedSearch(filters.q, (q) =>
        changeFilters({ q }),
    );
    const [isCreateOpen, setIsCreateOpen] = useState(false);

    return (
        <div className="mx-auto flex max-w-[1280px] flex-col gap-5 px-6 pt-7 pb-16">
            <div className="flex flex-wrap items-end justify-between gap-3">
                <div>
                    <p className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                        Khu vực người ra đề
                    </p>
                    <h1 className="mt-1.5 text-[28px] font-extrabold tracking-[-0.02em]">
                        Dataset của tôi
                    </h1>
                    {data ? (
                        <p className="mt-1 text-muted-foreground">
                            {formatNumber(data.totalItems)} dataset
                        </p>
                    ) : null}
                </div>
                <Button type="button" onClick={() => setIsCreateOpen(true)}>
                    <Plus aria-hidden="true" />
                    Tạo dataset
                </Button>
            </div>
            <CreateDatasetDialog open={isCreateOpen} onOpenChange={setIsCreateOpen} />
            <label className="flex h-9 w-full items-center gap-2 rounded-md border px-3 text-muted-foreground focus-within:ring-2 focus-within:ring-ring sm:w-72">
                <Search aria-hidden="true" className="size-4 shrink-0" />
                <input
                    type="search"
                    aria-label="Tìm dataset"
                    placeholder="Tìm dataset…"
                    value={searchText}
                    onChange={(event) => changeSearchText(event.target.value)}
                    className="min-w-0 flex-1 bg-transparent text-sm text-foreground outline-none"
                />
            </label>
            {isPending ? (
                <div
                    role="status"
                    aria-label="Đang tải danh sách dataset"
                    className="flex flex-col gap-3"
                >
                    {Array.from({ length: SKELETON_ROW_COUNT }, (_, index) => (
                        <Skeleton key={index} className="h-10" />
                    ))}
                </div>
            ) : null}
            {error && !data ? (
                <ErrorState
                    title="Không tải được danh sách dataset"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                />
            ) : null}
            {data && data.items.length === 0 ? (
                <EmptyState
                    title={
                        filters.q
                            ? "Không có dataset nào khớp từ khoá."
                            : "Bạn chưa có dataset nào."
                    }
                />
            ) : null}
            {data && data.items.length > 0 ? (
                <>
                    <MyDatasetsTable datasets={data.items} />
                    {data.totalPages > 1 ? (
                        <Pagination
                            result={data}
                            hrefOf={(page) => myDatasetsHref({ ...filters, page })}
                            unit="dataset"
                        />
                    ) : null}
                </>
            ) : null}
        </div>
    );
}
