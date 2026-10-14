import Link from "next/link";

import { Button } from "@/components/ui/button";
import { pageRangeLabel, pageWindow } from "@/features/admin/users/pagination";
import { usersHref, type UsersFilters } from "@/features/admin/users/search-params";
import type { AdminUsersResponse } from "@/features/admin/users/types";

import { PageStepLink } from "./PageStepLink";

interface UsersPaginationProps {
    filters: UsersFilters;
    result: Pick<AdminUsersResponse, "page" | "size" | "totalItems" | "totalPages">;
}

export function UsersPagination({ filters, result }: UsersPaginationProps) {
    const { page, totalPages } = result;
    const hrefOf = (targetPage: number) => usersHref({ ...filters, page: targetPage });

    return (
        <div className="flex flex-wrap items-center justify-between gap-2 px-3.5 py-2.5 text-[13px]">
            <span className="text-muted-foreground">{pageRangeLabel(result)}</span>
            {totalPages > 1 ? (
                <nav aria-label="Phân trang" className="flex flex-wrap gap-1">
                    <PageStepLink label="Trước" href={hrefOf(page - 1)} isAvailable={page > 1} />
                    {pageWindow(page, totalPages).map((pageNumber) => (
                        <Button
                            key={pageNumber}
                            asChild
                            size="sm"
                            variant={pageNumber === page ? "default" : "outline"}
                            className="w-7.5 px-0"
                        >
                            <Link
                                href={hrefOf(pageNumber)}
                                aria-label={`Trang ${pageNumber}`}
                                aria-current={pageNumber === page ? "page" : undefined}
                            >
                                {pageNumber}
                            </Link>
                        </Button>
                    ))}
                    <PageStepLink
                        label="Sau"
                        href={hrefOf(page + 1)}
                        isAvailable={page < totalPages}
                    />
                </nav>
            ) : null}
        </div>
    );
}
