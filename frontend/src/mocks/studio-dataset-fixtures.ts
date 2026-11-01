import type {
    DatasetColumn,
    DatasetTable,
    DatasetVariant,
    LogicalType,
} from "@/features/studio/types";

function column(
    name: string,
    type: LogicalType,
    options: Partial<Omit<DatasetColumn, "name" | "type">> = {},
): DatasetColumn {
    return {
        name,
        type,
        isPrimaryKey: options.isPrimaryKey ?? false,
        isNullable: options.isNullable ?? false,
        references: options.references ?? null,
    };
}

function key(name = "id"): DatasetColumn {
    return column(name, "INTEGER", { isPrimaryKey: true });
}

function reference(name: string, table: string): DatasetColumn {
    return column(name, "INTEGER", { isNullable: true, references: { table, column: "id" } });
}

export const SEED_SCHEMAS: DatasetTable[][] = [
    [
        { name: "departments", columns: [key(), column("name", "TEXT")] },
        {
            name: "employees",
            columns: [
                key(),
                column("full_name", "TEXT"),
                column("salary", "DECIMAL", { isNullable: true }),
                reference("department_id", "departments"),
            ],
        },
    ],
    [
        { name: "classes", columns: [key(), column("name", "TEXT")] },
        {
            name: "students",
            columns: [key(), column("full_name", "TEXT"), reference("class_id", "classes")],
        },
    ],
    [
        { name: "customers", columns: [key(), column("full_name", "TEXT")] },
        {
            name: "orders",
            columns: [
                key(),
                reference("customer_id", "customers"),
                column("ordered_at", "TIMESTAMP"),
            ],
        },
    ],
];

export const SEED_VARIANTS: DatasetVariant[][] = [
    [
        {
            id: "0192f3e1-0000-7000-8000-000000000001",
            name: "Mẫu",
            isPublic: true,
            tables: [
                { tableName: "departments", rowCount: 3 },
                { tableName: "employees", rowCount: 6 },
            ],
        },
        {
            id: "0192f3e1-0000-7000-8000-000000000002",
            name: "Có NULL",
            isPublic: false,
            tables: [{ tableName: "departments", rowCount: 2 }],
        },
    ],
    [],
    [],
];
