import { fireEvent, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { SETTER_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { MyCourses } from "./MyCourses";

const push = vi.fn();

vi.mock("next/navigation", () => ({
    useRouter: () => ({ push }),
}));

function renderMyCourses() {
    renderWithProviders(
        <ToastProvider>
            <MyCourses />
        </ToastProvider>,
    );
}

async function openCreateDialog() {
    renderMyCourses();
    await screen.findByRole("table", { name: "Khoá học của tôi" });
    fireEvent.click(screen.getByRole("button", { name: "Tạo khoá mới" }));
    return within(await screen.findByRole("dialog", { name: "Tạo khoá mới" }));
}

function fillCourse(dialog: ReturnType<typeof within>, slug: string) {
    fireEvent.change(dialog.getByRole("textbox", { name: "Tên khoá" }), {
        target: { value: "CTE và truy vấn đệ quy" },
    });
    fireEvent.change(dialog.getByRole("textbox", { name: "Slug" }), { target: { value: slug } });
    fireEvent.change(dialog.getByRole("textbox", { name: "Tóm tắt" }), {
        target: { value: "Viết truy vấn gọn hơn với WITH." },
    });
    fireEvent.change(dialog.getByRole("spinbutton", { name: "Thời lượng (phút)" }), {
        target: { value: "45" },
    });
}

describe("Khoá học của tôi trong Studio", () => {
    beforeEach(() => {
        writeMockSession(SETTER_SESSION_USER);
        push.mockReset();
    });

    it("liệt kê khoá của người ra đề kèm trạng thái và link tới trang soạn", async () => {
        renderMyCourses();

        const table = within(await screen.findByRole("table", { name: "Khoá học của tôi" }));
        const link = table.getByRole("link", { name: "Window function từ đầu" });
        expect(link).toHaveAttribute(
            "href",
            "/studio/courses/0192f3f1-0000-7000-8000-000000000001",
        );
        expect(table.getByText("Chờ duyệt")).toBeInTheDocument();
        expect(screen.getByText("2 khoá")).toBeInTheDocument();
    });

    it("tạo khoá nháp rồi mở trang soạn của khoá đó", async () => {
        const dialog = await openCreateDialog();

        fillCourse(dialog, "cte-va-de-quy");
        fireEvent.click(dialog.getByRole("button", { name: "Tạo khoá nháp" }));

        expect(
            await screen.findByText("Đã tạo khoá nháp CTE và truy vấn đệ quy."),
        ).toBeInTheDocument();
        expect(push).toHaveBeenCalledWith(expect.stringMatching(/^\/studio\/courses\/.+/));
    });

    it("báo lỗi ở ô slug khi slug sai dạng hoặc đã có khoá khác dùng", async () => {
        const dialog = await openCreateDialog();

        fillCourse(dialog, "Sai Dang");
        fireEvent.click(dialog.getByRole("button", { name: "Tạo khoá nháp" }));
        expect(
            await dialog.findByText("Chỉ gồm chữ thường không dấu, số và dấu gạch nối"),
        ).toBeInTheDocument();

        fillCourse(dialog, "join-trong-thuc-te");
        fireEvent.click(dialog.getByRole("button", { name: "Tạo khoá nháp" }));
        expect(await dialog.findByText("Slug đã được khoá khác dùng")).toBeInTheDocument();
        expect(push).not.toHaveBeenCalled();
    });
});
