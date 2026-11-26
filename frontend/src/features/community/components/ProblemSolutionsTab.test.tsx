import { fireEvent, screen, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { resetCommunity } from "@/mocks/community";
import { server } from "@/mocks/server";
import { DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { ProblemDiscussionTab } from "./ProblemDiscussionTab";
import { ProblemSolutionsTab } from "./ProblemSolutionsTab";

vi.mock("next/navigation", () => ({
    usePathname: () => "/problems/count-orders-by-status",
}));

const SLUG = "count-orders-by-status";

describe("tab lời giải và thảo luận của bài tập", () => {
    beforeEach(() => resetCommunity());

    it("khách thấy lời giải chính thức bị khoá và lời mời chia sẻ", async () => {
        renderWithProviders(<ProblemSolutionsTab problemSlug={SLUG} />);

        expect(await screen.findByText("AC bài này để mở lời giải chính thức.")).toBeVisible();
        expect(await screen.findByText("Chưa có ai chia sẻ lời giải")).toBeVisible();
        expect(screen.getByRole("link", { name: "Chia sẻ lời giải" })).toHaveAttribute(
            "href",
            `/community/new?problem=${SLUG}`,
        );
    });

    it("lời giải từ cộng đồng hiện số tim và số bình luận", async () => {
        server.use(
            http.get("*/api/community/posts", () =>
                HttpResponse.json({
                    items: [
                        {
                            id: "0192f3a0-0000-7000-9000-0000000000f1",
                            category: "SOLUTION",
                            title: "Dùng LEFT JOIN với COUNT",
                            excerpt: "",
                            tags: [],
                            problemId: null,
                            authorId: "0192f3a0-0000-7000-9000-0000000000f2",
                            authorUsername: "an.bui",
                            voteScore: 12,
                            commentCount: 3,
                            isPinned: false,
                            publishedAt: "2026-11-20T08:00:00Z",
                        },
                    ],
                    page: 1,
                    size: 20,
                    totalItems: 1,
                    totalPages: 1,
                }),
            ),
        );
        renderWithProviders(<ProblemSolutionsTab problemSlug={SLUG} />);

        const list = within(await screen.findByRole("list", { name: "Lời giải từ cộng đồng" }));
        expect(list.getByText("12 tim")).toBeVisible();
        expect(list.getByText("3 bình luận")).toBeVisible();
    });

    it("người được xem thấy lời giải chính thức kèm khối SQL chạy được", async () => {
        writeMockSession(DEMO_SESSION_USER);
        renderWithProviders(<ProblemSolutionsTab problemSlug={SLUG} />);

        expect(await screen.findByRole("heading", { name: "Lời giải chính thức" })).toBeVisible();
        expect(await screen.findByRole("button", { name: "Chạy" })).toBeVisible();
    });

    it("bình luận trong tab thảo luận gắn với bài tập", async () => {
        writeMockSession(DEMO_SESSION_USER);
        renderWithProviders(
            <ToastProvider>
                <ProblemDiscussionTab problemSlug={SLUG} />
            </ToastProvider>,
        );

        fireEvent.change(await screen.findByLabelText(/Hỏi hoặc gợi ý về bài này/), {
            target: { value: "Có cần đếm cả trạng thái không có đơn không?" },
        });
        fireEvent.click(screen.getByRole("button", { name: "Gửi" }));

        expect(
            await screen.findByText("Có cần đếm cả trạng thái không có đơn không?"),
        ).toBeVisible();
    });
});
