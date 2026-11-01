import { ROUTES } from "@/lib/routes";

import { DATASET_SORTS, listParams, readPage, readPageSize, readSort } from "./list-options";

export interface MyDatasetFilters {
    q: string;
    sort: string;
    size: number;
    page: number;
}

export function readMyDatasetFilters(searchParams: Pick<URLSearchParams, "get">): MyDatasetFilters {
    return {
        q: searchParams.get("q")?.trim() ?? "",
        sort: readSort(searchParams.get("sort"), DATASET_SORTS),
        size: readPageSize(searchParams.get("size")),
        page: readPage(searchParams.get("page")),
    };
}

function activeFilterParams({ q }: MyDatasetFilters): Record<string, string> {
    return q ? { q } : {};
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
