import { fireEvent, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";

import { resetAdminContests } from "@/mocks/admin-contests";
import { renderWithProviders } from "@/test/render";

import { ContestCatalogue } from "./ContestCatalogue";

describe("danh sách contest", () => {
    beforeEach(() => {
        localStorage.clear();
        resetAdminContests();
    });

    it("đưa contest đang diễn ra lên khối nổi bật có đồng hồ và nút vào thi", async () => {
        renderWithProviders(<ContestCatalogue />);

        const live = within(await screen.findByRole("region", { name: "SQL Cup đang thi" }));
        expect(live.getByText("Đang diễn ra")).toBeInTheDocument();
        expect(live.getByRole("timer", { name: "Còn lại" })).toBeInTheDocument();
        expect(live.getByRole("link", { name: "Vào thi" })).toHaveAttribute(
            "href",
            "/contests/sql-cup-live",
        );
    });

    it("tab sắp diễn ra có thẻ ngày, tab đã kết thúc liệt kê contest cũ", async () => {
        renderWithProviders(<ContestCatalogue />);

        const upcoming = await screen.findByRole("article", { name: "SQL Cup 2" });
        expect(within(upcoming).getByRole("link", { name: "SQL Cup 2" })).toHaveAttribute(
            "href",
            "/contests/sql-cup-2",
        );
        expect(within(upcoming).getByText("Tính rating")).toBeInTheDocument();
        expect(within(upcoming).getByText(/90 phút/)).toBeInTheDocument();

        fireEvent.mouseDown(screen.getByRole("tab", { name: /Đã kết thúc/ }));

        expect(await screen.findByRole("link", { name: "SQL Cup 1" })).toHaveAttribute(
            "href",
            "/contests/sql-cup-1",
        );
    });
});
