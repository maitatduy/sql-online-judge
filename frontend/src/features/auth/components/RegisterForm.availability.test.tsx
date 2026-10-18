import { fireEvent, screen, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it, vi } from "vitest";

import { server } from "@/mocks/server";
import { renderWithProviders } from "@/test/render";

import { RegisterForm } from "./RegisterForm";

vi.mock("next/navigation", () => ({
    useRouter: () => ({ push: vi.fn() }),
}));

const DEBOUNCE_SETTLE_MS = 700;

function typeUsername(username: string) {
    fireEvent.change(screen.getByLabelText("Tên người dùng"), { target: { value: username } });
}

function wait(durationMs: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, durationMs));
}

describe("RegisterForm - kiểm tra tên người dùng", () => {
    it("báo còn trống khi tên chưa có ai dùng", async () => {
        renderWithProviders(<RegisterForm />);

        typeUsername("minh.anh");

        expect(await screen.findByText("Còn trống", {}, { timeout: 3000 })).toBeInTheDocument();
    });

    it("báo đã có người dùng khi tên bị trùng", async () => {
        renderWithProviders(<RegisterForm />);

        typeUsername("taken");

        expect(
            await screen.findByText("Đã có người dùng", {}, { timeout: 3000 }),
        ).toBeInTheDocument();
    });

    it("hiện trạng thái đang kiểm tra ngay khi gõ tên hợp lệ", () => {
        renderWithProviders(<RegisterForm />);

        typeUsername("minh.anh");

        expect(screen.getByText("Đang kiểm tra…")).toBeInTheDocument();
    });

    it("không hỏi API khi tên chưa đúng định dạng", async () => {
        const onRequest = vi.fn();
        server.use(
            http.get("*/api/users/availability", () => {
                onRequest();
                return HttpResponse.json({ available: true });
            }),
        );
        renderWithProviders(<RegisterForm />);

        typeUsername("ab");
        await wait(DEBOUNCE_SETTLE_MS);

        expect(onRequest).not.toHaveBeenCalled();
        expect(screen.queryByText("Còn trống")).not.toBeInTheDocument();
        expect(screen.queryByText("Đang kiểm tra…")).not.toBeInTheDocument();
    });

    it("chỉ hỏi API một lần cho tên cuối cùng khi gõ liên tục", async () => {
        const requestedUsernames: (string | null)[] = [];
        server.use(
            http.get("*/api/users/availability", ({ request }) => {
                requestedUsernames.push(new URL(request.url).searchParams.get("username"));
                return HttpResponse.json({ available: true });
            }),
        );
        renderWithProviders(<RegisterForm />);

        typeUsername("min");
        typeUsername("minh");
        typeUsername("minh.anh");

        await screen.findByText("Còn trống", {}, { timeout: 3000 });
        expect(requestedUsernames).toEqual(["minh.anh"]);
    });

    it("không hiện gợi ý khi API kiểm tra tên bị lỗi", async () => {
        server.use(http.get("*/api/users/availability", () => HttpResponse.error()));
        renderWithProviders(<RegisterForm />);

        typeUsername("minh.anh");

        await waitFor(() => expect(screen.queryByText("Đang kiểm tra…")).not.toBeInTheDocument(), {
            timeout: 3000,
        });
        expect(screen.queryByText("Còn trống")).not.toBeInTheDocument();
        expect(screen.queryByText("Đã có người dùng")).not.toBeInTheDocument();
    });
});
