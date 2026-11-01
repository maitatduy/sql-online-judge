"use client";

import { useState } from "react";

import { Plus, Search } from "lucide-react";

import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { NativeSelect } from "@/components/shared/NativeSelect";
import { Skeleton } from "@/components/shared/Skeleton";
import { Button } from "@/components/ui/button";
import { ApiRequestError } from "@/lib/api-client";
import { DIFFICULTIES, DIFFICULTY_LABELS, type Difficulty } from "@/lib/difficulty";
import { formatNumber } from "@/lib/format";
import { useDebouncedSearch } from "@/lib/useDebouncedSearch";

import { useMyProblemFilters } from "../hooks/useMyProblemFilters";
import { useMyProblems } from "../hooks/useMyProblems";
import { PROBLEM_SORTS } from "../list-options";
import { myProblemsHref } from "../my-problem-filters";
import { CreateProblemDialog } from "./CreateProblemDialog";
import { ListFooter } from "./ListFooter";
import { MyProblemsTable } from "./MyProblemsTable";

const SKELETON_ROW_COUNT = 5;

export function MyProblems() {
    const { filters, changeFilters } = useMyProblemFilters();
    const { data, error, isPending, refetch } = useMyProblems(filters);
    const { searchText, changeSearchText } = useDebouncedSearch(filters.q, (q) =>
        changeFilters({ q }),
    );
    const hasFilter = filters.difficulty !== null || filters.q !== "";
    const [isCreateOpen, setIsCreateOpen] = useState(false);

    return (
        <div className="mx-auto flex max-w-[1280px] flex-col gap-5 px-6 pt-7 pb-16">
            <div className="flex flex-wrap items-end justify-between gap-3">
                <div>
                    <p className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                        Khu vực người ra đề
                    </p>
                    <h1 className="mt-1.5 text-[28px] font-extrabold tracking-[-0.02em]">
                        Bài của tôi
                    </h1>
                    {data ? (
                        <p className="mt-1 text-muted-foreground">
                            {formatNumber(data.totalItems)} bài
                        </p>
                    ) : null}
                </div>
                <Button type="button" onClick={() => setIsCreateOpen(true)}>
                    <Plus aria-hidden="true" />
                    Tạo bài mới
                </Button>
            </div>
            <CreateProblemDialog open={isCreateOpen} onOpenChange={setIsCreateOpen} />
            <div className="flex flex-wrap items-center gap-3">
                <label className="flex h-9 w-full items-center gap-2 rounded-md border px-3 text-muted-foreground focus-within:ring-2 focus-within:ring-ring sm:w-72">
                    <Search aria-hidden="true" className="size-4 shrink-0" />
                    <input
                        type="search"
                        aria-label="Tìm bài"
                        placeholder="Tìm bài…"
                        value={searchText}
                        onChange={(event) => changeSearchText(event.target.value)}
                        className="min-w-0 flex-1 bg-transparent text-sm text-foreground outline-none"
                    />
                </label>
                <NativeSelect
                    aria-label="Độ khó"
                    value={filters.difficulty ?? ""}
                    onChange={(event) =>
                        changeFilters({
                            difficulty: (event.target.value || null) as Difficulty | null,
                        })
                    }
                >
                    <option value="">Mọi độ khó</option>
                    {DIFFICULTIES.map((difficulty) => (
                        <option key={difficulty} value={difficulty}>
                            {DIFFICULTY_LABELS[difficulty]}
                        </option>
                    ))}
                </NativeSelect>
                <NativeSelect
                    aria-label="Sắp xếp"
                    value={filters.sort}
                    onChange={(event) => changeFilters({ sort: event.target.value })}
                >
                    {PROBLEM_SORTS.map((option) => (
                        <option key={option.value} value={option.value}>
                            {option.label}
                        </option>
                    ))}
                </NativeSelect>
            </div>
            {isPending ? (
                <div
                    role="status"
                    aria-label="Đang tải danh sách bài"
                    className="flex flex-col gap-3"
                >
                    {Array.from({ length: SKELETON_ROW_COUNT }, (_, index) => (
                        <Skeleton key={index} className="h-10" />
                    ))}
                </div>
            ) : null}
            {error && !data ? (
                <ErrorState
                    title="Không tải được danh sách bài"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                />
            ) : null}
            {data && data.items.length === 0 ? (
                <EmptyState
                    title={hasFilter ? "Không có bài nào khớp bộ lọc." : "Bạn chưa có bài nào."}
                />
            ) : null}
            {data && data.items.length > 0 ? (
                <>
                    <MyProblemsTable problems={data.items} />
                    <ListFooter
                        result={data}
                        hrefOf={(page) => myProblemsHref({ ...filters, page })}
                        unit="bài"
                        size={filters.size}
                        onSizeChange={(size) => changeFilters({ size })}
                    />
                </>
            ) : null}
        </div>
    );
}
