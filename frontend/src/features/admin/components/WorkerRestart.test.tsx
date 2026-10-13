import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { server } from "@/mocks/server";
import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { JudgeWorkersCard } from "./JudgeWorkersCard";

const MAX_REASON_LENGTH = 500;

function renderCard() {
    writeMockSession(ADMIN_SESSION_USER);
    return renderWithProviders(
        <ToastProvider>
            <JudgeWorkersCard />
        </ToastProvider>,
    );
}

async function openRestartDialog() {
    const restartButton = await screen.findByRole("button", {
        name: "Khởi động lại judge-my-02",
    });
    restartButton.focus();
    fireEvent.click(restartButton);
    return {
        restartButton,
        dialog: within(await screen.findByRole("dialog", { name: "Khởi động lại judge-my-02?" })),
    };
}

function typeReason(dialog: ReturnType<typeof within>, reason: string) {
    fireEvent.change(dialog.getByLabelText("Lý do (ghi vào nhật ký thao tác)"), {
        target: { value: reason },
    });
}

describe("khởi động lại worker", () => {
    it("báo worker mất kết nối kèm thời điểm và nút khởi động lại", async () => {
        renderCard();

        const alert = await screen.findByRole("alert");

        expect(alert).toHaveTextContent(/judge-my-02 mất kết nối từ \d{2}:\d{2}/);
        expect(
            within(alert).getByRole("button", { name: "Khởi động lại judge-my-02" }),
        ).toBeEnabled();
    });

    it("khoá nút xác nhận cho tới khi có lý do", async () => {
        renderCard();
        const { dialog } = await openRestartDialog();
        const confirmButton = dialog.getByRole("button", { name: "Khởi động lại" });

        expect(confirmButton).toBeDisabled();

        typeReason(dialog, "   ");
        expect(confirmButton).toBeDisabled();

        typeReason(dialog, "Worker treo sau khi cập nhật");
        expect(confirmButton).toBeEnabled();
    });

    it("nói rõ bài đang chờ không bị mất", async () => {
        renderCard();
        const { dialog } = await openRestartDialog();

        expect(
            dialog.getByText("Bài đang chờ sẽ chuyển sang worker khác, không bài nào bị mất."),
        ).toBeInTheDocument();
    });

    it("đóng hộp thoại bằng nút Huỷ và trả focus về nút đã mở", async () => {
        renderCard();
        const { dialog, restartButton } = await openRestartDialog();

        fireEvent.click(dialog.getByRole("button", { name: "Huỷ" }));

        await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
        await waitFor(() => expect(restartButton).toHaveFocus());
    });

    it("đóng hộp thoại bằng phím Esc", async () => {
        renderCard();
        await openRestartDialog();

        fireEvent.keyDown(screen.getByRole("dialog"), { key: "Escape" });

        await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    });

    it("gửi lý do đã cắt khoảng trắng rồi báo đang khởi động lại", async () => {
        let submittedBody: unknown = null;
        server.use(
            http.post("*/api/admin/judge/workers/:name/restart", async ({ request }) => {
                submittedBody = await request.clone().json();
                return undefined;
            }),
        );
        renderCard();
        const { dialog } = await openRestartDialog();

        typeReason(dialog, "  Worker treo sau khi cập nhật  ");
        fireEvent.click(dialog.getByRole("button", { name: "Khởi động lại" }));

        expect(await screen.findByText(/Đang khởi động lại/)).toHaveTextContent(
            "Đang khởi động lại judge-my-02",
        );
        expect(submittedBody).toEqual({ reason: "Worker treo sau khi cập nhật" });
        expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
        expect(
            screen.queryByRole("button", { name: "Khởi động lại judge-my-02" }),
        ).not.toBeInTheDocument();
        expect(
            within(screen.getByRole("row", { name: /^judge-my-02/ })).getByText("Khởi động lại"),
        ).toBeInTheDocument();
    });

    it("báo lỗi bằng toast và giữ nguyên cảnh báo khi không khởi động lại được", async () => {
        server.use(
            http.post("*/api/admin/judge/workers/:name/restart", () =>
                HttpResponse.json(
                    { status: 500, code: "INTERNAL_ERROR", message: "Đã có lỗi xảy ra" },
                    { status: 500 },
                ),
            ),
        );
        renderCard();
        const { dialog } = await openRestartDialog();

        typeReason(dialog, "Worker treo");
        fireEvent.click(dialog.getByRole("button", { name: "Khởi động lại" }));

        expect(
            await screen.findByText("Chưa khởi động lại được judge-my-02. Đã có lỗi xảy ra"),
        ).toBeInTheDocument();
        expect(screen.getByRole("alert", { hidden: true })).toHaveTextContent(
            "judge-my-02 mất kết nối",
        );
        expect(screen.getByRole("dialog")).toBeInTheDocument();
        expect(screen.queryByText(/Đang khởi động lại/)).not.toBeInTheDocument();
    });

    it("không gửi khi lý do dài quá giới hạn", async () => {
        let requestCount = 0;
        server.use(
            http.post("*/api/admin/judge/workers/:name/restart", () => {
                requestCount += 1;
                return undefined;
            }),
        );
        renderCard();
        const { dialog } = await openRestartDialog();

        typeReason(dialog, "a".repeat(MAX_REASON_LENGTH + 1));
        fireEvent.click(dialog.getByRole("button", { name: "Khởi động lại" }));

        expect(await dialog.findByText("Lý do tối đa 500 ký tự")).toBeInTheDocument();
        expect(requestCount).toBe(0);
    });

    it("gắn lỗi của máy chủ vào ô lý do", async () => {
        server.use(
            http.post("*/api/admin/judge/workers/:name/restart", () =>
                HttpResponse.json(
                    {
                        status: 400,
                        code: "VALIDATION_FAILED",
                        message: "Dữ liệu gửi lên không hợp lệ",
                        errors: [{ field: "reason", message: "Lý do chứa ký tự không hợp lệ" }],
                    },
                    { status: 400 },
                ),
            ),
        );
        renderCard();
        const { dialog } = await openRestartDialog();

        typeReason(dialog, "Worker treo");
        fireEvent.click(dialog.getByRole("button", { name: "Khởi động lại" }));

        expect(await dialog.findByText("Lý do chứa ký tự không hợp lệ")).toBeInTheDocument();
        expect(dialog.getByLabelText("Lý do (ghi vào nhật ký thao tác)")).toBeInvalid();
    });
});
