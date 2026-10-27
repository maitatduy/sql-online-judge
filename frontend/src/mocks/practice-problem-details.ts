import type { ProblemDetail, ProblemSummary, SchemaTable } from "@/features/practice/types";

type DetailContent = Pick<
    ProblemDetail,
    "statementMarkdown" | "starterCode" | "schema" | "examples" | "requiresOrder"
>;

const COMPANY_SCHEMA: SchemaTable[] = [
    {
        name: "departments",
        columns: [
            {
                name: "id",
                type: "INTEGER",
                isPrimaryKey: true,
                isNullable: false,
                references: null,
            },
            {
                name: "name",
                type: "VARCHAR(100)",
                isPrimaryKey: false,
                isNullable: false,
                references: null,
            },
        ],
    },
    {
        name: "employees",
        columns: [
            {
                name: "id",
                type: "INTEGER",
                isPrimaryKey: true,
                isNullable: false,
                references: null,
            },
            {
                name: "full_name",
                type: "VARCHAR(100)",
                isPrimaryKey: false,
                isNullable: false,
                references: null,
            },
            {
                name: "salary",
                type: "NUMERIC(12,0)",
                isPrimaryKey: false,
                isNullable: false,
                references: null,
            },
            {
                name: "department_id",
                type: "INTEGER",
                isPrimaryKey: false,
                isNullable: true,
                references: { table: "departments", column: "id" },
            },
        ],
    },
];

const DETAIL_CONTENT: Record<string, DetailContent> = {
    "highest-salary-per-department": {
        statementMarkdown: [
            "Tìm nhân viên có **lương cao nhất trong mỗi phòng ban**.",
            "",
            "Trả về các cột `department`, `employee`, `salary`. Nếu nhiều người cùng lương cao nhất thì trả về tất cả.",
            "",
            "Sắp xếp theo `salary` giảm dần, rồi theo `employee` tăng dần.",
        ].join("\n"),
        starterCode: "SELECT *\nFROM employees;",
        schema: COMPANY_SCHEMA,
        examples: [
            {
                columns: ["department", "employee", "salary"],
                rows: [
                    ["Kỹ thuật", "Hoàng Quốc Bảo", "48000000"],
                    ["Kỹ thuật", "Nguyễn Văn An", "48000000"],
                    ["Kinh doanh", "Trần Thị Bình", "32000000"],
                    ["Nhân sự", "Lê Minh Châu", "27500000"],
                ],
                isTruncated: false,
            },
        ],
        requiresOrder: true,
    },
};

function defaultContent(summary: ProblemSummary): DetailContent {
    return {
        statementMarkdown: `Viết câu truy vấn cho bài **${summary.title}**.`,
        starterCode: "SELECT 1;\n",
        schema: COMPANY_SCHEMA,
        examples: [],
        requiresOrder: false,
    };
}

export function buildProblemDetail(summary: ProblemSummary): ProblemDetail {
    return {
        id: summary.id,
        number: summary.number,
        slug: summary.slug,
        title: summary.title,
        difficulty: summary.difficulty,
        tags: summary.tags,
        dialects: ["POSTGRESQL"],
        engineVersions: { POSTGRESQL: "PostgreSQL 16" },
        allowedStatements: ["SELECT", "WITH"],
        timeLimitMs: 2000,
        hiddenTestcaseCount: 2,
        acceptanceRate: summary.acceptanceRate,
        solvedCount: summary.solvedCount,
        ...(DETAIL_CONTENT[summary.slug] ?? defaultContent(summary)),
    };
}
