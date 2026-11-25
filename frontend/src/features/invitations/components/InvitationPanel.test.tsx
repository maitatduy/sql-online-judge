import { fireEvent, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";

import { ADMIN_SESSION_USER, clearMockSession, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { InvitationPanel } from "./InvitationPanel";

const INVITEE = { ...ADMIN_SESSION_USER, username: "viet.luu", role: "LEARNER" as const };
const STRANGER = { ...ADMIN_SESSION_USER, username: "nguoi.la", role: "LEARNER" as const };

function renderPanel(token: string) {
    renderWithProviders(<InvitationPanel token={token} />);
}

describe("trang nhận lời mời", () => {
    beforeEach(() => {
        clearMockSession();
    });

    it("chưa đăng nhập: hiện ai mời, vai trò, email đã che và lối vào đăng nhập hoặc đăng ký", async () => {
        renderPanel("loi-moi-dang-cho");

        expect(await screen.findByText("admin.huy mời bạn làm Người ra đề")).toBeInTheDocument();
        expect(screen.getByText(/vi\*\*\*@example\.com/)).toBeInTheDocument();
        expect(
            await screen.findByRole("link", { name: "Đăng nhập để nhận quyền" }),
        ).toHaveAttribute("href", "/login?next=%2Finvitations%2Floi-moi-dang-cho");
        expect(screen.getByRole("link", { name: "Đăng ký tài khoản" })).toHaveAttribute(
            "href",
            "/register?next=%2Finvitations%2Floi-moi-dang-cho",
        );
        expect(screen.queryByRole("button", { name: "Nhận quyền" })).not.toBeInTheDocument();
    });

    it("đăng nhập đúng người: bấm Nhận quyền thì báo đã có vai trò mới", async () => {
        writeMockSession(INVITEE);
        renderPanel("loi-moi-dang-cho");

        fireEvent.click(await screen.findByRole("button", { name: "Nhận quyền" }));

        expect(await screen.findByText("Bạn đã là Người ra đề")).toBeInTheDocument();
        expect(screen.getByRole("link", { name: "Về trang chủ" })).toHaveAttribute("href", "/");
    });

    it("đăng nhập bằng tài khoản khác: báo lời mời gửi tới email khác", async () => {
        writeMockSession(STRANGER);
        renderPanel("loi-moi-dang-cho");

        fireEvent.click(await screen.findByRole("button", { name: "Nhận quyền" }));

        expect(
            await screen.findByText(
                "Lời mời này gửi tới vi***@example.com. Hãy đăng nhập đúng tài khoản đó.",
            ),
        ).toBeInTheDocument();
    });

    it.each([
        ["loi-moi-het-han", "Lời mời đã hết hạn"],
        ["loi-moi-da-huy", "Lời mời đã bị huỷ"],
        ["loi-moi-da-nhan", "Lời mời đã được nhận"],
        ["loi-moi-da-tu-choi", "Bạn đã từ chối lời mời"],
    ])("lời mời %s thì báo %s và không cho nhận", async (token, title) => {
        writeMockSession(INVITEE);
        renderPanel(token);

        expect(await screen.findByText(title)).toBeInTheDocument();
        expect(screen.queryByRole("button", { name: "Nhận quyền" })).not.toBeInTheDocument();
    });

    it("chưa đăng nhập vẫn từ chối được, có hỏi lại trước khi từ chối", async () => {
        renderPanel("loi-moi-de-tu-choi");

        fireEvent.click(await screen.findByRole("button", { name: "Từ chối lời mời" }));
        expect(await screen.findByRole("alertdialog")).toHaveTextContent("Người ra đề");
        fireEvent.click(screen.getByRole("button", { name: "Từ chối" }));

        expect(await screen.findByText("Bạn đã từ chối lời mời")).toBeInTheDocument();
        expect(screen.queryByRole("button", { name: "Từ chối lời mời" })).not.toBeInTheDocument();
    });

    it("link không tồn tại: báo không tìm thấy lời mời", async () => {
        renderPanel("khong-co");

        expect(await screen.findByText("Không tìm thấy lời mời")).toBeInTheDocument();
    });

    it("tiêu đề căn giữa và chỉ một câu thông tin ngắn", async () => {
        renderPanel("loi-moi-dang-cho");

        const heading = await screen.findByRole("heading", {
            level: 1,
            name: "admin.huy mời bạn làm Người ra đề",
        });

        expect(heading.parentElement).toHaveClass("text-center");
        expect(
            screen.getByText(/^Gửi tới vi\*\*\*@example\.com, hết hạn ngày/),
        ).toBeInTheDocument();
    });
});
