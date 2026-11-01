export const PAGE_SIZES = [10, 20, 50] as const;
export const DEFAULT_PAGE_SIZE = 20;
export const DEFAULT_SORT = "updatedAt,desc";
export const FIRST_PAGE = 1;

export interface SortOption {
    value: string;
    label: string;
}

export const PROBLEM_SORTS: readonly SortOption[] = [
    { value: DEFAULT_SORT, label: "Mới sửa nhất" },
    { value: "updatedAt,asc", label: "Cũ nhất" },
    { value: "title,asc", label: "Tên A-Z" },
];

export const DATASET_SORTS: readonly SortOption[] = [
    { value: DEFAULT_SORT, label: "Mới sửa nhất" },
    { value: "updatedAt,asc", label: "Cũ nhất" },
    { value: "name,asc", label: "Tên A-Z" },
];

export function readPage(value: string | null): number {
    const page = Number(value);
    return Number.isInteger(page) && page >= FIRST_PAGE ? page : FIRST_PAGE;
}

export function readPageSize(value: string | null): number {
    const size = Number(value);
    return PAGE_SIZES.some((allowed) => allowed === size) ? size : DEFAULT_PAGE_SIZE;
}

export function readSort(value: string | null, options: readonly SortOption[]): string {
    return options.some((option) => option.value === value) ? (value as string) : DEFAULT_SORT;
}

export function listParams(page: number, size: number, sort: string): Record<string, string> {
    return {
        ...(sort !== DEFAULT_SORT ? { sort } : {}),
        ...(size !== DEFAULT_PAGE_SIZE ? { size: String(size) } : {}),
        ...(page > FIRST_PAGE ? { page: String(page) } : {}),
    };
}
