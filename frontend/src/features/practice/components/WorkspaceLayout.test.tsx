import { screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { renderWithProviders } from "@/test/render";

import { Workspace } from "./Workspace";

vi.mock("next/navigation", () => ({ notFound: vi.fn() }));

vi.mock("@/components/shared/LazySqlEditor", () => ({
    LazySqlEditor: ({ ariaLabel }: { ariaLabel: string }) => <textarea aria-label={ariaLabel} />,
}));

const originalMatchMedia = window.matchMedia;

function useDesktopScreen(isDesktop: boolean) {
    window.matchMedia = (query: string) => ({ ...originalMatchMedia(query), matches: isDesktop });
}

describe("Bố cục workspace", () => {
    beforeEach(() => {
        localStorage.clear();
    });

    afterEach(() => {
        window.matchMedia = originalMatchMedia;
    });

    it("màn rộng: kéo được thanh chia giữa đề bài, code và kết quả, không có tab khu vực", async () => {
        useDesktopScreen(true);
        renderWithProviders(<Workspace slug="highest-salary-per-department" />);

        await screen.findByRole("heading", { level: 1 });
        expect(
            screen.getByRole("separator", { name: "Kéo để đổi độ rộng khung đề bài" }),
        ).toBeInTheDocument();
        expect(
            screen.getByRole("separator", { name: "Kéo để đổi chiều cao khung kết quả" }),
        ).toBeInTheDocument();
        expect(screen.queryByRole("tab", { name: "Code" })).not.toBeInTheDocument();
    });

    it("menu đề bài gọn: mỗi mục có icon và tên đọc được, kể cả khi chỉ hiện icon", async () => {
        useDesktopScreen(true);
        renderWithProviders(<Workspace slug="highest-salary-per-department" />);

        expect(await screen.findByRole("tab", { name: "Đề bài" })).toHaveAttribute(
            "aria-selected",
            "true",
        );
        expect(screen.getByRole("tab", { name: "Schema" })).toHaveAttribute("title", "Schema");
        expect(screen.getByRole("tab", { name: "Lịch sử nộp" })).toBeInTheDocument();
    });

    it("màn hẹp: không có thanh kéo, chuyển khu vực bằng tab Đề, Code, Kết quả", async () => {
        useDesktopScreen(false);
        renderWithProviders(<Workspace slug="highest-salary-per-department" />);

        await screen.findByRole("heading", { level: 1 });
        expect(screen.queryByRole("separator")).not.toBeInTheDocument();
        expect(screen.getByRole("tab", { name: "Code" })).toBeInTheDocument();
    });
});
