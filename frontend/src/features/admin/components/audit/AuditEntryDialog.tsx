import Link from "next/link";

import { Button } from "@/components/ui/button";
import { AUDIT_ACTION_LABELS } from "@/features/admin/audit/action-labels";
import { changedFieldOf, hasValueChange } from "@/features/admin/audit/entry-summary";
import type { AuditLogEntry } from "@/features/admin/audit/types";
import { formatAuditTime } from "@/features/admin/format";
import { ROLE_LABELS } from "@/features/admin/users/role-labels";
import { ActionDialog } from "@/features/admin/components/users/ActionDialog";
import { ROUTES } from "@/lib/routes";

interface AuditEntryDialogProps {
    entry: AuditLogEntry;
    onClose: () => void;
}

export function AuditEntryDialog({ entry, onClose }: AuditEntryDialogProps) {
    return (
        <ActionDialog
            title="Chi tiết thao tác"
            description="Nhật ký không sửa hay xoá được. Thao tác nhạy cảm (khoá, xoá tài khoản, đổi vai trò) bắt buộc nhập lý do."
            onClose={onClose}
            className="max-w-[520px]"
        >
            <dl className="flex flex-col gap-1 text-[13px]">
                <div>
                    <dt className="inline text-muted-foreground">Thao tác: </dt>
                    <dd className="inline">{AUDIT_ACTION_LABELS[entry.action]}</dd>
                </div>
                <div>
                    <dt className="inline text-muted-foreground">Người thực hiện: </dt>
                    <dd className="inline">
                        {entry.actorUsername} ({ROLE_LABELS[entry.actorRole]})
                    </dd>
                </div>
                <div>
                    <dt className="inline text-muted-foreground">Đối tượng: </dt>
                    <dd className="inline">
                        {entry.targetUsername ? (
                            <Link
                                href={ROUTES.adminUser(entry.targetUsername)}
                                className="text-brand hover:underline"
                            >
                                {entry.targetUsername}
                            </Link>
                        ) : (
                            (entry.details ?? "-")
                        )}
                    </dd>
                </div>
                <div>
                    <dt className="inline text-muted-foreground">Thời gian: </dt>
                    <dd className="inline">{formatAuditTime(entry.occurredAt)}</dd>
                </div>
                {entry.expiresAt ? (
                    <div>
                        <dt className="inline text-muted-foreground">Hết hạn: </dt>
                        <dd className="inline">{formatAuditTime(entry.expiresAt)}</dd>
                    </div>
                ) : null}
            </dl>
            {hasValueChange(entry) ? (
                <div>
                    <p className="mb-1.5 text-[13px] font-semibold">Thay đổi</p>
                    <div className="overflow-hidden rounded-lg border border-diff-border bg-diff-surface font-mono text-xs leading-[1.8]">
                        <p className="bg-diff-removed/12 px-3 py-2 text-diff-removed">
                            {`- ${changedFieldOf(entry.action)}: ${JSON.stringify(entry.previousValue)}`}
                        </p>
                        <p className="bg-diff-added/12 px-3 py-2 text-diff-added">
                            {`+ ${changedFieldOf(entry.action)}: ${JSON.stringify(entry.newValue)}`}
                        </p>
                    </div>
                </div>
            ) : null}
            {entry.reason ? (
                <div className="rounded-lg border bg-surface px-3 py-2.5 text-[13px] leading-relaxed">
                    <span className="font-semibold">Lý do: </span>
                    <span>{entry.reason}</span>
                </div>
            ) : null}
            <div className="flex justify-end">
                <Button type="button" variant="outline" onClick={onClose}>
                    Đóng
                </Button>
            </div>
        </ActionDialog>
    );
}
