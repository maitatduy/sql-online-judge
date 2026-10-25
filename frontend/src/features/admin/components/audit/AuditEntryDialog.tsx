import Link from "next/link";

import { Button } from "@/components/ui/button";
import { changedFieldOf, hasValueChange } from "@/features/admin/audit/entry-summary";
import type { AuditLogEntry } from "@/features/admin/audit/types";
import { ActionDialog } from "@/features/admin/components/users/ActionDialog";
import { formatAuditTime } from "@/features/admin/format";
import { ROLE_LABELS } from "@/features/admin/users/role-labels";
import { ROUTES } from "@/lib/routes";

import { AuditActionLabel } from "./AuditActionLabel";

const LABEL_CLASS = "text-sm text-muted-foreground";

interface AuditEntryDialogProps {
    entry: AuditLogEntry;
    onClose: () => void;
}

export function AuditEntryDialog({ entry, onClose }: AuditEntryDialogProps) {
    return (
        <ActionDialog
            title="Chi tiết thao tác"
            description={
                <span className="mt-1 flex flex-wrap items-center gap-2">
                    <AuditActionLabel action={entry.action} />
                    {formatAuditTime(entry.occurredAt)}
                </span>
            }
            onClose={onClose}
            className="max-w-[520px]"
        >
            <dl className="grid grid-cols-[auto_1fr] items-baseline gap-x-6 gap-y-2.5 text-[15px]">
                <dt className={LABEL_CLASS}>Người thực hiện</dt>
                <dd className="font-medium">
                    {entry.actorUsername} · {ROLE_LABELS[entry.actorRole]}
                </dd>
                <dt className={LABEL_CLASS}>Đối tượng</dt>
                <dd className="font-medium">
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
                {entry.expiresAt ? (
                    <>
                        <dt className={LABEL_CLASS}>Hết hạn</dt>
                        <dd className="font-medium">{formatAuditTime(entry.expiresAt)}</dd>
                    </>
                ) : null}
            </dl>
            {hasValueChange(entry) ? (
                <div>
                    <p className={`mb-1.5 ${LABEL_CLASS}`}>Thay đổi</p>
                    <div className="overflow-hidden rounded-lg border border-diff-border bg-diff-surface font-mono text-sm leading-relaxed">
                        <p className="bg-diff-removed/12 px-3.5 py-2 text-diff-removed">
                            {`- ${changedFieldOf(entry.action)}: ${JSON.stringify(entry.previousValue)}`}
                        </p>
                        <p className="bg-diff-added/12 px-3.5 py-2 text-diff-added">
                            {`+ ${changedFieldOf(entry.action)}: ${JSON.stringify(entry.newValue)}`}
                        </p>
                    </div>
                </div>
            ) : null}
            {entry.reason ? (
                <div>
                    <p className={`mb-1.5 ${LABEL_CLASS}`}>Lý do</p>
                    <p className="text-[15px] leading-relaxed">{entry.reason}</p>
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
