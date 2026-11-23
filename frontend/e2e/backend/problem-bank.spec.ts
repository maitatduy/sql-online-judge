import { expect, type Page, test } from "@playwright/test";

import { logIn, registerVerifiedLearner } from "./support/learner";
import { grantRole } from "./support/roles";

const PROBLEM_TITLE = "Tổng lương mỗi phòng ban";
const EDITED_TITLE = `${PROBLEM_TITLE} E2E`;
const NAVIGATION_TIMEOUT = 90_000;
const ACTION_TIMEOUT = 45_000;

function problemRow(page: Page, title: string) {
    return page
        .getByRole("table", { name: "Kho bài tập" })
        .getByRole("row")
        .filter({ has: page.getByRole("cell", { name: title, exact: true }) });
}

async function quickEditTitle(page: Page, from: string, to: string): Promise<void> {
    const row = problemRow(page, from);
    await row.getByRole("button", { name: /^Sửa nhanh/ }).click();
    const dialog = page.getByRole("dialog");
    await dialog.getByLabel("Tên bài").fill(to);
    await dialog.getByRole("button", { name: "Lưu" }).click();
    await expect(page.getByText("Đã lưu thay đổi.").first()).toBeVisible({
        timeout: ACTION_TIMEOUT,
    });
    await expect(problemRow(page, to)).toBeVisible({ timeout: ACTION_TIMEOUT });
}

async function confirmRowAction(page: Page, action: RegExp, confirmLabel: string) {
    await problemRow(page, PROBLEM_TITLE).getByRole("button", { name: action }).click();
    await page.getByRole("dialog").getByRole("button", { name: confirmLabel }).click();
}

test.describe("kho bài tập với backend thật", () => {
    test.setTimeout(240_000);

    test("lọc, sửa nhanh, ẩn rồi hiện lại và chấm lại một bài", async ({ page, request }) => {
        const admin = await registerVerifiedLearner(request);
        grantRole(admin.email, "ADMIN");

        await logIn(page, admin, "/admin/problems");
        await expect(page.getByRole("heading", { name: "Kho bài tập" })).toBeVisible({
            timeout: NAVIGATION_TIMEOUT,
        });
        await page.getByLabel("Tìm bài").fill(PROBLEM_TITLE);
        await expect(problemRow(page, PROBLEM_TITLE)).toHaveCount(1, { timeout: ACTION_TIMEOUT });

        await quickEditTitle(page, PROBLEM_TITLE, EDITED_TITLE);
        await quickEditTitle(page, EDITED_TITLE, PROBLEM_TITLE);

        await confirmRowAction(page, /^Ẩn bài/, "Ẩn bài");
        await expect(problemRow(page, PROBLEM_TITLE).getByText("Đang ẩn")).toBeVisible({
            timeout: ACTION_TIMEOUT,
        });
        await confirmRowAction(page, /^Hiện lại bài/, "Hiện lại");
        await expect(problemRow(page, PROBLEM_TITLE).getByText("Công khai")).toBeVisible({
            timeout: ACTION_TIMEOUT,
        });

        await confirmRowAction(page, /^Chấm lại bài/, "Chấm lại");
        await expect(
            page.getByText(/Đang chấm lại [\d.]+ bài nộp\.|Bài này đang được chấm lại/).first(),
        ).toBeVisible({ timeout: ACTION_TIMEOUT });
    });
});
