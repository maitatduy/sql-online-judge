import { fireEvent, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { SETTER_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";
import { visit } from "@/test/url-search-params";

import { MyProblems } from "./MyProblems";

vi.mock("next/navigation", async () => ({
    useSearchParams: (await import("@/test/url-search-params")).useUrlSearchParams,
}));

const TABLE_NAME = "Bài của tôi";

async function renderMyProblems(search = "") {
    visit(`/studio/problems${search}`);
    renderWithProviders(
        <ToastProvider>
            <MyProblems />
        </ToastProvider>,
    );
    return within(await screen.findByRole("table", { name: TABLE_NAME }));
}

function titlesIn(table: ReturnType<typeof within>) {
    return table
        .getAllByRole("row")
        .slice(1)
        .map((row: HTMLElement) => within(row).getAllByRole("cell")[0].textContent);
}

function rowOf(table: ReturnType<typeof within>, title: string) {
    return within(table.getByRole("row", { name: new RegExp(title) }));
}

describe("MyProblems - bộ lọc, phân trang và hành động", () => {
    beforeEach(() => {
        writeMockSession(SETTER_SESSION_USER);
    });

    it("lọc theo độ khó và ghi lên URL", async () => {
        await renderMyProblems();

        fireEvent.change(screen.getByRole("combobox", { name: "Độ khó" }), {
            target: { value: "HARD" },
        });

        const table = within(await screen.findByRole("table", { name: TABLE_NAME }));
        await vi.waitFor(() =>
            expect(titlesIn(table)).toEqual([
                "Chuỗi ngày hoạt động dài nhất",
                "Tỉ lệ giữ chân theo cohort tháng",
                "Khoảng trống trong lịch đặt phòng",
            ]),
        );
        expect(window.location.search).toBe("?difficulty=HARD");
    });

    it("sắp xếp theo tên A-Z", async () => {
        await renderMyProblems();

        fireEvent.change(screen.getByRole("combobox", { name: "Sắp xếp" }), {
            target: { value: "title,asc" },
        });

        const table = within(await screen.findByRole("table", { name: TABLE_NAME }));
        await vi.waitFor(() => expect(titlesIn(table)[0]).toBe("Cặp sinh viên cùng lớp"));
        expect(window.location.search).toBe("?sort=title%2Casc");
    });

    it("luôn hiện thanh phân trang và đổi được số dòng mỗi trang", async () => {
        await renderMyProblems();

        expect(screen.getByText("1-9 trong 9 bài")).toBeInTheDocument();
        fireEvent.change(screen.getByRole("combobox", { name: "Số dòng mỗi trang" }), {
            target: { value: "10" },
        });

        await vi.waitFor(() => expect(window.location.search).toBe("?size=10"));
        expect(screen.getByRole("combobox", { name: "Số dòng mỗi trang" })).toHaveValue("10");
    });

    it("xem chi tiết bài trong hộp thoại, có nút sang trang sửa", async () => {
        const table = await renderMyProblems();

        fireEvent.click(
            rowOf(table, "Phễu chuyển đổi 4 bước").getByRole("button", {
                name: "Xem chi tiết Phễu chuyển đổi 4 bước",
            }),
        );

        const dialog = within(
            await screen.findByRole("dialog", { name: "Phễu chuyển đổi 4 bước" }),
        );
        expect(await dialog.findByText("phieu-chuyen-doi-4-buoc")).toBeInTheDocument();
        expect(dialog.getByText("Nháp")).toBeInTheDocument();
        expect(dialog.getByText("Chưa có đề bài.")).toBeInTheDocument();
        expect(dialog.getByRole("link", { name: "Sửa bài" })).toHaveAttribute(
            "href",
            "/studio/problems/0192f3d0-0000-7000-8000-000000000001",
        );
    });

    it("chỉ sửa được bài nháp hoặc cần sửa", async () => {
        const table = await renderMyProblems();

        expect(
            rowOf(table, "Phễu chuyển đổi 4 bước").getByRole("link", {
                name: "Sửa Phễu chuyển đổi 4 bước",
            }),
        ).toHaveAttribute("href", "/studio/problems/0192f3d0-0000-7000-8000-000000000001");
        expect(
            rowOf(table, "Cặp sinh viên cùng lớp").getByRole("button", {
                name: "Sửa Cặp sinh viên cùng lớp",
            }),
        ).toBeDisabled();
    });

    it("xoá bài nháp sau khi xác nhận, bài biến mất khỏi danh sách", async () => {
        const table = await renderMyProblems();

        fireEvent.click(
            rowOf(table, "Phễu chuyển đổi 4 bước").getByRole("button", {
                name: "Xoá Phễu chuyển đổi 4 bước",
            }),
        );
        const dialog = within(await screen.findByRole("alertdialog"));
        expect(dialog.getByText("Xoá bài Phễu chuyển đổi 4 bước?")).toBeInTheDocument();
        fireEvent.click(dialog.getByRole("button", { name: "Xoá bài" }));

        expect(await screen.findByText("Đã xoá bài Phễu chuyển đổi 4 bước.")).toBeInTheDocument();
        await vi.waitFor(() => expect(titlesIn(table)).not.toContain("Phễu chuyển đổi 4 bước"));
        expect(screen.getByText("1-8 trong 8 bài")).toBeInTheDocument();
    });

    it("người ra đề không xoá được bài đã xuất bản", async () => {
        const table = await renderMyProblems();

        expect(
            rowOf(table, "Cặp sinh viên cùng lớp").getByRole("button", {
                name: "Xoá Cặp sinh viên cùng lớp",
            }),
        ).toBeDisabled();
    });
});
