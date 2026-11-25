import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { TopicTag, topicClassNameOf } from "./TopicTag";

describe("TopicTag", () => {
    it("cùng chủ đề luôn cùng màu, màu lấy từ token chủ đề", () => {
        expect(topicClassNameOf("join")).toBe(topicClassNameOf("join"));
        expect(topicClassNameOf("join")).toMatch(/^bg-topic-[1-8]\/12 text-topic-[1-8]$/);
    });

    it("các chủ đề khác nhau được tô màu khác nhau", () => {
        const colors = new Set(
            ["join", "group-by", "cte", "window-function", "subquery"].map(topicClassNameOf),
        );

        expect(colors.size).toBeGreaterThan(1);
    });

    it("hiện tên chủ đề", () => {
        render(<TopicTag slug="join" name="JOIN" />);

        expect(screen.getByText("JOIN")).toHaveClass(topicClassNameOf("join").split(" ")[0]);
    });
});
