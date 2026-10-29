import { fireEvent, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { Workspace } from "./Workspace";

vi.mock("next/navigation", () => ({ notFound: vi.fn() }));

vi.mock("@/components/shared/LazySqlEditor", () => ({
    LazySqlEditor: ({ value, ariaLabel }: { value: string; ariaLabel: string }) => (
        <textarea aria-label={ariaLabel} value={value} readOnly />
    ),
}));

const SLUG = "highest-salary-per-department";

async function renderWorkspace() {
    renderWithProviders(<Workspace slug={SLUG} />);
    await screen.findByRole("textbox", { name: "Mã SQL của bạn" });
    return within(screen.getByRole("tablist", { name: "Khu vực làm bài" }));
}

describe("chuyển khu vực làm bài trên màn hẹp", () => {
    beforeEach(() => {
        localStorage.clear();
        writeMockSession(DEMO_SESSION_USER);
    });

    it("mở đề bài trước, rồi chuyển sang editor khi chọn Code", async () => {
        const areas = await renderWorkspace();

        expect(areas.getByRole("tab", { name: "Đề" })).toHaveAttribute("aria-selected", "true");

        fireEvent.click(areas.getByRole("tab", { name: "Code" }));

        expect(areas.getByRole("tab", { name: "Code" })).toHaveAttribute("aria-selected", "true");
        expect(areas.getByRole("tab", { name: "Đề" })).toHaveAttribute("aria-selected", "false");
    });

    it("chuyển sang Kết quả khi bấm Run", async () => {
        const areas = await renderWorkspace();

        fireEvent.click(screen.getByRole("button", { name: "Run" }));

        expect(areas.getByRole("tab", { name: "Kết quả" })).toHaveAttribute(
            "aria-selected",
            "true",
        );
    });

    it("gắn verdict của lần nộp gần nhất vào tab Kết quả", async () => {
        const areas = await renderWorkspace();

        fireEvent.click(screen.getByRole("button", { name: "Submit" }));

        expect(await areas.findByRole("tab", { name: "Kết quả WA" })).toHaveAttribute(
            "aria-selected",
            "true",
        );
    });
});
