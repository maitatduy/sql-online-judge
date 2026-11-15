import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type { LessonRun } from "@/features/learning/types";

import { apiError } from "./api-error";
import { readMockSession } from "./session";

const runs = new Map<string, LessonRun>();

function completedRun(id: string, sql: string): LessonRun {
    if (/^\s*(drop|delete|update|insert)\b/i.test(sql)) {
        return {
            id,
            status: "COMPLETED",
            outcome: "FS",
            result: null,
            errorMessage: "Bài này chỉ cho phép SELECT, WITH",
            runtimeMs: 0,
        };
    }
    if (/khong_co/i.test(sql)) {
        return {
            id,
            status: "COMPLETED",
            outcome: "RE",
            result: null,
            errorMessage: 'relation "khong_co" does not exist',
            runtimeMs: 3,
        };
    }
    return {
        id,
        status: "COMPLETED",
        outcome: "OK",
        result: {
            columns: ["id", "name"],
            rows: [
                ["1", "Kinh doanh"],
                ["2", "Kỹ thuật"],
            ],
            isTruncated: false,
        },
        errorMessage: null,
        runtimeMs: 12,
    };
}

export const learningRunHandlers: RequestHandler[] = [
    http.post("*/api/lesson-cells/:cellKey/runs", async ({ request }) => {
        await delay(100);
        if (!readMockSession()) {
            return apiError(401, "UNAUTHENTICATED", "Bạn chưa đăng nhập");
        }
        const { sql } = (await request.json()) as { sql: string };
        const id = crypto.randomUUID();
        runs.set(id, completedRun(id, sql));
        return HttpResponse.json({ runId: id }, { status: 202 });
    }),
    http.get("*/api/lesson-runs/:runId", async ({ params }) => {
        await delay(100);
        const run = runs.get(String(params.runId));
        if (!run) {
            return apiError(404, "LESSON_RUN_NOT_FOUND", "Không tìm thấy lần chạy");
        }
        return HttpResponse.json(run);
    }),
];
