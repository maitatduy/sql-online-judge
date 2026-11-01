import { fireEvent, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { server } from "@/mocks/server";
import { SETTER_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { ProblemStudio } from "./ProblemStudio";

const DRAFT_ID = "0192f3d0-0000-7000-8000-000000000001";
const WITH_DATASET_ID = "0192f3d0-0000-7000-8000-000000000003";
const PENDING_ID = "0192f3d0-0000-7000-8000-000000000004";
const STORE_DATASET_ID = "0192f3e0-0000-7000-8000-000000000003";

function renderDatasetStep(problemId = DRAFT_ID) {
    renderWithProviders(
        <ToastProvider>
            <ProblemStudio problemId={problemId} step="database" />
        </ToastProvider>,
    );
}

function recordDatasetPuts(): unknown[] {
    const bodies: unknown[] = [];
    server.events.on("request:start", async ({ request }) => {
        if (request.method === "PUT" && request.url.endsWith("/dataset")) {
            bodies.push(await request.clone().json());
        }
    });
    return bodies;
}

async function chooseDatabase(name: string) {
    const select = await screen.findByRole("combobox", { name: "Database của bài" });
    await within(select).findByRole("option", { name });
    fireEvent.change(select, {
        target: { value: within(select).getByRole("option", { name }).getAttribute("value") },
    });
}

describe("ProblemStudio - bước Database", () => {
    beforeEach(() => {
        writeMockSession(SETTER_SESSION_USER);
        server.events.removeAllListeners();
    });

    it("bài chưa có database thì chưa chọn gì và chưa lưu được", async () => {
        renderDatasetStep();

        const select = await screen.findByRole("combobox", { name: "Database của bài" });
        expect(select).toHaveValue("");
        expect(await within(select).findByRole("option", { name: "Trường học" })).toBeVisible();
        expect(screen.getByRole("button", { name: "Lưu" })).toBeDisabled();
        const steps = within(screen.getByRole("navigation", { name: "Các bước soạn bài" }));
        expect(steps.getByRole("link", { name: /Database/ })).toHaveAttribute(
            "aria-current",
            "step",
        );
    });

    it("chọn database thì xem trước schema, bấm Lưu thì gửi lên và báo đã lưu", async () => {
        const puts = recordDatasetPuts();
        renderDatasetStep();

        await chooseDatabase("Cửa hàng trực tuyến");

        const preview = within(await screen.findByRole("region", { name: "Schema của database" }));
        expect(await preview.findByText("customers")).toBeInTheDocument();
        expect(preview.getByText("orders")).toBeInTheDocument();
        expect(preview.getByText("customer_id")).toBeInTheDocument();
        expect(screen.getByText("Có thay đổi chưa lưu")).toBeInTheDocument();
        fireEvent.click(screen.getByRole("button", { name: "Lưu" }));

        expect(await screen.findByText(/^Đã lưu \d{2}:\d{2}$/)).toBeInTheDocument();
        expect(puts).toEqual([{ datasetId: STORE_DATASET_ID }]);
        expect(
            screen.getByRole("link", { name: "Mở database Cửa hàng trực tuyến" }),
        ).toHaveAttribute("href", `/studio/databases/${STORE_DATASET_ID}`);
    });

    it("database chưa đủ dữ liệu thì báo lý do ngay dưới ô chọn", async () => {
        renderDatasetStep();

        await chooseDatabase("Trường học");
        fireEvent.click(screen.getByRole("button", { name: "Lưu" }));

        expect(await screen.findByRole("alert")).toHaveTextContent(
            "Cần ít nhất một bộ dữ liệu công khai",
        );
    });

    it("bài đã chọn database thì mở sẵn database đó và cho lưu lại để dùng bản mới", async () => {
        renderDatasetStep(WITH_DATASET_ID);

        const select = await screen.findByRole("combobox", { name: "Database của bài" });
        await within(select).findByRole("option", { name: "Công ty" });
        expect(within(select).getByRole("option", { name: "Công ty" })).toHaveProperty(
            "selected",
            true,
        );
        expect(await screen.findByText("departments")).toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Lưu" })).toBeEnabled();
    });

    it("bài đang chờ duyệt thì không đổi được database", async () => {
        renderDatasetStep(PENDING_ID);

        expect(await screen.findByRole("combobox", { name: "Database của bài" })).toBeDisabled();
        expect(screen.queryByRole("button", { name: "Lưu" })).not.toBeInTheDocument();
    });
});
