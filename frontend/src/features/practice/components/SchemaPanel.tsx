import { Fragment } from "react";

import type { SchemaColumn, SchemaTable } from "../types";

const CELL_CLASS = "border-b px-3 py-1.5 text-left";

interface SchemaPanelProps {
    tables: SchemaTable[];
}

interface Constraint {
    label: string;
    className: string;
}

function constraintsOf(column: SchemaColumn): Constraint[] {
    return [
        column.isPrimaryKey ? { label: "Khoá chính", className: "text-syntax-keyword" } : null,
        column.references
            ? {
                  label: `Khoá ngoại → ${column.references.table}.${column.references.column}`,
                  className: "text-syntax-function",
              }
            : null,
        column.isNullable ? { label: "Có thể NULL", className: "text-muted-foreground" } : null,
    ].filter((constraint) => constraint !== null);
}

function ColumnConstraints({ column }: { column: SchemaColumn }) {
    return constraintsOf(column).map((constraint, index) => (
        <Fragment key={constraint.label}>
            {index > 0 ? " · " : null}
            <span className={constraint.className}>{constraint.label}</span>
        </Fragment>
    ));
}

export function SchemaPanel({ tables }: SchemaPanelProps) {
    return (
        <div className="flex flex-col gap-4 px-6 py-5">
            {tables.map((table) => (
                <section key={table.name} className="overflow-hidden rounded-lg border">
                    <h2 className="bg-surface px-3 py-2 font-mono text-[13px] font-semibold">
                        {table.name}
                    </h2>
                    <table aria-label={table.name} className="w-full border-collapse text-[13px]">
                        <thead className="text-xs text-muted-foreground">
                            <tr>
                                <th scope="col" className={CELL_CLASS}>
                                    Cột
                                </th>
                                <th scope="col" className={CELL_CLASS}>
                                    Kiểu
                                </th>
                                <th scope="col" className={CELL_CLASS}>
                                    Ràng buộc
                                </th>
                            </tr>
                        </thead>
                        <tbody className="font-mono">
                            {table.columns.map((column) => (
                                <tr key={column.name}>
                                    <td className={CELL_CLASS}>{column.name}</td>
                                    <td className={CELL_CLASS}>{column.type}</td>
                                    <td className={CELL_CLASS}>
                                        <ColumnConstraints column={column} />
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </section>
            ))}
        </div>
    );
}
