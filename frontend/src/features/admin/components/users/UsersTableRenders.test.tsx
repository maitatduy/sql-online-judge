import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";
import { visit } from "@/test/url-search-params";

import { AdminUsers } from "./AdminUsers";

const avatarRenders = vi.hoisted(() => new Map<string, number>());

vi.mock("next/navigation", async () => ({
    useSearchParams: (await import("@/test/url-search-params")).useUrlSearchParams,
}));

vi.mock("./UserAvatar", async (importOriginal) => {
    const original = await importOriginal<typeof import("./UserAvatar")>();
    return {
        UserAvatar: (props: Parameters<typeof original.UserAvatar>[0]) => {
            avatarRenders.set(props.username, (avatarRenders.get(props.username) ?? 0) + 1);
            return original.UserAvatar(props);
        },
    };
});

async function renderUsers() {
    visit("/admin/users");
    writeMockSession(ADMIN_SESSION_USER);
    renderWithProviders(
        <ToastProvider>
            <AdminUsers />
        </ToastProvider>,
    );
    const row = await screen.findByRole("row", { name: "tam.nguyen" });
    await waitFor(() => expect(within(row).getByRole("checkbox")).toBeEnabled());
    return row;
}

describe("bảng người dùng không render lại dòng không đổi", () => {
    beforeEach(() => {
        avatarRenders.clear();
    });

    it("chọn một dòng chỉ render lại đúng dòng đó", async () => {
        const row = await renderUsers();
        const rendersBefore = new Map(avatarRenders);

        fireEvent.click(within(row).getByRole("checkbox", { name: "Chọn tam.nguyen" }));

        await screen.findByRole("region", { name: "Thao tác hàng loạt" });
        const rerenderedUsernames = [...avatarRenders].filter(
            ([username, count]) => count > (rendersBefore.get(username) ?? 0),
        );
        expect(rerenderedUsernames.map(([username]) => username)).toEqual(["tam.nguyen"]);
    });
});
