import Link from "next/link";

import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { Button } from "@/components/ui/button";
import { ROUTES } from "@/lib/routes";
import { cn } from "@/lib/utils";

interface ErrorFallbackProps {
    onRetry: () => void;
    className?: string;
}

export function ErrorFallback({ onRetry, className }: ErrorFallbackProps) {
    return (
        <main
            id={MAIN_CONTENT_ID}
            className={cn(
                "flex flex-col items-center justify-center gap-4 px-6 text-center",
                className,
            )}
        >
            <h1 className="text-3xl font-extrabold tracking-[-0.02em]">Đã có lỗi xảy ra</h1>
            <p className="max-w-md text-[17px] leading-[1.6] text-muted-foreground">
                Trang chưa tải được. Bạn thử lại sau ít phút nhé.
            </p>
            <div className="mt-4 flex flex-wrap justify-center gap-3">
                <Button type="button" size="lg" onClick={onRetry}>
                    Thử lại
                </Button>
                <Button asChild size="lg" variant="outline">
                    <Link href={ROUTES.home}>Về trang chủ</Link>
                </Button>
            </div>
        </main>
    );
}
