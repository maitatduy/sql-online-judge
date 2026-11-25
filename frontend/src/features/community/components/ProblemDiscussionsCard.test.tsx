import { screen, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";

import { server } from "@/mocks/server";
import { renderWithProviders } from "@/test/render";

import { ProblemDiscussionsCard } from "./ProblemDiscussionsCard";

const DISCUSSIONS = "*/api/community/problem-discussions";

describe("ProblemDiscussionsCard", () => {
    it("liệt kê thảo luận mới kèm tên bài, dẫn tới tab Thảo luận của bài", async () => {
        server.use(
            http.get(DISCUSSIONS, () =>
                HttpResponse.json([
                    {
                        commentId: "c1",
                        problemId: "p1",
                        problemSlug: "tong-luong-moi-phong-ban",
                        problemTitle: "Tổng lương mỗi phòng ban",
                        authorId: "u1",
                        authorUsername: "maitatduy",
                        excerpt: "Testcase 2 có phòng ban không có nhân viên",
                        isReply: false,
                        createdAt: new Date().toISOString(),
                    },
                    {
                        commentId: "c2",
                        problemId: "p1",
                        problemSlug: "tong-luong-moi-phong-ban",
                        problemTitle: "Tổng lương mỗi phòng ban",
                        authorId: "u2",
                        authorUsername: "an.bui",
                        excerpt: "Dùng LEFT JOIN là được",
                        isReply: true,
                        createdAt: new Date().toISOString(),
                    },
                ]),
            ),
        );
        renderWithProviders(<ProblemDiscussionsCard />);

        const card = await screen.findByRole("region", { name: "Thảo luận mới ở các bài" });
        const links = await within(card).findAllByRole("link");

        expect(links).toHaveLength(2);
        expect(links[0]).toHaveAttribute(
            "href",
            "/problems/tong-luong-moi-phong-ban?tab=discussion",
        );
        expect(links[0]).toHaveTextContent("Tổng lương mỗi phòng ban");
        expect(links[0]).toHaveTextContent("Testcase 2 có phòng ban không có nhân viên");
        expect(within(card).getByText(/^an\.bui trả lời/)).toBeInTheDocument();
    });

    it("chưa có thảo luận thì báo trống", async () => {
        server.use(http.get(DISCUSSIONS, () => HttpResponse.json([])));
        renderWithProviders(<ProblemDiscussionsCard />);

        expect(await screen.findByText("Chưa có thảo luận nào ở các bài.")).toBeInTheDocument();
    });

    it("lỗi thì báo trong khối kèm nút thử lại", async () => {
        server.use(
            http.get(DISCUSSIONS, () =>
                HttpResponse.json(
                    { status: 503, code: "PROBLEM_SERVICE_UNAVAILABLE", message: "Lỗi" },
                    { status: 503 },
                ),
            ),
        );
        renderWithProviders(<ProblemDiscussionsCard />);

        const alert = await screen.findByRole("alert");
        expect(alert).toHaveTextContent("Chưa tải được thảo luận");
        expect(within(alert).getByRole("button", { name: "Thử lại" })).toBeInTheDocument();
    });
});
