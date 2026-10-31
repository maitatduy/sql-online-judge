import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type { DatasetTable, StudioDataset, StudioDatasetSummary } from "@/features/studio/types";
import type { Page } from "@/lib/pagination";

import { validationFailed } from "./admin-user-errors";
import { apiError } from "./api-error";
import { readMockSession, SETTER_SESSION_USER } from "./session";
import { SEED_SCHEMAS } from "./studio-dataset-fixtures";
import { folded } from "./text";

const STORAGE_KEY = "sqljudge:mock:studio-datasets";
const HOUR_MS = 60 * 60_000;
const DEFAULT_PAGE_SIZE = 20;
const MAX_NAME_LENGTH = 120;
const FIRST_SLUG_SUFFIX = 2;
const IDENTIFIER = /^[a-z_][a-z0-9_]*$/;
const RESERVED_WORDS = new Set(["group", "order", "select", "table", "user", "where"]);

interface StoredDataset extends StudioDataset {
    ownerId: string;
}

type DatasetSeed = [
    name: string,
    description: string,
    latestVersion: number,
    hasDraft: boolean,
    hoursAgo: number,
];

const SEEDS: DatasetSeed[] = [
    ["Công ty", "Phòng ban và nhân viên kèm lương", 2, true, 0.1],
    ["Trường học", "Lớp, sinh viên và điểm thi", 1, false, 50],
    ["Cửa hàng trực tuyến", "Khách hàng, đơn hàng và sản phẩm", 1, false, 300],
];

function seedDatasets(): StoredDataset[] {
    return SEEDS.map(([name, description, latestVersion, hasDraft, hoursAgo], index) => {
        const updatedAt = new Date(Date.now() - hoursAgo * HOUR_MS).toISOString();
        const versions = Array.from({ length: latestVersion }, (_, versionIndex) => ({
            number: versionIndex + 1,
            frozenAt: hasDraft && versionIndex + 1 === latestVersion ? null : updatedAt,
        }));
        return {
            id: `0192f3e0-0000-7000-8000-${String(index + 1).padStart(12, "0")}`,
            ownerId: SETTER_SESSION_USER.id,
            slug: slugOf(name),
            name,
            description,
            versions,
            schema: {
                versionNumber: latestVersion,
                isDraft: hasDraft,
                tables: SEED_SCHEMAS[index],
            },
            variants: [],
            updatedAt,
        };
    });
}

function slugOf(name: string): string {
    return (
        folded(name)
            .replace(/[^a-z0-9]+/g, "-")
            .replace(/^-+|-+$/g, "") || "bai-tap"
    );
}

export function readStudioDatasets(): StoredDataset[] {
    const stored = sessionStorage.getItem(STORAGE_KEY);
    return stored ? (JSON.parse(stored) as StoredDataset[]) : seedDatasets();
}

function writeStudioDatasets(datasets: StoredDataset[]): void {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(datasets));
}

function uniqueSlug(name: string, datasets: StoredDataset[]): string {
    const base = slugOf(name);
    const taken = new Set(datasets.map((dataset) => dataset.slug));
    let slug = base;
    for (let suffix = FIRST_SLUG_SUFFIX; taken.has(slug); suffix += 1) {
        slug = `${base}-${suffix}`;
    }
    return slug;
}

function toSummary(dataset: StoredDataset): StudioDatasetSummary {
    const latest = dataset.versions[dataset.versions.length - 1];
    return {
        id: dataset.id,
        slug: dataset.slug,
        name: dataset.name,
        description: dataset.description,
        latestVersionNumber: latest.number,
        hasDraft: latest.frozenAt === null,
        updatedAt: dataset.updatedAt,
    };
}

function toDetail(dataset: StoredDataset): StudioDataset {
    return {
        id: dataset.id,
        slug: dataset.slug,
        name: dataset.name,
        description: dataset.description,
        versions: dataset.versions,
        schema: dataset.schema,
        variants: dataset.variants,
        updatedAt: dataset.updatedAt,
    };
}

function findDataset(datasetId: string, canModerate: boolean): StoredDataset | undefined {
    const user = readMockSession();
    return readStudioDatasets().find(
        (dataset) =>
            dataset.id === datasetId &&
            (dataset.ownerId === user?.id ||
                user?.role === "ADMIN" ||
                (canModerate && user?.role === "MODERATOR")),
    );
}

function saveDataset(changed: StoredDataset): void {
    writeStudioDatasets(
        readStudioDatasets().map((dataset) => (dataset.id === changed.id ? changed : dataset)),
    );
}

function nameProblem(name: string): string | null {
    if (!IDENTIFIER.test(name)) {
        return "Chỉ gồm chữ thường không dấu, số và dấu gạch dưới, không bắt đầu bằng số";
    }
    return RESERVED_WORDS.has(name) ? "Tên này trùng từ khoá SQL" : null;
}

function invalidSchemaField(tables: DatasetTable[]): { field: string; message: string } | null {
    if (tables.length === 0) {
        return { field: "tables", message: "Cần từ 1 đến 10 bảng" };
    }
    for (const [tableIndex, table] of tables.entries()) {
        const tableProblem = nameProblem(table.name);
        if (tableProblem) {
            return { field: `tables[${tableIndex}].name`, message: tableProblem };
        }
        for (const [columnIndex, column] of table.columns.entries()) {
            const columnProblem = nameProblem(column.name);
            if (columnProblem) {
                return {
                    field: `tables[${tableIndex}].columns[${columnIndex}].name`,
                    message: columnProblem,
                };
            }
        }
    }
    return null;
}

const notFound = () => apiError(404, "DATASET_NOT_FOUND", "Không tìm thấy dataset");

export const studioDatasetHandlers: RequestHandler[] = [
    http.get("*/api/studio/datasets/:datasetId", async ({ params }) => {
        await delay();
        const dataset = findDataset(String(params.datasetId), true);
        return dataset ? HttpResponse.json(toDetail(dataset)) : notFound();
    }),

    http.put("*/api/studio/datasets/:datasetId/schema", async ({ params, request }) => {
        await delay();
        const dataset = findDataset(String(params.datasetId), false);
        if (!dataset) {
            return notFound();
        }
        if (!dataset.schema.isDraft) {
            return apiError(422, "DATASET_VERSION_FROZEN", "Phiên bản mới nhất đã chốt");
        }
        const { tables } = (await request.json()) as { tables: DatasetTable[] };
        const problem = invalidSchemaField(tables);
        if (problem) {
            return validationFailed(problem);
        }
        const changed: StoredDataset = { ...dataset, schema: { ...dataset.schema, tables } };
        saveDataset(changed);
        return HttpResponse.json(toDetail(changed));
    }),

    http.post("*/api/studio/datasets/:datasetId/versions", async ({ params }) => {
        await delay();
        const dataset = findDataset(String(params.datasetId), false);
        if (!dataset) {
            return notFound();
        }
        if (dataset.schema.isDraft) {
            return apiError(422, "DATASET_DRAFT_EXISTS", "Dataset đang có bản nháp");
        }
        const number = dataset.schema.versionNumber + 1;
        const changed: StoredDataset = {
            ...dataset,
            versions: [...dataset.versions, { number, frozenAt: null }],
            schema: { ...dataset.schema, versionNumber: number, isDraft: true },
        };
        saveDataset(changed);
        return HttpResponse.json(toDetail(changed), { status: 201 });
    }),

    http.get("*/api/studio/datasets", async ({ request }) => {
        await delay();
        const user = readMockSession();
        if (!user) {
            return apiError(401, "UNAUTHENTICATED", "Bạn chưa đăng nhập");
        }
        const query = new URL(request.url).searchParams;
        const keyword = folded(query.get("q")?.trim() ?? "");
        const page = Number(query.get("page") ?? 1);
        const size = Number(query.get("size") ?? DEFAULT_PAGE_SIZE);
        const matches = readStudioDatasets()
            .filter((dataset) => dataset.ownerId === user.id)
            .filter((dataset) => folded(dataset.name).includes(keyword))
            .sort((first, second) => Date.parse(second.updatedAt) - Date.parse(first.updatedAt));
        const body: Page<StudioDatasetSummary> = {
            items: matches.slice((page - 1) * size, page * size).map(toSummary),
            page,
            size,
            totalItems: matches.length,
            totalPages: Math.max(1, Math.ceil(matches.length / size)),
        };
        return HttpResponse.json(body);
    }),

    http.post("*/api/studio/datasets", async ({ request }) => {
        await delay();
        const user = readMockSession();
        if (!user) {
            return apiError(401, "UNAUTHENTICATED", "Bạn chưa đăng nhập");
        }
        const body = (await request.json()) as { name?: string; description?: string };
        const name = body.name?.trim() ?? "";
        if (name === "" || name.length > MAX_NAME_LENGTH) {
            return validationFailed({
                field: "name",
                message: name === "" ? "Không được để trống" : "Tối đa 120 ký tự",
            });
        }
        const datasets = readStudioDatasets();
        const created: StoredDataset = {
            id: crypto.randomUUID(),
            ownerId: user.id,
            slug: uniqueSlug(name, datasets),
            name,
            description: body.description?.trim() ?? "",
            versions: [{ number: 1, frozenAt: null }],
            schema: { versionNumber: 1, isDraft: true, tables: [] },
            variants: [],
            updatedAt: new Date().toISOString(),
        };
        writeStudioDatasets([...datasets, created]);
        return HttpResponse.json(toDetail(created), {
            status: 201,
            headers: { Location: `/api/studio/datasets/${created.id}` },
        });
    }),
];
