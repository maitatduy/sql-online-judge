import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { Pagination } from "./Pagination";

const hrefOf = (page: number) => `/problems?page=${page}`;

function renderPagination(page: number, totalItems: number, size = 20) {
    render(
        <Pagination
            result={{ page, size, totalItems, totalPages: Math.ceil(totalItems / size) }}
            hrefOf={hrefOf}
        />,
    );
}

describe("Pagination", () => {
    it("nêu khoảng dòng và đánh dấu trang hiện tại", () => {
        renderPagination(2, 64);

        expect(screen.getByText("21-40 trong 64")).toBeInTheDocument();
        const navigation = within(screen.getByRole("navigation", { name: "Phân trang" }));
        expect(navigation.getByRole("link", { name: "Trang 2" })).toHaveAttribute(
            "aria-current",
            "page",
        );
        expect(navigation.getByRole("link", { name: "Trang 4" })).toHaveAttribute(
            "href",
            "/problems?page=4",
        );
    });

    it("dẫn tới trang trước và trang sau", () => {
        renderPagination(2, 64);

        expect(screen.getByRole("link", { name: "Trước" })).toHaveAttribute(
            "href",
            "/problems?page=1",
        );
        expect(screen.getByRole("link", { name: "Sau" })).toHaveAttribute(
            "href",
            "/problems?page=3",
        );
    });

    it("khoá nút Trước ở trang đầu và nút Sau ở trang cuối", () => {
        renderPagination(1, 15, 10);

        expect(screen.getByRole("button", { name: "Trước" })).toBeDisabled();
        expect(screen.getByRole("link", { name: "Sau" })).toBeInTheDocument();
    });

    it("không hiện điều hướng khi chỉ có một trang", () => {
        renderPagination(1, 12);

        expect(screen.getByText("1-12 trong 12")).toBeInTheDocument();
        expect(screen.queryByRole("navigation")).not.toBeInTheDocument();
    });
});
