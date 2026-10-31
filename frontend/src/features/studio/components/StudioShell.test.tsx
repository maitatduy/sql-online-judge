import { screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import {
    ADMIN_SESSION_USER,
    DEMO_SESSION_USER,
    SETTER_SESSION_USER,
    writeMockSession,
} from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { StudioShell } from "./StudioShell";

const replaceRoute = vi.fn();

vi.mock("next/navigation", () => ({
    usePathname: () => "/studio/problems",
    useRouter: () => ({ replace: replaceRoute }),
}));

function renderShell() {
    return renderWithProviders(
        <StudioShell>
            <p>Nội dung Studio</p>
        </StudioShell>,
    );
}

describe("StudioShell", () => {
    beforeEach(() => {
        replaceRoute.mockClear();
    });

    it("chuyển người chưa đăng nhập tới trang đăng nhập kèm đường quay lại", async () => {
        renderShell();

        await waitFor(() =>
            expect(replaceRoute).toHaveBeenCalledWith("/login?next=%2Fstudio%2Fproblems"),
        );
        expect(screen.queryByText("Nội dung Studio")).not.toBeInTheDocument();
    });

    it("chặn học viên bằng trang không có quyền, cần vai trò SETTER", async () => {
        writeMockSession(DEMO_SESSION_USER);
        renderShell();

        expect(
            await screen.findByRole("heading", { name: "Bạn không có quyền xem trang này" }),
        ).toBeInTheDocument();
        expect(screen.getByText("0 dòng. Cần vai trò SETTER.")).toBeInTheDocument();
        expect(screen.queryByText("Nội dung Studio")).not.toBeInTheDocument();
    });

    it.each([
        ["người ra đề", SETTER_SESSION_USER],
        ["quản trị viên", ADMIN_SESSION_USER],
    ])("cho %s vào Studio, kèm thanh trên cùng", async (_, user) => {
        writeMockSession(user);
        renderShell();

        expect(await screen.findByText("Nội dung Studio")).toBeInTheDocument();
        expect(screen.getByRole("banner")).toBeInTheDocument();
    });
});
