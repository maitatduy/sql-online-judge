import { formatNumber } from "@/lib/format";

const MAX_PAGE_BUTTONS = 5;

export interface PageInfo {
    page: number;
    size: number;
    totalItems: number;
    totalPages: number;
}

export interface Page<T> extends PageInfo {
    items: T[];
}

export function pageWindow(page: number, totalPages: number): number[] {
    const buttonCount = Math.min(MAX_PAGE_BUTTONS, totalPages);
    const lastStart = totalPages - buttonCount + 1;
    const firstPage = Math.max(1, Math.min(page - Math.floor(MAX_PAGE_BUTTONS / 2), lastStart));
    return Array.from({ length: buttonCount }, (_, index) => firstPage + index);
}

export function pageRangeLabel(
    { page, size, totalItems }: Pick<PageInfo, "page" | "size" | "totalItems">,
    unit?: string,
): string {
    if (totalItems === 0) {
        return `0 ${unit ?? "kết quả"}`;
    }
    const firstRow = (page - 1) * size + 1;
    const lastRow = Math.min(page * size, totalItems);
    const range = `${formatNumber(firstRow)}-${formatNumber(lastRow)} trong ${formatNumber(totalItems)}`;
    return unit ? `${range} ${unit}` : range;
}
