import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { TopBarSearch } from "./TopBarSearch";

describe("TopBarSearch", () => {
    it("có ô tìm kiếm kèm gợi ý phím tắt", () => {
        render(<TopBarSearch />);

        expect(screen.getByRole("searchbox", { name: "Tìm kiếm" })).toHaveAttribute(
            "placeholder",
            "Tìm bài, contest, người dùng…",
        );
        expect(screen.getByText("Ctrl K")).toBeInTheDocument();
    });

    it.each([{ ctrlKey: true }, { metaKey: true }])(
        "nhấn K cùng phím bổ trợ %j thì đưa con trỏ vào ô tìm kiếm",
        (modifier) => {
            render(<TopBarSearch />);

            fireEvent.keyDown(window, { key: "k", ...modifier });

            expect(screen.getByRole("searchbox", { name: "Tìm kiếm" })).toHaveFocus();
        },
    );

    it("nhấn K không kèm phím bổ trợ thì không làm gì", () => {
        render(<TopBarSearch />);

        fireEvent.keyDown(window, { key: "k" });

        expect(screen.getByRole("searchbox", { name: "Tìm kiếm" })).not.toHaveFocus();
    });
});
