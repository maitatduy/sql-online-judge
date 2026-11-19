import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { LessonMarkdown } from "./LessonMarkdown";

describe("LessonMarkdown", () => {
    it("định dạng tiêu đề và khối code như trang học", () => {
        render(<LessonMarkdown>{"## Đánh số dòng\n\n```sql\nSELECT 1;\n```"}</LessonMarkdown>);

        const heading = screen.getByRole("heading", { level: 2, name: "Đánh số dòng" });
        const wrapper = heading.parentElement;
        expect(wrapper).toHaveClass("[&_h2]:text-lg", "[&_pre]:bg-surface-2");
        expect(screen.getByText("SELECT 1;").closest("pre")).not.toBeNull();
    });
});
