import { cn } from "@/lib/utils";

import type { StandingCell } from "../types";

interface StandingCellViewProps {
    cell: StandingCell;
}

function describe(cell: StandingCell): string {
    if (cell.acceptedMinute !== null) {
        const wrong = cell.wrongAttempts > 0 ? `, sai ${cell.wrongAttempts} lần` : "";
        return `${cell.letter}: AC phút ${cell.acceptedMinute}${wrong}`;
    }
    const parts = [
        ...(cell.wrongAttempts > 0 ? [`sai ${cell.wrongAttempts} lần`] : []),
        ...(cell.pendingAttempts > 0 ? [`${cell.pendingAttempts} lượt đang chờ`] : []),
    ];
    return `${cell.letter}: ${parts.length > 0 ? parts.join(", ") : "chưa nộp"}`;
}

function toneOf(cell: StandingCell): string {
    if (cell.acceptedMinute !== null) {
        return "bg-verdict-ac/12 text-verdict-ac";
    }
    if (cell.pendingAttempts > 0) {
        return "bg-verdict-pending/12 text-verdict-pending";
    }
    return cell.wrongAttempts > 0 ? "bg-verdict-wa/12 text-verdict-wa" : "";
}

function headlineOf(cell: StandingCell): string {
    if (cell.acceptedMinute !== null) {
        return cell.wrongAttempts > 0 ? `+${cell.wrongAttempts}` : "+";
    }
    if (cell.pendingAttempts > 0) {
        return `?${cell.pendingAttempts}`;
    }
    return cell.wrongAttempts > 0 ? `-${cell.wrongAttempts}` : "";
}

export function StandingCellView({ cell }: StandingCellViewProps) {
    return (
        <td aria-label={describe(cell)} className="border-b px-1 py-1.5 text-center">
            <span
                aria-hidden="true"
                className={cn(
                    "mx-auto flex h-12 w-16 flex-col items-center justify-center rounded-md font-bold",
                    toneOf(cell),
                )}
            >
                {headlineOf(cell)}
                {cell.acceptedMinute !== null ? (
                    <span className="text-sm font-semibold">{cell.acceptedMinute}</span>
                ) : null}
            </span>
        </td>
    );
}
