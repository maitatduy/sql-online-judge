import { screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";

import { resetAdminContests } from "@/mocks/admin-contests";
import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { AdminContestDetail } from "./AdminContestDetail";

const UPCOMING_ID = "0193a000-0000-7000-8000-000000000002";
const FINISHED_ID = "0193a000-0000-7000-8000-000000000001";

describe("chi tiết contest cho admin", () => {
    beforeEach(() => {
        resetAdminContests();
        writeMockSession(ADMIN_SESSION_USER);
    });

    it("hiện thông tin contest và các bài theo chữ cái", async () => {
        renderWithProviders(<AdminContestDetail contestId={FINISHED_ID} />);

        expect(await screen.findByRole("heading", { name: "SQL Cup 1" })).toBeInTheDocument();
        expect(screen.getByText("Đã kết thúc")).toBeInTheDocument();
        expect(screen.getByText("20 phút")).toBeInTheDocument();
        const problems = within(screen.getByRole("list", { name: "Bài trong contest" }));
        expect(problems.getAllByRole("listitem")).toHaveLength(2);
        expect(problems.getByText("A")).toBeInTheDocument();
        expect(problems.getByText("Ghép phòng ban")).toBeInTheDocument();
    });

    it("báo chưa gắn bài khi contest chưa có bài nào", async () => {
        renderWithProviders(<AdminContestDetail contestId={UPCOMING_ID} />);

        expect(await screen.findByText("Chưa gắn bài nào.")).toBeInTheDocument();
    });

    it("hiện lỗi khi không có contest", async () => {
        renderWithProviders(
            <AdminContestDetail contestId="0193a000-0000-7000-8000-0000000000ff" />,
        );

        expect(await screen.findByRole("alert")).toHaveTextContent("Không tìm thấy contest");
    });
});
