import type { SchemaColumn, SchemaTable } from "../types";

const CELL_CLASS = "border-b px-3 py-1.5 text-left";

interface StatementSchemaTableProps {
    table: SchemaTable;
}

function keyNotesOf(columns: SchemaColumn[]): string[] {
    const primaryKeys = columns
        .filter((column) => column.isPrimaryKey)
        .map((column) => column.name);
    const notes: string[] = [];
    if (primaryKeys.length === 1) {
        notes.push(`${primaryKeys[0]} là khoá chính của bảng này.`);
    } else if (primaryKeys.length > 1) {
        notes.push(`(${primaryKeys.join(", ")}) là khoá chính của bảng này.`);
    }
    for (const column of columns) {
        if (column.references) {
            notes.push(
                `${column.name} là khoá ngoại tham chiếu tới ${column.references.table}.${column.references.column}.`,
            );
        }
    }
    const nullable = columns.filter((column) => column.isNullable).map((column) => column.name);
    if (nullable.length > 0) {
        notes.push(`${nullable.join(", ")} có thể NULL.`);
    }
    return notes;
}

export function StatementSchemaTable({ table }: StatementSchemaTableProps) {
    const notes = keyNotesOf(table.columns);

    return (
        <div className="flex flex-col gap-2">
            <p className="font-bold">
                Bảng: <span className="font-mono">{table.name}</span>
            </p>
            <div className="w-fit max-w-full overflow-x-auto rounded-lg border">
                <table aria-label={`Cột của bảng ${table.name}`} className="font-mono text-sm">
                    <thead className="bg-surface text-muted-foreground">
                        <tr>
                            <th scope="col" className={CELL_CLASS}>
                                Tên cột
                            </th>
                            <th scope="col" className={CELL_CLASS}>
                                Kiểu
                            </th>
                        </tr>
                    </thead>
                    <tbody>
                        {table.columns.map((column) => (
                            <tr key={column.name} className="last:[&>td]:border-b-0">
                                <td className={CELL_CLASS}>{column.name}</td>
                                <td className={CELL_CLASS}>{column.type}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
            {notes.length > 0 ? (
                <ul className="flex flex-col gap-0.5 text-muted-foreground">
                    {notes.map((note) => (
                        <li key={note}>{note}</li>
                    ))}
                </ul>
            ) : null}
        </div>
    );
}
