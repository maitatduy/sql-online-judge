"use client";

import { Plus, Search } from "lucide-react";
import Link from "next/link";

import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Pagination } from "@/components/shared/Pagination";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { Button } from "@/components/ui/button";
import { ApiRequestError } from "@/lib/api-client";
import { ROUTES } from "@/lib/routes";

import { useProblemBank } from "../../hooks/useProblemBank";
import { useProblemBankFilters } from "../../hooks/useProblemBankFilters";
import { bankHref, hasActiveBankFilter } from "../../problem-bank/search-params";
import { StaleDataNotice } from "../StaleDataNotice";
import { BlockLoading } from "../users/BlockStates";
import { ProblemBankFiltersBar } from "./ProblemBankFilters";
import { ProblemBankStats } from "./ProblemBankStats";
import { ProblemBankTable } from "./ProblemBankTable";

export function AdminProblemBank() {
    const { filters, changeFilters, clearFilters } = useProblemBankFilters();
    const { data, dataUpdatedAt, error, isPending, isRefetchError, refetch } =
        useProblemBank(filters);
    const hasProblems = data ? data.items.length > 0 : false;
    const isFiltered = hasActiveBankFilter(filters);

    return (
        <main id={MAIN_CONTENT_ID} className="flex flex-col gap-4 px-4 pt-6 pb-10 md:px-7">
            <div className="flex flex-wrap items-center justify-between gap-3">
                <h1 className="text-[26px] font-extrabold">Kho bài tập</h1>
                <Button asChild size="sm">
                    <Link href={ROUTES.studioProblems}>
                        <Plus aria-hidden="true" />
                        Tạo bài
                    </Link>
                </Button>
            </div>
            <ProblemBankStats filters={filters} onChange={changeFilters} />
            {isRefetchError ? <StaleDataNotice updatedAtMs={dataUpdatedAt} /> : null}
            <section
                aria-label="Danh sách bài tập"
                className="overflow-hidden rounded-lg border bg-card"
            >
                <ProblemBankFiltersBar
                    filters={filters}
                    onChange={changeFilters}
                    onClear={clearFilters}
                />
                {isPending ? (
                    <div className="p-4">
                        <BlockLoading label="Đang tải kho bài tập" rows={8} />
                    </div>
                ) : null}
                {!data && error ? (
                    <ErrorState
                        title="Chưa tải được kho bài tập"
                        message={error.message}
                        traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                        onRetry={() => void refetch()}
                        className="border-0"
                    />
                ) : null}
                {data && hasProblems ? (
                    <>
                        <ProblemBankTable problems={data.items} />
                        <Pagination
                            result={data}
                            hrefOf={(page) => bankHref({ ...filters, page })}
                            unit="bài"
                        />
                    </>
                ) : null}
                {data && !hasProblems && isFiltered ? (
                    <EmptyState
                        title="Không có bài khớp bộ lọc"
                        icon={<Search />}
                        action={
                            <Button
                                type="button"
                                size="sm"
                                variant="outline"
                                onClick={clearFilters}
                            >
                                Xoá lọc
                            </Button>
                        }
                        className="border-0"
                    />
                ) : null}
                {data && !hasProblems && !isFiltered ? (
                    <EmptyState
                        title="Chưa có bài nào được xuất bản"
                        action={
                            <Button asChild size="sm" variant="outline">
                                <Link href={ROUTES.studioProblems}>Tạo bài</Link>
                            </Button>
                        }
                        className="border-0"
                    />
                ) : null}
            </section>
        </main>
    );
}
