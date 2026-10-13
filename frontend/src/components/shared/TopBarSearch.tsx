"use client";

import { Search } from "lucide-react";
import { useEffect, useRef } from "react";

const FOCUS_SEARCH_KEY = "k";

export function TopBarSearch() {
    const inputRef = useRef<HTMLInputElement>(null);

    useEffect(() => {
        function handleKeyDown(event: KeyboardEvent) {
            const hasModifier = event.ctrlKey || event.metaKey;
            if (hasModifier && event.key.toLowerCase() === FOCUS_SEARCH_KEY) {
                event.preventDefault();
                inputRef.current?.focus();
            }
        }

        window.addEventListener("keydown", handleKeyDown);
        return () => window.removeEventListener("keydown", handleKeyDown);
    }, []);

    return (
        <label className="hidden h-9 w-70 items-center gap-2 rounded-md border bg-surface px-2.5 text-muted-foreground focus-within:ring-2 focus-within:ring-ring lg:flex">
            <Search aria-hidden="true" className="size-4 shrink-0" />
            <input
                ref={inputRef}
                type="search"
                aria-label="Tìm kiếm"
                aria-keyshortcuts="Control+K"
                placeholder="Tìm bài, contest, người dùng…"
                className="min-w-0 flex-1 bg-transparent text-sm text-foreground outline-none placeholder:text-muted-foreground"
            />
            <kbd className="inline-flex h-5 shrink-0 items-center rounded-sm border border-b-2 bg-surface px-1.5 font-mono text-[11px] font-medium">
                Ctrl K
            </kbd>
        </label>
    );
}
