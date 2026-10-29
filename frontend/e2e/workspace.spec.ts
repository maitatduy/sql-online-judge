import { expect, test } from "@playwright/test";

import { fillLoginForm, MOCK_LEARNER } from "./support/auth";

const PROBLEM_PATH = "/problems/highest-salary-per-department";

test.describe("làm bài trong Workspace", () => {
    test.beforeEach(async ({ page }) => {
        await page.goto("/login");
        await fillLoginForm(page, MOCK_LEARNER.email, MOCK_LEARNER.password);
        await expect(page).toHaveURL("/");
    });

    test("đọc đề, chạy thử rồi nộp bài và thấy verdict", async ({ page }, testInfo) => {
        const isNarrow = testInfo.project.name === "mobile";
        await page.goto(PROBLEM_PATH);

        await expect(
            page.getByRole("heading", { level: 1, name: "Lương cao nhất mỗi phòng ban" }),
        ).toBeVisible();
        const editor = page.getByRole("region", { name: "Editor" });
        if (isNarrow) {
            await expect(editor).toBeHidden();
            await page.getByRole("tab", { name: "Code" }).click();
        }
        await expect(editor).toBeVisible();
        await expect(page.locator(".monaco-editor").first()).toBeVisible({ timeout: 30_000 });

        await page.getByRole("button", { name: "Run" }).click();
        await expect(page.getByText("4 dòng · 41 ms")).toBeVisible();

        await page.getByRole("button", { name: "Submit" }).click();
        await expect(page.getByText("Sai ở testcase 2/3 · 48 ms")).toBeVisible();
        if (isNarrow) {
            await expect(page.getByRole("tab", { name: "Kết quả WA" })).toHaveAttribute(
                "aria-selected",
                "true",
            );
        }
    });
});
