import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { server } from "@/mocks/server";
import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";
import { visit } from "@/test/url-search-params";

import { AdminUsers } from "./AdminUsers";

vi.mock("next/navigation", async () => ({
    useSearchParams: (await import("@/test/url-search-params")).useUrlSearchParams,
}));

function renderUsers(search = "") {
    visit(search ? `/admin/users?${search}` : "/admin/users");
    writeMockSession(ADMIN_SESSION_USER);
    return renderWithProviders(
        <ToastProvider>
            <AdminUsers />
        </ToastProvider>,
    );
}

async function findUserRow(username: string) {
    return within(await screen.findByRole("row", { name: new RegExp(`^${username}`) }));
}

function serverError(traceId?: string) {
    return HttpResponse.json(
        { status: 500, code: "INTERNAL_ERROR", message: "Đã có lỗi xảy ra", traceId },
        { status: 500 },
    );
}

describe("AdminUsers", () => {
    beforeEach(() => {
        visit("/admin/users");
    });

    it("báo đang tải trước khi có danh sách", () => {
        renderUsers();

        expect(screen.getByRole("status", { name: "Đang tải danh sách người dùng" })).toBeVisible();
        expect(screen.getByRole("heading", { level: 1, name: "Người dùng" })).toBeInTheDocument();
    });

    it("hiện bảng với đủ các cột", async () => {
        renderUsers();
        await findUserRow("tam.nguyen");

        expect(screen.getAllByRole("columnheader").map((header) => header.textContent)).toEqual([
            "Người dùng",
            "Vai trò",
            "Trạng thái",
            "Hành động",
        ]);
        expect(screen.getAllByRole("rowheader")).toHaveLength(20);
    });

    it("hiện thông tin của một tài khoản chưa xác minh", async () => {
        renderUsers();

        const row = await findUserRow("tam.nguyen");

        expect(row.getByText("tam.nguyen@example.com")).toBeInTheDocument();
        expect(row.getByText("Học viên")).toBeInTheDocument();
        expect(row.getByText("Chưa xác minh")).toBeInTheDocument();
    });

    it("hiện vai trò và trạng thái bằng chữ", async () => {
        renderUsers();

        expect((await findUserRow("spam.bot88")).getByText("Bị khoá")).toBeInTheDocument();

        const setter = await findUserRow("datahust");

        expect(setter.getByText("Người ra đề")).toBeInTheDocument();
        expect(setter.getByText("Hoạt động")).toBeInTheDocument();
    });

    it("có nút Xem chi tiết ở cột hành động của mỗi dòng", async () => {
        renderUsers();

        const row = await findUserRow("tam.nguyen");

        expect(row.getByRole("button", { name: "Xem chi tiết tam.nguyen" })).toBeInTheDocument();
    });

    it("không mở gì khi bấm vào dòng", async () => {
        renderUsers();

        fireEvent.click((await findUserRow("datahust")).getByText("Người ra đề"));

        expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
        expect(window.location.search).toBe("");
    });

    it("đọc lại khoảng kết quả khi danh sách đổi", async () => {
        renderUsers();

        expect(await screen.findByText("1-20 trong 62")).toHaveAttribute("aria-live", "polite");
    });

    it("phân trang bằng liên kết giữ trang trên URL", async () => {
        renderUsers();
        await findUserRow("tam.nguyen");

        const pagination = within(screen.getByRole("navigation", { name: "Phân trang" }));

        expect(screen.getByText("1-20 trong 62")).toBeInTheDocument();
        expect(pagination.getByRole("link", { name: "Trang 1" })).toHaveAttribute(
            "aria-current",
            "page",
        );
        expect(pagination.getByRole("link", { name: "Trang 2" })).toHaveAttribute(
            "href",
            "/admin/users?page=2",
        );
        expect(pagination.getByRole("link", { name: "Sau" })).toHaveAttribute(
            "href",
            "/admin/users?page=2",
        );
        expect(pagination.getByRole("button", { name: "Trước" })).toBeDisabled();
    });

    it("mở đúng trang ghi trên URL", async () => {
        renderUsers("page=4");
        await findUserRow("quang.le");

        const pagination = within(screen.getByRole("navigation", { name: "Phân trang" }));

        expect(screen.getAllByRole("rowheader")).toHaveLength(2);
        expect(screen.getByText("61-62 trong 62")).toBeInTheDocument();
        expect(pagination.getByRole("link", { name: "Trước" })).toHaveAttribute(
            "href",
            "/admin/users?page=3",
        );
        expect(pagination.getByRole("button", { name: "Sau" })).toBeDisabled();
    });

    it("báo không tìm thấy khi trang không có tài khoản nào", async () => {
        renderUsers("page=9");

        expect(await screen.findByText("Không tìm thấy người dùng")).toBeInTheDocument();
        expect(screen.queryByRole("table")).not.toBeInTheDocument();
    });

    it("hiện lỗi kèm mã lỗi và tải lại được", async () => {
        server.use(
            http.get("*/api/admin/users", () => serverError("4bf92f3577b34da6"), { once: true }),
        );
        renderUsers();

        expect(await screen.findByRole("alert")).toHaveTextContent("4bf92f3577b34da6");

        fireEvent.click(screen.getByRole("button", { name: "Thử lại" }));

        expect((await findUserRow("tam.nguyen")).getByText("Học viên")).toBeInTheDocument();
    });

    it("giữ danh sách đang hiện và báo dữ liệu đã cũ khi làm mới nền bị lỗi", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
        render(
            <QueryClientProvider client={queryClient}>
                <ToastProvider>
                    <AdminUsers />
                </ToastProvider>
            </QueryClientProvider>,
        );
        await findUserRow("tam.nguyen");

        server.use(http.get("*/api/admin/users", () => serverError()));
        await queryClient.invalidateQueries({ queryKey: ["admin", "users"] });
        await waitFor(() => expect(queryClient.isFetching()).toBe(0));

        expect(await screen.findByText(/^Chưa cập nhật được số liệu mới\./)).toBeInTheDocument();
        expect((await findUserRow("tam.nguyen")).getByText("Học viên")).toBeInTheDocument();
    });
});

describe("AdminUsers - tìm kiếm và lọc", () => {
    const searchBox = () => screen.getByRole("searchbox", { name: "Tìm người dùng" });
    const roleSelect = () => screen.getByRole("combobox", { name: "Lọc theo vai trò" });
    const stateSelect = () => screen.getByRole("combobox", { name: "Lọc theo trạng thái" });
    const usernames = () =>
        screen.getAllByRole("rowheader").map((cell) => cell.querySelector("strong")?.textContent);

    beforeEach(() => {
        visit("/admin/users");
    });

    it("tìm theo username sau khi ngừng gõ và ghi từ khoá lên URL", async () => {
        renderUsers();
        await findUserRow("tam.nguyen");

        fireEvent.change(searchBox(), { target: { value: "viet" } });

        await findUserRow("viet.luu");
        await waitFor(() => expect(usernames()).toEqual(["viet.luu"]));
        expect(window.location.search).toBe("?q=viet");
        expect(screen.getByText("1-1 trong 1")).toBeInTheDocument();
    });

    it("lọc theo vai trò", async () => {
        renderUsers();
        await findUserRow("tam.nguyen");

        fireEvent.change(roleSelect(), { target: { value: "SETTER" } });

        await waitFor(() => expect(usernames()).toEqual(["datahust", "an.bui", "quang.le"]));
        expect(window.location.search).toBe("?role=SETTER");
    });

    it("lọc theo trạng thái", async () => {
        renderUsers();
        await findUserRow("tam.nguyen");

        fireEvent.change(stateSelect(), { target: { value: "BANNED" } });

        await waitFor(() => expect(usernames()).toEqual(["spam.bot88", "copy.paste"]));
        expect(window.location.search).toBe("?status=BANNED");
    });

    it("áp bộ lọc ghi sẵn trên URL", async () => {
        renderUsers("q=thu&role=MODERATOR");

        await findUserRow("mod.thu");

        expect(usernames()).toEqual(["mod.thu"]);
        expect(searchBox()).toHaveValue("thu");
        expect(roleSelect()).toHaveValue("MODERATOR");
        expect(stateSelect()).toHaveValue("");
    });

    it("về trang 1 khi đổi bộ lọc", async () => {
        renderUsers("page=2");
        await screen.findByText("21-40 trong 62");

        fireEvent.change(roleSelect(), { target: { value: "LEARNER" } });

        expect(await screen.findByText("1-20 trong 56")).toBeInTheDocument();
        expect(window.location.search).toBe("?role=LEARNER");
    });

    it("giữ bộ lọc trong liên kết phân trang", async () => {
        renderUsers("role=LEARNER");
        await screen.findByText("1-20 trong 56");

        expect(screen.getByRole("link", { name: "Trang 2" })).toHaveAttribute(
            "href",
            "/admin/users?role=LEARNER&page=2",
        );
        expect(screen.getByRole("link", { name: "Sau" })).toHaveAttribute(
            "href",
            "/admin/users?role=LEARNER&page=2",
        );
    });

    it("chỉ hiện nút Xoá lọc khi đang lọc và xoá được mọi bộ lọc", async () => {
        renderUsers();
        await findUserRow("tam.nguyen");

        expect(screen.queryByRole("button", { name: "Xoá lọc" })).not.toBeInTheDocument();

        fireEvent.change(searchBox(), { target: { value: "viet" } });
        await waitFor(() => expect(usernames()).toEqual(["viet.luu"]));
        fireEvent.change(stateSelect(), { target: { value: "ACTIVE" } });
        fireEvent.click(screen.getByRole("button", { name: "Xoá lọc" }));

        await findUserRow("tam.nguyen");
        expect(searchBox()).toHaveValue("");
        expect(stateSelect()).toHaveValue("");
        expect(window.location.search).toBe("");
        expect(screen.queryByRole("button", { name: "Xoá lọc" })).not.toBeInTheDocument();
    });

    it("báo không tìm thấy kèm gợi ý và xoá lọc được ngay tại đó", async () => {
        renderUsers("q=khong-co-ai");

        expect(await screen.findByText("Không tìm thấy người dùng")).toBeInTheDocument();
        expect(screen.getByText("Thử từ khoá khác hoặc bỏ bớt bộ lọc.")).toBeInTheDocument();

        fireEvent.click(screen.getAllByRole("button", { name: "Xoá lọc" })[1]);

        expect((await findUserRow("tam.nguyen")).getByText("Học viên")).toBeInTheDocument();
    });

    it("giữ thanh lọc khi danh sách tải lỗi", async () => {
        server.use(http.get("*/api/admin/users", () => serverError()));
        renderUsers("role=SETTER");

        expect(await screen.findByRole("alert")).toBeInTheDocument();
        expect(roleSelect()).toHaveValue("SETTER");
    });
});

describe("AdminUsers - thẻ số", () => {
    const tile = (label: string) => screen.getByRole("button", { name: new RegExp(`^${label}`) });
    const findTileValue = (label: string, value: string) =>
        waitFor(() => expect(within(tile(label)).getByText(value)).toBeInTheDocument());
    const usernames = () =>
        screen.getAllByRole("rowheader").map((cell) => cell.querySelector("strong")?.textContent);

    beforeEach(() => {
        visit("/admin/users");
    });

    it("hiện số tài khoản của 5 nhóm", async () => {
        renderUsers();

        await findTileValue("Tổng tài khoản", "62");
        await findTileValue("Chưa xác minh email", "1");
        await findTileValue("Người ra đề", "3");
        await findTileValue("Kiểm duyệt", "2");
        await findTileValue("Đang bị khoá", "2");
    });

    it("đánh dấu thẻ Tổng tài khoản khi chưa lọc", async () => {
        renderUsers();
        await findUserRow("tam.nguyen");

        expect(tile("Tổng tài khoản")).toHaveAttribute("aria-pressed", "true");
        expect(tile("Đang bị khoá")).toHaveAttribute("aria-pressed", "false");
    });

    it("bấm thẻ Đang bị khoá thì lọc tài khoản bị khoá và đánh dấu thẻ", async () => {
        renderUsers();
        await findUserRow("tam.nguyen");

        fireEvent.click(tile("Đang bị khoá"));

        await waitFor(() => expect(usernames()).toEqual(["spam.bot88", "copy.paste"]));
        expect(tile("Đang bị khoá")).toHaveAttribute("aria-pressed", "true");
        expect(tile("Tổng tài khoản")).toHaveAttribute("aria-pressed", "false");
        expect(window.location.search).toBe("?status=BANNED");
        expect(screen.getByRole("combobox", { name: "Lọc theo trạng thái" })).toHaveValue("BANNED");
    });

    it.each([
        ["Chưa xác minh email", "?status=UNVERIFIED"],
        ["Người ra đề", "?role=SETTER"],
        ["Kiểm duyệt", "?role=MODERATOR"],
    ])("bấm thẻ %s thì thay mọi bộ lọc đang có bằng %s", async (label, expectedSearch) => {
        renderUsers("q=viet&role=LEARNER&status=ACTIVE&page=2");
        await screen.findByText("Không tìm thấy người dùng");

        fireEvent.click(tile(label));

        await waitFor(() => expect(window.location.search).toBe(expectedSearch));
        expect(screen.getByRole("searchbox", { name: "Tìm người dùng" })).toHaveValue("");
        expect(tile(label)).toHaveAttribute("aria-pressed", "true");
    });

    it("bấm thẻ Tổng tài khoản thì xoá mọi bộ lọc", async () => {
        renderUsers("role=SETTER");
        await findUserRow("an.bui");

        fireEvent.click(tile("Tổng tài khoản"));

        await screen.findByText("1-20 trong 62");
        expect(window.location.search).toBe("");
        expect(tile("Tổng tài khoản")).toHaveAttribute("aria-pressed", "true");
    });

    it("không đánh dấu thẻ khi bộ lọc hẹp hơn nhóm của thẻ", async () => {
        renderUsers("q=an&role=SETTER");
        await findUserRow("an.bui");

        expect(tile("Người ra đề")).toHaveAttribute("aria-pressed", "false");
        expect(tile("Tổng tài khoản")).toHaveAttribute("aria-pressed", "false");
    });

    it("hiện gạch ngang ở thẻ số và vẫn hiện danh sách khi không tải được số liệu", async () => {
        server.use(http.get("*/api/admin/user-summary", () => serverError()));
        renderUsers();

        expect((await findUserRow("tam.nguyen")).getByText("Học viên")).toBeInTheDocument();
        await findTileValue("Tổng tài khoản", "-");
        expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    });
});
