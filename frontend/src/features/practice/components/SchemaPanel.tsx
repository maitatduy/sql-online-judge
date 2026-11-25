import { Table2 } from "lucide-react";
import { Fragment } from "react";

import { formatNumber } from "@/lib/format";
import { cn } from "@/lib/utils";

import type { SchemaColumn, SchemaTable } from "../types";

const CELL_CLASS = "border-t px-4 py-2.5 text-left align-middle";
const HEAD_CLASS = "px-4 py-2 text-left text-[13px] font-semibold text-muted-foreground";
const PILL_CLASS = "rounded-sm px-1.5 py-0.5 font-sans text-[13px] font-semibold whitespace-nowrap";

interface SchemaPanelProps {
    tables: SchemaTable[];
}

interface Constraint {
    label: string;
    className: string;
}

function constraintsOf(column: SchemaColumn): Constraint[] {
    return [
        column.isPrimaryKey
            ? { label: "Khoá chính", className: "bg-syntax-keyword/12 text-syntax-keyword" }
            : null,
        column.references
            ? {
                  label: `Khoá ngoại → ${column.references.table}.${column.references.column}`,
                  className: "bg-syntax-function/12 text-syntax-function",
              }
            : null,
        column.isNullable
            ? { label: "Có thể NULL", className: "bg-surface-2 text-muted-foreground" }
            : null,
    ].filter((constraint) => constraint !== null);
}

export function SchemaPanel({ tables }: SchemaPanelProps) {
    return (
        <div className="flex flex-col gap-5 px-6 py-6">
            {tables.map((table) => (
                <section
                    key={table.name}
                    aria-label={`Bảng ${table.name}`}
                    className="overflow-hidden rounded-lg border bg-card"
                >
                    <div className="flex items-center gap-2.5 bg-surface px-4 py-3">
                        <Table2 aria-hidden="true" className="size-[18px] text-muted-foreground" />
                        <h2 className="font-mono text-base font-bold">{table.name}</h2>
                        <span className="ml-auto text-sm text-muted-foreground">
                            {`${formatNumber(table.columns.length)} cột`}
                        </span>
                    </div>
                    <table aria-label={table.name} className="w-full border-collapse text-[15px]">
                        <thead>
                            <tr className="border-t">
                                <th scope="col" className={HEAD_CLASS}>
                                    Cột
                                </th>
                                <th scope="col" className={HEAD_CLASS}>
                                    Kiểu
                                </th>
                                <th scope="col" className={HEAD_CLASS}>
                                    Ràng buộc
                                </th>
                            </tr>
                        </thead>
                        <tbody className="font-mono">
                            {table.columns.map((column) => (
                                <tr key={column.name} className="hover:bg-surface">
                                    <td className={cn(CELL_CLASS, "font-semibold")}>
                                        {column.name}
                                    </td>
                                    <td className={cn(CELL_CLASS, "text-muted-foreground")}>
                                        {column.type}
                                    </td>
                                    <td className={CELL_CLASS}>
                                        <span className="flex flex-wrap gap-1.5">
                                            {constraintsOf(column).map((constraint, index) => (
                                                <Fragment key={constraint.label}>
                                                    {index > 0 ? (
                                                        <span className="sr-only">, </span>
                                                    ) : null}
                                                    <span
                                                        className={cn(
                                                            PILL_CLASS,
                                                            constraint.className,
                                                        )}
                                                    >
                                                        {constraint.label}
                                                    </span>
                                                </Fragment>
                                            ))}
                                        </span>
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
