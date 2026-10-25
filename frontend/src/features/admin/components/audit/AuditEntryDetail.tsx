import Link from "next/link";

import { AUDIT_ACTION_LABELS } from "@/features/admin/audit/action-labels";
import type { AuditLogEntry } from "@/features/admin/audit/types";
import { ROLE_LABELS } from "@/features/admin/users/role-labels";
import { ROUTES } from "@/lib/routes";

import { formatAuditTime } from "./AuditTable";

interface AuditEntryDetailProps {
    entry: AuditLogEntry;
}

function hasValueChange({ previousValue, newValue }: AuditLogEntry): boolean {
    return previousValue !== null || newValue !== null;
}

export function AuditEntryDetail({ entry }: AuditEntryDetailProps) {
    return (
        <aside
            aria-label="Chi tiết thao tác"
            className="flex w-full flex-col gap-3 rounded-lg border bg-card p-4.5 lg:w-95 lg:flex-none"
        >
            <h2 className="text-base font-bold">Chi tiết</h2>
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
                <div className="flex flex-col gap-1 rounded-md bg-surface-2 px-3 py-2 font-mono text-xs">
                    <span>Trước: {entry.previousValue ?? "(trống)"}</span>
                    <span>Sau: {entry.newValue ?? "(trống)"}</span>
                </div>
            ) : null}
            {entry.reason ? (
                <div className="rounded-md border px-3 py-2 text-[13px] leading-relaxed">
                    <span className="font-semibold">Lý do: </span>
                    <span>{entry.reason}</span>
                </div>
            ) : null}
            <p className="text-xs text-muted-foreground">Nhật ký không sửa hay xoá được.</p>
        </aside>
    );
}
