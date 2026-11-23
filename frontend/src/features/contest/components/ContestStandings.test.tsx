import { screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";

import { resetAdminContests } from "@/mocks/admin-contests";
import { registerForMockContest, resetContestRegistrations } from "@/mocks/contests";
import { DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { ContestStandings } from "./ContestStandings";

const SLUG = "sql-cup-live";

describe("bảng xếp hạng contest", () => {
    beforeEach(() => {
        localStorage.clear();
        resetAdminContests();
        resetContestRegistrations();
    });

    it("liệt kê hạng, số bài giải, phạt và từng ô theo chữ cái", async () => {
        renderWithProviders(<ContestStandings slug={SLUG} page={1} />);

        const table = within(await screen.findByRole("table", { name: "Bảng xếp hạng" }));
        const first = within(table.getByRole("row", { name: /quang\.le/ }));
        expect(first.getAllByRole("cell")[0]).toHaveTextContent("1");
        expect(first.getByRole("cell", { name: "A: AC phút 12" })).toBeInTheDocument();
        const third = within(table.getByRole("row", { name: /minh\.anh/ }));
        expect(
            third.getByRole("cell", { name: "A: sai 2 lần, 1 lượt đang chờ" }),
        ).toBeInTheDocument();
        expect(screen.getByText("Đóng băng")).toBeInTheDocument();
        expect(screen.getByRole("link", { name: "SQL Cup đang thi" })).toHaveAttribute(
            "href",
            "/contests/sql-cup-live",
        );
    });

    it("contest đã áp rating thì có cột tăng giảm rating", async () => {
        renderWithProviders(<ContestStandings slug="sql-cup-1" page={1} />);

        const table = within(await screen.findByRole("table", { name: "Bảng xếp hạng" }));
        expect(table.getByRole("columnheader", { name: "Rating" })).toBeInTheDocument();
        expect(
            within(table.getByRole("row", { name: /quang.le/ })).getByText("Tăng 20"),
        ).toBeInTheDocument();
    });

    it("người đã đăng ký thấy kết quả thật của mình", async () => {
        writeMockSession(DEMO_SESSION_USER);
        registerForMockContest(SLUG, DEMO_SESSION_USER.id);
        renderWithProviders(<ContestStandings slug={SLUG} page={1} />);

        const mine = within(await screen.findByRole("region", { name: "Kết quả của bạn" }));
        expect(mine.getByText("1 bài")).toBeInTheDocument();
        expect(mine.getByText("65 phút phạt")).toBeInTheDocument();
    });

    it("chưa đăng ký thì không có ô kết quả của bạn", async () => {
        writeMockSession(DEMO_SESSION_USER);
        renderWithProviders(<ContestStandings slug={SLUG} page={1} />);

        await screen.findByRole("table", { name: "Bảng xếp hạng" });
        expect(screen.queryByRole("region", { name: "Kết quả của bạn" })).toBeNull();
    });
});
