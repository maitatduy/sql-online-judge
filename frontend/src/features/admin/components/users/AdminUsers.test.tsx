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
            "Rating",
            "Bài AC",
            "Tham gia",
            "Hoạt động",
            "Trạng thái",
        ]);
        expect(screen.getAllByRole("rowheader")).toHaveLength(20);
    });

    it("hiện thông tin của một tài khoản chưa xác minh", async () => {
        renderUsers();

        const row = await findUserRow("tam.nguyen");

        expect(row.getByText("tam.nguyen@example.com")).toBeInTheDocument();
        expect(row.getByText("Học viên")).toBeInTheDocument();
        expect(row.getByText("11/10/2026")).toBeInTheDocument();
        expect(row.getByText("10 phút trước")).toBeInTheDocument();
        expect(row.getByText("Chưa xác minh")).toBeInTheDocument();
    });

    it("hiện vai trò và trạng thái bằng chữ", async () => {
        renderUsers();

        expect((await findUserRow("spam.bot88")).getByText("Bị khoá")).toBeInTheDocument();

        const setter = await findUserRow("datahust");

        expect(setter.getByText("Người ra đề")).toBeInTheDocument();
        expect(setter.getByText("Hoạt động")).toBeInTheDocument();
    });

    it("hiện gạch ngang khi chưa có rating và định dạng số theo vi-VN", async () => {
        renderUsers("page=4");

        const unrated = await findUserRow("admin.huy");

        expect(unrated.getAllByRole("cell")[2]).toHaveTextContent(/^-$/);
        expect(unrated.getByText("Đang trực tuyến")).toBeInTheDocument();
        expect((await findUserRow("quang.le")).getByText("2.184")).toBeInTheDocument();
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

describe("AdminUsers - sắp xếp", () => {
    const sortButton = (name: string) => screen.getByRole("button", { name });
    const header = (name: string) => screen.getByRole("columnheader", { name });
    const firstUsernames = (count: number) =>
        screen
            .getAllByRole("rowheader")
            .slice(0, count)
            .map((cell) => cell.querySelector("strong")?.textContent);

    beforeEach(() => {
        visit("/admin/users");
    });

    it("chưa sắp xếp cột nào khi mới mở", async () => {
        renderUsers();
        await findUserRow("tam.nguyen");

        expect(header("Rating")).toHaveAttribute("aria-sort", "none");
        expect(header("Bài AC")).toHaveAttribute("aria-sort", "none");
    });

    it("bấm Rating thì xếp giảm dần, bấm lại thì đảo chiều", async () => {
        renderUsers();
        await findUserRow("tam.nguyen");

        fireEvent.click(sortButton("Rating"));

        await waitFor(() =>
            expect(firstUsernames(3)).toEqual(["quang.le", "trang.hoang", "an.bui"]),
        );
        expect(header("Rating")).toHaveAttribute("aria-sort", "descending");
        expect(window.location.search).toBe("?sort=rating%2Cdesc");

        fireEvent.click(sortButton("Rating"));

        await waitFor(() => expect(firstUsernames(2)).toEqual(["tam.nguyen", "spam.bot88"]));
        expect(header("Rating")).toHaveAttribute("aria-sort", "ascending");
        expect(window.location.search).toBe("?sort=rating%2Casc");
    });

    it("bấm cột khác thì xếp giảm dần theo cột đó", async () => {
        renderUsers("sort=rating,asc");
        await findUserRow("tam.nguyen");

        fireEvent.click(sortButton("Bài AC"));

        await waitFor(() =>
            expect(firstUsernames(3)).toEqual(["quang.le", "trang.hoang", "mod.thu"]),
        );
        expect(header("Bài AC")).toHaveAttribute("aria-sort", "descending");
        expect(header("Rating")).toHaveAttribute("aria-sort", "none");
    });

    it("về trang 1 khi đổi cách sắp xếp và giữ cách sắp xếp khi chuyển trang", async () => {
        renderUsers("page=2");
        await screen.findByText("21-40 trong 62");

        fireEvent.click(sortButton("Rating"));

        await findUserRow("quang.le");
        expect(screen.getByText("1-20 trong 62")).toBeInTheDocument();
        expect(screen.getByRole("link", { name: "Trang 2" })).toHaveAttribute(
            "href",
            "/admin/users?sort=rating%2Cdesc&page=2",
        );
    });

    it("giữ cách sắp xếp khi xoá lọc", async () => {
        renderUsers("role=SETTER&sort=rating,desc");
        await findUserRow("quang.le");

        fireEvent.click(screen.getByRole("button", { name: "Xoá lọc" }));

        await waitFor(() =>
            expect(firstUsernames(3)).toEqual(["quang.le", "trang.hoang", "an.bui"]),
        );
        expect(window.location.search).toBe("?sort=rating%2Cdesc");
    });

    it("không coi sắp xếp là bộ lọc cần xoá", async () => {
        renderUsers("sort=rating,desc");
        await findUserRow("quang.le");

        expect(screen.queryByRole("button", { name: "Xoá lọc" })).not.toBeInTheDocument();
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

    it("bấm thẻ Tổng tài khoản thì xoá bộ lọc nhưng giữ cách sắp xếp", async () => {
        renderUsers("role=SETTER&sort=rating,desc");
        await findUserRow("quang.le");

        fireEvent.click(tile("Tổng tài khoản"));

        await screen.findByText("1-20 trong 62");
        expect(window.location.search).toBe("?sort=rating%2Cdesc");
        expect(tile("Tổng tài khoản")).toHaveAttribute("aria-pressed", "true");
    });

    it("không đánh dấu thẻ khi bộ lọc hẹp hơn nhóm của thẻ", async () => {
        renderUsers("q=an&role=SETTER");
        await findUserRow("an.bui");

        expect(tile("Người ra đề")).toHaveAttribute("aria-pressed", "false");
        expect(tile("Tổng tài khoản")).toHaveAttribute("aria-pressed", "false");
    });

    it("hiện gạch ngang ở thẻ số và vẫn hiện danh sách khi không tải được số liệu", async () => {
        server.use(http.get("*/api/admin/users/summary", () => serverError()));
        renderUsers();

        expect((await findUserRow("tam.nguyen")).getByText("Học viên")).toBeInTheDocument();
        await findTileValue("Tổng tài khoản", "-");
        expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    });
});

describe("AdminUsers - panel chi tiết", () => {
    const PANEL_NAME = "Chi tiết người dùng";
    const findPanel = async () =>
        within(await screen.findByRole("complementary", { name: PANEL_NAME }));
    const queryPanel = () => screen.queryByRole("complementary", { name: PANEL_NAME });
    const fact = (panel: ReturnType<typeof within>, label: string) =>
        panel.getByText(label).nextElementSibling;

    beforeEach(() => {
        visit("/admin/users");
    });

    it("không hiện panel khi chưa chọn tài khoản nào", async () => {
        renderUsers();
        await findUserRow("tam.nguyen");

        expect(queryPanel()).not.toBeInTheDocument();
    });

    it("bấm username thì mở panel, tô dòng và ghi tài khoản lên URL", async () => {
        renderUsers();
        await findUserRow("tam.nguyen");

        fireEvent.click(screen.getByRole("button", { name: "tam.nguyen" }));

        const panel = await findPanel();

        expect(await panel.findByText("Nguyễn Thành Tâm")).toBeInTheDocument();
        expect(fact(panel, "Email")).toHaveTextContent("tam.nguyen@example.com");
        expect(fact(panel, "Xác minh email")).toHaveTextContent("Chưa xác minh");
        expect(fact(panel, "Đăng nhập gần nhất")).toHaveTextContent("10 phút trước");
        expect(fact(panel, "Tham gia")).toHaveTextContent("11/10/2026 qua email");
        expect(panel.getByText("Chưa có vi phạm nào.")).toBeInTheDocument();
        expect(window.location.search).toBe("?user=tam.nguyen");
        expect(screen.getByRole("row", { name: /^tam\.nguyen/ })).toHaveAttribute(
            "aria-current",
            "true",
        );
        expect(screen.getByRole("row", { name: /^datahust/ })).not.toHaveAttribute("aria-current");
    });

    it("bấm vào dòng cũng mở panel", async () => {
        renderUsers();

        fireEvent.click((await screen.findAllByText("Người ra đề", { selector: "td span" }))[0]);

        expect(await (await findPanel()).findByText("CLB Data HUST")).toBeInTheDocument();
    });

    it("mở panel theo URL kể cả khi tài khoản không nằm ở trang đang xem", async () => {
        renderUsers("user=viet.luu");

        const panel = await findPanel();

        expect(await panel.findByText("Lưu Quốc Việt")).toBeInTheDocument();
        expect(fact(panel, "Rating")).toHaveTextContent("1.212");
        expect(fact(panel, "Bài AC")).toHaveTextContent("41");
        expect(fact(panel, "Vi phạm")).toHaveTextContent("2");
        expect(panel.getByRole("combobox", { name: "Vai trò" })).toHaveValue("LEARNER");
        expect(fact(panel, "Tham gia")).toHaveTextContent("03/02/2026 qua GitHub");
        expect(fact(panel, "Xác minh email")).toHaveTextContent("Đã xác minh");
        expect(panel.getByText("Nghi đạo code trong contest")).toBeInTheDocument();
        expect(panel.getByText("12/09, mod.thu đã cảnh cáo")).toBeInTheDocument();
        expect(panel.getByRole("link", { name: "Mở trang chi tiết" })).toHaveAttribute(
            "href",
            "/admin/users/viet.luu",
        );
        expect(panel.getByRole("link", { name: "Xem hồ sơ công khai" })).toHaveAttribute(
            "href",
            "/u/viet.luu",
        );
    });

    it("nêu thời hạn khoá của tài khoản bị khoá", async () => {
        renderUsers("user=copy.paste");

        const panel = await findPanel();

        expect(await panel.findByText(/^đến \d{2}\/\d{2}\/\d{4}$/)).toBeInTheDocument();
        expect(panel.getByText("Chưa đặt tên")).toBeInTheDocument();
    });

    it("ghi vĩnh viễn khi khoá không có thời hạn", async () => {
        renderUsers("user=spam.bot88");

        expect(await (await findPanel()).findByText("vĩnh viễn")).toBeInTheDocument();
    });

    it("đóng panel bằng nút Đóng", async () => {
        renderUsers("user=viet.luu&role=LEARNER");
        const panel = await findPanel();

        fireEvent.click(panel.getByRole("button", { name: "Đóng" }));

        await waitFor(() => expect(queryPanel()).not.toBeInTheDocument());
        expect(window.location.search).toBe("?role=LEARNER");
    });

    it("giữ panel khi đổi bộ lọc và trong liên kết phân trang", async () => {
        renderUsers("user=viet.luu");
        await (await findPanel()).findByText("Lưu Quốc Việt");

        fireEvent.change(screen.getByRole("combobox", { name: "Lọc theo vai trò" }), {
            target: { value: "LEARNER" },
        });

        await screen.findByText("1-20 trong 56");
        expect(window.location.search).toBe("?role=LEARNER&user=viet.luu");
        expect(queryPanel()).toBeInTheDocument();
        expect(screen.getByRole("link", { name: "Trang 2" })).toHaveAttribute(
            "href",
            "/admin/users?role=LEARNER&page=2&user=viet.luu",
        );
    });

    it("báo đang tải trong panel trước khi có chi tiết", async () => {
        renderUsers("user=viet.luu");

        expect(
            await screen.findByRole("status", { name: "Đang tải chi tiết người dùng" }),
        ).toBeInTheDocument();
    });

    it("hiện lỗi trong panel và tải lại được, danh sách không bị ảnh hưởng", async () => {
        server.use(
            http.get("*/api/admin/users/viet.luu", () => serverError("9f1c"), { once: true }),
        );
        renderUsers("user=viet.luu");

        const panel = await findPanel();

        expect(await panel.findByRole("alert")).toHaveTextContent("9f1c");
        expect((await findUserRow("tam.nguyen")).getByText("Học viên")).toBeInTheDocument();

        fireEvent.click(panel.getByRole("button", { name: "Thử lại" }));

        expect(await panel.findByText("Lưu Quốc Việt")).toBeInTheDocument();
    });

    it("đóng panel và báo khi tài khoản trên URL không tồn tại", async () => {
        renderUsers("user=khong.co");

        expect(await screen.findByText("Không tìm thấy người dùng khong.co")).toBeInTheDocument();
        await waitFor(() => expect(queryPanel()).not.toBeInTheDocument());
        expect(window.location.search).toBe("");
    });
});

describe("AdminUsers - đổi vai trò từ panel", () => {
    const SELF_NOTE = "Bạn không thể tự đổi vai trò hoặc khoá tài khoản của chính mình.";

    async function openPanel(username: string) {
        renderUsers(`user=${username}`);
        const panel = within(
            await screen.findByRole("complementary", { name: "Chi tiết người dùng" }),
        );
        const roleSelect = await panel.findByRole("combobox", { name: "Vai trò" });
        return { panel, roleSelect };
    }

    beforeEach(() => {
        visit("/admin/users");
    });

    it("chỉ hiện nút Lưu khi vai trò đang chọn khác vai trò hiện tại", async () => {
        const { panel, roleSelect } = await openPanel("viet.luu");
        await waitFor(() => expect(roleSelect).toBeEnabled());

        expect(panel.queryByRole("button", { name: "Lưu" })).not.toBeInTheDocument();

        fireEvent.change(roleSelect, { target: { value: "SETTER" } });

        expect(panel.getByRole("button", { name: "Lưu" })).toBeInTheDocument();

        fireEvent.change(roleSelect, { target: { value: "LEARNER" } });

        expect(panel.queryByRole("button", { name: "Lưu" })).not.toBeInTheDocument();
    });

    it("lưu vai trò mới, báo thành công và cập nhật bảng cùng thẻ số", async () => {
        const { panel, roleSelect } = await openPanel("tam.nguyen");
        await waitFor(() => expect(roleSelect).toBeEnabled());

        fireEvent.change(roleSelect, { target: { value: "MODERATOR" } });
        fireEvent.click(panel.getByRole("button", { name: "Lưu" }));

        expect(
            await screen.findByText("Đã đổi vai trò của tam.nguyen thành Kiểm duyệt."),
        ).toBeInTheDocument();
        expect(roleSelect).toHaveValue("MODERATOR");
        expect(panel.queryByRole("button", { name: "Lưu" })).not.toBeInTheDocument();
        await waitFor(() =>
            expect(
                within(screen.getByRole("row", { name: /^tam\.nguyen/ })).getByText("Kiểm duyệt"),
            ).toBeInTheDocument(),
        );
        await waitFor(() =>
            expect(
                within(screen.getByRole("button", { name: /^Kiểm duyệt/ })).getByText("3"),
            ).toBeInTheDocument(),
        );
    });

    it("khoá ô vai trò và giải thích khi xem tài khoản của chính mình", async () => {
        const { panel, roleSelect } = await openPanel("admin.huy");

        expect(await panel.findByText(SELF_NOTE)).toBeInTheDocument();
        expect(roleSelect).toBeDisabled();
        expect(roleSelect).toHaveValue("ADMIN");
    });

    it("không hiện lời giải thích với tài khoản của người khác", async () => {
        const { panel, roleSelect } = await openPanel("mod.thu");
        await waitFor(() => expect(roleSelect).toBeEnabled());

        expect(panel.queryByText(SELF_NOTE)).not.toBeInTheDocument();
    });

    it("báo lỗi và giữ lựa chọn khi lưu thất bại", async () => {
        server.use(http.patch("*/api/admin/users/viet.luu", () => serverError()));
        const { panel, roleSelect } = await openPanel("viet.luu");
        await waitFor(() => expect(roleSelect).toBeEnabled());

        fireEvent.change(roleSelect, { target: { value: "SETTER" } });
        fireEvent.click(panel.getByRole("button", { name: "Lưu" }));

        expect(
            await screen.findByText("Chưa đổi được vai trò của viet.luu. Đã có lỗi xảy ra"),
        ).toBeInTheDocument();
        expect(roleSelect).toHaveValue("SETTER");
        expect(panel.getByRole("button", { name: "Lưu" })).toBeEnabled();
    });
});
