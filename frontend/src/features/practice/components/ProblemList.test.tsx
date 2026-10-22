import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "@/mocks/server";
import { DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";
import { visit } from "@/test/url-search-params";

import { ProblemList } from "./ProblemList";

vi.mock("next/navigation", async () => ({
    useSearchParams: (await import("@/test/url-search-params")).useUrlSearchParams,
}));

const TABLE_NAME = "Danh sách bài tập";

async function renderList(search = "") {
    visit(`/problems${search}`);
    renderWithProviders(<ProblemList />);
    return within(await screen.findByRole("table", { name: TABLE_NAME }));
}

function rowOf(table: ReturnType<typeof within>, title: string) {
    return within(table.getByRole("row", { name: new RegExp(title) }));
}

describe("ProblemList", () => {
    beforeEach(() => {
        visit("/problems");
    });

    it("hiện bài với số, tên dẫn tới workspace, chủ đề, độ khó, tỉ lệ AC và người giải", async () => {
        const table = await renderList();
        const row = rowOf(table, "Lương cao nhất mỗi phòng ban");

        expect(row.getByRole("link", { name: "Lương cao nhất mỗi phòng ban" })).toHaveAttribute(
            "href",
            "/problems/highest-salary-per-department",
        );
        expect(row.getByText("184")).toBeInTheDocument();
        expect(row.getByText("Subquery")).toBeInTheDocument();
        expect(row.getByText("Trung bình")).toHaveClass("text-difficulty-medium");
        expect(row.getByText("41,2%")).toBeInTheDocument();
        expect(row.getByText("12.380")).toBeInTheDocument();
    });

    it("ẩn cột và bộ lọc tiến độ khi chưa đăng nhập", async () => {
        const table = await renderList();

        expect(table.queryByRole("columnheader", { name: "Tiến độ" })).not.toBeInTheDocument();
        expect(
            screen.queryByRole("combobox", { name: "Lọc theo tiến độ" }),
        ).not.toBeInTheDocument();
    });

    it("hiện tiến độ của người đã đăng nhập bằng chữ, không chỉ bằng màu", async () => {
        writeMockSession(DEMO_SESSION_USER);
        const table = await renderList();

        expect(await table.findByRole("columnheader", { name: "Tiến độ" })).toBeInTheDocument();
        expect(rowOf(table, "Lương cao nhất mỗi phòng ban").getByText("Đã AC")).toBeInTheDocument();
        expect(rowOf(table, "Sản phẩm chưa bán được").getByText("Đã thử")).toBeInTheDocument();
        expect(screen.getByRole("combobox", { name: "Lọc theo tiến độ" })).toBeInTheDocument();
    });

    it("lọc theo độ khó và chủ đề, ghi bộ lọc lên URL và về trang 1", async () => {
        const table = await renderList("?page=2");

        fireEvent.change(screen.getByRole("combobox", { name: "Lọc theo độ khó" }), {
            target: { value: "HARD" },
        });
        await screen.findByRole("option", { name: "Window Function" });
        fireEvent.change(screen.getByRole("combobox", { name: "Lọc theo chủ đề" }), {
            target: { value: "window-function" },
        });

        await waitFor(() =>
            expect(window.location.search).toBe("?difficulty=HARD&topic=window-function"),
        );
        await waitFor(() => expect(table.getAllByRole("row")).toHaveLength(4));
        expect(
            table.getByRole("link", { name: "Chuỗi ngày đăng nhập liên tiếp dài nhất" }),
        ).toBeInTheDocument();
    });

    it("tìm theo tên bài sau khi ngừng gõ", async () => {
        const table = await renderList();

        fireEvent.change(screen.getByRole("searchbox", { name: "Tìm bài tập" }), {
            target: { value: "  lương " },
        });

        await waitFor(() => expect(window.location.search).toBe("?q=l%C6%B0%C6%A1ng"));
        await waitFor(() => expect(table.getAllByRole("row")).toHaveLength(2));
    });

    it("chia 24 bài thành hai trang và dẫn tới trang sau bằng link", async () => {
        const table = await renderList();

        expect(screen.getByText("1-20 trong 24")).toBeInTheDocument();
        expect(table.getAllByRole("row")).toHaveLength(21);
        const navigation = within(screen.getByRole("navigation", { name: "Phân trang" }));
        expect(navigation.getByRole("link", { name: "Trang 2" })).toHaveAttribute(
            "href",
            "/problems?page=2",
        );
    });

    it("báo không có bài khớp bộ lọc và cho xoá bộ lọc", async () => {
        visit("/problems?q=khongcobainao");
        renderWithProviders(<ProblemList />);

        expect(await screen.findByText("Không có bài khớp bộ lọc")).toBeInTheDocument();
        fireEvent.click(screen.getByRole("button", { name: "Xoá bộ lọc" }));

        await waitFor(() => expect(window.location.search).toBe(""));
        expect(await screen.findByRole("table", { name: TABLE_NAME })).toBeInTheDocument();
    });

    it("hiện trạng thái đang tải trước khi có dữ liệu", async () => {
        renderWithProviders(<ProblemList />);

        expect(screen.getByRole("status", { name: "Đang tải danh sách bài" })).toBeInTheDocument();
        await screen.findByRole("table", { name: TABLE_NAME });
    });

    it("báo lỗi kèm mã lỗi và thử lại được", async () => {
        let remainingFailures = 1;
        server.use(
            http.get("*/api/problems", () => {
                if (remainingFailures === 0) {
                    return undefined;
                }
                remainingFailures -= 1;
                return HttpResponse.json(
                    {
                        status: 500,
                        code: "INTERNAL_ERROR",
                        message: "Đã có lỗi xảy ra",
                        traceId: "req_7f2c91",
                    },
                    { status: 500 },
                );
            }),
        );
        renderWithProviders(<ProblemList />);

        const alert = await screen.findByRole("alert");
        expect(alert).toHaveTextContent("Chưa tải được danh sách bài");
        expect(alert).toHaveTextContent("req_7f2c91");

        fireEvent.click(within(alert).getByRole("button", { name: "Thử lại" }));

        expect(await screen.findByRole("table", { name: TABLE_NAME })).toBeInTheDocument();
    });
});
