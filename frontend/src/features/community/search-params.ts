import { ROUTES } from "@/lib/routes";

import { POST_CATEGORIES, POST_TABS } from "./labels";
import type { PostCategory, PostTab } from "./types";

const FIRST_PAGE = 1;

export interface PostFilters {
    category: PostCategory | null;
    tab: PostTab;
    q: string;
    page: number;
}

export const DEFAULT_POST_FILTERS: PostFilters = {
    category: null,
    tab: "HOT",
    q: "",
    page: FIRST_PAGE,
};

function knownValue<T extends string>(values: readonly T[], value: string | null): T | null {
    return values.find((candidate) => candidate === value) ?? null;
}

export function readPostFilters(params: Pick<URLSearchParams, "get">): PostFilters {
    const page = Number(params.get("page"));
    return {
        category: knownValue(POST_CATEGORIES, params.get("category")),
        tab: knownValue(POST_TABS, params.get("tab")) ?? "HOT",
        q: params.get("q")?.trim() ?? "",
        page: Number.isInteger(page) && page >= FIRST_PAGE ? page : FIRST_PAGE,
    };
}

export function communityHref({ category, tab, q, page }: PostFilters): string {
    const query = new URLSearchParams();
    if (category) {
        query.set("category", category);
    }
    if (tab !== "HOT") {
        query.set("tab", tab);
    }
    if (q) {
        query.set("q", q);
    }
    if (page > FIRST_PAGE) {
        query.set("page", String(page));
    }
    const queryString = query.toString();
    return `${ROUTES.community}${queryString ? `?${queryString}` : ""}`;
}
