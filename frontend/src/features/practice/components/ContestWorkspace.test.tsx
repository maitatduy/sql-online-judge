import { fireEvent, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { resetAdminContests } from "@/mocks/admin-contests";
import { registerForMockContest, resetContestRegistrations } from "@/mocks/contests";
import { DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { ContestWorkspace } from "./ContestWorkspace";

vi.mock("next/navigation", () => ({ notFound: vi.fn() }));

vi.mock("@/components/shared/LazySqlEditor", () => ({
    LazySqlEditor: ({
        value,
        onChange,
        ariaLabel,
    }: {
        value: string;
        onChange: (value: string) => void;
        ariaLabel: string;
    }) => (
        <textarea
            aria-label={ariaLabel}
            value={value}
            onChange={(event) => onChange(event.target.value)}
        />
    ),
}));

const SLUG = "sql-cup-live";

function renderContestWorkspace() {
    renderWithProviders(<ContestWorkspace slug={SLUG} letter="A" />);
}

describe("màn thi contest", () => {
    beforeEach(() => {
        localStorage.clear();
        resetAdminContests();
        resetContestRegistrations();
        writeMockSession(DEMO_SESSION_USER);
    });

    it("hiện đề theo chữ cái, thanh chuyển bài có màu kết quả, đồng hồ và breadcrumb", async () => {
        registerForMockContest(SLUG, DEMO_SESSION_USER.id);
        renderContestWorkspace();

        expect(
            await screen.findByRole("heading", { name: "Lương cao nhất mỗi phòng ban" }),
        ).toBeInTheDocument();
        expect(screen.getByText("A. Lương cao nhất mỗi phòng ban")).toBeInTheDocument();
        const problems = within(screen.getByRole("navigation", { name: "Bài trong contest" }));
        expect(await problems.findByRole("link", { name: "A, đã AC" })).toHaveAttribute(
            "aria-current",
            "page",
        );
        expect(await screen.findByRole("timer", { name: "Còn lại" })).toBeInTheDocument();
        const breadcrumb = within(screen.getByRole("navigation", { name: "Đường dẫn" }));
        expect(await breadcrumb.findByRole("link", { name: "SQL Cup đang thi" })).toHaveAttribute(
            "href",
            "/contests/sql-cup-live",
        );
        expect(breadcrumb.getByRole("link", { name: "Contest" })).toHaveAttribute(
            "href",
            "/contests",
        );
        expect(screen.queryByRole("tab", { name: /Lịch sử nộp/ })).toBeNull();
    });

    it("chạy thử và nộp bài qua contest, bài sai không lộ diff", async () => {
        registerForMockContest(SLUG, DEMO_SESSION_USER.id);
        renderContestWorkspace();
        const editor = await screen.findByRole("textbox", { name: "Mã SQL của bạn" });

        fireEvent.click(screen.getByRole("button", { name: "Run" }));
        const run = within(screen.getByRole("tabpanel", { name: "Chạy thử" }));
        expect(await run.findByRole("table", { name: "Kết quả chạy thử" })).toBeInTheDocument();

        fireEvent.change(editor, {
            target: { value: "SELECT DISTINCT ON (department) department FROM employees" },
        });
        fireEvent.click(screen.getByRole("button", { name: "Submit" }));
        const submission = within(screen.getByRole("tabpanel", { name: "Nộp bài" }));
        expect(await submission.findByText("WA")).toBeInTheDocument();
        expect(
            submission.queryByRole("table", { name: "Dòng thiếu so với kết quả mong đợi" }),
        ).toBeNull();
    });

    it("nộp đúng thì nút về danh sách bài dẫn về sảnh chờ của contest", async () => {
        registerForMockContest(SLUG, DEMO_SESSION_USER.id);
        renderContestWorkspace();
        const editor = await screen.findByRole("textbox", { name: "Mã SQL của bạn" });

        fireEvent.change(editor, {
            target: {
                value: "SELECT * FROM employees e WHERE e.salary = (SELECT MAX(salary) FROM employees) ORDER BY 1;",
            },
        });
        fireEvent.click(screen.getByRole("button", { name: "Submit" }));

        const submission = within(screen.getByRole("tabpanel", { name: "Nộp bài" }));
        expect(await submission.findByRole("link", { name: "Về danh sách bài" })).toHaveAttribute(
            "href",
            "/contests/sql-cup-live",
        );
    });

    it("chưa đăng ký thì báo cần đăng ký và chỉ đường về sảnh chờ", async () => {
        renderContestWorkspace();

        expect(await screen.findByText("Bạn cần đăng ký contest để xem đề")).toBeInTheDocument();
        expect(screen.getByRole("link", { name: "Về sảnh chờ" })).toHaveAttribute(
            "href",
            "/contests/sql-cup-live",
        );
    });
});
