"use client";

import { Search } from "lucide-react";

import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Pagination } from "@/components/shared/Pagination";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { Button } from "@/components/ui/button";
import { ApiRequestError } from "@/lib/api-client";
import { useCurrentUser } from "@/lib/useCurrentUser";

import { useProblemFilters } from "../hooks/useProblemFilters";
import { useProblems } from "../hooks/useProblems";
import { hasActiveProblemFilter, problemsHref } from "../problem-filters";
import { ProblemFilters } from "./ProblemFilters";
import { ProblemTable } from "./ProblemTable";
import { ProblemTableSkeleton } from "./ProblemTableSkeleton";

export function ProblemList() {
    const { filters, changeFilters, clearFilters } = useProblemFilters();
    const { data: problems, error, isPending, refetch } = useProblems(filters);
    const { data: currentUser } = useCurrentUser();
    const isSignedIn = currentUser !== undefined;
    const isFiltered = hasActiveProblemFilter(filters);

    return (
        <main
            id={MAIN_CONTENT_ID}
            className="mx-auto flex w-full max-w-7xl flex-col gap-5 px-4 pt-7 pb-16 md:px-6"
        >
            <div>
                <h1 className="text-[22px] font-extrabold tracking-tight">Luyện tập</h1>
                <p className="mt-1 text-muted-foreground">Chấm tự động với testcase ẩn</p>
            </div>
            <section aria-label="Bài tập" className="overflow-hidden rounded-lg border bg-card">
                <ProblemFilters
                    filters={filters}
                    showProgress={isSignedIn}
                    onChange={changeFilters}
                    onClear={clearFilters}
                />
                {isPending ? <ProblemTableSkeleton /> : null}
                {!problems && error ? (
                    <ErrorState
                        title="Chưa tải được danh sách bài"
                        message={error.message}
                        traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                        onRetry={() => void refetch()}
                        className="border-0"
                    />
                ) : null}
                {problems && problems.items.length > 0 ? (
                    <>
                        <ProblemTable problems={problems.items} showProgress={isSignedIn} />
                        <Pagination
                            result={problems}
                            hrefOf={(page) => problemsHref({ ...filters, page })}
                        />
                    </>
                ) : null}
                {problems && problems.items.length === 0 && isFiltered ? (
                    <EmptyState
                        icon={<Search />}
                        title="Không có bài khớp bộ lọc"
                        description="Thử từ khoá khác hoặc bỏ bớt bộ lọc."
                        action={
                            <Button
                                type="button"
                                size="sm"
                                variant="outline"
                                onClick={clearFilters}
                            >
                                Xoá bộ lọc
                            </Button>
                        }
                        className="border-0"
                    />
                ) : null}
                {problems && problems.items.length === 0 && !isFiltered ? (
                    <EmptyState title="Chưa có bài nào được xuất bản" className="border-0" />
                ) : null}
            </section>
        </main>
    );
}
