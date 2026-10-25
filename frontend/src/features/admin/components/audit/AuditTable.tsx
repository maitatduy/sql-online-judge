import { AUDIT_ACTION_LABELS } from "@/features/admin/audit/action-labels";
import type { AuditLogEntry } from "@/features/admin/audit/types";
import { formatDate } from "@/lib/format";
import { cn } from "@/lib/utils";

import { formatVietnamTime } from "../../format";
import { AuditActionLabel } from "./AuditActionLabel";

const HEADER_CELL_CLASS = "px-3.5 py-2.5 text-left text-xs font-semibold text-muted-foreground";
const CELL_CLASS = "border-t px-3.5 py-2.5 align-top";

interface AuditTableProps {
    entries: AuditLogEntry[];
    selectedId: string | null;
    onSelect: (entry: AuditLogEntry) => void;
}

export function formatAuditTime(occurredAt: string): string {
    return `${formatDate(occurredAt)} ${formatVietnamTime(occurredAt)}`;
}

export function AuditTable({ entries, selectedId, onSelect }: AuditTableProps) {
    return (
        <table className="w-full min-w-[760px] border-collapse text-[13px]">
            <thead>
                <tr>
                    <th scope="col" className={cn(HEADER_CELL_CLASS, "w-36")}>
                        Thời gian
                    </th>
                    <th scope="col" className={cn(HEADER_CELL_CLASS, "w-32")}>
                        Người thực hiện
                    </th>
                    <th scope="col" className={HEADER_CELL_CLASS}>
                        Thao tác
                    </th>
                    <th scope="col" className={HEADER_CELL_CLASS}>
                        Đối tượng
                    </th>
                    <th scope="col" className={HEADER_CELL_CLASS}>
                        Lý do
                    </th>
                </tr>
            </thead>
            <tbody>
                {entries.map((entry) => (
                    <tr key={entry.id} className={cn(entry.id === selectedId && "bg-brand-soft")}>
                        <td className={CELL_CLASS}>
                            <button
                                type="button"
                                onClick={() => onSelect(entry)}
                                aria-label={`Xem chi tiết: ${AUDIT_ACTION_LABELS[entry.action]} lúc ${formatAuditTime(entry.occurredAt)}`}
                                className="text-left text-muted-foreground hover:text-brand hover:underline"
                            >
                                {formatAuditTime(entry.occurredAt)}
                            </button>
                        </td>
                        <td className={cn(CELL_CLASS, "font-semibold")}>{entry.actorUsername}</td>
                        <td className={CELL_CLASS}>
                            <AuditActionLabel action={entry.action} />
                        </td>
                        <td className={cn(CELL_CLASS, "font-mono text-[12.5px]")}>
                            {entry.targetUsername ?? entry.details ?? "-"}
                        </td>
                        <td className={cn(CELL_CLASS, "text-muted-foreground")}>
                            {entry.reason ?? "-"}
                        </td>
                    </tr>
                ))}
            </tbody>
        </table>
    );
}
