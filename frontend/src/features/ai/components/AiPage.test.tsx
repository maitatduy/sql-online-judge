import { fireEvent, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { resetAi } from "@/mocks/ai";
import { DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";
import { visit } from "@/test/url-search-params";

import { AiPage } from "./AiPage";

vi.mock("next/navigation", async () => ({
    useSearchParams: (await import("@/test/url-search-params")).useUrlSearchParams,
    usePathname: () => "/ai",
}));

describe("trang trợ lý AI", () => {
    beforeEach(() => {
        resetAi();
        visit("/ai");
    });

    it("hỏi câu đầu tiên thì tạo hội thoại mới, đặt tên theo câu hỏi và ghi vào URL", async () => {
        writeMockSession(DEMO_SESSION_USER);
        renderWithProviders(<AiPage />);

        fireEvent.click(
            await screen.findByRole("button", { name: "Khi nào dùng window function?" }),
        );

        expect(await screen.findByText(/hãy gom các dòng theo cột cần nhóm/)).toBeVisible();
        const history = screen.getByRole("navigation", { name: "Hội thoại" });
        expect(
            await within(history).findByRole("button", { name: /Khi nào dùng window function\?/ }),
        ).toBeVisible();
        expect(window.location.search).toMatch(/^\?c=/);
    });

    it("khách được mời đăng nhập", async () => {
        renderWithProviders(<AiPage />);

        expect(await screen.findByText("Đăng nhập để hỏi trợ lý AI")).toBeVisible();
    });
});
