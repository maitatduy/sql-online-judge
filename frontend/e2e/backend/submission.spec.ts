import { expect, type Page, test } from "@playwright/test";

import { logIn, registerVerifiedLearner } from "./support/learner";

const PROBLEM_PATH = "/problems/tong-luong-moi-phong-ban";
const JUDGE_TIMEOUT = 60_000;
const ACCEPTED_SQL = [
    "SELECT d.name AS department, SUM(e.salary) AS total_salary",
    "FROM departments d",
    "JOIN employees e ON e.department_id = d.id",
    "GROUP BY d.name",
    "ORDER BY d.name;",
].join("\n");
const WRONG_SQL = ACCEPTED_SQL.replace("SUM(e.salary)", "MAX(e.salary)");

async function writeCode(page: Page, sql: string): Promise<void> {
    const editor = page.locator(".monaco-editor").first();
    await expect(editor).toBeVisible({ timeout: 30_000 });
    await editor.click();
    await page.keyboard.press("ControlOrMeta+A");
    await page.keyboard.press("Delete");
    await page.keyboard.insertText(sql);
}

test.describe("nộp bài với máy chấm thật", () => {
    test.setTimeout(180_000);

    test("chạy thử, nộp sai thấy diff, sửa lại rồi được AC", async ({ page, request }) => {
        const learner = await registerVerifiedLearner(request);
        await logIn(page, learner, PROBLEM_PATH);
        await expect(
            page.getByRole("heading", { level: 1, name: "Tổng lương mỗi phòng ban" }),
        ).toBeVisible();

        await writeCode(page, WRONG_SQL);
        await page.getByRole("button", { name: "Run" }).click();
        await expect(page.getByText(/^3 dòng, [\d.]+ ms$/)).toBeVisible({
            timeout: JUDGE_TIMEOUT,
        });

        await page.getByRole("button", { name: "Submit" }).click();
        await expect(page.getByText("Sai kết quả", { exact: true })).toBeVisible({
            timeout: JUDGE_TIMEOUT,
        });
        await expect(page.getByText(/^Sai ở testcase 1\/2/)).toBeVisible();
        await expect(page.getByText("Mong đợi 3 dòng, kết quả của bạn có 3 dòng.")).toBeVisible();

        await writeCode(page, ACCEPTED_SQL);
        await page.getByRole("button", { name: "Submit" }).click();
        await expect(page.getByText("Đúng", { exact: true })).toBeVisible({
            timeout: JUDGE_TIMEOUT,
        });
        await expect(page.getByText(/^2\/2 testcase/)).toBeVisible();
    });
});
