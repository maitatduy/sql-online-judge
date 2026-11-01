"use client";

import { Plus } from "lucide-react";

import { Button } from "@/components/ui/button";

import { addTable } from "../schema-editing";
import type { DatasetTable } from "../types";
import { SchemaTableCard } from "./SchemaTableCard";

interface SchemaEditorProps {
    tables: DatasetTable[];
    errorOf: (path: string) => string | undefined;
    onChange: (tables: DatasetTable[]) => void;
}

export function SchemaEditor({ tables, errorOf, onChange }: SchemaEditorProps) {
    return (
        <section aria-labelledby="schema-title" className="flex flex-col gap-4">
            <div>
                <h2 id="schema-title" className="text-base font-bold">
                    Schema
                </h2>
                <p className="mt-1 text-[13px] text-muted-foreground">
                    Tên bảng và tên cột viết thường không dấu, ví dụ employees, department_id.
                </p>
            </div>
            {tables.map((table, tableIndex) => (
                <SchemaTableCard
                    key={tableIndex}
                    tables={tables}
                    tableIndex={tableIndex}
                    errorOf={errorOf}
                    onTablesChange={onChange}
                />
            ))}
            {errorOf("tables") ? (
                <p className="text-[13px] text-destructive">{errorOf("tables")}</p>
            ) : null}
            <div>
                <Button type="button" variant="outline" onClick={() => onChange(addTable(tables))}>
                    <Plus aria-hidden="true" />
                    Thêm bảng
                </Button>
            </div>
        </section>
    );
}
