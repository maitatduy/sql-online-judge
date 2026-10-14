export type UsersSortField = "rating" | "acceptedCount";
export type SortDirection = "asc" | "desc";

export interface UsersSort {
    field: UsersSortField;
    direction: SortDirection;
}

const SORT_FIELDS: UsersSortField[] = ["rating", "acceptedCount"];
const SORT_DIRECTIONS: SortDirection[] = ["asc", "desc"];
const SEPARATOR = ",";

export function parseUsersSort(value: string | null): UsersSort | null {
    const [fieldText, directionText, ...extraParts] = (value ?? "").split(SEPARATOR);
    const field = SORT_FIELDS.find((knownField) => knownField === fieldText);
    const direction = SORT_DIRECTIONS.find((knownDirection) => knownDirection === directionText);
    return field && direction && extraParts.length === 0 ? { field, direction } : null;
}

export function formatUsersSort({ field, direction }: UsersSort): string {
    return `${field}${SEPARATOR}${direction}`;
}

export function nextUsersSort(currentSort: UsersSort | null, field: UsersSortField): UsersSort {
    const isDescending = currentSort?.field === field && currentSort.direction === "desc";
    return { field, direction: isDescending ? "asc" : "desc" };
}
