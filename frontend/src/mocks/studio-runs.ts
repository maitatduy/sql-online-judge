import { delay, http, HttpResponse, type RequestHandler } from "msw";

import { parseCsv } from "@/features/studio/csv";
import type { CreateStudioRunRequest, StudioRun, VariantRunResult } from "@/features/studio/types";

import { validationFailed } from "./admin-user-errors";
import { apiError } from "./api-error";
import { readStudioDatasets } from "./studio-datasets";
import { findAccessibleStudioProblem } from "./studio-problems";

const FORBIDDEN_STATEMENT = /\b(drop|delete|update|insert|create|alter|truncate)\b/i;
const MISSING_TABLE = "khong_ton_tai";
const MOCK_RUNTIME_MS = 4;

interface MockRun {
    run: StudioRun;
    problemId: string;
    datasetId: string;
    polls: number;
}

const runs = new Map<string, MockRun>();

function variantResultsOf(datasetId: string, sql: string): VariantRunResult[] {
    const dataset = readStudioDatasets().find((candidate) => candidate.id === datasetId);
    if (!dataset) {
        return [];
    }
    const firstTable = dataset.schema.tables[0]?.name ?? "";
    return dataset.variants.map((variant) => {
        const base = {
            variantId: variant.id,
            variantName: variant.name,
            isPublic: variant.isPublic,
            runtimeMs: MOCK_RUNTIME_MS,
        };
        if (sql.includes(MISSING_TABLE)) {
            return {
                ...base,
                status: "FAILED",
                columns: [],
                rows: [],
                rowCount: 0,
                truncated: false,
                message: `relation "${MISSING_TABLE}" does not exist`,
            };
        }
        const [header = [], ...records] = parseCsv(
            dataset.csvs?.[`${variant.id}/${firstTable}`] ?? "",
        );
        const rows = records.map((record) => record.map((value) => (value === "" ? null : value)));
        return {
            ...base,
            status: "SUCCEEDED",
            columns: header,
            rows,
            rowCount: rows.length,
            truncated: false,
            message: null,
        };
    });
}

function completed(mockRun: MockRun): StudioRun {
    const { run, datasetId } = mockRun;
    const isForbidden = FORBIDDEN_STATEMENT.test(run.sql);
    return {
        ...run,
        status: "COMPLETED",
        completedAt: new Date().toISOString(),
        rejection: isForbidden
            ? { verdict: "FS", message: "Bài này chỉ cho phép SELECT, WITH" }
            : null,
        variants: isForbidden ? [] : variantResultsOf(datasetId, run.sql),
    };
}

export const studioRunHandlers: RequestHandler[] = [
    http.post("*/api/studio/problems/:problemId/runs", async ({ params, request }) => {
        await delay();
        const problemId = String(params.problemId);
        const problem = findAccessibleStudioProblem(problemId);
        if (!problem) {
            return apiError(404, "PROBLEM_NOT_FOUND", "Không tìm thấy bài tập");
        }
        const body = (await request.json()) as CreateStudioRunRequest;
        if (!body.sql?.trim()) {
            return validationFailed({ field: "sql", message: "Không được để trống" });
        }
        if (!problem.datasetId) {
            return apiError(422, "PROBLEM_DATASET_MISSING", "Bài chưa chọn database");
        }
        const run: StudioRun = {
            id: crypto.randomUUID(),
            dialect: body.dialect,
            status: "PENDING",
            sql: body.sql,
            createdAt: new Date().toISOString(),
            completedAt: null,
            rejection: null,
            systemError: null,
            variants: [],
        };
        runs.set(run.id, { run, problemId, datasetId: problem.datasetId, polls: 0 });
        return HttpResponse.json(run, {
            status: 202,
            headers: { Location: `/api/studio/problems/${problemId}/runs/${run.id}` },
        });
    }),

    http.get("*/api/studio/problems/:problemId/runs/:runId", async ({ params }) => {
        await delay();
        const mockRun = runs.get(String(params.runId));
        if (!mockRun || mockRun.problemId !== String(params.problemId)) {
            return apiError(404, "STUDIO_RUN_NOT_FOUND", "Không tìm thấy lần chạy thử");
        }
        if (mockRun.polls === 0) {
            mockRun.polls += 1;
            return HttpResponse.json(mockRun.run);
        }
        return HttpResponse.json(completed(mockRun));
    }),
];
