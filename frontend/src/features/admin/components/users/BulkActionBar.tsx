import Link from "next/link";

import { Button } from "@/components/ui/button";
import { formatNumber } from "@/lib/format";
import { ROUTES } from "@/lib/routes";

interface BulkActionBarProps {
    selectedCount: number;
    onClear: () => void;
}

export function BulkActionBar({ selectedCount, onClear }: BulkActionBarProps) {
    return (
        <div
            role="region"
            aria-label="Thao tác hàng loạt"
            className="flex flex-wrap items-center gap-2.5 border-b border-brand/30 bg-brand-soft px-3.5 py-2 text-[13px]"
        >
            <strong className="text-brand">{`Đã chọn ${formatNumber(selectedCount)}`}</strong>
            <Button type="button" size="sm" variant="ghost" onClick={onClear}>
                Bỏ chọn
            </Button>
            <span className="flex-1" />
            <Button asChild size="sm" variant="outline">
                <Link href={ROUTES.adminMessages}>Gửi thông báo</Link>
            </Button>
        </div>
    );
}
