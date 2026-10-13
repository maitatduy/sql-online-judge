import { Skeleton } from "@/components/shared/Skeleton";

const STAT_TILE_COUNT = 4;

export function ProfileSkeleton() {
    return (
        <div
            role="status"
            aria-busy="true"
            aria-label="Đang tải hồ sơ"
            className="flex w-full flex-wrap items-start gap-6"
        >
            <div className="flex min-w-0 flex-[1_1_280px] flex-col gap-4 rounded-lg border bg-card p-6 lg:max-w-[340px]">
                <Skeleton className="size-22 rounded-full" />
                <Skeleton className="h-6 w-3/5" />
                <Skeleton className="h-3 w-2/5" />
                <Skeleton className="h-6 w-1/2 rounded-full" />
                <Skeleton className="h-3 w-full" />
                <Skeleton className="h-3 w-4/5" />
                <Skeleton className="h-3 w-3/5" />
            </div>
            <div className="grid min-w-0 flex-[999_1_600px] grid-cols-[repeat(auto-fit,minmax(160px,1fr))] gap-3.5">
                {Array.from({ length: STAT_TILE_COUNT }, (_, index) => (
                    <div
                        key={index}
                        className="flex flex-col gap-2.5 rounded-lg border bg-card p-4"
                    >
                        <Skeleton className="h-2.5 w-3/5" />
                        <Skeleton className="h-6.5 w-1/2" />
                    </div>
                ))}
            </div>
        </div>
    );
}
