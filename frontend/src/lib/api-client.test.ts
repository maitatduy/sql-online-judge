import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";

import { server } from "@/mocks/server";

import { apiClient, ApiRequestError } from "./api-client";
import { clearAccessToken, setAccessToken } from "./auth-session";

async function captureError(request: Promise<unknown>): Promise<ApiRequestError> {
    try {
        await request;
    } catch (error) {
        if (error instanceof ApiRequestError) {
            return error;
        }
        throw error;
    }
    throw new Error("Request was expected to fail");
}

describe("apiClient", () => {
    it("get trả về JSON và gửi kèm query string", async () => {
        server.use(
            http.get("*/api/problems", ({ request }) => {
                const url = new URL(request.url);
                return HttpResponse.json({ topic: url.searchParams.get("topic") });
            }),
        );

        const result = await apiClient.get<{ topic: string }>("/api/problems", {
            params: { topic: "join", page: undefined },
        });

        expect(result).toEqual({ topic: "join" });
    });

    it("post gửi body dạng JSON", async () => {
        server.use(
            http.post("*/api/echo", async ({ request }) => {
                return HttpResponse.json({
                    contentType: request.headers.get("Content-Type"),
                    body: await request.json(),
                });
            }),
        );

        const result = await apiClient.post("/api/echo", { email: "a@b.vn" });

        expect(result).toEqual({
            contentType: "application/json",
            body: { email: "a@b.vn" },
        });
    });

    it("patch gửi body dạng JSON bằng phương thức PATCH", async () => {
        server.use(
            http.patch("*/api/admin/users/viet.luu", async ({ request }) =>
                HttpResponse.json({ method: request.method, body: await request.json() }),
            ),
        );

        await expect(
            apiClient.patch("/api/admin/users/viet.luu", { role: "SETTER" }),
        ).resolves.toEqual({ method: "PATCH", body: { role: "SETTER" } });
    });

    it("delete gửi body dạng JSON bằng phương thức DELETE", async () => {
        server.use(
            http.delete("*/api/admin/users/viet.luu", async ({ request }) =>
                HttpResponse.json({ method: request.method, body: await request.json() }),
            ),
        );

        await expect(
            apiClient.delete("/api/admin/users/viet.luu", { reason: "Trùng" }),
        ).resolves.toEqual({ method: "DELETE", body: { reason: "Trùng" } });
    });

    it("putForm gửi FormData bằng phương thức PUT, không ép kiểu JSON", async () => {
        server.use(
            http.put("*/api/admin/users/viet.luu/avatar", async ({ request }) => {
                const form = await request.formData();
                return HttpResponse.json({
                    method: request.method,
                    contentType: request.headers.get("Content-Type")?.split(";")[0],
                    fileType: (form.get("file") as File).type,
                });
            }),
        );
        const form = new FormData();
        form.append("file", new File(["png"], "avatar.png", { type: "image/png" }));

        await expect(apiClient.putForm("/api/admin/users/viet.luu/avatar", form)).resolves.toEqual({
            method: "PUT",
            contentType: "multipart/form-data",
            fileType: "image/png",
        });
    });

    it("trả undefined khi response là 204", async () => {
        server.use(http.post("*/api/logout", () => new HttpResponse(null, { status: 204 })));

        await expect(apiClient.post("/api/logout")).resolves.toBeUndefined();
    });

    it("gắn header Bearer khi đã có access token, không gắn khi chưa đăng nhập", async () => {
        const authorizations: (string | null)[] = [];
        server.use(
            http.get("*/api/auth/me", ({ request }) => {
                authorizations.push(request.headers.get("Authorization"));
                return HttpResponse.json({});
            }),
        );

        await apiClient.get("/api/auth/me");
        setAccessToken("access-token");
        await apiClient.get("/api/auth/me");
        clearAccessToken();

        expect(authorizations).toEqual([null, "Bearer access-token"]);
    });

    it("trả undefined khi response thành công không có body, ví dụ 202", async () => {
        server.use(http.post("*/api/jobs", () => new HttpResponse(null, { status: 202 })));

        await expect(apiClient.post("/api/jobs")).resolves.toBeUndefined();
    });

    it("chuyển ApiError thành ApiRequestError có status, code, message, errors", async () => {
        server.use(
            http.post("*/api/auth/register", () =>
                HttpResponse.json(
                    {
                        status: 400,
                        code: "VALIDATION_FAILED",
                        message: "Dữ liệu gửi lên không hợp lệ",
                        errors: [{ field: "email", message: "Email không hợp lệ" }],
                    },
                    { status: 400 },
                ),
            ),
        );

        const error = await captureError(apiClient.post("/api/auth/register", {}));

        expect(error.status).toBe(400);
        expect(error.code).toBe("VALIDATION_FAILED");
        expect(error.message).toBe("Dữ liệu gửi lên không hợp lệ");
        expect(error.errors).toEqual([{ field: "email", message: "Email không hợp lệ" }]);
    });

    it("giữ traceId của ApiError để hiển thị cho người dùng", async () => {
        server.use(
            http.get("*/api/problems", () =>
                HttpResponse.json(
                    {
                        status: 500,
                        code: "INTERNAL_ERROR",
                        message: "Đã có lỗi xảy ra, vui lòng thử lại sau",
                        traceId: "4bf92f3577b34da6",
                    },
                    { status: 500 },
                ),
            ),
        );

        const error = await captureError(apiClient.get("/api/problems"));

        expect(error.traceId).toBe("4bf92f3577b34da6");
    });

    it("đọc số giây chờ từ header Retry-After khi bị 429", async () => {
        server.use(
            http.post("*/api/auth/login", () =>
                HttpResponse.json(
                    { status: 429, code: "LOGIN_RATE_LIMITED", message: "Thử lại sau" },
                    { status: 429, headers: { "Retry-After": "30" } },
                ),
            ),
        );

        const error = await captureError(apiClient.post("/api/auth/login", {}));

        expect(error.status).toBe(429);
        expect(error.retryAfterSeconds).toBe(30);
    });

    it("dùng mã lỗi chung khi response lỗi không phải ApiError", async () => {
        server.use(
            http.get("*/api/broken", () => new HttpResponse("<html>502</html>", { status: 502 })),
        );

        const error = await captureError(apiClient.get("/api/broken"));

        expect(error.status).toBe(502);
        expect(error.code).toBe("UNEXPECTED_ERROR");
        expect(error.errors).toEqual([]);
    });

    it("báo NETWORK_ERROR khi không kết nối được", async () => {
        server.use(http.get("*/api/offline", () => HttpResponse.error()));

        const error = await captureError(apiClient.get("/api/offline"));

        expect(error.status).toBe(0);
        expect(error.code).toBe("NETWORK_ERROR");
    });
});
