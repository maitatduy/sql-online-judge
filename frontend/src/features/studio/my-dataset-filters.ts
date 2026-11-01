import { ROUTES } from "@/lib/routes";

import { DATASET_SORTS, listParams, readPage, readPageSize, readSort } from "./list-options";

export type DatasetVersionFilter = "DRAFT" | "FROZEN";

export const DATASET_VERSION_FILTER_LABELS: Record<DatasetVersionFilter, string> = {
    DRAFT: "Đang có bản nháp",
    FROZEN: "Đã chốt hết",
};

export interface MyDatasetFilters {
    status: DatasetVersionFilter | null;
    q: string;
    sort: string;
    size: number;
    page: number;
}

function readStatus(value: string | null): DatasetVersionFilter | null {
    return value === "DRAFT" || value === "FROZEN" ? value : null;
}

export function readMyDatasetFilters(searchParams: Pick<URLSearchParams, "get">): MyDatasetFilters {
    return {
        status: readStatus(searchParams.get("status")),
        q: searchParams.get("q")?.trim() ?? "",
        sort: readSort(searchParams.get("sort"), DATASET_SORTS),
        size: readPageSize(searchParams.get("size")),
        page: readPage(searchParams.get("page")),
    };
}

function activeFilterParams({ status, q }: MyDatasetFilters) {
    return { ...(status ? { status } : {}), ...(q ? { q } : {}) };
}

export function myDatasetsQueryOf(filters: MyDatasetFilters) {
    return {
        ...activeFilterParams(filters),
        sort: filters.sort,
        page: filters.page,
        size: filters.size,
    };
}

export function myDatasetsHref(filters: MyDatasetFilters): string {
    const query = new URLSearchParams({
        ...activeFilterParams(filters),
        ...listParams(filters.page, filters.size, filters.sort),
    });
    const queryString = query.toString();
    return queryString ? `${ROUTES.studioDatasets}?${queryString}` : ROUTES.studioDatasets;
}
