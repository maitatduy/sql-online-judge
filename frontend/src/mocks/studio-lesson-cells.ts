import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type { CellRequest, StudioCell } from "@/features/course-studio/types";

import { apiError } from "./api-error";
import { lessonAccess } from "./studio-course-outline";
import { readStudioDatasets } from "./studio-datasets";

const STORAGE_KEY = "sqljudge:mock:studio-lesson-cells";

type CellStore = Record<string, StudioCell[]>;

const SEEDED_CELLS: CellStore = {
    "0192f3f3-0000-7000-8000-000000000001": [
        {
            id: "0192f3f4-0000-7000-8000-000000000001",
            position: 1,
            dialect: "POSTGRESQL",
            starterSql: "SELECT * FROM customers;",
            datasetVariantId: "0192f3e1-0000-7000-8000-000000000003",
        },
    ],
};

function readCells(): CellStore {
    const stored = sessionStorage.getItem(STORAGE_KEY);
    return stored ? (JSON.parse(stored) as CellStore) : SEEDED_CELLS;
}

function writeCells(lessonId: string, cells: StudioCell[]): void {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify({ ...readCells(), [lessonId]: cells }));
}

function lessonOfCell(cellId: string): string | undefined {
    return Object.entries(readCells()).find(([, cells]) =>
        cells.some((cell) => cell.id === cellId),
    )?.[0];
}

function cellNotFound(): Response {
    return apiError(404, "LESSON_CELL_NOT_FOUND", "Không tìm thấy ô SQL");
}

function datasetProblem(variantId: string): Response | null {
    const dataset = readStudioDatasets().find((item) =>
        item.variants.some((variant) => variant.id === variantId),
    );
    const variant = dataset?.variants.find((item) => item.id === variantId);
    if (!dataset || !variant) {
        return apiError(422, "DATASET_VARIANT_NOT_FOUND", "Không tìm thấy bộ dữ liệu");
    }
    if (!variant.isPublic) {
        return apiError(
            422,
            "DATASET_VARIANT_NOT_PUBLIC",
            "Chỉ gắn được bộ dữ liệu công khai vào bài giảng",
        );
    }
    return dataset.schema.isDraft
        ? apiError(
              422,
              "DATASET_NOT_FROZEN",
              "Hãy chốt phiên bản database trước khi gắn vào bài giảng",
          )
        : null;
}

export const studioLessonCellHandlers: RequestHandler[] = [
    http.get("*/api/studio/lessons/:lessonId/cells", async ({ params }) => {
        await delay();
        const lessonId = String(params.lessonId);
        const denied = lessonAccess(lessonId, false);
        return denied ?? HttpResponse.json(readCells()[lessonId] ?? []);
    }),
    http.post("*/api/studio/lessons/:lessonId/cells", async ({ params, request }) => {
        await delay();
        const lessonId = String(params.lessonId);
        const body = (await request.json()) as CellRequest;
        const denied = lessonAccess(lessonId, true) ?? datasetProblem(body.datasetVariantId);
        if (denied) {
            return denied;
        }
        const cells = readCells()[lessonId] ?? [];
        const cell: StudioCell = {
            ...body,
            id: crypto.randomUUID(),
            position: (cells.at(-1)?.position ?? 0) + 1,
        };
        writeCells(lessonId, [...cells, cell]);
        return HttpResponse.json(cell, { status: 201 });
    }),
    http.patch("*/api/studio/cells/:cellId", async ({ params, request }) => {
        await delay();
        const cellId = String(params.cellId);
        const lessonId = lessonOfCell(cellId);
        if (!lessonId) {
            return cellNotFound();
        }
        const body = (await request.json()) as CellRequest;
        const denied = lessonAccess(lessonId, true) ?? datasetProblem(body.datasetVariantId);
        if (denied) {
            return denied;
        }
        const cells = (readCells()[lessonId] ?? []).map((cell) =>
            cell.id === cellId ? { ...cell, ...body } : cell,
        );
        writeCells(lessonId, cells);
        return HttpResponse.json(cells.find((cell) => cell.id === cellId));
    }),
    http.delete("*/api/studio/cells/:cellId", async ({ params }) => {
        await delay();
        const cellId = String(params.cellId);
        const lessonId = lessonOfCell(cellId);
        if (!lessonId) {
            return cellNotFound();
        }
        const denied = lessonAccess(lessonId, true);
        if (denied) {
            return denied;
        }
        writeCells(
            lessonId,
            (readCells()[lessonId] ?? []).filter((cell) => cell.id !== cellId),
        );
        return new HttpResponse(null, { status: 204 });
    }),
];
