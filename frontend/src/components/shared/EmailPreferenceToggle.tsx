"use client";

import { Mail } from "lucide-react";

import { useNotificationPreferences } from "@/lib/notifications";

export function EmailPreferenceToggle() {
    const { query, update } = useNotificationPreferences(true);

    if (!query.data) {
        return null;
    }
    const isEnabled = update.isPending
        ? Boolean(update.variables?.isEmailEnabled)
        : query.data.isEmailEnabled;

    return (
        <label className="flex items-center gap-3 rounded-lg border bg-card px-4 py-3 text-[15px]">
            <Mail aria-hidden="true" className="size-5 shrink-0 text-muted-foreground" />
            <span className="flex-1">
                Gửi email khi bài được duyệt, rating thay đổi hoặc nội dung bị gỡ
            </span>
            <input
                type="checkbox"
                role="switch"
                checked={isEnabled}
                disabled={update.isPending}
                onChange={(event) => update.mutate({ isEmailEnabled: event.target.checked })}
                className="size-5 accent-brand"
            />
        </label>
    );
}
