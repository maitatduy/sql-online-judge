import { fireEvent, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { MINH_ANH_PROFILE } from "@/mocks/profile-fixtures";
import { renderWithProviders } from "@/test/render";

import { ProfileTabs } from "./ProfileTabs";

const navigation = vi.hoisted(() => ({
    search: "",
    replace: vi.fn(),
}));

vi.mock("next/navigation", () => ({
    usePathname: () => "/u/minh.anh",
    useRouter: () => ({ replace: navigation.replace }),
    useSearchParams: () => new URLSearchParams(navigation.search),
}));

function renderTabs() {
    return renderWithProviders(
        <ProfileTabs username="minh.anh" tabCounts={MINH_ANH_PROFILE.tabCounts} />,
    );
}

describe("ProfileTabs", () => {
    beforeEach(() => {
        navigation.search = "";
        navigation.replace.mockClear();
    });

    it("có 4 tab kèm số đếm", () => {
        renderTabs();

        expect(screen.getAllByRole("tab").map((tab) => tab.textContent)).toEqual([
            "Bài nộp gần đây",
            "Lời giải đã chia sẻ · 6",
            "Bài viết · 3",
            "Contest đã thi · 8",
        ]);
    });

    it("mặc định mở tab bài nộp gần đây", async () => {
        renderTabs();

        expect(screen.getByRole("tab", { name: "Bài nộp gần đây" })).toHaveAttribute(
            "aria-selected",
            "true",
        );
        expect(await screen.findByRole("table", { name: "Bài nộp gần đây" })).toBeInTheDocument();
    });

    it("mở đúng tab ghi trong đường dẫn và báo sắp ra mắt", () => {
        navigation.search = "tab=posts";
        renderTabs();

        expect(screen.getByRole("tab", { name: "Bài viết · 3" })).toHaveAttribute(
            "aria-selected",
            "true",
        );
        expect(screen.getByRole("tabpanel")).toHaveTextContent("Sắp ra mắt");
    });

    it("quay về tab mặc định khi đường dẫn ghi tab không tồn tại", () => {
        navigation.search = "tab=khong-co";
        renderTabs();

        expect(screen.getByRole("tab", { name: "Bài nộp gần đây" })).toHaveAttribute(
            "aria-selected",
            "true",
        );
    });

    it("chọn tab thì ghi vào đường dẫn, không cuộn trang", () => {
        renderTabs();

        fireEvent.mouseDown(screen.getByRole("tab", { name: "Contest đã thi · 8" }), {
            button: 0,
        });

        expect(navigation.replace).toHaveBeenCalledWith("/u/minh.anh?tab=contests", {
            scroll: false,
        });
    });

    it("chọn lại tab mặc định thì bỏ tham số tab, giữ các tham số khác", () => {
        navigation.search = "tab=posts&ref=share";
        renderTabs();

        fireEvent.mouseDown(screen.getByRole("tab", { name: "Bài nộp gần đây" }), { button: 0 });

        expect(navigation.replace).toHaveBeenCalledWith("/u/minh.anh?ref=share", {
            scroll: false,
        });
    });
});
