import { fireEvent, screen, waitFor } from "@testing-library/react";
import { delay, http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "@/mocks/server";
import { renderWithProviders } from "@/test/render";

import { OAuthButtons } from "./OAuthButtons";

const push = vi.fn();

vi.mock("next/navigation", () => ({
    useRouter: () => ({ push }),
}));

describe("OAuthButtons", () => {
    beforeEach(() => {
        push.mockClear();
    });

    it("hai nút đều bấm được", () => {
        renderWithProviders(<OAuthButtons nextPath="/" />);

        expect(screen.getByRole("button", { name: "Tiếp tục với GitHub" })).toBeEnabled();
        expect(screen.getByRole("button", { name: "Tiếp tục với Google" })).toBeEnabled();
    });

    it.each([
        ["GitHub", "github"],
        ["Google", "google"],
    ])("bấm %s thì gọi đúng nhà cung cấp và chuyển trang", async (label, provider) => {
        const requestedProviders: unknown[] = [];
        server.use(
            http.post("*/api/auth/oauth/:provider", ({ params }) => {
                requestedProviders.push(params.provider);
                return HttpResponse.json({ accessToken: "t", user: {} });
            }),
        );
        renderWithProviders(<OAuthButtons nextPath="/problems" />);

        fireEvent.click(screen.getByRole("button", { name: `Tiếp tục với ${label}` }));

        await waitFor(() => expect(push).toHaveBeenCalledWith("/problems"));
        expect(requestedProviders).toEqual([provider]);
    });

    it("khoá cả hai nút trong lúc đang kết nối", async () => {
        server.use(
            http.post("*/api/auth/oauth/:provider", async () => {
                await delay(200);
                return HttpResponse.json({ accessToken: "t", user: {} });
            }),
        );
        renderWithProviders(<OAuthButtons nextPath="/" />);

        fireEvent.click(screen.getByRole("button", { name: "Tiếp tục với GitHub" }));

        expect(
            await screen.findByRole("button", { name: "Đang kết nối với GitHub…" }),
        ).toBeDisabled();
        expect(screen.getByRole("button", { name: "Tiếp tục với Google" })).toBeDisabled();
        await waitFor(() => expect(push).toHaveBeenCalled());
    });

    it("báo lỗi và cho bấm lại khi kết nối thất bại", async () => {
        server.use(http.post("*/api/auth/oauth/:provider", () => HttpResponse.error()));
        renderWithProviders(<OAuthButtons nextPath="/" />);

        fireEvent.click(screen.getByRole("button", { name: "Tiếp tục với Google" }));

        expect(await screen.findByRole("alert")).toHaveTextContent("Không kết nối được máy chủ");
        expect(screen.getByRole("button", { name: "Tiếp tục với Google" })).toBeEnabled();
        expect(push).not.toHaveBeenCalled();
    });
});
