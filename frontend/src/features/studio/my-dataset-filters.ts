import { ROUTES } from "@/lib/routes";

const FIRST_PAGE = 1;
export const MY_DATASETS_PAGE_SIZE = 20;

export interface MyDatasetFilters {
    q: string;
    page: number;
}

function readPage(value: string | null): number {
    const page = Number(value);
    return Number.isInteger(page) && page >= FIRST_PAGE ? page : FIRST_PAGE;
}

export function readMyDatasetFilters(searchParams: Pick<URLSearchParams, "get">): MyDatasetFilters {
    return {
        q: searchParams.get("q")?.trim() ?? "",
        page: readPage(searchParams.get("page")),
    };
}

export function myDatasetsQueryOf({ q, page }: MyDatasetFilters) {
    return { ...(q ? { q } : {}), page, size: MY_DATASETS_PAGE_SIZE };
}

export function myDatasetsHref({ q, page }: MyDatasetFilters): string {
    const query = new URLSearchParams(q ? { q } : {});
    if (page > FIRST_PAGE) {
        query.set("page", String(page));
    }
    const queryString = query.toString();
    return queryString ? `${ROUTES.studioDatasets}?${queryString}` : ROUTES.studioDatasets;
}
