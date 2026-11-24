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
        ["Contest đã thi", "8"],
    ])("thẻ %s hiện %s theo định dạng số tiếng Việt", (label, value) => {
        render(
            <ProfileStatTiles rating={MINH_ANH_PROFILE.rating} solved={MINH_ANH_PROFILE.solved} />,
        );

        expect(tile(label)).toHaveTextContent(`${label}${value}`);
    });

    it("hiện gạch ngang khi chưa có rating và hạng", () => {
        render(<ProfileStatTiles rating={NEWBIE_PROFILE.rating} solved={NEWBIE_PROFILE.solved} />);

        expect(tile("Rating")).toHaveTextContent("Rating-");
        expect(tile("Hạng toàn cục")).toHaveTextContent("Hạng toàn cục-");
    });
});
