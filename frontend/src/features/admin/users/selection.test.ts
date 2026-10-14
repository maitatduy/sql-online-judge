import { describe, expect, it } from "vitest";

import { pageSelectionOf, toggleOne, togglePage } from "./selection";

const PAGE = ["an.bui", "mod.thu", "viet.luu"];

describe("toggleOne", () => {
    it("thêm tài khoản chưa được chọn", () => {
        expect([...toggleOne(new Set(["an.bui"]), "mod.thu")]).toEqual(["an.bui", "mod.thu"]);
    });

    it("bỏ tài khoản đang được chọn", () => {
        expect([...toggleOne(new Set(["an.bui", "mod.thu"]), "an.bui")]).toEqual(["mod.thu"]);
    });

    it("không sửa tập hợp ban đầu", () => {
        const selected = new Set(["an.bui"]);

        toggleOne(selected, "mod.thu");

        expect([...selected]).toEqual(["an.bui"]);
    });
});

describe("pageSelectionOf", () => {
    it.each([
        [[], "none"],
        [["quang.le"], "none"],
        [["an.bui"], "some"],
        [["an.bui", "mod.thu", "viet.luu", "quang.le"], "all"],
    ] as const)("với lựa chọn %j thì trang ở trạng thái %s", (selected, expected) => {
        expect(pageSelectionOf(new Set(selected), PAGE)).toBe(expected);
    });

    it("coi trang trống là chưa chọn gì", () => {
        expect(pageSelectionOf(new Set(["an.bui"]), [])).toBe("none");
    });
});

describe("togglePage", () => {
    it("chọn cả trang và giữ lựa chọn ở trang khác", () => {
        expect([...togglePage(new Set(["quang.le", "an.bui"]), PAGE)]).toEqual([
            "quang.le",
            "an.bui",
            "mod.thu",
            "viet.luu",
        ]);
    });

    it("bỏ chọn cả trang khi trang đã được chọn hết, giữ lựa chọn ở trang khác", () => {
        expect([...togglePage(new Set(["quang.le", ...PAGE]), PAGE)]).toEqual(["quang.le"]);
    });
});
