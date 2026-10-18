import { afterEach, describe, expect, it } from "vitest";

import { clearAccessToken, getAccessToken, setAccessToken } from "./auth-session";

describe("auth-session", () => {
    afterEach(() => {
        clearAccessToken();
    });

    it("chưa đăng nhập thì không có access token", () => {
        expect(getAccessToken()).toBeNull();
    });

    it("giữ access token trong bộ nhớ, không ghi vào storage", () => {
        setAccessToken("access-token");

        expect(getAccessToken()).toBe("access-token");
        expect(localStorage.length).toBe(0);
        expect(sessionStorage.getItem("access-token")).toBeNull();
    });

    it("xoá token khi đăng xuất", () => {
        setAccessToken("access-token");

        clearAccessToken();

        expect(getAccessToken()).toBeNull();
    });
});
