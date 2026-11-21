import { fireEvent, screen } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { server } from "@/mocks/server";
import { SETTER_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { CourseStudio } from "./CourseStudio";

const DRAFT_COURSE_ID = "0192f3f1-0000-7000-8000-000000000001";
const IN_REVIEW_COURSE_ID = "0192f3f1-0000-7000-8000-000000000002";

function renderCourse(courseId: string) {
    renderWithProviders(
        <ToastProvider>
            <CourseStudio courseId={courseId} />
        </ToastProvider>,
    );
}

describe("gửi khoá đi duyệt", () => {
    beforeEach(() => {
        writeMockSession(SETTER_SESSION_USER);
    });

    it("hiện lý do admin trả về, gửi duyệt xong thì khoá chuyển sang chờ duyệt", async () => {
        renderCourse(DRAFT_COURSE_ID);

        expect(await screen.findByText("Thêm ví dụ cho bài DENSE_RANK.")).toBeInTheDocument();
        fireEvent.click(screen.getByRole("button", { name: "Gửi duyệt" }));

        expect(await screen.findByText("Đã gửi khoá đi duyệt.")).toBeInTheDocument();
        expect(await screen.findByText(/Khoá đang chờ admin duyệt/)).toBeInTheDocument();
        expect(screen.queryByRole("button", { name: "Gửi duyệt" })).not.toBeInTheDocument();
        expect(screen.getByRole("textbox", { name: "Tên khoá" })).toBeDisabled();
    });

    it("khoá đang chờ duyệt thì báo đang chờ, không có nút gửi", async () => {
        renderCourse(IN_REVIEW_COURSE_ID);

        expect(await screen.findByText(/Khoá đang chờ admin duyệt/)).toBeInTheDocument();
        expect(screen.queryByRole("button", { name: "Gửi duyệt" })).not.toBeInTheDocument();
    });

    it("khoá chưa có bài giảng thì chưa gửi duyệt được", async () => {
        server.use(
            http.get(`*/api/studio/courses/${DRAFT_COURSE_ID}`, () =>
                HttpResponse.json({
                    id: DRAFT_COURSE_ID,
                    slug: "khoa-trong",
                    status: "DRAFT",
                    revisionId: "0192f3f1-0000-7000-8000-000000000099",
                    revisionStatus: "DRAFT",
                    title: "Khoá trống",
                    summary: "Chưa có bài.",
                    difficulty: "EASY",
                    estimatedMinutes: 0,
                    lessonCount: 0,
                    submittedAt: null,
                    updatedAt: new Date().toISOString(),
                    latestReview: null,
                }),
            ),
        );
        renderCourse(DRAFT_COURSE_ID);

        expect(await screen.findByRole("button", { name: "Gửi duyệt" })).toBeDisabled();
        expect(screen.getByText("Thêm ít nhất một bài giảng để gửi duyệt.")).toBeInTheDocument();
    });
});
