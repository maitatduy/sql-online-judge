import type { DatasetColumn, DatasetTable, LogicalType } from "./types";

const IDENTIFIER = /^[a-z_][a-z0-9_]*$/;
const MAX_IDENTIFIER_LENGTH = 64;
const NEW_TABLE_NAME = "new_table";
const NEW_COLUMN_NAME = "new_column";
const IDENTIFIER_MESSAGE =
    "Chỉ gồm chữ thường không dấu, số và dấu gạch dưới, không bắt đầu bằng số";

export type SchemaErrors = Record<string, string>;

export const LOGICAL_TYPE_LABELS: Record<LogicalType, string> = {
    INTEGER: "Số nguyên",
    DECIMAL: "Số thập phân",
    TEXT: "Văn bản",
    DATE: "Ngày",
    TIMESTAMP: "Thời điểm",
    BOOLEAN: "Đúng hoặc sai",
};

export function tablePath(tableIndex: number): string {
    return `tables[${tableIndex}]`;
}

export function columnPath(tableIndex: number, columnIndex: number): string {
    return `${tablePath(tableIndex)}.columns[${columnIndex}]`;
}

function freeName(base: string, taken: string[]): string {
    let name = base;
    for (let suffix = 2; taken.includes(name); suffix += 1) {
        name = `${base}_${suffix}`;
    }
    return name;
}

export function addTable(tables: DatasetTable[]): DatasetTable[] {
    const name = freeName(
        NEW_TABLE_NAME,
        tables.map((table) => table.name),
    );
    const id: DatasetColumn = {
        name: "id",
        type: "INTEGER",
        isPrimaryKey: true,
        isNullable: false,
        references: null,
    };
    return [...tables, { name, columns: [id] }];
}

export function addColumn(tables: DatasetTable[], tableIndex: number): DatasetTable[] {
    return tables.map((table, index) => {
        if (index !== tableIndex) {
            return table;
        }
        const column: DatasetColumn = {
            name: freeName(
                NEW_COLUMN_NAME,
                table.columns.map((existing) => existing.name),
            ),
            type: "TEXT",
            isPrimaryKey: false,
            isNullable: true,
            references: null,
        };
        return { ...table, columns: [...table.columns, column] };
    });
}

function mapReferences(
    tables: DatasetTable[],
    change: (reference: NonNullable<DatasetColumn["references"]>) => DatasetColumn["references"],
): DatasetTable[] {
    return tables.map((table) => ({
        ...table,
        columns: table.columns.map((column) =>
            column.references ? { ...column, references: change(column.references) } : column,
        ),
    }));
}

export function removeTable(tables: DatasetTable[], tableIndex: number): DatasetTable[] {
    const removed = tables[tableIndex].name;
    return mapReferences(
        tables.filter((_, index) => index !== tableIndex),
        (reference) => (reference.table === removed ? null : reference),
    );
}

export function removeColumn(
    tables: DatasetTable[],
    tableIndex: number,
    columnIndex: number,
): DatasetTable[] {
    const table = tables[tableIndex];
    const removed = table.columns[columnIndex].name;
    const withoutColumn = tables.map((current, index) =>
        index === tableIndex
            ? { ...current, columns: current.columns.filter((_, at) => at !== columnIndex) }
            : current,
    );
    return mapReferences(withoutColumn, (reference) =>
        reference.table === table.name && reference.column === removed ? null : reference,
    );
}

export function renameTable(
    tables: DatasetTable[],
    tableIndex: number,
    name: string,
): DatasetTable[] {
    const previous = tables[tableIndex].name;
    const renamed = tables.map((table, index) =>
        index === tableIndex ? { ...table, name } : table,
    );
    return mapReferences(renamed, (reference) =>
        reference.table === previous ? { ...reference, table: name } : reference,
    );
}

export function updateColumn(
    tables: DatasetTable[],
    tableIndex: number,
    columnIndex: number,
    change: Partial<DatasetColumn>,
): DatasetTable[] {
    const table = tables[tableIndex];
    const previous = table.columns[columnIndex];
    const updated = tables.map((current, index) =>
        index === tableIndex
            ? {
                  ...current,
                  columns: current.columns.map((column, at) =>
                      at === columnIndex ? { ...column, ...change } : column,
                  ),
              }
            : current,
    );
    if (change.name === undefined || change.name === previous.name) {
        return updated;
    }
    return mapReferences(updated, (reference) =>
        reference.table === table.name && reference.column === previous.name
            ? { ...reference, column: change.name as string }
            : reference,
    );
}

export function referenceOptions(tables: DatasetTable[], type: LogicalType): string[] {
    return tables.flatMap((table) =>
        table.columns
            .filter((column) => column.isPrimaryKey && column.type === type)
            .map((column) => `${table.name}.${column.name}`),
    );
}

function identifierError(name: string): string | undefined {
    if (name.trim() === "") {
        return "Không được để trống";
    }
    if (name.length > MAX_IDENTIFIER_LENGTH) {
        return "Tối đa 64 ký tự";
    }
    return IDENTIFIER.test(name) ? undefined : IDENTIFIER_MESSAGE;
}

export function schemaErrors(tables: DatasetTable[]): SchemaErrors {
    const errors: SchemaErrors = {};
    if (tables.length === 0) {
        errors.tables = "Cần ít nhất một bảng";
    }
    tables.forEach((table, tableIndex) => {
        const tableError = identifierError(table.name);
        if (tableError) {
            errors[`${tablePath(tableIndex)}.name`] = tableError;
        }
        table.columns.forEach((column, columnIndex) => {
            const columnError = identifierError(column.name);
            if (columnError) {
                errors[`${columnPath(tableIndex, columnIndex)}.name`] = columnError;
            }
        });
    });
    return errors;
}
