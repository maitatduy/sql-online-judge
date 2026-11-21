import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { AdminCourses } from "./AdminCourses";

const PUBLISHED_TITLE = "CTE và truy vấn đệ quy";

async function renderCourses() {
    renderWithProviders(
        <ToastProvider>
            <AdminCourses />
        </ToastProvider>,
    );
    return within(await screen.findByRole("table", { name: "Khoá học" }));
}

function rowOf(table: ReturnType<typeof within>, title: string) {
    return within(table.getByRole("row", { name: new RegExp(title) }));
}

describe("quản lý khoá học", () => {
    beforeEach(() => {
        writeMockSession(ADMIN_SESSION_USER);
    });

    it("liệt kê mọi khoá kèm trạng thái và link mở trong Studio", async () => {
        const table = await renderCourses();

        const published = rowOf(table, PUBLISHED_TITLE);
        expect(published.getByText("Đã xuất bản")).toBeInTheDocument();
        expect(published.getByRole("link", { name: PUBLISHED_TITLE })).toHaveAttribute(
            "href",
            "/studio/courses/0192f3f1-0000-7000-8000-000000000003",
        );
        expect(rowOf(table, "JOIN trong thực tế").getByText("Chờ duyệt")).toBeInTheDocument();
    });

    it("lọc theo trạng thái", async () => {
        await renderCourses();

        fireEvent.change(screen.getByRole("combobox", { name: "Trạng thái" }), {
            target: { value: "PUBLISHED" },
        });

        const table = within(await screen.findByRole("table", { name: "Khoá học" }));
        await waitFor(() =>
            expect(table.queryByText("Window function từ đầu")).not.toBeInTheDocument(),
        );
        expect(table.getByText(PUBLISHED_TITLE)).toBeInTheDocument();
    });

    it("ẩn khoá sau khi xác nhận rồi hiện lại", async () => {
        const table = await renderCourses();

        fireEvent.click(rowOf(table, PUBLISHED_TITLE).getByRole("button", { name: "Ẩn" }));
        fireEvent.click(
            within(await screen.findByRole("alertdialog")).getByRole("button", {
                name: "Ẩn khoá",
            }),
        );

        expect(await screen.findByText(`Đã ẩn khoá ${PUBLISHED_TITLE}.`)).toBeInTheDocument();
        const hidden = rowOf(table, PUBLISHED_TITLE);
        expect(await hidden.findByText("Đã ẩn")).toBeInTheDocument();

        fireEvent.click(hidden.getByRole("button", { name: "Hiện lại" }));
        expect(await screen.findByText(`Đã hiện lại khoá ${PUBLISHED_TITLE}.`)).toBeInTheDocument();
        expect(await rowOf(table, PUBLISHED_TITLE).findByText("Đã xuất bản")).toBeInTheDocument();
    });
});
