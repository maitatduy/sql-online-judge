"use client";

import { useState } from "react";

import { Plus } from "lucide-react";

import { Button } from "@/components/ui/button";
import { ApiRequestError } from "@/lib/api-client";

import { addTable, schemaErrors } from "../schema-editing";
import type { DatasetTable } from "../types";
import { SchemaTableCard } from "./SchemaTableCard";

interface SchemaEditorProps {
    initialTables: DatasetTable[];
    disabled: boolean;
    saveError: Error | null;
    onValidChange: (tables: DatasetTable[]) => void;
    onInvalidChange: () => void;
}

function serverErrorsOf(error: Error | null): Record<string, string> {
    return error instanceof ApiRequestError
        ? Object.fromEntries(
              error.errors.map((fieldError) => [fieldError.field, fieldError.message]),
          )
        : {};
}

export function SchemaEditor({
    initialTables,
    disabled,
    saveError,
    onValidChange,
    onInvalidChange,
}: SchemaEditorProps) {
    const [tables, setTables] = useState(initialTables);
    const clientErrors = schemaErrors(tables);
    const serverErrors = serverErrorsOf(saveError);

    function changeTables(next: DatasetTable[]) {
        setTables(next);
        if (Object.keys(schemaErrors(next)).length === 0) {
            onValidChange(next);
        } else {
            onInvalidChange();
        }
    }

    function errorOf(path: string): string | undefined {
        return clientErrors[path] ?? serverErrors[path];
    }

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
                    disabled={disabled}
                    errorOf={errorOf}
                    onTablesChange={changeTables}
                />
            ))}
            {errorOf("tables") ? (
                <p className="text-[13px] text-destructive">{errorOf("tables")}</p>
            ) : null}
            <div>
                <Button
                    type="button"
                    variant="outline"
                    disabled={disabled}
                    onClick={() => changeTables(addTable(tables))}
                >
                    <Plus aria-hidden="true" />
                    Thêm bảng
                </Button>
            </div>
        </section>
    );
}
