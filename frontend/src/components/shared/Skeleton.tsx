import { cn } from "@/lib/utils";

interface SkeletonProps {
    className?: string;
}

export function Skeleton({ className }: SkeletonProps) {
    return (
        <span
            aria-hidden="true"
            className={cn(
                "block animate-pulse rounded-md bg-surface-2 motion-reduce:animate-none",
                className,
            )}
        />
    );
}
