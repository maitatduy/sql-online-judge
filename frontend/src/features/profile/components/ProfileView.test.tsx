import { fireEvent, screen } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { notFound } from "next/navigation";
import { beforeAll, beforeEach, describe, expect, it, vi } from "vitest";

import { MINH_ANH_PROFILE } from "@/mocks/profile-fixtures";
import { server } from "@/mocks/server";
import { renderWithProviders } from "@/test/render";

import { ProfileView } from "./ProfileView";

vi.mock("next/navigation", () => ({
    notFound: vi.fn(),
    usePathname: () => "/u/minh.anh",
    useRouter: () => ({ replace: vi.fn() }),
    useSearchParams: () => new URLSearchParams(),
}));

async function preloadChartModule(): Promise<void> {
    await import("./RatingChart");
}

describe("ProfileView", () => {
    beforeAll(preloadChartModule);

    beforeEach(() => {
        vi.mocked(notFound).mockClear();
    });

    it("báo đang tải rồi hiện hồ sơ trong vùng nội dung chính", async () => {
        renderWithProviders(<ProfileView username="minh.anh" />);

        expect(screen.getByRole("main")).toHaveAttribute("id", "main-content");
        expect(screen.getByRole("status", { name: "Đang tải hồ sơ" })).toHaveAttribute(
            "aria-busy",
            "true",
        );

        expect(
            await screen.findByRole("heading", { level: 1, name: "Nguyễn Minh Anh" }),
        ).toBeInTheDocument();
        expect(screen.queryByLabelText("Đang tải hồ sơ")).not.toBeInTheDocument();
    });

    it("hiện thẻ số và tiến độ theo độ khó bên cạnh thẻ danh tính", async () => {
        renderWithProviders(<ProfileView username="minh.anh" />);

        expect(await screen.findByText("Bài đã AC")).toBeInTheDocument();
        expect(screen.getByRole("img", { name: "Đã giải 63 trên 330 bài" })).toBeInTheDocument();
    });

    it("chỉ có một h1 và các khối đều là h2, không nhảy cấp tiêu đề", async () => {
        renderWithProviders(<ProfileView username="minh.anh" />);

        await screen.findByRole("heading", { level: 2, name: "Rating contest" });
        await screen.findByRole("heading", { level: 2, name: /bài nộp trong năm qua$/ });

        expect(screen.getAllByRole("heading", { level: 1 })).toHaveLength(1);
        expect(screen.getAllByRole("heading").map((heading) => heading.tagName)).toEqual([
            "H1",
            "H2",
            "H2",
            "H2",
            "H2",
        ]);
    });

    it("chuyển sang trang không tìm thấy khi không có người dùng", async () => {
        renderWithProviders(<ProfileView username="khong-co" />);

        await vi.waitFor(() => expect(notFound).toHaveBeenCalled());
    });

    it("báo lỗi kèm mã lỗi khi máy chủ lỗi, không coi là không tìm thấy", async () => {
        renderWithProviders(<ProfileView username="error" />);

        const alert = await screen.findByRole("alert");

        expect(alert).toHaveTextContent("Không tải được hồ sơ");
        expect(alert).toHaveTextContent("Mã lỗi: 4bf92f3577b34da6");
        expect(notFound).not.toHaveBeenCalled();
    });

    it("thử lại thành công thì hiện hồ sơ", async () => {
        server.use(
            http.get(
                "*/api/users/:username",
                () =>
                    HttpResponse.json(
                        { status: 500, code: "INTERNAL_ERROR", message: "Đã có lỗi xảy ra" },
                        { status: 500 },
                    ),
                { once: true },
            ),
        );
        renderWithProviders(<ProfileView username={MINH_ANH_PROFILE.username} />);

        fireEvent.click(await screen.findByRole("button", { name: "Thử lại" }));

        expect(
            await screen.findByRole("heading", { level: 1, name: "Nguyễn Minh Anh" }),
        ).toBeInTheDocument();
    });
});
