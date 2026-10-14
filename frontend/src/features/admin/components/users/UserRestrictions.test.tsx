import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { http } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { apiClient } from "@/lib/api-client";
import { server } from "@/mocks/server";
import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";
import { visit } from "@/test/url-search-params";

import { AdminUserPage } from "./AdminUserPage";

vi.mock("next/navigation", async () => ({
    useSearchParams: (await import("@/test/url-search-params")).useUrlSearchParams,
}));

const SAVE = "Lưu thay đổi";

async function renderCard(username: string) {
    visit(`/admin/users/${username}`);
    writeMockSession(ADMIN_SESSION_USER);
    renderWithProviders(
        <ToastProvider>
            <AdminUserPage username={username} />
        </ToastProvider>,
    );
    const card = within(await screen.findByRole("region", { name: "Quyền và trạng thái" }));
    await card.findByRole("spinbutton", { name: "Hạn mức AI mỗi ngày" });
    return card;
}

const quota = (card: ReturnType<typeof within>) =>
    card.getByRole("spinbutton", { name: "Hạn mức AI mỗi ngày" });
const ratedBan = (card: ReturnType<typeof within>) =>
    card.getByRole("checkbox", { name: "Cấm tham gia contest có xếp hạng" });
const role = (card: ReturnType<typeof within>) => card.getByRole("combobox", { name: "Vai trò" });
const reason = (card: ReturnType<typeof within>) =>
    card.getByRole("textbox", { name: "Lý do thay đổi" });

function captureBodies() {
    const bodies: Record<string, unknown> = {};
    server.use(
        http.patch("*/api/admin/users/:username/restrictions", async ({ request }) => {
            bodies.restrictions = await request.clone().json();
            return undefined;
        }),
        http.patch("*/api/admin/users/:username", async ({ request }) => {
            bodies.user = await request.clone().json();
            return undefined;
        }),
    );
    return bodies;
}

describe("khối Quyền và trạng thái", () => {
    beforeEach(() => {
        visit("/admin/users");
    });

    it("điền sẵn giá trị hiện tại và chưa cho lưu khi chưa đổi gì", async () => {
        const card = await renderCard("viet.luu");

        expect(role(card)).toHaveValue("LEARNER");
        expect(quota(card)).toHaveValue(20);
        expect(ratedBan(card)).not.toBeChecked();
        expect(card.queryByRole("textbox", { name: "Lý do thay đổi" })).not.toBeInTheDocument();
        expect(card.getByRole("button", { name: SAVE })).toBeDisabled();
    });

    it("bắt buộc lý do rồi lưu hạn mức AI mới", async () => {
        const bodies = captureBodies();
        const card = await renderCard("viet.luu");

        fireEvent.change(quota(card), { target: { value: "5" } });

        expect(card.getByRole("button", { name: SAVE })).toBeDisabled();

        fireEvent.change(reason(card), { target: { value: "  Hỏi AI quá nhiều  " } });
        fireEvent.click(card.getByRole("button", { name: SAVE }));

        expect(
            await screen.findByText("Đã lưu quyền và trạng thái của viet.luu."),
        ).toBeInTheDocument();
        expect(bodies.restrictions).toEqual({ aiDailyQuota: 5, reason: "Hỏi AI quá nhiều" });
        expect(bodies.user).toBeUndefined();
        await waitFor(() => expect(card.getByRole("button", { name: SAVE })).toBeDisabled());
        expect(quota(card)).toHaveValue(5);
    });

    it("lưu cùng lúc vai trò và lệnh cấm thi với một lý do", async () => {
        const bodies = captureBodies();
        const card = await renderCard("viet.luu");

        fireEvent.change(role(card), { target: { value: "SETTER" } });
        fireEvent.click(ratedBan(card));
        fireEvent.change(reason(card), { target: { value: "Ra đề, không thi xếp hạng" } });
        fireEvent.click(card.getByRole("button", { name: SAVE }));

        await screen.findByText("Đã lưu quyền và trạng thái của viet.luu.");
        expect(bodies.user).toEqual({ role: "SETTER", reason: "Ra đề, không thi xếp hạng" });
        expect(bodies.restrictions).toEqual({
            isBannedFromRatedContests: true,
            reason: "Ra đề, không thi xếp hạng",
        });
    });

    it("hỏi xác nhận riêng trước khi cấp quyền Quản trị", async () => {
        const bodies = captureBodies();
        const card = await renderCard("viet.luu");

        fireEvent.change(role(card), { target: { value: "ADMIN" } });
        fireEvent.change(reason(card), { target: { value: "Bổ sung quản trị viên" } });
        fireEvent.click(card.getByRole("button", { name: SAVE }));

        const confirm = within(
            await screen.findByRole("dialog", { name: "Cấp quyền Quản trị cho viet.luu?" }),
        );

        expect(bodies.user).toBeUndefined();

        fireEvent.click(confirm.getByRole("button", { name: "Cấp quyền Quản trị" }));

        await screen.findByText("Đã lưu quyền và trạng thái của viet.luu.");
        expect(bodies.user).toEqual({ role: "ADMIN", reason: "Bổ sung quản trị viên" });
    });

    it("báo lỗi khi hạn mức AI ngoài khoảng cho phép", async () => {
        const card = await renderCard("viet.luu");

        fireEvent.change(quota(card), { target: { value: "300" } });
        fireEvent.change(reason(card), { target: { value: "Thử" } });
        fireEvent.click(card.getByRole("button", { name: SAVE }));

        expect(await card.findByText("Hạn mức AI từ 0 đến 200 tin mỗi ngày")).toBeInTheDocument();
        expect(quota(card)).toBeInvalid();
    });

    it("khoá cả khối khi xem tài khoản của chính mình", async () => {
        const card = await renderCard("admin.huy");

        expect(
            card.getByText("Bạn không thể tự đổi quyền hoặc hạn chế của chính mình."),
        ).toBeInTheDocument();
        expect(role(card)).toBeDisabled();
        expect(quota(card)).toBeDisabled();
        expect(ratedBan(card)).toBeDisabled();
        expect(card.queryByRole("button", { name: SAVE })).not.toBeInTheDocument();
    });

    it("chỉ cho xem với tài khoản đã xoá", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        await apiClient.delete("/api/admin/users/viet.luu", { reason: "Tạo nhầm" });
        const card = await renderCard("viet.luu");

        expect(quota(card)).toBeDisabled();
        expect(card.queryByRole("button", { name: SAVE })).not.toBeInTheDocument();
    });
});
