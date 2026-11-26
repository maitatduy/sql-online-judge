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
        expect(screen.queryByRole("button", { name: /Vote/ })).not.toBeInTheDocument();
        const like = screen.getByRole("button", { name: /^Thả tim bài viết, [0-9]+ tim$/ });
        expect(like).toHaveAttribute("aria-pressed", "false");
        fireEvent.click(like);
        await waitFor(() => expect(like).toHaveAttribute("aria-pressed", "true"));
        expect(like).toHaveClass("text-like");

        fireEvent.click(screen.getByRole("button", { name: "Lưu bài" }));
        expect(await screen.findByRole("button", { name: "Bỏ lưu bài" })).toBeVisible();

        fireEvent.change(await screen.findByLabelText("Viết bình luận… hỗ trợ Markdown"), {
            target: { value: "Dùng COUNT(cột) thay vì COUNT(*)" },
        });
        fireEvent.click(screen.getByRole("button", { name: "Gửi" }));

        expect(await screen.findByText("Dùng COUNT(cột) thay vì COUNT(*)")).toBeVisible();
        expect(await screen.findByRole("heading", { name: "1 bình luận" })).toBeVisible();

        const heart = screen.getByRole("button", { name: "Thả tim bình luận, 0 tim" });
        expect(heart).toHaveAttribute("aria-pressed", "false");
        fireEvent.click(heart);
        const liked = await screen.findByRole("button", { name: "Thả tim bình luận, 1 tim" });
        expect(liked).toHaveAttribute("aria-pressed", "true");
        expect(liked).toHaveClass("text-like");
    });

    it("trả lời một câu trả lời thì gắn @tên và nằm cùng nhóm dưới bình luận gốc", async () => {
        writeMockSession(DEMO_SESSION_USER);
        renderPost();
        fireEvent.change(await screen.findByLabelText("Viết bình luận… hỗ trợ Markdown"), {
            target: { value: "Gốc" },
        });
        fireEvent.click(screen.getByRole("button", { name: "Gửi" }));
        fireEvent.click(await screen.findByRole("button", { name: "Trả lời" }));
        fireEvent.change(screen.getByLabelText("Trả lời minh.anh"), {
            target: { value: "Cấp hai" },
        });
        fireEvent.click(screen.getAllByRole("button", { name: "Gửi" })[1]);
        await screen.findByText("Cấp hai", { selector: "p" });

        fireEvent.click(screen.getAllByRole("button", { name: "Trả lời" })[1]);
        const replyField = screen.getByLabelText("Trả lời minh.anh");
        expect(replyField).toHaveValue("@minh.anh ");
        fireEvent.change(replyField, { target: { value: "@minh.anh cấp ba" } });
        fireEvent.click(screen.getAllByRole("button", { name: "Gửi" })[1]);

        const nested = await screen.findByText("@minh.anh cấp ba", { selector: "p" });
        const replies = screen.getByText("Cấp hai", { selector: "p" }).closest("ul");
        expect(replies).toContainElement(nested);
        expect(replies?.querySelectorAll("ul")).toHaveLength(0);
    });

    it("bài không tồn tại thì báo không tìm thấy", async () => {
        renderPost("missing");

        expect(await screen.findByText("Không tìm thấy bài viết")).toBeVisible();
    });
});
