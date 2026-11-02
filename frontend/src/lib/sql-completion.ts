export interface CompletionSchema {
    tables: { name: string; columns: string[] }[];
}

export type SqlSuggestionKind = "keyword" | "table" | "column";

export interface SqlSuggestion {
    label: string;
    kind: SqlSuggestionKind;
    detail: string;
}

export interface CompletionContext {
    textBeforeCursor: string;
    fullText: string;
}

const SQL_KEYWORDS = [
    "SELECT",
    "FROM",
    "WHERE",
    "JOIN",
    "INNER JOIN",
    "LEFT JOIN",
    "RIGHT JOIN",
    "FULL JOIN",
    "CROSS JOIN",
    "ON",
    "GROUP BY",
    "HAVING",
    "ORDER BY",
    "LIMIT",
    "OFFSET",
    "AS",
    "AND",
    "OR",
    "NOT",
    "IN",
    "IS NULL",
    "IS NOT NULL",
    "BETWEEN",
    "LIKE",
    "DISTINCT",
    "CASE",
    "WHEN",
    "THEN",
    "ELSE",
    "END",
    "WITH",
    "UNION",
    "UNION ALL",
    "EXISTS",
    "ASC",
    "DESC",
    "COUNT",
    "SUM",
    "AVG",
    "MIN",
    "MAX",
    "COALESCE",
    "CAST",
    "ROUND",
    "OVER",
    "PARTITION BY",
    "ROW_NUMBER",
    "RANK",
    "DENSE_RANK",
];
const KEYWORD_DETAIL = "Từ khoá SQL";
const TABLE_DETAIL = "Bảng";
const QUALIFIER_BEFORE_CURSOR = /([A-Za-z_][A-Za-z0-9_]*)\.$/;
const TABLE_REFERENCE =
    /\b(?:from|join)\s+([A-Za-z_][A-Za-z0-9_]*)(?:\s+(?:as\s+)?([A-Za-z_][A-Za-z0-9_]*))?/gi;
const NOT_AN_ALIAS = new Set([
    "on",
    "where",
    "join",
    "inner",
    "left",
    "right",
    "full",
    "cross",
    "group",
    "order",
    "limit",
    "having",
    "union",
]);

export function sqlSuggestions(
    schema: CompletionSchema,
    context: CompletionContext,
): SqlSuggestion[] {
    const qualifier = QUALIFIER_BEFORE_CURSOR.exec(context.textBeforeCursor)?.[1];
    if (qualifier) {
        const tableName = tableNameOf(qualifier, context.fullText, schema);
        const table = schema.tables.find(
            (candidate) => candidate.name.toLowerCase() === tableName.toLowerCase(),
        );
        return table ? table.columns.map((column) => columnSuggestion(column, table.name)) : [];
    }
    return [
        ...SQL_KEYWORDS.map((keyword): SqlSuggestion => ({
            label: keyword,
            kind: "keyword",
            detail: KEYWORD_DETAIL,
        })),
        ...schema.tables.map((table): SqlSuggestion => ({
            label: table.name,
            kind: "table",
            detail: TABLE_DETAIL,
        })),
        ...schema.tables.flatMap((table) =>
            table.columns.map((column) => columnSuggestion(column, table.name)),
        ),
    ];
}

function tableNameOf(qualifier: string, fullText: string, schema: CompletionSchema): string {
    const lowerQualifier = qualifier.toLowerCase();
    if (schema.tables.some((table) => table.name.toLowerCase() === lowerQualifier)) {
        return qualifier;
    }
    for (const match of fullText.matchAll(TABLE_REFERENCE)) {
        const [, tableName, alias] = match;
        if (
            alias &&
            !NOT_AN_ALIAS.has(alias.toLowerCase()) &&
            alias.toLowerCase() === lowerQualifier
        ) {
            return tableName;
        }
    }
    return "";
}

function columnSuggestion(column: string, tableName: string): SqlSuggestion {
    return { label: column, kind: "column", detail: tableName };
}
