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

    it("lọc theo độ khó và ghi lựa chọn lên URL", async () => {
        renderWithProviders(<CourseCatalogue />);
        await cardOf("SQL cơ bản");

        fireEvent.click(screen.getByRole("radio", { name: "Trung bình" }));

        await waitFor(() =>
            expect(screen.queryByRole("heading", { name: "SQL cơ bản" })).not.toBeInTheDocument(),
        );
        expect(screen.getByRole("heading", { name: "JOIN thực chiến" })).toBeInTheDocument();
        expect(window.location.search).toBe("?difficulty=MEDIUM");
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
