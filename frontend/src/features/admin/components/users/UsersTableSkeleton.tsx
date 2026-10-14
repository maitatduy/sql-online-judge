import { Skeleton } from "@/components/shared/Skeleton";

const SKELETON_ROW_COUNT = 8;

export function UsersTableSkeleton() {
    return (
        <div
            role="status"
            aria-busy="true"
            aria-label="Đang tải danh sách người dùng"
            className="flex flex-col"
        >
            {Array.from({ length: SKELETON_ROW_COUNT }, (_, index) => (
                <div key={index} className="flex items-center gap-2.5 border-b px-3 py-2.5">
                    <Skeleton className="size-7.5 shrink-0 rounded-full" />
                    <div className="flex flex-1 flex-col gap-1.5">
                        <Skeleton className="h-3 w-1/4" />
                        <Skeleton className="h-2.5 w-2/5" />
                    </div>
                    <Skeleton className="h-3 w-1/5" />
                </div>
            ))}
        </div>
    );
}
