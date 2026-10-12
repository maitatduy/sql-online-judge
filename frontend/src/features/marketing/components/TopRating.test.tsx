import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { TopRating } from "./TopRating";

const users = [
    { username: "quang.le", rating: 2184 },
    { username: "trang.hoang", rating: 2097 },
];

describe("TopRating", () => {
    it("hiện thứ hạng, tên và rating định dạng vi-VN", () => {
        render(<TopRating users={users} />);

        const rows = within(screen.getByRole("table", { name: "Top rating" })).getAllByRole("row");
        const firstUserRow = rows[1];

        expect(rows).toHaveLength(users.length + 1);
        expect(within(firstUserRow).getByText("1")).toBeInTheDocument();
        expect(within(firstUserRow).getByText("quang.le")).toBeInTheDocument();
        expect(within(firstUserRow).getByText("2.184")).toBeInTheDocument();
    });

    it("avatar hiện chữ cái đầu của tên", () => {
        render(<TopRating users={users} />);

        expect(screen.getByText("QL")).toBeInTheDocument();
        expect(screen.getByText("TH")).toBeInTheDocument();
    });
});
