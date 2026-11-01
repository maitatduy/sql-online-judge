import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type { DatasetTable, StudioDataset, StudioDatasetSummary } from "@/features/studio/types";
import type { Page } from "@/lib/pagination";

import { validationFailed } from "./admin-user-errors";
import { apiError } from "./api-error";
import { readMockSession, SETTER_SESSION_USER } from "./session";
import { SEED_SCHEMAS, SEED_VARIANT_CSVS, SEED_VARIANTS } from "./studio-dataset-fixtures";
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
    usedByProblems: number;
    csvs?: Record<string, string>;
    deletedAt?: string;
}

type DatasetSeed = [
    name: string,
    description: string,
    latestVersion: number,
    hasDraft: boolean,
    hoursAgo: number,
    usedByProblems: number,
];

const SEEDS: DatasetSeed[] = [
    ["Công ty", "Phòng ban và nhân viên kèm lương", 2, true, 0.1, 0],
    ["Trường học", "Lớp, sinh viên và điểm thi", 1, false, 50, 2],
    ["Cửa hàng trực tuyến", "Khách hàng, đơn hàng và sản phẩm", 1, false, 300, 0],
];

function seedDatasets(): StoredDataset[] {
    return SEEDS.map(
        ([name, description, latestVersion, hasDraft, hoursAgo, usedByProblems], index) => {
            const updatedAt = new Date(Date.now() - hoursAgo * HOUR_MS).toISOString();
            const versions = Array.from({ length: latestVersion }, (_, versionIndex) => ({
                number: versionIndex + 1,
                frozenAt: hasDraft && versionIndex + 1 === latestVersion ? null : updatedAt,
            }));
            return {
                id: `0192f3e0-0000-7000-8000-${String(index + 1).padStart(12, "0")}`,
                ownerId: SETTER_SESSION_USER.id,
                usedByProblems,
                slug: slugOf(name),
                name,
                description,
                versions,
                schema: {
                    versionNumber: latestVersion,
                    isDraft: hasDraft,
                    tables: SEED_SCHEMAS[index],
                },
                variants: SEED_VARIANTS[index],
                csvs: SEED_VARIANT_CSVS,
                updatedAt,
            };
        },
    );
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
    return {
        id: dataset.id,
        slug: dataset.slug,
        name: dataset.name,
        description: dataset.description,
        updatedAt: dataset.updatedAt,
    };
}

function editableDraft(dataset: StoredDataset): StoredDataset {
    if (dataset.schema.isDraft) {
        return dataset;
    }
    const number = dataset.schema.versionNumber + 1;
    return {
        ...dataset,
        versions: [...dataset.versions, { number, frozenAt: null }],
        schema: { ...dataset.schema, versionNumber: number, isDraft: true },
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

function sortedDatasets(datasets: StoredDataset[], sort: string): StoredDataset[] {
    if (sort === "name,asc") {
        return [...datasets].sort((first, second) => first.name.localeCompare(second.name, "vi"));
    }
    const newestFirst = [...datasets].sort(
        (first, second) => Date.parse(second.updatedAt) - Date.parse(first.updatedAt),
    );
    return sort === "updatedAt,asc" ? newestFirst.reverse() : newestFirst;
}

function findDataset(datasetId: string, canModerate: boolean): StoredDataset | undefined {
    const user = readMockSession();
    return readStudioDatasets().find(
        (dataset) =>
            dataset.id === datasetId &&
            !dataset.deletedAt &&
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

function csvRowCount(csv: string): number {
    return csv.split("\n").filter((line) => line.trim() !== "").length - 1;
}

const variantNameTaken = () =>
    apiError(409, "DATASET_VARIANT_NAME_TAKEN", "Tên bộ dữ liệu đã được dùng");

const notFound = () => apiError(404, "DATASET_NOT_FOUND", "Không tìm thấy database");

export const studioDatasetHandlers: RequestHandler[] = [
    http.get("*/api/studio/datasets/:datasetId", async ({ params }) => {
        await delay();
        const dataset = findDataset(String(params.datasetId), true);
        return dataset ? HttpResponse.json(toDetail(dataset)) : notFound();
    }),

    http.delete("*/api/studio/datasets/:datasetId", async ({ params }) => {
        await delay();
        const dataset = findDataset(String(params.datasetId), false);
        if (!dataset) {
            return notFound();
        }
        if (dataset.usedByProblems > 0) {
            return apiError(
                409,
                "DATASET_IN_USE",
                `Database đang được ${dataset.usedByProblems} bài dùng nên chưa xoá được`,
            );
        }
        saveDataset({ ...dataset, deletedAt: new Date().toISOString() });
        return new HttpResponse(null, { status: 204 });
    }),

    http.put("*/api/studio/datasets/:datasetId/schema", async ({ params, request }) => {
        await delay();
        const dataset = findDataset(String(params.datasetId), false);
        if (!dataset) {
            return notFound();
        }
        const { tables } = (await request.json()) as { tables: DatasetTable[] };
        const problem = invalidSchemaField(tables);
        if (problem) {
            return validationFailed(problem);
        }
        const draft = editableDraft(dataset);
        const changed: StoredDataset = { ...draft, schema: { ...draft.schema, tables } };
        saveDataset(changed);
        return HttpResponse.json(toDetail(changed));
    }),

    http.post("*/api/studio/datasets/:datasetId/variants", async ({ params, request }) => {
        await delay();
        const dataset = findDataset(String(params.datasetId), false);
        if (!dataset) {
            return notFound();
        }
        const body = (await request.json()) as { name?: string; isPublic?: boolean };
        const name = body.name?.trim() ?? "";
        if (name === "") {
            return validationFailed({ field: "name", message: "Không được để trống" });
        }
        const draft = editableDraft(dataset);
        if (draft.variants.some((variant) => variant.name === name)) {
            return variantNameTaken();
        }
        const changed: StoredDataset = {
            ...draft,
            variants: [
                ...draft.variants,
                { id: crypto.randomUUID(), name, isPublic: body.isPublic ?? false, tables: [] },
            ],
        };
        saveDataset(changed);
        return HttpResponse.json(toDetail(changed), { status: 201 });
    }),

    http.patch(
        "*/api/studio/datasets/:datasetId/variants/:variantId",
        async ({ params, request }) => {
            await delay();
            const dataset = findDataset(String(params.datasetId), false);
            const variantId = String(params.variantId);
            if (!dataset?.variants.some((variant) => variant.id === variantId)) {
                return notFound();
            }
            const change = (await request.json()) as { name?: string; isPublic?: boolean };
            const name = change.name?.trim();
            if (
                dataset.variants.some(
                    (variant) => variant.name === name && variant.id !== variantId,
                )
            ) {
                return variantNameTaken();
            }
            const draft = editableDraft(dataset);
            const changed: StoredDataset = {
                ...draft,
                variants: draft.variants.map((variant) =>
                    variant.id === variantId
                        ? {
                              ...variant,
                              name: name ?? variant.name,
                              isPublic: change.isPublic ?? variant.isPublic,
                          }
                        : variant,
                ),
            };
            saveDataset(changed);
            return HttpResponse.json(toDetail(changed));
        },
    ),

    http.delete("*/api/studio/datasets/:datasetId/variants/:variantId", async ({ params }) => {
        await delay();
        const dataset = findDataset(String(params.datasetId), false);
        const variantId = String(params.variantId);
        if (!dataset?.variants.some((variant) => variant.id === variantId)) {
            return notFound();
        }
        const draft = editableDraft(dataset);
        saveDataset({
            ...draft,
            variants: draft.variants.filter((variant) => variant.id !== variantId),
        });
        return new HttpResponse(null, { status: 204 });
    }),

    http.get(
        "*/api/studio/datasets/:datasetId/variants/:variantId/tables/:tableName",
        async ({ params }) => {
            await delay();
            const dataset = findDataset(String(params.datasetId), true);
            const tableName = String(params.tableName);
            const csv = dataset?.csvs?.[`${String(params.variantId)}/${tableName}`];
            if (csv === undefined) {
                return apiError(
                    404,
                    "DATASET_VARIANT_NOT_FOUND",
                    "Bộ dữ liệu chưa có CSV cho bảng này",
                );
            }
            return HttpResponse.json({ tableName, csv, rowCount: csvRowCount(csv) });
        },
    ),

    http.put(
        "*/api/studio/datasets/:datasetId/variants/:variantId/tables/:tableName",
        async ({ params, request }) => {
            await delay();
            const dataset = findDataset(String(params.datasetId), false);
            const variantId = String(params.variantId);
            const tableName = String(params.tableName);
            const table = dataset?.schema.tables.find((candidate) => candidate.name === tableName);
            if (
                !dataset ||
                !table ||
                !dataset.variants.some((variant) => variant.id === variantId)
            ) {
                return notFound();
            }
            const { csv } = (await request.json()) as { csv: string };
            const header = table.columns.map((column) => column.name).join(",");
            if (csv.split("\n")[0].trim() !== header) {
                return validationFailed({
                    field: "csv",
                    message: `Dòng tiêu đề phải là: ${header}`,
                });
            }
            const rowCount = csvRowCount(csv);
            const draft = editableDraft(dataset);
            const changed: StoredDataset = {
                ...draft,
                csvs: { ...draft.csvs, [`${variantId}/${tableName}`]: csv },
                variants: draft.variants.map((variant) =>
                    variant.id === variantId
                        ? {
                              ...variant,
                              tables: [
                                  ...variant.tables.filter(
                                      (existing) => existing.tableName !== tableName,
                                  ),
                                  { tableName, rowCount },
                              ],
                          }
                        : variant,
                ),
            };
            saveDataset(changed);
            return HttpResponse.json(toDetail(changed));
        },
    ),

    http.patch("*/api/studio/datasets/:datasetId", async ({ params, request }) => {
        await delay();
        const dataset = findDataset(String(params.datasetId), false);
        if (!dataset) {
            return notFound();
        }
        const change = (await request.json()) as { name?: string; description?: string };
        if (change.name !== undefined && change.name.trim() === "") {
            return validationFailed({ field: "name", message: "Không được để trống" });
        }
        const changed: StoredDataset = {
            ...dataset,
            name: change.name?.trim() ?? dataset.name,
            description: change.description?.trim() ?? dataset.description,
            updatedAt: new Date().toISOString(),
        };
        saveDataset(changed);
        return HttpResponse.json(toDetail(changed));
    }),

    http.get("*/api/studio/datasets", async ({ request }) => {
        await delay();
        const user = readMockSession();
        if (!user) {
            return apiError(401, "UNAUTHENTICATED", "Bạn chưa đăng nhập");
        }
        const query = new URL(request.url).searchParams;
        const keyword = folded(query.get("q")?.trim() ?? "");
        const sort = query.get("sort") ?? "updatedAt,desc";
        const page = Number(query.get("page") ?? 1);
        const size = Number(query.get("size") ?? DEFAULT_PAGE_SIZE);
        const matches = sortedDatasets(
            readStudioDatasets().filter(
                (dataset) =>
                    dataset.ownerId === user.id &&
                    !dataset.deletedAt &&
                    folded(dataset.name).includes(keyword),
            ),
            sort,
        );
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
            usedByProblems: 0,
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
