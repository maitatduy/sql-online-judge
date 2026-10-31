"use client";

import { Search } from "lucide-react";

export function TopBarSearch() {
    return (
        <label className="hidden h-9 w-70 items-center gap-2 rounded-md border bg-surface px-2.5 text-muted-foreground focus-within:ring-2 focus-within:ring-ring lg:flex">
            <Search aria-hidden="true" className="size-4 shrink-0" />
            <input
                type="search"
                aria-label="Tìm kiếm"
                placeholder="Tìm bài, contest, người dùng…"
                className="min-w-0 flex-1 bg-transparent text-sm text-foreground outline-none placeholder:text-muted-foreground"
            />
        </label>
    );
}
