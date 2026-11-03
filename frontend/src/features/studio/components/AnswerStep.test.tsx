import { fireEvent, screen, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { server } from "@/mocks/server";
import { SETTER_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { ProblemStudio } from "./ProblemStudio";

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

const WITHOUT_DATASET_ID = "0192f3d0-0000-7000-8000-000000000001";
const WITH_DATASET_ID = "0192f3d0-0000-7000-8000-000000000003";
const LOCKED_WITH_DATASET_ID = "0192f3d0-0000-7000-8000-000000000004";
const SAMPLE_VARIANT_ID = "0192f3e1-0000-7000-8000-000000000001";
const HIDDEN_VARIANT_ID = "0192f3e1-0000-7000-8000-000000000002";

function renderAnswerStep(problemId = WITH_DATASET_ID) {
    renderWithProviders(
        <ToastProvider>
            <ProblemStudio problemId={problemId} step="answer" />
        </ToastProvider>,
    );
}

function recordAnswerPuts(): unknown[] {
    const bodies: unknown[] = [];
    server.events.on("request:start", async ({ request }) => {
        if (request.method === "PUT" && request.url.endsWith("/answer")) {
            bodies.push(await request.clone().json());
        }
    });
    return bodies;
}

describe("ProblemStudio - bước Đáp án và testcase", () => {
    beforeEach(() => {
        writeMockSession(SETTER_SESSION_USER);
        server.events.removeAllListeners();
    });

    it("soạn đáp án PostgreSQL, chọn và sắp xếp testcase, cài đặt so khớp rồi lưu", async () => {
        const puts = recordAnswerPuts();
        renderAnswerStep();

        const postgres = await screen.findByRole("textbox", { name: "Đáp án PostgreSQL" });
        expect(screen.getByRole("checkbox", { name: "Thêm đáp án MySQL" })).not.toBeChecked();
        expect(screen.queryByRole("textbox", { name: "Đáp án MySQL" })).not.toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Lưu" })).toBeDisabled();
        const steps = within(screen.getByRole("navigation", { name: "Các bước soạn bài" }));
        expect(steps.getByRole("link", { name: /Đáp án và testcase/ })).toHaveAttribute(
            "aria-current",
            "step",
        );

        fireEvent.change(postgres, { target: { value: "SELECT name FROM departments" } });
        fireEvent.click(screen.getByRole("checkbox", { name: "Có NULL" }));
        fireEvent.click(screen.getByRole("checkbox", { name: "Mẫu" }));
        fireEvent.click(screen.getByRole("button", { name: "Đưa Mẫu lên trước" }));
        fireEvent.click(screen.getByRole("checkbox", { name: "So khớp đúng thứ tự dòng" }));
        expect(screen.getByText("Có thay đổi chưa lưu")).toBeInTheDocument();
        fireEvent.click(screen.getByRole("button", { name: "Lưu" }));

        expect(await screen.findByText(/^Đã lưu \d{2}:\d{2}$/)).toBeInTheDocument();
        expect(puts).toEqual([
            {
                solutions: [{ dialect: "POSTGRESQL", sql: "SELECT name FROM departments" }],
                testcaseVariantIds: [SAMPLE_VARIANT_ID, HIDDEN_VARIANT_ID],
                requiresOrder: true,
                compareColumnNames: false,
                floatTolerance: 0.000001,
            },
        ]);
        const testcases = within(screen.getByRole("list", { name: "Testcase đã chọn" }));
        expect(testcases.getAllByRole("listitem").map((item) => item.textContent)).toEqual([
            expect.stringContaining("Mẫu"),
            expect.stringContaining("Có NULL"),
        ]);
    });

    it("bật thêm đáp án SQLite thì có trình soạn riêng và được gửi kèm khi lưu", async () => {
        const puts = recordAnswerPuts();
        renderAnswerStep();

        fireEvent.change(await screen.findByRole("textbox", { name: "Đáp án PostgreSQL" }), {
            target: { value: "SELECT 1" },
        });
        fireEvent.click(screen.getByRole("checkbox", { name: "Thêm đáp án SQLite" }));
        fireEvent.change(screen.getByRole("textbox", { name: "Đáp án SQLite" }), {
            target: { value: "SELECT 1;" },
        });
        fireEvent.click(screen.getByRole("checkbox", { name: "Mẫu" }));
        fireEvent.click(screen.getByRole("button", { name: "Lưu" }));

        await screen.findByText(/^Đã lưu/);
        expect(puts).toEqual([
            expect.objectContaining({
                solutions: [
                    { dialect: "POSTGRESQL", sql: "SELECT 1" },
                    { dialect: "SQLITE", sql: "SELECT 1;" },
                ],
            }),
        ]);
    });

    it("mở lại đáp án đã lưu với đúng dialect, testcase và cài đặt", async () => {
        server.use(
            http.get("*/api/studio/problems/:problemId/answer", () =>
                HttpResponse.json({
                    solutions: [
                        { dialect: "POSTGRESQL", sql: "SELECT 2" },
                        { dialect: "MYSQL", sql: "SELECT 2" },
                    ],
                    testcases: [
                        { variantId: HIDDEN_VARIANT_ID, variantName: "Có NULL", isPublic: false },
                    ],
                    variants: [
                        { id: SAMPLE_VARIANT_ID, name: "Mẫu", isPublic: true },
                        { id: HIDDEN_VARIANT_ID, name: "Có NULL", isPublic: false },
                    ],
                    requiresOrder: false,
                    compareColumnNames: true,
                    floatTolerance: 0.01,
                }),
            ),
        );
        renderAnswerStep();

        expect(await screen.findByRole("textbox", { name: "Đáp án MySQL" })).toHaveValue(
            "SELECT 2",
        );
        expect(screen.getByRole("checkbox", { name: "Có NULL" })).toBeChecked();
        expect(screen.getByRole("checkbox", { name: "Mẫu" })).not.toBeChecked();
        expect(screen.getByRole("checkbox", { name: "So khớp tên cột" })).toBeChecked();
        expect(screen.getByRole("spinbutton", { name: "Sai số khi so số thực" })).toHaveValue(0.01);
        expect(screen.getByRole("button", { name: "Lưu" })).toBeDisabled();
    });

    it("hiện lỗi máy chủ ngay dưới phần liên quan", async () => {
        server.use(
            http.put("*/api/studio/problems/:problemId/answer", () =>
                HttpResponse.json(
                    {
                        status: 400,
                        code: "VALIDATION_FAILED",
                        message: "Dữ liệu gửi lên không hợp lệ",
                        errors: [
                            {
                                field: "testcaseVariantIds",
                                message: "Bộ dữ liệu không thuộc database của bài",
                            },
                        ],
                    },
                    { status: 400 },
                ),
            ),
        );
        renderAnswerStep();

        fireEvent.change(await screen.findByRole("textbox", { name: "Đáp án PostgreSQL" }), {
            target: { value: "SELECT 1" },
        });
        fireEvent.click(screen.getByRole("checkbox", { name: "Mẫu" }));
        fireEvent.click(screen.getByRole("button", { name: "Lưu" }));

        expect(
            await screen.findByText("Bộ dữ liệu không thuộc database của bài"),
        ).toBeInTheDocument();
    });

    it("bài chưa lưu database thì hướng dẫn quay lại bước Database", async () => {
        renderAnswerStep(WITHOUT_DATASET_ID);

        expect(
            await screen.findByText("Chọn và lưu database cho bài ở bước 3 trước."),
        ).toBeInTheDocument();
        expect(screen.getByRole("link", { name: "Sang bước Database" })).toHaveAttribute(
            "href",
            `/studio/problems/${WITHOUT_DATASET_ID}/database`,
        );
        expect(
            screen.queryByRole("textbox", { name: "Đáp án PostgreSQL" }),
        ).not.toBeInTheDocument();
    });

    it("bài đang chờ duyệt thì chỉ xem, không sửa được", async () => {
        renderAnswerStep(LOCKED_WITH_DATASET_ID);

        await screen.findByRole("textbox", { name: "Đáp án PostgreSQL" });

        expect(screen.getByRole("checkbox", { name: "Mẫu" })).toBeDisabled();
        expect(screen.getByRole("checkbox", { name: "Thêm đáp án MySQL" })).toBeDisabled();
        expect(screen.queryByRole("button", { name: "Lưu" })).not.toBeInTheDocument();
    });
});
