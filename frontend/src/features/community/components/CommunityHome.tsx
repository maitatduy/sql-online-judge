"use client";

import { Search } from "lucide-react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";

import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Pagination } from "@/components/shared/Pagination";
import { Skeleton } from "@/components/shared/Skeleton";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { Input } from "@/components/ui/input";
import { ApiRequestError } from "@/lib/api-client";
import { useDebouncedSearch } from "@/lib/useDebouncedSearch";
import { useNow } from "@/lib/useNow";
import { cn } from "@/lib/utils";

import { useCommunityPosts } from "../hooks/useCommunity";
import { POST_TAB_LABELS, POST_TABS } from "../labels";
import { communityHref, readPostFilters } from "../search-params";
import { CategoryNav } from "./CategoryNav";
import { CommunityAside } from "./CommunityAside";
import { PostRow } from "./PostRow";

export function CommunityHome() {
    const filters = readPostFilters(useSearchParams());
    const now = useNow();
    const { data, error, isPending, refetch } = useCommunityPosts(filters);
    const { searchText, changeSearchText } = useDebouncedSearch(filters.q, (q) =>
        window.history.replaceState(null, "", communityHref({ ...filters, q, page: 1 })),
    );

    return (
        <main
            id={MAIN_CONTENT_ID}
            className="mx-auto grid w-full max-w-7xl gap-7 px-4 pt-6 pb-16 md:px-6 lg:grid-cols-[200px_minmax(0,1fr)_280px]"
        >
            <CategoryNav active={filters.category} />
            <section className="flex min-w-0 flex-col gap-3">
                <div className="flex flex-wrap items-center justify-between gap-3">
                    <h1 className="text-[28px] font-extrabold">Cộng đồng</h1>
                    <div className="relative w-full sm:w-72">
                        <Search
                            aria-hidden="true"
                            className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground"
                        />
                        <Input
                            type="search"
                            aria-label="Tìm bài viết"
                            placeholder="Tìm bài viết…"
                            value={searchText}
                            onChange={(event) => changeSearchText(event.target.value)}
                            className="pl-8.5 shadow-none"
                        />
                    </div>
                </div>
                <nav aria-label="Sắp xếp bài viết" className="flex gap-1 overflow-x-auto border-b">
                    {POST_TABS.map((tab) => (
                        <Link
                            key={tab}
                            href={communityHref({ ...filters, tab, page: 1 })}
                            aria-current={filters.tab === tab ? "page" : undefined}
                            className={cn(
                                "-mb-px border-b-2 px-3 py-2.5 text-[15px] font-semibold whitespace-nowrap",
                                filters.tab === tab
                                    ? "border-brand text-brand"
                                    : "border-transparent text-muted-foreground hover:text-foreground",
                            )}
                        >
                            {POST_TAB_LABELS[tab]}
                        </Link>
                    ))}
                </nav>
                {isPending ? (
                    <div
                        role="status"
                        aria-label="Đang tải bài viết"
                        className="flex flex-col gap-4 py-4"
                    >
                        {Array.from({ length: 5 }, (_, index) => (
                            <Skeleton key={index} className="h-20" />
                        ))}
                    </div>
                ) : null}
                {!data && error ? (
                    <ErrorState
                        title="Chưa tải được bài viết"
                        message={error.message}
                        traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                        onRetry={() => void refetch()}
                    />
                ) : null}
                {data && data.items.length > 0 ? (
                    <>
                        <ul aria-label="Bài viết">
                            {data.items.map((post) => (
                                <PostRow key={post.id} post={post} now={now} />
                            ))}
                        </ul>
                        <Pagination
                            result={data}
                            hrefOf={(page) => communityHref({ ...filters, page })}
                            unit="bài viết"
                        />
                    </>
                ) : null}
                {data && data.items.length === 0 ? (
                    <EmptyState
                        title={
                            filters.tab === "SAVED" ? "Bạn chưa lưu bài nào" : "Chưa có bài viết"
                        }
                        className="border-0"
                    />
                ) : null}
            </section>
            <CommunityAside />
        </main>
    );
}
