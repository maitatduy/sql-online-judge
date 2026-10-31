import { fireEvent, screen, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { server } from "@/mocks/server";
import { SETTER_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";
import { visit } from "@/test/url-search-params";

import { MyProblems } from "./MyProblems";

vi.mock("next/navigation", async () => ({
    useSearchParams: (await import("@/test/url-search-params")).useUrlSearchParams,
}));

async function openCreateDialog() {
    visit("/studio/problems");
    renderWithProviders(
        <ToastProvider>
            <MyProblems />
        </ToastProvider>,
    );
    await screen.findByRole("table", { name: "Bài của tôi" });
    fireEvent.click(screen.getByRole("button", { name: "Tạo bài mới" }));
    return within(await screen.findByRole("dialog", { name: "Tạo bài mới" }));
}

describe("tạo bài mới trong Studio", () => {
    beforeEach(() => {
        writeMockSession(SETTER_SESSION_USER);
    });

    it("tạo bài nháp, báo thành công và đưa bài lên đầu danh sách", async () => {
        const dialog = await openCreateDialog();

        fireEvent.change(dialog.getByRole("textbox", { name: "Tên bài" }), {
            target: { value: "Khách hàng mua liên tiếp 3 tháng" },
        });
        fireEvent.click(dialog.getByRole("button", { name: "Tạo bài nháp" }));

        expect(
            await screen.findByText("Đã tạo bài nháp Khách hàng mua liên tiếp 3 tháng."),
        ).toBeInTheDocument();
        expect(screen.queryByRole("dialog", { name: "Tạo bài mới" })).not.toBeInTheDocument();
        const table = within(screen.getByRole("table", { name: "Bài của tôi" }));
        expect(await table.findByText("Khách hàng mua liên tiếp 3 tháng")).toBeInTheDocument();
        expect(await screen.findByRole("tab", { name: "Nháp 3" })).toBeInTheDocument();
    });

    it("báo lỗi ngay khi tên bài trống, không gửi yêu cầu", async () => {
        const createRequest = vi.fn();
        server.events.on("request:start", ({ request }) => {
            if (request.method === "POST") {
                createRequest();
            }
        });
        const dialog = await openCreateDialog();

        fireEvent.change(dialog.getByRole("textbox", { name: "Tên bài" }), {
            target: { value: "   " },
        });
        fireEvent.click(dialog.getByRole("button", { name: "Tạo bài nháp" }));

        expect(await dialog.findByText("Không được để trống")).toBeInTheDocument();
        expect(createRequest).not.toHaveBeenCalled();
        server.events.removeAllListeners();
    });

    it("gắn lỗi máy chủ vào đúng ô tên bài", async () => {
        server.use(
            http.post("*/api/studio/problems", () =>
                HttpResponse.json(
                    {
                        status: 400,
                        code: "VALIDATION_FAILED",
                        message: "Dữ liệu gửi lên không hợp lệ",
                        errors: [{ field: "title", message: "Tối đa 200 ký tự" }],
                    },
                    { status: 400 },
                ),
            ),
        );
        const dialog = await openCreateDialog();

        fireEvent.change(dialog.getByRole("textbox", { name: "Tên bài" }), {
            target: { value: "Bài thử" },
        });
        fireEvent.click(dialog.getByRole("button", { name: "Tạo bài nháp" }));

        expect(await dialog.findByText("Tối đa 200 ký tự")).toBeInTheDocument();
        expect(dialog.getByRole("textbox", { name: "Tên bài" })).toHaveAttribute(
            "aria-invalid",
            "true",
        );
    });
});
