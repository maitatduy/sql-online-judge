import { screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { renderWithProviders } from "@/test/render";

import InvitationPage from "./page";

describe("trang nhận lời mời", () => {
    it("có logo, lối về trang chủ và nội dung nằm trong vùng chính", async () => {
        renderWithProviders(
            await InvitationPage({ params: Promise.resolve({ token: "loi-moi-dang-cho" }) }),
        );

        expect(screen.getByRole("link", { name: "SQL Online Judge" })).toHaveAttribute("href", "/");
        expect(screen.getByRole("link", { name: "Về trang chủ" })).toHaveAttribute("href", "/");
        expect(screen.getByRole("main")).toHaveAttribute("id", "main-content");
        expect(
            await screen.findByRole("heading", {
                level: 1,
                name: "admin.huy mời bạn làm Người ra đề",
            }),
        ).toBeInTheDocument();
    });
});
