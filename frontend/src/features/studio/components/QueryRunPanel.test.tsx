import { fireEvent, screen, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { server } from "@/mocks/server";
import { SETTER_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { ProblemStudio } from "./ProblemStudio";

const WITHOUT_DATASET_ID = "0192f3d0-0000-7000-8000-000000000001";
const WITH_DATASET_ID = "0192f3d0-0000-7000-8000-000000000003";
const RESULT_TIMEOUT = { timeout: 4000 };

function renderDatabaseStep(problemId = WITH_DATASET_ID) {
    renderWithProviders(
        <ToastProvider>
            <ProblemStudio problemId={problemId} step="database" />
        </ToastProvider>,
    );
}

async function runQuery(sql: string, dialect = "SQLITE") {
    const panel = within(await screen.findByRole("region", { name: "Chạy thử SQL" }));
    fireEvent.change(panel.getByRole("combobox", { name: "Dialect" }), {
        target: { value: dialect },
    });
    fireEvent.change(panel.getByRole("textbox", { name: "Câu lệnh SQL" }), {
        target: { value: sql },
    });
    fireEvent.click(panel.getByRole("button", { name: "Chạy thử" }));
    return panel;
}

describe("ProblemStudio - chạy thử SQL", () => {
    beforeEach(() => {
        writeMockSession(SETTER_SESSION_USER);
        server.events.removeAllListeners();
    });

    it("chạy trên mọi bộ dữ liệu và hiện bảng kết quả của từng bộ", async () => {
        const bodies: unknown[] = [];
        server.events.on("request:start", async ({ request }) => {
            if (request.method === "POST" && request.url.endsWith("/runs")) {
                bodies.push(await request.clone().json());
            }
        });
        renderDatabaseStep();

        const panel = await runQuery("SELECT * FROM departments");

        expect(await panel.findByText("Đang chạy…")).toBeInTheDocument();
        const sample = within(
            await panel.findByRole("region", { name: "Bộ dữ liệu Mẫu" }, RESULT_TIMEOUT),
        );
        expect(sample.getByText("Công khai")).toBeInTheDocument();
        expect(sample.getByRole("table", { name: "Kết quả bộ Mẫu" })).toBeInTheDocument();
        expect(sample.getByRole("columnheader", { name: "name" })).toBeInTheDocument();
        expect(sample.getByRole("cell", { name: "Kinh doanh" })).toBeInTheDocument();
        expect(sample.getByText("3 dòng")).toBeInTheDocument();
        const hidden = within(panel.getByRole("region", { name: "Bộ dữ liệu Có NULL" }));
        expect(hidden.getByText("Ẩn")).toBeInTheDocument();
        expect(hidden.getByRole("cell", { name: "Pháp chế" })).toBeInTheDocument();
        expect(bodies).toEqual([{ dialect: "SQLITE", sql: "SELECT * FROM departments" }]);
    });

    it("câu lệnh bị chặn thì báo lý do, không hiện bảng", async () => {
        renderDatabaseStep();

        const panel = await runQuery("DROP TABLE departments");

        expect(await panel.findByRole("alert", {}, RESULT_TIMEOUT)).toHaveTextContent(
            "FS: Bài này chỉ cho phép SELECT, WITH",
        );
        expect(panel.queryByRole("table")).not.toBeInTheDocument();
    });

    it("lỗi SQL được báo riêng cho từng bộ dữ liệu", async () => {
        renderDatabaseStep();

        const panel = await runQuery("SELECT * FROM khong_ton_tai");

        const sample = within(
            await panel.findByRole("region", { name: "Bộ dữ liệu Mẫu" }, RESULT_TIMEOUT),
        );
        expect(sample.getByText('relation "khong_ton_tai" does not exist')).toBeInTheDocument();
    });

    it("kết quả bị cắt thì báo đang hiện bao nhiêu trong tổng số dòng", async () => {
        server.use(
            http.get("*/api/studio/problems/:problemId/runs/:runId", ({ params }) =>
                HttpResponse.json({
                    id: params.runId,
                    dialect: "POSTGRESQL",
                    status: "COMPLETED",
                    sql: "SELECT * FROM big",
                    createdAt: "2026-11-02T08:00:00Z",
                    completedAt: "2026-11-02T08:00:01Z",
                    rejection: null,
                    systemError: null,
                    variants: [
                        {
                            variantId: "v1",
                            variantName: "Lớn",
                            isPublic: false,
                            status: "SUCCEEDED",
                            columns: ["id"],
                            rows: [["1"], [null]],
                            rowCount: 15000,
                            truncated: true,
                            runtimeMs: 12,
                            message: null,
                        },
                    ],
                }),
            ),
        );
        renderDatabaseStep();

        const panel = await runQuery("SELECT * FROM big", "POSTGRESQL");

        const big = within(
            await panel.findByRole("region", { name: "Bộ dữ liệu Lớn" }, RESULT_TIMEOUT),
        );
        expect(big.getByText("Hiển thị 2 trong 15.000 dòng")).toBeInTheDocument();
        expect(big.getByRole("cell", { name: "NULL" })).toBeInTheDocument();
    });

    it("bài chưa lưu database thì chưa chạy thử được", async () => {
        renderDatabaseStep(WITHOUT_DATASET_ID);

        const panel = within(await screen.findByRole("region", { name: "Chạy thử SQL" }));

        expect(panel.getByText("Lưu database cho bài trước khi chạy thử.")).toBeInTheDocument();
        expect(panel.getByRole("button", { name: "Chạy thử" })).toBeDisabled();
    });
});
