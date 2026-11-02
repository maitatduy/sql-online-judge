import { describe, expect, it } from "vitest";

import { type CompletionSchema, sqlSuggestions } from "./sql-completion";

const SCHEMA: CompletionSchema = {
    tables: [
        { name: "departments", columns: ["id", "name"] },
        { name: "employees", columns: ["id", "full_name", "department_id"] },
    ],
};

function labelsOf(textBeforeCursor: string, fullText = textBeforeCursor): string[] {
    return sqlSuggestions(SCHEMA, { textBeforeCursor, fullText }).map(
        (suggestion) => suggestion.label,
    );
}

describe("sqlSuggestions", () => {
    it("gợi ý từ khoá, tên bảng và tên cột kèm bảng chứa cột", () => {
        const suggestions = sqlSuggestions(SCHEMA, {
            textBeforeCursor: "SELECT ",
            fullText: "SELECT ",
        });

        expect(suggestions.map((suggestion) => suggestion.label)).toEqual(
            expect.arrayContaining(["SELECT", "GROUP BY", "COUNT", "departments", "full_name"]),
        );
        expect(suggestions.find((suggestion) => suggestion.label === "employees")).toMatchObject({
            kind: "table",
        });
        expect(
            suggestions.filter((suggestion) => suggestion.label === "id").map((s) => s.detail),
        ).toEqual(["departments", "employees"]);
    });

    it("sau tên bảng và dấu chấm chỉ gợi ý cột của bảng đó", () => {
        expect(labelsOf("SELECT departments.")).toEqual(["id", "name"]);
    });

    it("hiểu bí danh khai báo ở bất kỳ đâu trong câu lệnh, kể cả có AS", () => {
        const fullText =
            "SELECT e. FROM employees e JOIN departments AS d ON d.id = e.department_id";

        expect(labelsOf("SELECT e.", fullText)).toEqual(["id", "full_name", "department_id"]);
        expect(
            labelsOf("SELECT e. FROM employees e JOIN departments AS d ON d.", fullText),
        ).toEqual(["id", "name"]);
    });

    it("tên bảng và bí danh không phân biệt hoa thường", () => {
        expect(labelsOf("SELECT Departments.")).toEqual(["id", "name"]);
        expect(labelsOf("SELECT E.", "SELECT E. FROM EMPLOYEES e")).toEqual([
            "id",
            "full_name",
            "department_id",
        ]);
    });

    it("bí danh không khớp bảng nào thì không gợi ý gì", () => {
        expect(labelsOf("SELECT * FROM employees e WHERE x.")).toEqual([]);
    });

    it("không có schema thì vẫn gợi ý từ khoá", () => {
        expect(
            sqlSuggestions({ tables: [] }, { textBeforeCursor: "SEL", fullText: "SEL" }).map(
                (suggestion) => suggestion.label,
            ),
        ).toContain("SELECT");
    });
});
