import { fireEvent, render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import type { MonthlySubmissions, SubmissionTotals, VerdictShare } from "../types";
import { SubmissionTotalCard } from "./SubmissionTotalCard";

const ALL_TIME_TOTALS: SubmissionTotals = { allTime: 1_284_530, today: 9807, last7Days: 44_307 };
const ALL_TIME_SHARES: VerdictShare[] = [
    { verdict: "AC", count: 552_348, percent: 43 },
    { verdict: "WA", count: 449_586, percent: 35 },
    { verdict: "OM", count: 64_227, percent: 5 },
    { verdict: "TLE", count: 89_917, percent: 7 },
    { verdict: "RE", count: 102_762, percent: 8 },
    { verdict: "FS", count: 25_690, percent: 2 },
];

const YEAR_SO_FAR: MonthlySubmissions = {
    year: 2026,
    months: [
        { month: 1, count: 86_420 },
        { month: 2, count: 79_310 },
        { month: 3, count: 98_210 },
    ],
};

function renderCard(totals = ALL_TIME_TOTALS, shares = ALL_TIME_SHARES, monthly = YEAR_SO_FAR) {
    render(<SubmissionTotalCard totals={totals} verdictShares={shares} monthly={monthly} />);
}

function donut() {
    return screen.getByRole("img", { name: /^Tổng .+ bài nộp/ });
}

function monthItems() {
    return within(
        screen.getByRole("list", { name: "Số bài nộp theo tháng năm 2026" }),
    ).getAllByRole("listitem");
}

function legendItems() {
    return within(screen.getByRole("list", { name: "Phân bố kết quả chấm" })).getAllByRole(
        "listitem",
    );
}

function fact(name: string) {
    return within(screen.getByRole("group", { name }));
}

describe("SubmissionTotalCard", () => {
    it("có tiêu đề Tổng số bài nộp", () => {
        renderCard();

        expect(
            screen.getByRole("heading", { level: 2, name: "Tổng số bài nộp" }),
        ).toBeInTheDocument();
    });

    it("mô tả biểu đồ tròn bằng tổng số và tỉ lệ từng verdict", () => {
        renderCard();

        expect(
            screen.getByRole("img", {
                name: "Tổng 1.284.530 bài nộp: AC 43%, WA 35%, OM 5%, TLE 7%, RE 8%, FS 2%",
            }),
        ).toBeInTheDocument();
    });

    it("ghi tổng số bài nộp ở giữa biểu đồ tròn", () => {
        renderCard();

        const donut = screen.getByRole("img", { name: /^Tổng 1\.284\.530 bài nộp/ });

        expect(donut).toHaveTextContent("1.284.530");
        expect(donut).toHaveTextContent("bài nộp");
    });

    it("vẽ một cung cho mỗi verdict có bài nộp", () => {
        renderCard();

        expect(screen.getAllByTestId("verdict-arc")).toHaveLength(6);
    });

    it("rê chuột vào một cung thì giữa vòng hiện chi tiết của verdict đó", () => {
        renderCard();

        fireEvent.mouseEnter(screen.getAllByTestId("verdict-arc")[1]);

        expect(donut()).toHaveTextContent("WA");
        expect(donut()).toHaveTextContent("35%");
        expect(donut()).toHaveTextContent("449.586");
        expect(donut()).not.toHaveTextContent("1.284.530");
        expect(donut()).not.toHaveTextContent("Tổng cộng");
    });

    it("rời chuột khỏi cung thì giữa vòng trở lại tổng số", () => {
        renderCard();
        const arc = screen.getAllByTestId("verdict-arc")[1];

        fireEvent.mouseEnter(arc);
        fireEvent.mouseLeave(arc);

        expect(donut()).toHaveTextContent("Tổng cộng");
        expect(donut()).toHaveTextContent("1.284.530");
    });

    it("rê chuột vào một dòng chú giải cũng hiện chi tiết verdict đó giữa vòng", () => {
        renderCard();

        fireEvent.mouseEnter(legendItems()[4]);

        expect(donut()).toHaveTextContent("RE");
        expect(donut()).toHaveTextContent("102.762");

        fireEvent.mouseLeave(legendItems()[4]);

        expect(donut()).toHaveTextContent("1.284.530");
    });

    it("làm nổi cung đang rê chuột và làm mờ các cung còn lại", () => {
        renderCard();
        const arcs = screen.getAllByTestId("verdict-arc");

        fireEvent.mouseEnter(arcs[0]);

        expect(arcs[0]).not.toHaveClass("opacity-30");
        expect(arcs[1]).toHaveClass("opacity-30");
    });

    it("có biểu đồ số bài nộp theo tháng với đủ 12 tháng", () => {
        renderCard();

        expect(
            screen.getByRole("heading", { level: 3, name: "Bài nộp theo tháng năm 2026" }),
        ).toBeInTheDocument();
        expect(monthItems()).toHaveLength(12);
        expect(monthItems()[0]).toHaveTextContent("Tháng 1");
        expect(monthItems()[0]).toHaveTextContent("86.420");
        expect(monthItems()[2]).toHaveTextContent("98.210");
    });

    it("vẽ cột cao theo số bài nộp, tháng cao nhất cao hết khung", () => {
        renderCard();

        const columns = screen.getAllByTestId("month-column");

        expect(columns).toHaveLength(3);
        expect(columns[2]).toHaveStyle({ height: "100%" });
        expect(columns[0]).toHaveStyle({ height: `${(86_420 / 98_210) * 100}%` });
    });

    it("rê chuột vào cột thấy tháng và số bài nộp", () => {
        renderCard();

        expect(monthItems()[2]).toHaveAttribute("title", "Tháng 3 năm 2026: 98.210 bài nộp");
    });

    it("ghi rõ tháng chưa có số liệu", () => {
        renderCard();

        expect(monthItems()[11]).toHaveTextContent("Tháng 12");
        expect(monthItems()[11]).toHaveTextContent("Chưa có số liệu");
    });

    it("không vẽ cung cho verdict không có bài nộp", () => {
        renderCard({ allTime: 200, today: 0, last7Days: 200 }, [
            { verdict: "AC", count: 150, percent: 75 },
            { verdict: "WA", count: 50, percent: 25 },
            { verdict: "FS", count: 0, percent: 0 },
        ]);

        expect(screen.getAllByTestId("verdict-arc")).toHaveLength(2);
    });

    it("nêu số bài nộp hôm nay, 7 ngày qua và tỉ lệ AC", () => {
        renderCard();

        expect(fact("Hôm nay").getByText("9.807")).toBeInTheDocument();
        expect(fact("7 ngày qua").getByText("44.307")).toBeInTheDocument();
        expect(fact("Tỉ lệ AC").getByText("43%")).toBeInTheDocument();
    });

    it.each([
        ["AC", "Đúng", "552.348", "43%"],
        ["WA", "Sai kết quả", "449.586", "35%"],
        ["OM", "Đúng dữ liệu, sai thứ tự", "64.227", "5%"],
        ["TLE", "Quá thời gian", "89.917", "7%"],
        ["RE", "Lỗi cú pháp hoặc thực thi", "102.762", "8%"],
        ["FS", "Dùng lệnh bị cấm", "25.690", "2%"],
    ])("dòng %s ghi giải thích, số bài nộp và phần trăm", (verdict, label, count, percent) => {
        renderCard();

        const row = within(
            legendItems().find((item) => within(item).queryByText(verdict)) as HTMLElement,
        );

        expect(row.getByText(label)).toBeInTheDocument();
        expect(row.getByText(count)).toBeInTheDocument();
        expect(row.getByText(percent)).toBeInTheDocument();
    });

    it("hiện phần trăm lẻ với dấu phẩy thập phân", () => {
        renderCard({ allTime: 10, today: 1, last7Days: 5 }, [
            { verdict: "AC", count: 10, percent: 41.2 },
        ]);

        expect(within(legendItems()[0]).getByText("41,2%")).toBeInTheDocument();
    });

    it("báo trống khi chưa có bài nộp nào", () => {
        renderCard({ allTime: 0, today: 0, last7Days: 0 }, [], { year: 2026, months: [] });

        expect(screen.getByText("Chưa có bài nộp nào")).toBeInTheDocument();
        expect(screen.queryByRole("img")).not.toBeInTheDocument();
        expect(screen.queryByRole("list")).not.toBeInTheDocument();
    });
});
