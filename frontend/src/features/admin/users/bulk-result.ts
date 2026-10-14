import { formatNumber } from "@/lib/format";

import type { BulkUserActionResponse } from "./types";

export function accountCountLabel(count: number): string {
    return `${formatNumber(count)} tài khoản`;
}

export function affectedSummary({
    affectedCount,
    skippedUsernames,
}: BulkUserActionResponse): string {
    const affected = accountCountLabel(affectedCount);
    return skippedUsernames.length > 0
        ? `${affected}, bỏ qua ${accountCountLabel(skippedUsernames.length)}`
        : affected;
}
