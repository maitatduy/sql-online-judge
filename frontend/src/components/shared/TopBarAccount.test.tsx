import { fireEvent, screen, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it, vi } from "vitest";

import { getAccessToken, setAccessToken } from "@/lib/auth-session";
import { server } from "@/mocks/server";
import { DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { TopBarAccount } from "./TopBarAccount";

const push = vi.fn();

vi.mock("next/navigation", () => ({
    usePathname: () => "/u/minh.anh",
    useRouter: () => ({ push }),
}));

describe("TopBarAccount", () => {
    it("hiện avatar dẫn tới hồ sơ của người đang đăng nhập", async () => {
        writeMockSession(DEMO_SESSION_USER);
        renderWithProviders(<TopBarAccount />);

        const profileLink = await screen.findByRole("link", { name: "Hồ sơ của tôi" });

        expect(profileLink).toHaveAttribute("href", "/u/minh.anh");
        expect(profileLink).toHaveTextContent("MA");
        expect(screen.queryByRole("link", { name: "Đăng nhập" })).not.toBeInTheDocument();
    });

    it("hiện chuỗi ngày học của người đang đăng nhập", async () => {
        writeMockSession(DEMO_SESSION_USER);
        renderWithProviders(<TopBarAccount />);

        expect(await screen.findByTitle("Chuỗi ngày học")).toHaveTextContent(
            "12 ngày học liên tiếp",
        );
    });

    it("hiện nút đăng nhập quay lại đúng trang đang xem khi chưa đăng nhập", async () => {
        renderWithProviders(<TopBarAccount />);

        expect(await screen.findByRole("link", { name: "Đăng nhập" })).toHaveAttribute(
            "href",
            "/login?next=%2Fu%2Fminh.anh",
        );
        expect(screen.queryByRole("link", { name: "Hồ sơ của tôi" })).not.toBeInTheDocument();
    });

    it("không mời đăng nhập khi máy chủ lỗi", async () => {
        let requestCount = 0;
        server.use(
            http.get("*/api/auth/me", () => {
                requestCount += 1;
                return HttpResponse.json(
                    { status: 500, code: "INTERNAL_ERROR", message: "Đã có lỗi xảy ra" },
                    { status: 500 },
                );
            }),
        );
        const { container } = renderWithProviders(<TopBarAccount />);

        await waitFor(() => expect(requestCount).toBe(1));
        await waitFor(() => expect(container).toBeEmptyDOMElement());
    });

    it("đăng xuất thì thu hồi phiên, xoá token và về trang chủ", async () => {
        let hasLoggedOut = false;
        server.use(
            http.post("*/api/auth/logout", () => {
                hasLoggedOut = true;
                return new HttpResponse(null, { status: 204 });
            }),
        );
        writeMockSession(DEMO_SESSION_USER);
        setAccessToken("access-token");
        renderWithProviders(<TopBarAccount />);

        fireEvent.click(await screen.findByRole("button", { name: "Đăng xuất" }));

        await waitFor(() => expect(push).toHaveBeenCalledWith("/"));
        expect(hasLoggedOut).toBe(true);
        expect(getAccessToken()).toBeNull();
    });

    it("chưa hiện nút nào khi đang tải", () => {
        renderWithProviders(<TopBarAccount />);

        expect(screen.queryByRole("link")).not.toBeInTheDocument();
    });
});
