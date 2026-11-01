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
    [
        {
            id: "0192f3e1-0000-7000-8000-000000000003",
            name: "Mẫu",
            isPublic: true,
            tables: [
                { tableName: "customers", rowCount: 2 },
                { tableName: "orders", rowCount: 2 },
            ],
        },
    ],
];

export const SEED_VARIANT_CSVS: Record<string, string> = {
    "0192f3e1-0000-7000-8000-000000000003/customers":
        "id,full_name\n1,Lê Văn Hải\n2,Ngô Thu Trang\n",
    "0192f3e1-0000-7000-8000-000000000003/orders":
        "id,customer_id,ordered_at\n1,1,2026-01-05 09:30:00\n2,2,2026-01-06 14:00:00\n",
    "0192f3e1-0000-7000-8000-000000000001/departments":
        "id,name\n1,Kỹ thuật\n2,Kinh doanh\n3,Nhân sự\n",
    "0192f3e1-0000-7000-8000-000000000001/employees":
        "id,full_name,salary,department_id\n1,Hoàng Quốc Bảo,48000000,1\n2,Nguyễn Văn An,48000000,1\n3,Phạm Minh Tuấn,35000000,1\n4,Trần Thị Bình,32000000,2\n5,Đỗ Thu Hà,29000000,2\n6,Mai Lan,30000000,\n",
    "0192f3e1-0000-7000-8000-000000000002/departments": "id,name\n1,Kế toán\n2,Pháp chế\n",
};
