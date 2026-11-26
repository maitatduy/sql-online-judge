import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "@/mocks/server";
import { renderWithProviders } from "@/test/render";
import { visit } from "@/test/url-search-params";

import { CourseCatalogue } from "./CourseCatalogue";

vi.mock("next/navigation", async () => ({
    useSearchParams: (await import("@/test/url-search-params")).useUrlSearchParams,
}));

async function cardOf(title: string) {
    const heading = await screen.findByRole("heading", { name: title });
    const card = heading.closest("article");
    if (!card) throw new Error(`Không thấy thẻ khoá học ${title}`);
    return within(card);
}

describe("CourseCatalogue", () => {
    beforeEach(() => {
        visit("/learn");
    });

    it("hiện thẻ khoá học dẫn tới trang khoá, kèm độ khó, số bài và thời lượng", async () => {
        renderWithProviders(<CourseCatalogue />);
        const card = await cardOf("SQL cơ bản");

        expect(card.getByRole("link", { name: "SQL cơ bản" })).toHaveAttribute(
            "href",
            "/learn/sql-co-ban",
        );
        expect(card.getByText("Dễ")).toHaveClass("text-difficulty-easy");
        expect(card.getByText("4 bài")).toBeInTheDocument();
        expect(card.getByText("40 phút")).toBeInTheDocument();
    });

    it("có breadcrumb về trang chủ", async () => {
        renderWithProviders(<CourseCatalogue />);

        const breadcrumb = within(await screen.findByRole("navigation", { name: "Đường dẫn" }));
        expect(breadcrumb.getByRole("link", { name: "Trang chủ" })).toHaveAttribute("href", "/");
        expect(breadcrumb.getByText("Khoá học")).toHaveAttribute("aria-current", "page");
    });

    it("chia khoá học thành trang theo URL, giữ bộ lọc khi sang trang", async () => {
        const requested: string[] = [];
        server.use(
            http.get("*/api/courses", ({ request }) => {
                const params = new URL(request.url).searchParams;
                requested.push(params.toString());
                return HttpResponse.json({
                    items: [],
                    page: Number(params.get("page")),
                    size: Number(params.get("size")),
                    totalItems: 14,
                    totalPages: 3,
                });
            }),
        );
        visit("/learn?difficulty=EASY&page=2");
        renderWithProviders(<CourseCatalogue />);

        const navigation = within(await screen.findByRole("navigation", { name: "Phân trang" }));
        expect(requested[0]).toBe("difficulty=EASY&page=2&size=6");
        expect(screen.getByText("7-12 trong 14 khoá học")).toBeInTheDocument();
        expect(navigation.getByRole("link", { name: "Trang 3" })).toHaveAttribute(
            "href",
            "/learn?difficulty=EASY&page=3",
        );
        expect(navigation.getByRole("link", { name: "Trang 1" })).toHaveAttribute(
            "href",
            "/learn?difficulty=EASY",
        );
    });

    it("lọc theo độ khó và ghi lựa chọn lên URL", async () => {
        renderWithProviders(<CourseCatalogue />);
        await cardOf("SQL cơ bản");

        fireEvent.click(screen.getByRole("radio", { name: "Trung bình" }));

        await waitFor(() =>
            expect(screen.queryByRole("heading", { name: "SQL cơ bản" })).not.toBeInTheDocument(),
        );
        expect(screen.getByRole("heading", { name: "JOIN thực chiến" })).toBeInTheDocument();
        expect(window.location.search).toBe("?difficulty=MEDIUM");
    });

    it("đổi độ khó thì quay về trang 1", async () => {
        visit("/learn?page=2");
        renderWithProviders(<CourseCatalogue />);
        await screen.findByRole("radio", { name: "Trung bình" });

        fireEvent.click(screen.getByRole("radio", { name: "Trung bình" }));

        await waitFor(() => expect(window.location.search).toBe("?difficulty=MEDIUM"));
        expect(screen.getByRole("radio", { name: "Trung bình" })).toBeChecked();
    });

    it("báo trống ngắn gọn khi không có khoá nào khớp độ khó", async () => {
        visit("/learn?difficulty=HARD");
        renderWithProviders(<CourseCatalogue />);

        expect(await screen.findByText("Chưa có khoá học nào")).toBeInTheDocument();
    });

    it("hiện lỗi kèm nút thử lại khi không tải được danh sách", async () => {
        server.use(
            http.get("*/api/courses", () =>
                HttpResponse.json(
                    { status: 500, code: "INTERNAL_ERROR", message: "Đã có lỗi xảy ra" },
                    { status: 500 },
                ),
            ),
        );
        renderWithProviders(<CourseCatalogue />);

        expect(await screen.findByText("Chưa tải được khoá học")).toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Thử lại" })).toBeInTheDocument();
    });
});
