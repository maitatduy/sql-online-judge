import { formatRelativeTime } from "@/lib/format";

import type { AdminUserSummary } from "./types";

const ONLINE_LABEL = "Đang trực tuyến";
const NEVER_SIGNED_IN_LABEL = "Chưa đăng nhập lần nào";

export function lastActiveLabel(
    { isOnline, lastActiveAt }: Pick<AdminUserSummary, "isOnline" | "lastActiveAt">,
    now: Date,
): string {
    if (isOnline) {
        return ONLINE_LABEL;
    }
    return lastActiveAt ? formatRelativeTime(lastActiveAt, now) : NEVER_SIGNED_IN_LABEL;
}
