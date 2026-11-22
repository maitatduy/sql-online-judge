import { fireEvent, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { resetAdminContests } from "@/mocks/admin-contests";
import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { AdminContestDetail } from "./AdminContestDetail";

const UPCOMING_ID = "0193a000-0000-7000-8000-000000000002";
const FINISHED_ID = "0193a000-0000-7000-8000-000000000001";
const BOOKING_GAPS = "Khoảng trống trong lịch đặt phòng";
const BEST_SELLERS = "Sản phẩm bán chạy theo quý";

function renderDetail(contestId: string) {
    renderWithProviders(
        <ToastProvider>
            <AdminContestDetail contestId={contestId} />
        </ToastProvider>,
    );
}

async function addProblem(title: string) {
    const library = within(await screen.findByRole("region", { name: "Thêm từ kho bài" }));
    fireEvent.click(await library.findByRole("button", { name: `Thêm ${title}` }));
}

function selectedTitles(): string[] {
    const list = within(screen.getByRole("list", { name: "Bài trong contest" }));
    return list.getAllByRole("listitem").map((item) => item.textContent ?? "");
}

describe("chi tiết contest cho admin", () => {
    beforeEach(() => {
        resetAdminContests();
        sessionStorage.clear();
        writeMockSession(ADMIN_SESSION_USER);
    });

    it("contest đã kết thúc chỉ hiện thông tin và các bài theo chữ cái", async () => {
        renderDetail(FINISHED_ID);

        expect(await screen.findByRole("heading", { name: "SQL Cup 1" })).toBeInTheDocument();
        expect(screen.getByText("Đã kết thúc")).toBeInTheDocument();
        expect(screen.getByText("20 phút")).toBeInTheDocument();
        expect(selectedTitles()).toEqual([
            expect.stringContaining("Ghép phòng ban"),
            expect.stringContaining("Xếp hạng lương"),
        ]);
        expect(screen.queryByRole("button", { name: "Lưu danh sách bài" })).toBeNull();
    });

    it("contest sắp diễn ra chưa có bài thì báo chưa gắn bài", async () => {
        renderDetail(UPCOMING_ID);

        expect(await screen.findByText("Chưa gắn bài nào.")).toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Lưu danh sách bài" })).toBeDisabled();
    });

    it("thêm bài đã duyệt cho contest, đổi thứ tự rồi lưu", async () => {
        renderDetail(UPCOMING_ID);

        await addProblem(BOOKING_GAPS);
        await addProblem(BEST_SELLERS);
        expect(selectedTitles()).toEqual([
            expect.stringMatching(new RegExp(`^A${BOOKING_GAPS}`)),
            expect.stringMatching(new RegExp(`^B${BEST_SELLERS}`)),
        ]);

        fireEvent.click(screen.getByRole("button", { name: `Đưa ${BEST_SELLERS} lên` }));
        expect(selectedTitles()[0]).toMatch(new RegExp(`^A${BEST_SELLERS}`));
        fireEvent.click(screen.getByRole("button", { name: "Lưu danh sách bài" }));

        expect(await screen.findByText("Đã lưu danh sách bài.")).toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Lưu danh sách bài" })).toBeDisabled();
    });

    it("bỏ bài khỏi danh sách", async () => {
        renderDetail(UPCOMING_ID);

        await addProblem(BOOKING_GAPS);
        fireEvent.click(screen.getByRole("button", { name: `Bỏ ${BOOKING_GAPS}` }));

        expect(screen.getByText("Chưa gắn bài nào.")).toBeInTheDocument();
    });

    it("hiện lỗi khi không có contest", async () => {
        renderDetail("0193a000-0000-7000-8000-0000000000ff");

        expect(await screen.findByRole("alert")).toHaveTextContent("Không tìm thấy contest");
    });
});
