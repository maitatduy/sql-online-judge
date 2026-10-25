import Link from "next/link";

import { PageStepLink } from "@/components/shared/PageStepLink";
import { Button } from "@/components/ui/button";
import { pageRangeLabel, pageWindow, type PageInfo } from "@/lib/pagination";

interface PaginationProps {
    result: PageInfo;
    hrefOf: (page: number) => string;
    unit?: string;
}

export function Pagination({ result, hrefOf, unit }: PaginationProps) {
    const { page, totalPages } = result;

    return (
        <div className="flex flex-wrap items-center justify-between gap-2 px-3.5 py-2.5 text-[13px]">
            <span aria-live="polite" className="text-muted-foreground">
                {pageRangeLabel(result, unit)}
            </span>
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
