import { fireEvent, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { SETTER_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { CourseStudio } from "./CourseStudio";

const PUBLISHED_COURSE_ID = "0192f3f1-0000-7000-8000-000000000003";

function renderCourse() {
    renderWithProviders(
        <ToastProvider>
            <CourseStudio courseId={PUBLISHED_COURSE_ID} />
        </ToastProvider>,
    );
}

describe("sửa khoá đã xuất bản", () => {
    beforeEach(() => {
        writeMockSession(SETTER_SESSION_USER);
    });

    it("khoá đã xuất bản chỉ xem được cho tới khi tạo bản sửa", async () => {
        renderCourse();

        expect(await screen.findByRole("textbox", { name: "Tên khoá" })).toBeDisabled();
        expect(screen.getByRole("button", { name: "Sửa khoá" })).toBeInTheDocument();
        expect(screen.queryByRole("button", { name: "Gửi duyệt" })).not.toBeInTheDocument();
    });

    it("tạo bản sửa thì mở khoá soạn và báo học viên vẫn học bản cũ", async () => {
        renderCourse();

        fireEvent.click(await screen.findByRole("button", { name: "Sửa khoá" }));

        expect(await screen.findByText("Đã tạo bản sửa của khoá.")).toBeInTheDocument();
        expect(await screen.findByRole("textbox", { name: "Tên khoá" })).toBeEnabled();
        const panel = within(screen.getByRole("region", { name: "Gửi duyệt" }));
        expect(
            panel.getByText(
                "Đây là bản sửa. Học viên vẫn học bản đang xuất bản cho tới khi bản này được duyệt.",
            ),
        ).toBeInTheDocument();
        expect(screen.queryByRole("button", { name: "Sửa khoá" })).not.toBeInTheDocument();
    });
});
