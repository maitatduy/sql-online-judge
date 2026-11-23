import { fireEvent, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { resetCommunity } from "@/mocks/community";
import { DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { PostDetailView } from "./PostDetailView";

vi.mock("next/navigation", () => ({
    usePathname: () => "/community/post",
}));

const POST_ID = "0192f3a0-0000-7000-9000-000000000002";

function renderPost(postId = POST_ID) {
    renderWithProviders(
        <ToastProvider>
            <PostDetailView postId={postId} />
        </ToastProvider>,
    );
}

describe("chi tiết bài viết", () => {
    beforeEach(() => resetCommunity());

    it("hiện tiêu đề, nội dung và mời đăng nhập khi chưa đăng nhập", async () => {
        renderPost();

        expect(
            await screen.findByRole("heading", {
                level: 1,
                name: "LEFT JOIN trả về NULL ở cột đếm",
            }),
        ).toBeVisible();
        expect(screen.getByText(/phòng trống vẫn ra 1/)).toBeVisible();
        expect(await screen.findByRole("link", { name: "Đăng nhập" })).toHaveAttribute(
            "href",
            "/login?next=%2Fcommunity%2Fpost",
        );
    });

    it("vote, lưu bài và bình luận khi đã đăng nhập", async () => {
        writeMockSession(DEMO_SESSION_USER);
        renderPost();

        await screen.findByRole("button", { name: "Báo cáo bài viết" });
        const upvote = screen.getByRole("button", { name: "Vote lên" });
        fireEvent.click(upvote);
        await waitFor(() => expect(upvote).toHaveAttribute("aria-pressed", "true"));

        fireEvent.click(screen.getByRole("button", { name: "Lưu bài" }));
        expect(await screen.findByRole("button", { name: "Bỏ lưu bài" })).toBeVisible();

        fireEvent.change(await screen.findByLabelText("Viết bình luận… hỗ trợ Markdown"), {
            target: { value: "Dùng COUNT(cột) thay vì COUNT(*)" },
        });
        fireEvent.click(screen.getByRole("button", { name: "Gửi" }));

        expect(await screen.findByText("Dùng COUNT(cột) thay vì COUNT(*)")).toBeVisible();
        expect(await screen.findByRole("heading", { name: "1 bình luận" })).toBeVisible();
    });

    it("bài không tồn tại thì báo không tìm thấy", async () => {
        renderPost("missing");

        expect(await screen.findByText("Không tìm thấy bài viết")).toBeVisible();
    });
});
