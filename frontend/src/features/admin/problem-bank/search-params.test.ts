import { describe, expect, it } from "vitest";

import { bankHref, hasActiveBankFilter, NO_BANK_FILTERS, readBankFilters } from "./search-params";

describe("bộ lọc kho bài tập trên URL", () => {
    it("đọc bộ lọc hợp lệ và bỏ qua giá trị lạ", () => {
        const filters = readBankFilters(
            new URLSearchParams("q=+lương+&visibility=HIDDEN&difficulty=HARD&tag=join&page=3"),
        );
        const unknown = readBankFilters(
            new URLSearchParams("visibility=SECRET&difficulty=EXTREME&page=-2"),
        );

        expect(filters).toEqual({
            q: "lương",
            visibility: "HIDDEN",
            difficulty: "HARD",
            tag: "join",
            page: 3,
        });
        expect(unknown).toEqual(NO_BANK_FILTERS);
    });

    it("ghi lại đúng đường dẫn, bỏ tham số mặc định", () => {
        expect(bankHref(NO_BANK_FILTERS)).toBe("/admin/problems");
        expect(
            bankHref({ ...NO_BANK_FILTERS, visibility: "CONTEST_ONLY", tag: "cte", page: 2 }),
        ).toBe("/admin/problems?visibility=CONTEST_ONLY&tag=cte&page=2");
        expect(hasActiveBankFilter(NO_BANK_FILTERS)).toBe(false);
        expect(hasActiveBankFilter({ ...NO_BANK_FILTERS, q: "41" })).toBe(true);
    });
});
