import { formatNumber } from "@/lib/format";
import type { ResultCell, ResultSet } from "@/lib/result-set";
import { cn } from "@/lib/utils";

const CELL_CLASS = "border-b px-3 py-1.5 text-left whitespace-nowrap";

export type RowMark = "missing" | "extra";

const ROW_MARK_APPEARANCE: Record<RowMark, { label: string; rowClass: string; textClass: string }> =
    {
        missing: { label: "Thiếu", rowClass: "bg-diff-added/12", textClass: "text-diff-added" },
        extra: { label: "Thừa", rowClass: "bg-diff-removed/12", textClass: "text-diff-removed" },
    };

interface ResultTableProps {
    label: string;
    result: ResultSet;
    rowMarks?: Partial<Record<number, RowMark>>;
}

function CellValue({ value }: { value: ResultCell }) {
    if (value === null) {
        return <span className="text-muted-foreground italic">NULL</span>;
    }
    return <span>{value}</span>;
}

export function ResultTable({ label, result, rowMarks }: ResultTableProps) {
    const hasMarks = rowMarks !== undefined;

    return (
        <div className="overflow-hidden rounded-lg border">
            <div className="overflow-x-auto">
                <table aria-label={label} className="w-full border-collapse font-mono text-[13px]">
                    <thead className="bg-surface text-muted-foreground">
                        <tr>
                            {hasMarks ? (
                                <th scope="col" className={cn(CELL_CLASS, "w-0")}>
                                    <span className="sr-only">Đánh dấu</span>
                                </th>
                            ) : null}
                            {result.columns.map((column, columnIndex) => (
                                <th
                                    key={columnIndex}
                                    scope="col"
                                    className={cn(CELL_CLASS, "font-semibold")}
                                >
                                    {column}
                                </th>
                            ))}
                        </tr>
                    </thead>
                    <tbody>
                        {result.rows.length === 0 ? (
                            <tr>
                                <td
                                    colSpan={result.columns.length + (hasMarks ? 1 : 0)}
                                    className={cn(CELL_CLASS, "text-muted-foreground")}
                                >
                                    0 dòng
                                </td>
                            </tr>
                        ) : null}
                        {result.rows.map((row, rowIndex) => {
                            const mark = rowMarks?.[rowIndex];
                            const appearance = mark ? ROW_MARK_APPEARANCE[mark] : null;
                            return (
                                <tr key={rowIndex} className={appearance?.rowClass}>
                                    {hasMarks ? (
                                        <td
                                            className={cn(
                                                CELL_CLASS,
                                                "font-sans text-xs font-semibold",
                                                appearance?.textClass,
                                            )}
                                        >
                                            {appearance?.label}
                                        </td>
                                    ) : null}
                                    {row.map((value, columnIndex) => (
                                        <td key={columnIndex} className={CELL_CLASS}>
                                            <CellValue value={value} />
                                        </td>
                                    ))}
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
            </div>
            {result.isTruncated ? (
                <p className="border-t px-3 py-1.5 text-xs text-muted-foreground">
                    {`Chỉ hiện ${formatNumber(result.rows.length)} dòng đầu.`}
                </p>
            ) : null}
        </div>
    );
}
