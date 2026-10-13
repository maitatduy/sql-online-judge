export type Dialect = "POSTGRESQL" | "MYSQL" | "SQLITE";

export const DIALECT_LABELS: Record<Dialect, string> = {
    POSTGRESQL: "PostgreSQL",
    MYSQL: "MySQL",
    SQLITE: "SQLite",
};
