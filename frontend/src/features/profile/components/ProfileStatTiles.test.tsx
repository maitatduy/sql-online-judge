import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { MINH_ANH_PROFILE, NEWBIE_PROFILE } from "@/mocks/profile-fixtures";

import { ProfileStatTiles } from "./ProfileStatTiles";

function tile(label: string): HTMLElement {
    const group = screen.getByText(label).parentElement;
    if (!group) {
        throw new Error(`Không thấy thẻ số ${label}`);
    }
    return group;
}

describe("ProfileStatTiles", () => {
    it.each([
        ["Bài đã AC", "63"],
        ["Rating", "1.487"],
        ["Hạng toàn cục", "#2.341"],
        ["Chuỗi ngày", "12"],
    ])("thẻ %s hiện %s theo định dạng số tiếng Việt", (label, value) => {
        render(
            <ProfileStatTiles rating={MINH_ANH_PROFILE.rating} stats={MINH_ANH_PROFILE.stats} />,
        );

        expect(tile(label)).toHaveTextContent(`${label}${value}`);
    });

    it("hiện gạch ngang khi chưa có hạng toàn cục", () => {
        render(<ProfileStatTiles rating={NEWBIE_PROFILE.rating} stats={NEWBIE_PROFILE.stats} />);

        expect(tile("Hạng toàn cục")).toHaveTextContent("Hạng toàn cục-");
    });
});
