import { ChevronLeft, ChevronRight } from "lucide-react";

import { Button } from "@/components/ui/button";
import { formatNumber } from "@/lib/format";
import { pageRangeLabel, type PageInfo } from "@/lib/pagination";

interface HistoryPagerProps {
    result: PageInfo;
    isLoading: boolean;
    onPageChange: (page: number) => void;
}

export function HistoryPager({ result, isLoading, onPageChange }: HistoryPagerProps) {
    const { page, totalPages } = result;
    if (totalPages <= 1) {
        return null;
    }

    return (
        <div className="flex items-center justify-between gap-2 pt-3 text-[13px]">
            <span aria-live="polite" className="text-muted-foreground">
                {pageRangeLabel(result)}
            </span>
            <nav aria-label="Phân trang lịch sử nộp" className="flex items-center gap-1">
                <Button
                    type="button"
                    variant="outline"
                    size="icon"
                    aria-label="Trang trước"
                    disabled={page <= 1 || isLoading}
                    onClick={() => onPageChange(page - 1)}
                    className="size-8"
                >
                    <ChevronLeft aria-hidden="true" />
                </Button>
                <span className="min-w-16 text-center font-semibold">
                    {`Trang ${formatNumber(page)}/${formatNumber(totalPages)}`}
                </span>
                <Button
                    type="button"
                    variant="outline"
                    size="icon"
                    aria-label="Trang sau"
                    disabled={page >= totalPages || isLoading}
                    onClick={() => onPageChange(page + 1)}
                    className="size-8"
                >
                    <ChevronRight aria-hidden="true" />
                </Button>
            </nav>
        </div>
    );
}
