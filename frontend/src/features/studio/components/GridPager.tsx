import { Button } from "@/components/ui/button";
import { pageRangeLabel, pageWindow } from "@/lib/pagination";

interface GridPagerProps {
    page: number;
    pageSize: number;
    totalRows: number;
    onChange: (page: number) => void;
}

export function GridPager({ page, pageSize, totalRows, onChange }: GridPagerProps) {
    const totalPages = Math.max(1, Math.ceil(totalRows / pageSize));

    return (
        <div className="flex flex-wrap items-center justify-between gap-2 text-[13px]">
            <span aria-live="polite" className="text-muted-foreground">
                {pageRangeLabel({ page, size: pageSize, totalItems: totalRows }, "dòng")}
            </span>
            {totalPages > 1 ? (
                <nav aria-label="Phân trang dữ liệu" className="flex flex-wrap gap-1">
                    <Button
                        type="button"
                        size="sm"
                        variant="outline"
                        disabled={page <= 1}
                        onClick={() => onChange(page - 1)}
                    >
                        Trước
                    </Button>
                    {pageWindow(page, totalPages).map((pageNumber) => (
                        <Button
                            key={pageNumber}
                            type="button"
                            size="sm"
                            variant={pageNumber === page ? "default" : "outline"}
                            aria-label={`Trang ${pageNumber}`}
                            aria-current={pageNumber === page ? "page" : undefined}
                            className="w-7.5 px-0"
                            onClick={() => onChange(pageNumber)}
                        >
                            {pageNumber}
                        </Button>
                    ))}
                    <Button
                        type="button"
                        size="sm"
                        variant="outline"
                        disabled={page >= totalPages}
                        onClick={() => onChange(page + 1)}
                    >
                        Sau
                    </Button>
                </nav>
            ) : null}
        </div>
    );
}
