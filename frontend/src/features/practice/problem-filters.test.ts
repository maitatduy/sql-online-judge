import { describe, expect, it } from "vitest";

import {
    hasActiveProblemFilter,
    NO_PROBLEM_FILTERS,
    problemsHref,
    problemsQueryOf,
    readProblemFilters,
} from "./problem-filters";

describe("readProblemFilters", () => {
    it("đọc từ khoá, độ khó, chủ đề, tiến độ và trang từ URL", () => {
        const filters = readProblemFilters(
            new URLSearchParams("q=+lương+&difficulty=MEDIUM&tag=join&progress=SOLVED&page=3"),
        );

        expect(filters).toEqual({
            q: "lương",
            difficulty: "MEDIUM",
            tag: "join",
            progress: "SOLVED",
            page: 3,
        });
    });

    it("bỏ qua giá trị lạ và trang không hợp lệ", () => {
        const filters = readProblemFilters(
            new URLSearchParams("difficulty=EXTREME&progress=DONE&page=-2&tag="),
        );

        expect(filters).toEqual(NO_PROBLEM_FILTERS);
    });
});

describe("problemsHref", () => {
    it("chỉ ghi những bộ lọc đang bật và bỏ trang 1", () => {
        expect(problemsHref({ ...NO_PROBLEM_FILTERS, tag: "window-function", page: 1 })).toBe(
            "/problems?tag=window-function",
        );
    });

    it("ghi đủ bộ lọc và số trang", () => {
        expect(
            problemsHref({
                q: "join",
                difficulty: "HARD",
                tag: "cte",
                progress: "ATTEMPTED",
                page: 2,
            }),
        ).toBe("/problems?q=join&difficulty=HARD&tag=cte&progress=ATTEMPTED&page=2");
    });

    it("trả về đường dẫn gốc khi không có bộ lọc", () => {
        expect(problemsHref(NO_PROBLEM_FILTERS)).toBe("/problems");
    });
});

describe("problemsQueryOf", () => {
    it("chỉ gửi các bộ lọc đang bật cho API", () => {
        expect(problemsQueryOf({ ...NO_PROBLEM_FILTERS, difficulty: "EASY", page: 2 })).toEqual({
            difficulty: "EASY",
            page: 2,
            size: 20,
        });
    });
});

describe("hasActiveProblemFilter", () => {
    it("không tính số trang là bộ lọc", () => {
        expect(hasActiveProblemFilter({ ...NO_PROBLEM_FILTERS, page: 4 })).toBe(false);
        expect(hasActiveProblemFilter({ ...NO_PROBLEM_FILTERS, q: "null" })).toBe(true);
    });
});
