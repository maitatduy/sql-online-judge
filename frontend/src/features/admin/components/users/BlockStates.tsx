import { Skeleton } from "@/components/shared/Skeleton";
import { Button } from "@/components/ui/button";
import { ApiRequestError } from "@/lib/api-client";

interface BlockLoadingProps {
    label: string;
    rows?: number;
}

export function BlockLoading({ label, rows = 3 }: BlockLoadingProps) {
    return (
        <div role="status" aria-busy="true" aria-label={label} className="flex flex-col gap-2.5">
            {Array.from({ length: rows }, (_, index) => (
                <Skeleton key={index} className="h-4" />
            ))}
        </div>
    );
}

interface BlockErrorProps {
    error: Error;
    onRetry: () => void;
}

export function BlockError({ error, onRetry }: BlockErrorProps) {
    const traceId = error instanceof ApiRequestError ? error.traceId : undefined;

    return (
        <div role="alert" className="flex flex-wrap items-center gap-2 text-[13px]">
            <span className="text-destructive">{`Chưa tải được dữ liệu. ${error.message}`}</span>
            {traceId ? <span className="text-muted-foreground">{`Mã lỗi: ${traceId}`}</span> : null}
            <Button type="button" size="sm" variant="outline" onClick={onRetry}>
                Thử lại
            </Button>
        </div>
    );
}

export function BlockEmpty({ children }: { children: React.ReactNode }) {
    return <p className="text-[13px] text-muted-foreground">{children}</p>;
}
