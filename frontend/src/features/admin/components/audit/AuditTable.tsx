import { AUDIT_ACTION_LABELS } from "@/features/admin/audit/action-labels";
import type { AuditLogEntry } from "@/features/admin/audit/types";
import { formatAuditTime } from "@/features/admin/format";
import { cn } from "@/lib/utils";

import { AuditActionLabel } from "./AuditActionLabel";

const HEADER_CELL_CLASS =
    "border-b bg-surface px-3 py-2.25 text-left text-xs font-semibold whitespace-nowrap text-muted-foreground";
const CELL_CLASS = "border-b px-3 py-2.25 align-middle";

interface AuditTableProps {
    entries: AuditLogEntry[];
    selectedId: string | null;
    onSelect: (entryId: string) => void;
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
                    <tr
                        key={entry.id}
                        aria-current={entry.id === selectedId ? "true" : undefined}
                        onClick={() => onSelect(entry.id)}
                        className={cn(
                            "cursor-pointer hover:bg-surface",
                            entry.id === selectedId && "bg-brand-soft hover:bg-brand-soft",
                        )}
                    >
                        <td
                            className={cn(
                                CELL_CLASS,
                                "border-l-3",
                                entry.id === selectedId ? "border-l-brand" : "border-l-transparent",
                            )}
                        >
                            <button
                                type="button"
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
