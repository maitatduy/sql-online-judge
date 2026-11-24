import { fireEvent, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { resetCommunity } from "@/mocks/community";
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
