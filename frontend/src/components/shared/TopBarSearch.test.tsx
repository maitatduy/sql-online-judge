import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { TopBarSearch } from "./TopBarSearch";

describe("TopBarSearch", () => {
    it("có ô tìm kiếm, không kèm gợi ý phím tắt", () => {
        render(<TopBarSearch />);

        const search = screen.getByRole("searchbox", { name: "Tìm kiếm" });
        expect(search).toHaveAttribute("placeholder", "Tìm bài, contest, người dùng…");
        expect(search).not.toHaveAttribute("aria-keyshortcuts");
        expect(screen.queryByText(/Ctrl/)).not.toBeInTheDocument();
    });

    it("nhấn Ctrl+K không chiếm con trỏ của trang", () => {
        render(<TopBarSearch />);

        fireEvent.keyDown(window, { key: "k", ctrlKey: true });

        expect(screen.getByRole("searchbox", { name: "Tìm kiếm" })).not.toHaveFocus();
    });
});
