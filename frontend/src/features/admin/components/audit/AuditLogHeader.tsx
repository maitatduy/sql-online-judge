import { Download } from "lucide-react";

import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import type { AuditFilters } from "@/features/admin/audit/search-params";
import { useExportAuditLog } from "@/features/admin/hooks/useExportAuditLog";

interface AuditLogHeaderProps {
    filters: AuditFilters;
}

export function AuditLogHeader({ filters }: AuditLogHeaderProps) {
    const showToast = useToast();
    const exportAuditLog = useExportAuditLog();

    function handleExport() {
        exportAuditLog.mutate(filters, {
            onError: (failure) => showToast(`Chưa xuất được nhật ký. ${failure.message}`, "error"),
        });
    }

    return (
        <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
                <h1 className="text-[26px] font-extrabold">Nhật ký thao tác</h1>
                <p className="mt-0.5 text-[13px] text-muted-foreground">
                    Mọi thay đổi của quản trị viên và kiểm duyệt viên · chỉ đọc
                </p>
            </div>
            <Button
                type="button"
                size="sm"
                variant="outline"
                disabled={exportAuditLog.isPending}
                onClick={handleExport}
            >
                <Download aria-hidden="true" />
                Xuất CSV
            </Button>
        </div>
    );
}
