import { screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";

import { resetAdminContests } from "@/mocks/admin-contests";
import { renderWithProviders } from "@/test/render";

import { ContestCatalogue } from "./ContestCatalogue";

describe("danh sách contest", () => {
    beforeEach(() => {
        localStorage.clear();
        resetAdminContests();
    });

    it("chia contest theo giai đoạn, mỗi contest dẫn tới sảnh chờ", async () => {
        renderWithProviders(<ContestCatalogue />);

        const running = within(await screen.findByRole("region", { name: "Đang diễn ra" }));
        expect(await running.findByRole("link", { name: "SQL Cup đang thi" })).toHaveAttribute(
            "href",
            "/contests/sql-cup-live",
        );
        const upcoming = within(screen.getByRole("region", { name: "Sắp diễn ra" }));
        expect(await upcoming.findByRole("link", { name: "SQL Cup 2" })).toBeInTheDocument();
        expect(upcoming.getByText("90 phút")).toBeInTheDocument();
        expect(upcoming.getByText("Tính rating")).toBeInTheDocument();
        const finished = within(screen.getByRole("region", { name: "Đã kết thúc" }));
        expect(await finished.findByRole("link", { name: "SQL Cup 1" })).toBeInTheDocument();
    });
});
