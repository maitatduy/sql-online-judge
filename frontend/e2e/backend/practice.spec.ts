import { expect, type Page, type Response, test } from "@playwright/test";

const API_URL = process.env.E2E_API_URL ?? "http://localhost:8080";
const PROBLEM_PATH = "/problems/highest-salary-per-department";

function backendResponse(page: Page, path: string): Promise<Response> {
    return page.waitForResponse(
        (response) =>
            response.url().startsWith(`${API_URL}${path}`) && !response.fromServiceWorker(),
    );
}

test.describe("luyện tập với backend thật", () => {
    test("danh sách bài lấy từ problem-service và lọc được theo độ khó", async ({ page }) => {
        const problems = backendResponse(page, "/api/problems");
        await page.goto("/problems");

        expect((await problems).status()).toBe(200);
        const row = page.getByRole("row", { name: /Lương cao nhất mỗi phòng ban/ });
        await expect(row).toBeVisible();
        await expect(row).toContainText("41,2%");

        const filtered = backendResponse(page, "/api/problems?difficulty=HARD");
        await page.getByLabel("Lọc theo độ khó").selectOption("HARD");

        expect((await filtered).status()).toBe(200);
        await expect(
            page.getByRole("link", { name: "Khoảng trống trong lịch đặt phòng" }),
        ).toBeVisible();
        await expect(row).toBeHidden();
    });

    test("mở bài thấy đề, ví dụ, schema và mã khởi đầu từ backend", async ({ page }) => {
        const detail = backendResponse(page, "/api/problems/highest-salary-per-department");
        await page.goto(PROBLEM_PATH);

        expect((await detail).status()).toBe(200);
        await expect(
            page.getByRole("heading", { level: 1, name: "Lương cao nhất mỗi phòng ban" }),
        ).toBeVisible();
        await expect(
            page.getByRole("table", { name: "Ví dụ kết quả mong đợi" }).getByRole("row"),
        ).toHaveCount(5);
        await expect(page.getByText("Giới hạn thời gian 2 giây · 2 testcase ẩn.")).toBeVisible();

        await page.getByRole("tab", { name: "Schema" }).click();
        await expect(page.getByRole("table", { name: "employees" })).toBeVisible();

        await expect(page.locator(".monaco-editor .view-lines")).toContainText("FROM employees", {
            timeout: 30_000,
        });
    });

    test("bài không tồn tại hiện trang không tìm thấy", async ({ page }) => {
        const detail = backendResponse(page, "/api/problems/khong-co-bai-nay");
        await page.goto("/problems/khong-co-bai-nay");

        expect((await detail).status()).toBe(404);
        await expect(page.getByRole("heading", { name: "Không tìm thấy trang" })).toBeVisible();
    });
});
