import { expect, type Page, test } from "@playwright/test";

import { accessTokenOf, attachProblemAndStart, createContest } from "./support/contests";
import { logIn, registerVerifiedLearner } from "./support/learner";
import { grantRole } from "./support/roles";

const PROBLEM_SLUG = "tong-luong-moi-phong-ban";
const JUDGE_TIMEOUT = 60_000;
const STANDINGS_TIMEOUT = 30_000;
const NAVIGATION_TIMEOUT = 90_000;
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

test.describe("thi contest với máy chấm thật", () => {
    test.setTimeout(240_000);

    test("đăng ký, vào thi, nộp sai rồi đúng và thấy mình trên bảng xếp hạng", async ({
        page,
        request,
    }) => {
        const admin = await registerVerifiedLearner(request);
        grantRole(admin.email, "ADMIN");
        const contestSlug = `e2e-cup-${Date.now()}`;
        await createContest(request, await accessTokenOf(request, admin), contestSlug);
        attachProblemAndStart(contestSlug, PROBLEM_SLUG);

        const learner = await registerVerifiedLearner(request);
        const lobbyPath = `/contests/${contestSlug}`;
        await logIn(page, learner, lobbyPath);
        await expect(page.getByText("Đang diễn ra").first()).toBeVisible();
        await page.getByRole("button", { name: "Đăng ký" }).click();
        await expect(page.getByText("Bạn đã đăng ký")).toBeVisible();

        await page.getByRole("link", { name: "Tổng lương mỗi phòng ban" }).click();
        await expect(page).toHaveURL(`${lobbyPath}/problems/A`, { timeout: NAVIGATION_TIMEOUT });
        await expect(page.getByText("A. Tổng lương mỗi phòng ban")).toBeVisible();

        await writeCode(page, WRONG_SQL);
        await page.getByRole("button", { name: "Submit" }).click();
        await expect(page.getByText("Sai kết quả", { exact: true })).toBeVisible({
            timeout: JUDGE_TIMEOUT,
        });
        await expect(page.getByText("Mong đợi 3 dòng, kết quả của bạn có 3 dòng.")).toHaveCount(0);

        await writeCode(page, ACCEPTED_SQL);
        await page.getByRole("button", { name: "Submit" }).click();
        await expect(page.getByText("Đúng", { exact: true })).toBeVisible({
            timeout: JUDGE_TIMEOUT,
        });

        await page.getByRole("link", { name: "Bảng xếp hạng" }).click();
        await expect(page).toHaveURL(`${lobbyPath}/standings`, { timeout: NAVIGATION_TIMEOUT });
        const mine = page.getByRole("region", { name: "Kết quả của bạn" });
        await expect(mine.getByText("1 bài")).toBeVisible({ timeout: STANDINGS_TIMEOUT });
        await expect(page.getByRole("cell", { name: /^A: AC phút \d+, sai 1 lần$/ })).toBeVisible({
            timeout: STANDINGS_TIMEOUT,
        });
    });
});
