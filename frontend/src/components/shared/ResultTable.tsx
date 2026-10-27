import { formatNumber } from "@/lib/format";
import type { ResultCell, ResultSet } from "@/lib/result-set";
import { cn } from "@/lib/utils";

const CELL_CLASS = "border-b px-3 py-1.5 text-left whitespace-nowrap";

interface ResultTableProps {
    label: string;
    result: ResultSet;
}

function CellValue({ value }: { value: ResultCell }) {
    if (value === null) {
        return <span className="text-muted-foreground italic">NULL</span>;
    }
    return <span>{value}</span>;
}

export function ResultTable({ label, result }: ResultTableProps) {
    return (
        <div className="overflow-hidden rounded-lg border">
            <div className="overflow-x-auto">
                <table aria-label={label} className="w-full border-collapse font-mono text-[13px]">
                    <thead className="bg-surface text-muted-foreground">
                        <tr>
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
                                    colSpan={result.columns.length}
                                    className={cn(CELL_CLASS, "text-muted-foreground")}
                                >
                                    0 dòng
                                </td>
                            </tr>
                        ) : null}
                        {result.rows.map((row, rowIndex) => (
                            <tr key={rowIndex}>
                                {row.map((value, columnIndex) => (
                                    <td key={columnIndex} className={CELL_CLASS}>
                                        <CellValue value={value} />
                                    </td>
                                ))}
                            </tr>
                        ))}
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
