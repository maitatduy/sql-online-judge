import { fireEvent, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { resetCommunity } from "@/mocks/community";
import { DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { PostBody } from "./PostBody";
import { PostEditor } from "./PostEditor";

const pushRoute = vi.fn();

vi.mock("next/navigation", () => ({
    useRouter: () => ({ push: pushRoute }),
}));

const PROBLEM = {
    id: "0192f3b0-0000-7000-8000-000000000008",
    slug: "count-orders-by-status",
    title: "Đếm đơn hàng theo trạng thái",
};

function renderEditor() {
    renderWithProviders(
        <ToastProvider>
            <PostEditor initialProblem={PROBLEM} />
        </ToastProvider>,
    );
}

describe("trình soạn bài viết", () => {
    beforeEach(() => {
        resetCommunity();
        pushRoute.mockReset();
        writeMockSession(DEMO_SESSION_USER);
    });

    it("đăng bài kèm bài tập đã gắn rồi chuyển tới bài vừa đăng", async () => {
        renderEditor();

        expect(screen.getByRole("radio", { name: "Chia sẻ lời giải" })).toBeChecked();
        expect(screen.getByText(PROBLEM.title)).toBeVisible();
        fireEvent.change(screen.getByLabelText("Tiêu đề"), {
            target: { value: "Đếm theo trạng thái bằng GROUP BY" },
        });
        fireEvent.change(screen.getByLabelText("Tag"), { target: { value: "Group-By, join" } });
        fireEvent.change(screen.getByLabelText("Nội dung"), {
            target: { value: "Dùng `GROUP BY status`." },
        });
        fireEvent.click(screen.getByRole("button", { name: "Đăng bài" }));

        await waitFor(() => expect(pushRoute).toHaveBeenCalledTimes(1));
        expect(pushRoute.mock.calls[0][0]).toMatch(/^\/community\/[0-9a-f-]+$/);
        expect(await screen.findByText("Đã đăng bài.")).toBeVisible();
    });

    it("báo lỗi ngay khi tiêu đề quá ngắn và nội dung trống", async () => {
        renderEditor();

        fireEvent.change(screen.getByLabelText("Tiêu đề"), { target: { value: "SQL" } });
        fireEvent.click(screen.getByRole("button", { name: "Lưu nháp" }));

        expect(await screen.findByText("Từ 5 đến 200 ký tự")).toBeVisible();
        expect(screen.getByText("Không được để trống")).toBeVisible();
        expect(pushRoute).not.toHaveBeenCalled();
    });

    it("khối sql run chạy trên dữ liệu mẫu của bài và hiện bảng kết quả", async () => {
        renderWithProviders(
            <PostBody
                markdown={
                    "Thử nhé:\n\n```sql run\nSELECT status, COUNT(*) FROM orders GROUP BY status;\n```"
                }
                problemSlug={PROBLEM.slug}
            />,
        );

        fireEvent.click(await screen.findByRole("button", { name: "Chạy" }));

        expect(await screen.findByRole("table")).toBeInTheDocument();
    });
});
