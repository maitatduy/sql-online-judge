import { cn } from "@/lib/utils";

import type { RunCell } from "../types";

interface RunResultTableProps {
    label: string;
    columns: string[];
    rows: RunCell[][];
}

export function RunResultTable({ label, columns, rows }: RunResultTableProps) {
    return (
        <div className="overflow-x-auto rounded-md border">
            <table aria-label={label} className="w-full border-collapse text-[13px]">
                <thead className="bg-surface-2">
                    <tr>
                        {columns.map((column) => (
                            <th
                                key={column}
                                scope="col"
                                className="px-3 py-1.5 text-left font-semibold whitespace-nowrap"
                            >
                                {column}
                            </th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {rows.map((row, rowIndex) => (
                        <tr key={rowIndex} className="border-t">
                            {row.map((cell, cellIndex) => (
                                <td
                                    key={cellIndex}
                                    className={cn(
                                        "px-3 py-1.5 align-top",
                                        cell === null && "text-muted-foreground italic",
                                    )}
                                >
                                    {cell === null ? "NULL" : String(cell)}
                                </td>
                            ))}
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
