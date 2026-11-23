import { useState } from "react";

import { Button } from "@/components/ui/button";
import { formatNumber } from "@/lib/format";

import { type BulkAction, BulkProblemDialog } from "./BulkProblemDialog";

interface ProblemBankBulkBarProps {
    selectedIds: string[];
    onClear: () => void;
}

export function ProblemBankBulkBar({ selectedIds, onClear }: ProblemBankBulkBarProps) {
    const [action, setAction] = useState<BulkAction | null>(null);

    return (
        <div
            role="region"
            aria-label="Thao tác hàng loạt"
            className="flex flex-wrap items-center gap-2.5 border-b border-brand/30 bg-brand-soft px-3.5 py-2 text-[13px]"
        >
            <strong className="text-brand">{`Đã chọn ${formatNumber(selectedIds.length)}`}</strong>
            <Button type="button" size="sm" variant="ghost" onClick={onClear}>
                Bỏ chọn
            </Button>
            <span className="flex-1" />
            <Button type="button" size="sm" variant="outline" onClick={() => setAction("tag")}>
                Gắn chủ đề
            </Button>
            <Button
                type="button"
                size="sm"
                variant="outline"
                onClick={() => setAction("difficulty")}
            >
                Đổi độ khó
            </Button>
            <Button
                type="button"
                size="sm"
                variant="outline"
                onClick={() => setAction("hide")}
                className="text-destructive"
            >
                Ẩn bài
            </Button>
            {action ? (
                <BulkProblemDialog
                    key={action}
                    action={action}
                    problemIds={selectedIds}
                    onClose={() => setAction(null)}
                    onDone={() => {
                        setAction(null);
                        onClear();
                    }}
                />
            ) : null}
        </div>
    );
}
