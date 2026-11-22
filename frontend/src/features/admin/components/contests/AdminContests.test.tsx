import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { resetAdminContests } from "@/mocks/admin-contests";
import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { AdminContests } from "./AdminContests";

const pushRoute = vi.fn();

vi.mock("next/navigation", () => ({
    useRouter: () => ({ push: pushRoute }),
}));

function renderContests() {
    renderWithProviders(
        <ToastProvider>
            <AdminContests />
        </ToastProvider>,
    );
}

async function openCreateDialog() {
    fireEvent.click(await screen.findByRole("button", { name: "Tạo contest" }));
    return within(await screen.findByRole("dialog", { name: "Tạo contest" }));
}

function fillContest(dialog: ReturnType<typeof within>, slug: string) {
    fireEvent.change(dialog.getByLabelText("Slug"), { target: { value: slug } });
    fireEvent.change(dialog.getByLabelText("Tên contest"), { target: { value: "SQL Cup 3" } });
    fireEvent.change(dialog.getByLabelText("Bắt đầu lúc"), {
        target: { value: "2099-12-01T19:30" },
    });
}

describe("quản lý contest", () => {
    beforeEach(() => {
        resetAdminContests();
        pushRoute.mockClear();
        writeMockSession(ADMIN_SESSION_USER);
    });

    it("liệt kê contest kèm giai đoạn, giờ bắt đầu và link chi tiết", async () => {
        renderContests();

        const table = within(await screen.findByRole("table", { name: "Contest" }));
        const row = within(table.getByRole("row", { name: /SQL Cup 2/ }));

        expect(row.getByRole("link", { name: "SQL Cup 2" })).toHaveAttribute(
            "href",
            "/admin/contests/0193a000-0000-7000-8000-000000000002",
        );
        expect(row.getByText("Sắp diễn ra")).toBeInTheDocument();
        expect(row.getByText("90 phút")).toBeInTheDocument();
        expect(row.getByText("Có")).toBeInTheDocument();
        expect(
            within(table.getByRole("row", { name: /SQL Cup 1/ })).getByText("Đã kết thúc"),
        ).toBeInTheDocument();
    });

    it("tạo contest với mặc định đóng băng 30 phút, phạt 20 phút rồi mở trang chi tiết", async () => {
        renderContests();
        const dialog = await openCreateDialog();

        expect(dialog.getByLabelText("Thời lượng (phút)")).toHaveValue(90);
        expect(dialog.getByLabelText("Đóng băng trước khi kết thúc (phút)")).toHaveValue(30);
        expect(dialog.getByLabelText("Phạt mỗi lần sai (phút)")).toHaveValue(20);
        fillContest(dialog, "sql-cup-3");
        fireEvent.click(dialog.getByRole("button", { name: "Tạo contest" }));

        await waitFor(() =>
            expect(pushRoute).toHaveBeenCalledWith(
                expect.stringMatching(/^\/admin\/contests\/[0-9a-f-]+$/),
            ),
        );
        expect(await screen.findByText("Đã tạo contest SQL Cup 3.")).toBeInTheDocument();
    });

    it("báo lỗi ngay ở ô slug khi slug đã có contest khác dùng", async () => {
        renderContests();
        const dialog = await openCreateDialog();

        fillContest(dialog, "sql-cup-2");
        fireEvent.click(dialog.getByRole("button", { name: "Tạo contest" }));

        expect(await dialog.findByText("Slug đã được contest khác dùng")).toBeInTheDocument();
        expect(pushRoute).not.toHaveBeenCalled();
    });

    it("không cho đóng băng dài hơn thời lượng contest", async () => {
        renderContests();
        const dialog = await openCreateDialog();

        fillContest(dialog, "dong-bang-dai");
        fireEvent.change(dialog.getByLabelText("Đóng băng trước khi kết thúc (phút)"), {
            target: { value: "120" },
        });
        fireEvent.click(dialog.getByRole("button", { name: "Tạo contest" }));

        expect(
            await dialog.findByText("Thời gian đóng băng phải ngắn hơn thời lượng contest"),
        ).toBeInTheDocument();
        expect(pushRoute).not.toHaveBeenCalled();
    });
});
