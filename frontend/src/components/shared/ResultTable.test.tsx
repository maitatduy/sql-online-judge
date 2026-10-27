import { render, screen, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { ResultTable } from "./ResultTable";

describe("ResultTable", () => {
    it("hiện cột và giá trị đúng như máy chủ đã chuẩn hoá", () => {
        render(
            <ResultTable
                label="Kết quả mong đợi"
                result={{
                    columns: ["department", "salary"],
                    rows: [["Kinh doanh", "32000000"]],
                    isTruncated: false,
                }}
            />,
        );

        const table = within(screen.getByRole("table", { name: "Kết quả mong đợi" }));
        expect(table.getByRole("columnheader", { name: "department" })).toBeInTheDocument();
        expect(table.getByRole("cell", { name: "32000000" })).toBeInTheDocument();
    });

    it("phân biệt NULL với chuỗi 'NULL'", () => {
        render(
            <ResultTable
                label="Kết quả"
                result={{ columns: ["email", "note"], rows: [[null, "NULL"]], isTruncated: false }}
            />,
        );

        const [nullCell, textCell] = screen.getAllByRole("cell");
        expect(nullCell).toHaveTextContent("NULL");
        expect(within(nullCell).getByText("NULL")).toHaveClass("text-muted-foreground", "italic");
        expect(within(textCell).getByText("NULL")).not.toHaveClass("italic");
    });

    it("báo khi kết quả bị cắt bớt", () => {
        render(
            <ResultTable
                label="Kết quả"
                result={{ columns: ["id"], rows: [["1"]], isTruncated: true }}
            />,
        );

        expect(screen.getByText("Chỉ hiện 1 dòng đầu.")).toBeInTheDocument();
    });

    it("hiện đủ các cột trùng tên mà không báo lỗi key", () => {
        const consoleError = vi.spyOn(console, "error").mockImplementation(() => undefined);
        render(
            <ResultTable
                label="Kết quả"
                result={{ columns: ["id", "id"], rows: [["1", "2"]], isTruncated: false }}
            />,
        );

        expect(screen.getAllByRole("columnheader", { name: "id" })).toHaveLength(2);
        expect(screen.getByRole("cell", { name: "2" })).toBeInTheDocument();
        expect(consoleError).not.toHaveBeenCalled();
        consoleError.mockRestore();
    });

    it("báo 0 dòng khi không có kết quả", () => {
        render(
            <ResultTable
                label="Kết quả"
                result={{ columns: ["id", "name"], rows: [], isTruncated: false }}
            />,
        );

        expect(screen.getByRole("cell", { name: "0 dòng" })).toHaveAttribute("colspan", "2");
    });
});
