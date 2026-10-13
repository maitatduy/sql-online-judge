import { Skeleton } from "@/components/shared/Skeleton";

const STAT_TILE_COUNT = 4;

export function OverviewSkeleton() {
    return (
        <div
            role="status"
            aria-busy="true"
            aria-label="Đang tải số liệu tổng quan"
            className="grid grid-cols-[repeat(auto-fit,minmax(200px,1fr))] gap-4"
        >
            {Array.from({ length: STAT_TILE_COUNT }, (_, index) => (
                <div key={index} className="flex flex-col gap-2.5 rounded-lg border bg-card p-4.5">
                    <Skeleton className="h-3 w-3/5" />
                    <Skeleton className="h-7 w-1/2" />
                    <Skeleton className="h-2.5 w-4/5" />
                </div>
            ))}
        </div>
    );
}
