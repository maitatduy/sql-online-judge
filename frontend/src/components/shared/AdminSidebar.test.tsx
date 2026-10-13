import { fireEvent, render, screen, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import type { AdminNavigationCounts } from "@/lib/admin-navigation";

import { AdminSidebar } from "./AdminSidebar";

const ADMIN_USER = { username: "admin.huy", displayName: "Admin Huy" };
const BUSY_COUNTS: AdminNavigationCounts = {
    abuseReports: 12,
    supportMessages: 3,
    problemReviews: 5,
    hasLiveContest: true,
    hasJudgeIncident: true,
};
const QUIET_COUNTS: AdminNavigationCounts = {
    abuseReports: 0,
    supportMessages: 0,
    problemReviews: 0,
    hasLiveContest: false,
    hasJudgeIncident: false,
};
const EXPECTED_LINKS = [
    ["Tổng quan", "/admin"],
    ["Người dùng", "/admin/users"],
    ["Báo cáo vi phạm", "/admin/reports"],
    ["Thông báo & tin nhắn", "/admin/messages"],
    ["Kho bài tập", "/admin/problems"],
    ["Duyệt bài tập", "/admin/problem-reviews"],
    ["Lời giải chính thức", "/admin/solutions"],
    ["Khoá học", "/admin/courses"],
    ["Contest", "/admin/contests"],
    ["Trang nội dung", "/admin/pages"],
    ["Bài nộp", "/admin/submissions"],
    ["Máy chấm", "/admin/judge"],
    ["Trợ lý AI", "/admin/ai"],
    ["Nhật ký thao tác", "/admin/audit-log"],
    ["Cấu hình", "/admin/settings"],
];

function renderSidebar(activePath = "/admin", counts?: AdminNavigationCounts) {
    render(<AdminSidebar activePath={activePath} counts={counts} user={ADMIN_USER} />);
    return screen.getByRole("navigation", { name: "Quản trị" });
}

function menuLink(navigation: HTMLElement, label: string) {
    return within(navigation).getByRole("link", { name: new RegExp(`^${label}`) });
}

describe("AdminSidebar", () => {
    it.each(EXPECTED_LINKS)("mục %s dẫn tới %s", (label, href) => {
        const navigation = renderSidebar();

        expect(menuLink(navigation, label)).toHaveAttribute("href", href);
    });

    it("chia mục thành ba nhóm", () => {
        const navigation = renderSidebar();

        expect(
            within(
                within(navigation).getByRole("list", { name: "Người & cộng đồng" }),
            ).getAllByRole("link"),
        ).toHaveLength(3);
        expect(
            within(within(navigation).getByRole("list", { name: "Nội dung" })).getAllByRole("link"),
        ).toHaveLength(6);
        expect(
            within(within(navigation).getByRole("list", { name: "Hệ thống" })).getAllByRole("link"),
        ).toHaveLength(5);
    });

    it("chỉ đánh dấu Tổng quan khi đang ở trang gốc", () => {
        const navigation = renderSidebar("/admin");

        expect(within(navigation).getAllByRole("link", { current: "page" })).toHaveLength(1);
        expect(menuLink(navigation, "Tổng quan")).toHaveAttribute("aria-current", "page");
    });

    it("đánh dấu mục cha khi đang ở trang con", () => {
        const navigation = renderSidebar("/admin/users/0192f3a0");

        expect(within(navigation).getAllByRole("link", { current: "page" })).toHaveLength(1);
        expect(menuLink(navigation, "Người dùng")).toHaveAttribute("aria-current", "page");
    });

    it("hiện số việc đang chờ bên cạnh mục tương ứng", () => {
        const navigation = renderSidebar("/admin", BUSY_COUNTS);

        expect(menuLink(navigation, "Báo cáo vi phạm")).toHaveTextContent("12 đang chờ");
        expect(menuLink(navigation, "Thông báo & tin nhắn")).toHaveTextContent("3 đang chờ");
        expect(menuLink(navigation, "Duyệt bài tập")).toHaveTextContent("5 đang chờ");
    });

    it("báo contest đang diễn ra và sự cố máy chấm bằng chữ", () => {
        const navigation = renderSidebar("/admin", BUSY_COUNTS);

        expect(menuLink(navigation, "Contest")).toHaveTextContent("Live");
        expect(menuLink(navigation, "Máy chấm")).toHaveTextContent("Có worker mất kết nối");
    });

    it("không hiện nhãn nào khi không có việc chờ", () => {
        const navigation = renderSidebar("/admin", QUIET_COUNTS);

        expect(navigation).not.toHaveTextContent("đang chờ");
        expect(navigation).not.toHaveTextContent("Live");
        expect(navigation).not.toHaveTextContent("Có worker mất kết nối");
    });

    it("không hiện nhãn nào khi chưa có số đếm", () => {
        const navigation = renderSidebar("/admin");

        expect(navigation).not.toHaveTextContent("đang chờ");
    });

    it("hiện người đang đăng nhập và đường về trang người học", () => {
        const navigation = renderSidebar();

        expect(navigation).toHaveTextContent("admin.huy");
        expect(navigation).toHaveTextContent("Quản trị viên");
        expect(
            within(navigation).getByRole("link", { name: "Về trang người học" }),
        ).toHaveAttribute("href", "/");
    });

    it("không hiện chữ Admin Console", () => {
        const navigation = renderSidebar();

        expect(within(navigation).queryByText("Admin Console")).not.toBeInTheDocument();
        expect(within(navigation).getByRole("link", { name: "Admin Console" })).toHaveAttribute(
            "href",
            "/admin",
        );
    });

    it.each(["Người & cộng đồng", "Nội dung", "Hệ thống"])(
        "chỉ giữ tên nhóm %s cho trình đọc màn hình",
        (groupTitle) => {
            const navigation = renderSidebar();

            expect(within(navigation).getByText(groupTitle)).toHaveClass("sr-only");
        },
    );

    it("giữ tên mục trên một dòng", () => {
        const navigation = renderSidebar();

        expect(within(navigation).getByText("Thông báo & tin nhắn")).toHaveClass(
            "whitespace-nowrap",
        );
    });

    it("không có nút thu gọn khi không được phép thu gọn", () => {
        renderSidebar();

        expect(screen.queryByRole("button", { name: /menu/ })).not.toBeInTheDocument();
    });
});

describe("AdminSidebar thu gọn", () => {
    function renderCollapsible(isCollapsed: boolean, counts?: AdminNavigationCounts) {
        const onToggleCollapse = vi.fn();
        render(
            <AdminSidebar
                activePath="/admin/reports"
                counts={counts}
                user={ADMIN_USER}
                isCollapsed={isCollapsed}
                onToggleCollapse={onToggleCollapse}
            />,
        );
        return {
            onToggleCollapse,
            navigation: screen.getByRole("navigation", { name: "Quản trị" }),
        };
    }

    it("có nút thu gọn khi đang mở rộng", () => {
        const { onToggleCollapse } = renderCollapsible(false);
        const toggle = screen.getByRole("button", { name: "Thu gọn menu" });

        expect(toggle).toHaveAttribute("aria-expanded", "true");

        fireEvent.click(toggle);

        expect(onToggleCollapse).toHaveBeenCalledTimes(1);
    });

    it("có nút mở rộng khi đang thu gọn", () => {
        const { onToggleCollapse } = renderCollapsible(true);
        const toggle = screen.getByRole("button", { name: "Mở rộng menu" });

        expect(toggle).toHaveAttribute("aria-expanded", "false");

        fireEvent.click(toggle);

        expect(onToggleCollapse).toHaveBeenCalledTimes(1);
    });

    it("vẫn đọc được tên mục và mục đang chọn khi thu gọn", () => {
        const { navigation } = renderCollapsible(true);
        const reportsLink = menuLink(navigation, "Báo cáo vi phạm");

        expect(reportsLink).toHaveAttribute("href", "/admin/reports");
        expect(reportsLink).toHaveAttribute("aria-current", "page");
        expect(reportsLink).toHaveAttribute("title", "Báo cáo vi phạm");
        expect(within(navigation).getAllByRole("link")).toHaveLength(17);
    });

    it("làm mờ tên mục thay vì gỡ khỏi bố cục để chuyển động không giật", () => {
        const { navigation } = renderCollapsible(true);
        const label = within(navigation).getByText("Báo cáo vi phạm");

        expect(label).toHaveClass("opacity-0");
        expect(label).not.toHaveClass("sr-only");
    });

    it("giữ nguyên tên mục hiện rõ khi mở rộng", () => {
        const { navigation } = renderCollapsible(false);

        expect(within(navigation).getByText("Báo cáo vi phạm")).not.toHaveClass("opacity-0");
    });

    it("chỉ đọc số việc đang chờ một lần khi thu gọn", () => {
        const { navigation } = renderCollapsible(true, BUSY_COUNTS);

        expect(menuLink(navigation, "Báo cáo vi phạm").textContent).toBe(
            "Báo cáo vi phạm12 đang chờ",
        );
    });

    it("đặt nút thu gọn ngoài danh sách menu để không bị cắt ở mép", () => {
        const { navigation } = renderCollapsible(false);

        expect(
            within(navigation).queryByRole("button", { name: "Thu gọn menu" }),
        ).not.toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Thu gọn menu" })).toBeInTheDocument();
    });

    it("vẫn chia ba nhóm có tên khi thu gọn", () => {
        const { navigation } = renderCollapsible(true);

        expect(within(navigation).getByRole("list", { name: "Hệ thống" })).toBeInTheDocument();
    });

    it("vẫn báo số việc đang chờ khi thu gọn", () => {
        const { navigation } = renderCollapsible(true, BUSY_COUNTS);

        expect(menuLink(navigation, "Báo cáo vi phạm")).toHaveTextContent("12 đang chờ");
        expect(menuLink(navigation, "Máy chấm")).toHaveTextContent("Có worker mất kết nối");
    });
});
