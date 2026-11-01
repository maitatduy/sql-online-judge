import { formatNumber } from "@/lib/format";

import { rowsFromCsv } from "../csv";
import type { SqlImportPreview } from "../types";

const PREVIEW_ROW_COUNT = 3;

interface SqlImportPreviewListProps {
    preview: SqlImportPreview;
}

function summaryOf(preview: SqlImportPreview, tableName: string): string {
    const columns = preview.tables.find((table) => table.name === tableName)?.columns.length;
    const rows = preview.data.find((table) => table.tableName === tableName)?.rowCount ?? 0;
    const rowText = `${formatNumber(rows)} dòng`;
    return columns === undefined
        ? `${tableName}: ${rowText}`
        : `${tableName}: ${formatNumber(columns)} cột, ${rowText}`;
}

export function SqlImportPreviewList({ preview }: SqlImportPreviewListProps) {
    const tableNames = [
        ...preview.tables.map((table) => table.name),
        ...preview.data
            .map((table) => table.tableName)
            .filter((name) => !preview.tables.some((table) => table.name === name)),
    ];

    return (
        <ul aria-label="Bản xem trước" className="flex max-h-72 flex-col gap-3 overflow-y-auto">
            {tableNames.map((tableName) => {
                const data = preview.data.find((table) => table.tableName === tableName);
                const columns = data ? (data.csv.split("\n")[0] ?? "").split(",") : [];
                const rows = data
                    ? rowsFromCsv(data.csv, columns).rows.slice(0, PREVIEW_ROW_COUNT)
                    : [];
                return (
                    <li key={tableName} className="rounded-md border p-3">
                        <p className="text-sm font-semibold">{summaryOf(preview, tableName)}</p>
                        {rows.length > 0 ? (
                            <table className="mt-2 w-full border-collapse text-[13px]">
                                <thead>
                                    <tr>
                                        {columns.map((column) => (
                                            <th
                                                key={column}
                                                scope="col"
                                                className="border-b px-2 py-1 text-left font-semibold text-muted-foreground"
                                            >
                                                {column}
                                            </th>
                                        ))}
                                    </tr>
                                </thead>
                                <tbody>
                                    {rows.map((row, rowIndex) => (
                                        <tr key={rowIndex}>
                                            {row.map((value, columnIndex) => (
                                                <td
                                                    key={columnIndex}
                                                    className="border-b px-2 py-1"
                                                >
                                                    {value ?? "NULL"}
                                                </td>
                                            ))}
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        ) : null}
                    </li>
                );
            })}
        </ul>
    );
}
