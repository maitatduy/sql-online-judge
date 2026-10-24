import { http, HttpResponse } from "msw";
import { afterEach, describe, expect, it } from "vitest";

import { server } from "@/mocks/server";

import { apiClient, ApiRequestError } from "./api-client";
import { clearAccessToken, getAccessToken, setAccessToken } from "./auth-session";

const EXPORT_PATH = "/api/admin/user-exports";
const CSV_CONTENT = "username,role\nviet.luu,SETTER\n";

function csvResponse(contentDisposition?: string) {
    const headers: Record<string, string> = { "Content-Type": "text/csv; charset=utf-8" };
    if (contentDisposition) {
        headers["Content-Disposition"] = contentDisposition;
    }
    return new HttpResponse(CSV_CONTENT, { status: 200, headers });
}

describe("apiClient.postForFile", () => {
    afterEach(() => {
        clearAccessToken();
    });

    it("trả blob và tên file lấy từ Content-Disposition, gửi body dạng JSON", async () => {
        const captured: { body?: unknown; contentType?: string | null } = {};
        server.use(
            http.post(`*${EXPORT_PATH}`, async ({ request }) => {
                captured.body = await request.json();
                captured.contentType = request.headers.get("Content-Type");
                return csvResponse('attachment; filename="users-20261020-0930.csv"');
            }),
        );

        const file = await apiClient.postForFile(EXPORT_PATH, { scope: "ALL" });

        expect(file.fileName).toBe("users-20261020-0930.csv");
        expect(file.blob.type).toContain("text/csv");
        expect(await file.blob.text()).toBe(CSV_CONTENT);
        expect(captured).toEqual({ body: { scope: "ALL" }, contentType: "application/json" });
    });

    it("ưu tiên filename* mã hoá UTF-8", async () => {
        server.use(
            http.post(`*${EXPORT_PATH}`, () =>
                csvResponse(
                    "attachment; filename=\"nguoi-dung.csv\"; filename*=UTF-8''ng%C6%B0%E1%BB%9Di-d%C3%B9ng.csv",
                ),
            ),
        );

        const file = await apiClient.postForFile(EXPORT_PATH, {});

        expect(file.fileName).toBe("người-dùng.csv");
    });

    it("trả tên file null khi thiếu Content-Disposition", async () => {
        server.use(http.post(`*${EXPORT_PATH}`, () => csvResponse()));

        const file = await apiClient.postForFile(EXPORT_PATH, {});

        expect(file.fileName).toBeNull();
    });

    it("chuyển ApiError thành ApiRequestError như các request JSON", async () => {
        server.use(
            http.post(`*${EXPORT_PATH}`, () =>
                HttpResponse.json(
                    {
                        status: 400,
                        code: "VALIDATION_FAILED",
                        message: "Dữ liệu gửi lên không hợp lệ",
                        errors: [{ field: "reason", message: "Không được để trống" }],
                    },
                    { status: 400 },
                ),
            ),
        );

        const error = await apiClient.postForFile(EXPORT_PATH, {}).catch((caught) => caught);

        expect(error).toBeInstanceOf(ApiRequestError);
        expect(error).toMatchObject({
            status: 400,
            code: "VALIDATION_FAILED",
            errors: [{ field: "reason", message: "Không được để trống" }],
        });
    });

    it("gặp 401 thì refresh rồi tải lại file với token mới", async () => {
        setAccessToken("expired-token");
        server.use(
            http.post(`*${EXPORT_PATH}`, ({ request }) =>
                request.headers.get("Authorization") === "Bearer fresh-token"
                    ? csvResponse('attachment; filename="users.csv"')
                    : HttpResponse.json(
                          { status: 401, code: "UNAUTHORIZED", message: "Bạn cần đăng nhập" },
                          { status: 401 },
                      ),
            ),
            http.post("*/api/auth/refresh", () =>
                HttpResponse.json({ accessToken: "fresh-token", user: {} }),
            ),
        );

        const file = await apiClient.postForFile(EXPORT_PATH, {});

        expect(file.fileName).toBe("users.csv");
        expect(getAccessToken()).toBe("fresh-token");
    });
});
