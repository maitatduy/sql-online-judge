import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "@/mocks/server";
import { ADMIN_SESSION_USER, DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { AdminShell } from "./AdminShell";

const replaceRoute = vi.fn();

vi.mock("next/navigation", () => ({
    usePathname: () => "/admin",
    useRouter: () => ({ replace: replaceRoute }),
}));

function renderShell() {
    return renderWithProviders(
        <AdminShell>
            <main>Nội dung quản trị</main>
        </AdminShell>,
    );
}

describe("AdminShell", () => {
    beforeEach(() => {
        localStorage.clear();
        replaceRoute.mockClear();
    });

    it("chuyển người chưa đăng nhập tới trang đăng nhập kèm đường quay lại", async () => {
        renderShell();

        await waitFor(() => expect(replaceRoute).toHaveBeenCalledWith("/login?next=%2Fadmin"));
        expect(screen.queryByText("Nội dung quản trị")).not.toBeInTheDocument();
    });

    it("hiện trang không có quyền cho người không phải quản trị viên", async () => {
        writeMockSession(DEMO_SESSION_USER);
        renderShell();

        expect(
            await screen.findByRole("heading", { name: "Bạn không có quyền xem trang này" }),
        ).toBeInTheDocument();
        expect(screen.getByRole("link", { name: "Về trang chủ" })).toHaveAttribute("href", "/");
        expect(screen.queryByText("Nội dung quản trị")).not.toBeInTheDocument();
        expect(replaceRoute).not.toHaveBeenCalled();
    });

    it("hiện nội dung quản trị cho quản trị viên", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        renderShell();

        expect(await screen.findByText("Nội dung quản trị")).toBeInTheDocument();
        expect(
            screen.queryByRole("heading", { name: "Bạn không có quyền xem trang này" }),
        ).not.toBeInTheDocument();
    });

    it("hiện sidebar quản trị kèm số việc đang chờ cho quản trị viên", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        renderShell();

        const navigation = await screen.findByRole("navigation", { name: "Quản trị" });

        expect(within(navigation).getByRole("link", { name: /^Tổng quan/ })).toHaveAttribute(
            "aria-current",
            "page",
        );
        await waitFor(() =>
            expect(
                within(navigation).getByRole("link", { name: /^Duyệt bài tập/ }),
            ).toHaveTextContent("5 đang chờ"),
        );
        expect(navigation).toHaveTextContent("admin.huy");
    });

    it("có nút mở menu quản trị cho màn hẹp", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        renderShell();

        expect(await screen.findByRole("button", { name: "Mở menu quản trị" })).toBeInTheDocument();
    });

    it("thu gọn sidebar và ghi nhớ lựa chọn", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        renderShell();

        fireEvent.click(await screen.findByRole("button", { name: "Thu gọn menu" }));

        expect(screen.getByRole("button", { name: "Mở rộng menu" })).toBeInTheDocument();
        expect(localStorage.getItem("sqljudge:admin:sidebar-collapsed")).toBe("true");

        fireEvent.click(screen.getByRole("button", { name: "Mở rộng menu" }));

        expect(screen.getByRole("button", { name: "Thu gọn menu" })).toBeInTheDocument();
        expect(localStorage.getItem("sqljudge:admin:sidebar-collapsed")).toBe("false");
    });

    it("mở lại ở trạng thái thu gọn đã ghi nhớ", async () => {
        localStorage.setItem("sqljudge:admin:sidebar-collapsed", "true");
        writeMockSession(ADMIN_SESSION_USER);
        renderShell();

        expect(await screen.findByRole("button", { name: "Mở rộng menu" })).toBeInTheDocument();
    });

    it("không gọi API quản trị khi người xem không phải quản trị viên", async () => {
        let adminRequestCount = 0;
        server.use(
            http.all("*/api/admin/*", () => {
                adminRequestCount += 1;
                return undefined;
            }),
        );
        writeMockSession(DEMO_SESSION_USER);
        renderShell();

        await screen.findByRole("heading", { name: "Bạn không có quyền xem trang này" });

        expect(screen.queryByRole("navigation", { name: "Quản trị" })).not.toBeInTheDocument();
        expect(adminRequestCount).toBe(0);
    });

    it("chưa hiện nội dung quản trị khi đang kiểm tra quyền", () => {
        writeMockSession(ADMIN_SESSION_USER);
        renderShell();

        expect(screen.getByRole("status", { name: "Đang kiểm tra quyền truy cập" })).toBeVisible();
        expect(screen.queryByText("Nội dung quản trị")).not.toBeInTheDocument();
    });

    it("hiện lỗi có nút thử lại khi không kiểm tra được quyền", async () => {
        server.use(
            http.get(
                "*/api/auth/me",
                () =>
                    HttpResponse.json(
                        {
                            status: 500,
                            code: "INTERNAL_ERROR",
                            message: "Đã có lỗi xảy ra",
                            traceId: "4bf92f3577b34da6",
                        },
                        { status: 500 },
                    ),
                { once: true },
            ),
        );
        writeMockSession(ADMIN_SESSION_USER);
        renderShell();

        expect(await screen.findByRole("alert")).toHaveTextContent("4bf92f3577b34da6");
        expect(screen.queryByText("Nội dung quản trị")).not.toBeInTheDocument();
        expect(replaceRoute).not.toHaveBeenCalled();

        fireEvent.click(screen.getByRole("button", { name: "Thử lại" }));

        expect(await screen.findByText("Nội dung quản trị")).toBeInTheDocument();
    });
});
