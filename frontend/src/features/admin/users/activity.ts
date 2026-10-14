import { formatRelativeTime } from "@/lib/format";

import type { AdminUserSummary } from "./types";

const ONLINE_LABEL = "Đang trực tuyến";

export function lastActiveLabel(
    { isOnline, lastActiveAt }: Pick<AdminUserSummary, "isOnline" | "lastActiveAt">,
    now: Date,
): string {
    return isOnline ? ONLINE_LABEL : formatRelativeTime(lastActiveAt, now);
}
