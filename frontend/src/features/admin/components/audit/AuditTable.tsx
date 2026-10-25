import { Eye } from "lucide-react";

import { Button } from "@/components/ui/button";
import { AUDIT_ACTION_LABELS } from "@/features/admin/audit/action-labels";
import { targetSummaryOf } from "@/features/admin/audit/entry-summary";
import type { AuditLogEntry } from "@/features/admin/audit/types";
import { formatAuditMoment, formatAuditTime } from "@/features/admin/format";
import { cn } from "@/lib/utils";

import { AuditActionLabel } from "./AuditActionLabel";

const HEADER_CELL_CLASS =
    "border-b bg-surface px-3 py-2.25 text-left text-xs font-semibold whitespace-nowrap text-muted-foreground";
const CELL_CLASS = "border-b px-3 py-2.25 align-middle";

interface AuditTableProps {
    entries: AuditLogEntry[];
    selectedId: string | null;
    onSelect: (entryId: string) => void;
    now: Date;
}

export function AuditTable({ entries, selectedId, onSelect, now }: AuditTableProps) {
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
                    <th scope="col" className={cn(HEADER_CELL_CLASS, "w-24 text-right")}>
                        Hành động
                    </th>
                </tr>
            </thead>
            <tbody>
                {entries.map((entry) => (
                    <tr
                        key={entry.id}
                        aria-current={entry.id === selectedId ? "true" : undefined}
                        className={cn(
                            "hover:bg-surface",
                            entry.id === selectedId && "bg-brand-soft hover:bg-brand-soft",
                        )}
                    >
                        <td
                            className={cn(
                                CELL_CLASS,
                                "border-l-3",
                                entry.id === selectedId ? "border-l-brand" : "border-l-transparent",
                                "text-muted-foreground",
                            )}
                        >
                            {formatAuditMoment(entry.occurredAt, now)}
                        </td>
                        <td className={cn(CELL_CLASS, "font-semibold")}>{entry.actorUsername}</td>
                        <td className={CELL_CLASS}>
                            <AuditActionLabel action={entry.action} />
                        </td>
                        <td className={cn(CELL_CLASS, "font-mono text-[12.5px]")}>
                            {targetSummaryOf(entry)}
                        </td>
                        <td className={cn(CELL_CLASS, "text-muted-foreground")}>
                            {entry.reason ?? "-"}
                        </td>
                        <td className={cn(CELL_CLASS, "text-right")}>
                            <Button
                                type="button"
                                variant="ghost"
                                size="icon"
                                aria-label={`Xem chi tiết: ${AUDIT_ACTION_LABELS[entry.action]} lúc ${formatAuditTime(entry.occurredAt)}`}
                                aria-pressed={entry.id === selectedId}
                                onClick={() => onSelect(entry.id)}
                                className="size-8 text-muted-foreground hover:text-brand"
                            >
                                <Eye aria-hidden="true" />
                            </Button>
                        </td>
                    </tr>
                ))}
            </tbody>
        </table>
    );
}
