import { describe, expect, it } from "vitest";

import { NO_USERS_FILTERS } from "./search-params";
import { usersQueryOf } from "./users-query";

describe("usersQueryOf", () => {
    it("trả về điều kiện rỗng khi không lọc", () => {
        expect(usersQueryOf(NO_USERS_FILTERS)).toEqual({});
    });

    it("chuyển trạng thái chưa xác minh thành điều kiện trạng thái và xác minh email", () => {
        expect(
            usersQueryOf({ ...NO_USERS_FILTERS, q: "an", role: "SETTER", state: "UNVERIFIED" }),
        ).toEqual({ q: "an", role: "SETTER", status: "ACTIVE", emailVerified: false });
    });

    it("không mang theo trang và tài khoản đang mở", () => {
        expect(
            usersQueryOf({
                ...NO_USERS_FILTERS,
                state: "BANNED",
                page: 3,
                openUsername: "viet.luu",
            }),
        ).toEqual({ status: "BANNED" });
    });
});
