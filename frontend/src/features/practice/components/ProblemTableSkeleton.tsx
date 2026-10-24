import { Skeleton } from "@/components/shared/Skeleton";

const SKELETON_ROW_COUNT = 8;

export function ProblemTableSkeleton() {
    return (
        <div
            role="status"
            aria-busy="true"
            aria-label="Đang tải danh sách bài"
            className="flex flex-col gap-4 p-4"
        >
            {Array.from({ length: SKELETON_ROW_COUNT }, (_, index) => (
                <div key={index} className="flex items-center gap-4">
                    <Skeleton className="h-3 w-10" />
                    <Skeleton className="h-3 flex-1" />
                    <Skeleton className="h-5 w-18" />
                    <Skeleton className="h-3 w-14" />
                </div>
            ))}
        </div>
    );
}
