import { fireEvent, screen, waitFor } from "@testing-library/react";
import { http } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { resetAi } from "@/mocks/ai";
import { apiError } from "@/mocks/api-error";
import { server } from "@/mocks/server";
import { DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { DiscussionSummary } from "./DiscussionSummary";
import { PostEditor } from "./PostEditor";

vi.mock("next/navigation", () => ({
    useRouter: () => ({ push: vi.fn() }),
}));

describe("công cụ AI trong cộng đồng", () => {
    beforeEach(() => {
        resetAi();
        writeMockSession(DEMO_SESSION_USER);
    });

    it("tóm tắt thảo luận hiện bản tóm tắt thay cho nút", async () => {
        renderWithProviders(<DiscussionSummary postId="0192f3a0-0000-7000-9000-000000000002" />);

        fireEvent.click(screen.getByRole("button", { name: "Tóm tắt thảo luận" }));

        expect(await screen.findByRole("heading", { name: "Tóm tắt thảo luận" })).toBeVisible();
        expect(screen.getByText(/dùng COUNT\(cột\) thay cho COUNT\(\*\)/)).toBeVisible();
    });

    it("đang thi contest thì báo AI bị khoá", async () => {
        server.use(
            http.post("*/api/ai/discussion-summaries", () =>
                apiError(403, "AI_DISABLED_DURING_CONTEST", "Khoá"),
            ),
        );
        renderWithProviders(<DiscussionSummary postId="0192f3a0-0000-7000-9000-000000000002" />);

        fireEvent.click(screen.getByRole("button", { name: "Tóm tắt thảo luận" }));

        expect(
            await screen.findByText("Trợ lý AI tạm khoá khi bạn đang thi contest tính rating."),
        ).toBeVisible();
    });

    it("AI sửa chính tả thay nội dung đang soạn và cho hoàn tác", async () => {
        renderWithProviders(
            <ToastProvider>
                <PostEditor />
            </ToastProvider>,
        );
        const body = screen.getByLabelText("Nội dung");
        fireEvent.change(body, { target: { value: "Minh khong hieu JOIN" } });

        fireEvent.click(screen.getByRole("button", { name: "AI sửa chính tả" }));

        await waitFor(() => expect(body).toHaveValue("Minh không hieu JOIN"));
        fireEvent.click(screen.getByRole("button", { name: "Hoàn tác sửa" }));
        expect(body).toHaveValue("Minh khong hieu JOIN");
    });
});
