import { Button } from "@/components/ui/button";

import type { PendingImport } from "../types";

interface PendingImportNoticeProps {
    pendingImport: PendingImport;
    onCancel: () => void;
}

export function PendingImportNotice({ pendingImport, onCancel }: PendingImportNoticeProps) {
    return (
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-md bg-brand-soft px-3 py-2 text-[13px]">
            <p>
                Dữ liệu của {pendingImport.data.length} bảng sẽ được lưu vào bộ{" "}
                {pendingImport.variantName} khi bấm Lưu.
            </p>
            <Button type="button" variant="outline" size="sm" onClick={onCancel}>
                Huỷ nhập
            </Button>
        </div>
    );
}
