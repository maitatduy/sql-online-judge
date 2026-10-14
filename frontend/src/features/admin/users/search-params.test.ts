import { describe, expect, it } from "vitest";

import { hasActiveFilter, NO_USERS_FILTERS, readUsersFilters, usersHref } from "./search-params";

function read(search: string) {
    return readUsersFilters(new URLSearchParams(search));
}

describe("readUsersFilters", () => {
    it("trả về bộ lọc trống khi URL không có tham số", () => {
        expect(read("")).toEqual({ q: "", role: null, state: null, page: 1, openUsername: null });
    });

    it("đọc đủ từ khoá, vai trò, trạng thái, trang và tài khoản đang mở", () => {
        expect(read("q=viet&role=SETTER&status=BANNED&page=3&user=viet.luu")).toEqual({
            q: "viet",
            role: "SETTER",
            state: "BANNED",
            page: 3,
            openUsername: "viet.luu",
        });
    });

    it("cắt khoảng trắng thừa của từ khoá", () => {
        expect(read("q=%20viet%20").q).toBe("viet");
    });

    it.each(["role=BOSS", "role=setter", "role="])("bỏ qua vai trò lạ trong '%s'", (search) => {
        expect(read(search).role).toBeNull();
    });

    it.each(["status=DELETED", "status=banned"])("bỏ qua trạng thái lạ trong '%s'", (search) => {
        expect(read(search).state).toBeNull();
    });

    it.each(["page=0", "page=-2", "page=abc", "page=2.5"])("coi '%s' là trang 1", (search) => {
        expect(read(search).page).toBe(1);
    });
});

describe("usersHref", () => {
    it("trả về đường dẫn gốc khi không có bộ lọc", () => {
        expect(usersHref(NO_USERS_FILTERS)).toBe("/admin/users");
    });

    it("chỉ gắn những tham số khác mặc định", () => {
        expect(usersHref({ ...NO_USERS_FILTERS, page: 2 })).toBe("/admin/users?page=2");
        expect(usersHref({ ...NO_USERS_FILTERS, state: "UNVERIFIED" })).toBe(
            "/admin/users?status=UNVERIFIED",
        );
    });

    it("mã hoá từ khoá và giữ thứ tự tham số ổn định", () => {
        expect(
            usersHref({
                q: "lê anh",
                role: "LEARNER",
                state: "ACTIVE",
                page: 2,
                openUsername: "viet.luu",
            }),
        ).toBe("/admin/users?q=l%C3%AA+anh&role=LEARNER&status=ACTIVE&page=2&user=viet.luu");
    });

    it("đọc lại được đúng bộ lọc đã ghi", () => {
        const filters = {
            q: "a&b=c",
            role: "ADMIN",
            state: "BANNED",
            page: 4,
            openUsername: "an.bui",
        } as const;

        expect(read(usersHref(filters).split("?")[1])).toEqual(filters);
    });
});

describe("hasActiveFilter", () => {
    it("không tính số trang là bộ lọc", () => {
        expect(hasActiveFilter({ ...NO_USERS_FILTERS, page: 3 })).toBe(false);
    });

    it("không tính tài khoản đang mở là bộ lọc", () => {
        expect(hasActiveFilter({ ...NO_USERS_FILTERS, openUsername: "viet.luu" })).toBe(false);
    });

    it.each([{ q: "viet" }, { role: "SETTER" as const }, { state: "BANNED" as const }])(
        "tính %j là bộ lọc",
        (change) => {
            expect(hasActiveFilter({ ...NO_USERS_FILTERS, ...change })).toBe(true);
        },
    );
});
