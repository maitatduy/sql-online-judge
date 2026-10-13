import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { EmptyState } from "./EmptyState";

describe("EmptyState", () => {
    it("hiện một câu tiêu đề, mô tả và một hành động", () => {
        render(
            <EmptyState
                title="Chưa có bài nộp nào"
                description="Bài nộp đầu tiên của bạn sẽ hiện ở đây."
                action={<a href="/problems">Giải bài đầu tiên</a>}
            />,
        );

        expect(screen.getByText("Chưa có bài nộp nào")).toBeInTheDocument();
        expect(screen.getByText("Bài nộp đầu tiên của bạn sẽ hiện ở đây.")).toBeInTheDocument();
        expect(screen.getByRole("link", { name: "Giải bài đầu tiên" })).toBeInTheDocument();
    });

    it("chỉ cần tiêu đề", () => {
        render(<EmptyState title="Chưa có huy hiệu nào" />);

        expect(screen.getByText("Chưa có huy hiệu nào")).toBeInTheDocument();
        expect(screen.queryByRole("link")).not.toBeInTheDocument();
    });
});
