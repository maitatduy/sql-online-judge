import { Search, X } from "lucide-react";

import { NativeSelect } from "@/components/shared/NativeSelect";
import { Button } from "@/components/ui/button";
import {
    AUDIT_CATEGORIES,
    AUDIT_CATEGORY_LABELS,
    UPCOMING_AUDIT_CATEGORY_LABELS,
} from "@/features/admin/audit/action-labels";
import {
    AUDIT_PERIOD_LABELS,
    AUDIT_PERIODS,
    NO_AUDIT_FILTERS,
    type AuditFilters,
    type AuditPeriod,
} from "@/features/admin/audit/search-params";
import type { AuditCategory } from "@/features/admin/audit/types";
import { useAuditActors } from "@/features/admin/hooks/useAuditActors";
import type { AuditFilterChange } from "@/features/admin/hooks/useAuditFilters";
import { cn } from "@/lib/utils";
import { useDebouncedSearch } from "@/lib/useDebouncedSearch";

const ALL_TIME_OPTION = "all";
const SELECT_CLASS = "h-8.5 shadow-none";

interface AuditFilterBarProps {
    filters: AuditFilters;
    onChange: (change: AuditFilterChange) => void;
}

function actorOptionsOf(usernames: string[], selected: string | null): string[] {
    return selected && !usernames.includes(selected) ? [selected, ...usernames] : usernames;
}

export function AuditFilterBar({ filters, onChange }: AuditFilterBarProps) {
    const { data: actors } = useAuditActors();
    const { searchText, changeSearchText } = useDebouncedSearch(filters.q ?? "", (q) =>
        onChange({ q: q || null }),
    );

    function handleUserCleared() {
        onChange({ user: null, period: filters.period ?? NO_AUDIT_FILTERS.period });
    }

    return (
        <div
            role="search"
            aria-label="Bộ lọc nhật ký"
            className="flex flex-wrap items-center gap-2 rounded-lg border bg-card px-3.5 py-3"
        >
            {filters.user ? (
                <span className="inline-flex h-8.5 items-center gap-1 rounded-full bg-surface-2 pr-1 pl-3 text-[13px] font-semibold">
                    Đối tượng: {filters.user}
                    <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        aria-label="Bỏ lọc đối tượng"
                        className="size-6 rounded-full"
                        onClick={handleUserCleared}
                    >
                        <X aria-hidden="true" />
                    </Button>
                </span>
            ) : null}
            <label className="flex h-8.5 min-w-0 flex-[1_1_220px] items-center gap-2 rounded-md border px-2.5 text-muted-foreground focus-within:border-ring focus-within:ring-[3px] focus-within:ring-ring/50">
                <Search aria-hidden="true" className="size-4 shrink-0" />
                <input
                    type="search"
                    aria-label="Tìm trong nhật ký"
                    placeholder="Người thực hiện, đối tượng, lý do…"
                    value={searchText}
                    onChange={(event) => changeSearchText(event.target.value)}
                    className="min-w-0 flex-1 bg-transparent text-sm text-foreground outline-none"
                />
            </label>
            <NativeSelect
                aria-label="Loại thao tác"
                className={cn("w-[170px]", SELECT_CLASS)}
                value={filters.category ?? ""}
                onChange={(event) =>
                    onChange({ category: (event.target.value || null) as AuditCategory | null })
                }
            >
                <option value="">Mọi thao tác</option>
                {AUDIT_CATEGORIES.map((category) => (
                    <option key={category} value={category}>
                        {AUDIT_CATEGORY_LABELS[category]}
                    </option>
                ))}
                {UPCOMING_AUDIT_CATEGORY_LABELS.map((label) => (
                    <option key={label} disabled>
                        {label} (sắp có)
                    </option>
                ))}
            </NativeSelect>
            <NativeSelect
                aria-label="Người thực hiện"
                className={cn("w-40", SELECT_CLASS)}
                value={filters.actor ?? ""}
                onChange={(event) => onChange({ actor: event.target.value || null })}
            >
                <option value="">Mọi người</option>
                {actorOptionsOf(actors?.usernames ?? [], filters.actor).map((username) => (
                    <option key={username} value={username}>
                        {username}
                    </option>
                ))}
            </NativeSelect>
            <NativeSelect
                aria-label="Khoảng thời gian"
                className={cn("w-[140px]", SELECT_CLASS)}
                value={filters.period ?? ALL_TIME_OPTION}
                onChange={(event) =>
                    onChange({
                        period:
                            event.target.value === ALL_TIME_OPTION
                                ? null
                                : (event.target.value as AuditPeriod),
                    })
                }
            >
                {AUDIT_PERIODS.map((period) => (
                    <option key={period} value={period}>
                        {AUDIT_PERIOD_LABELS[period]}
                    </option>
                ))}
                <option value={ALL_TIME_OPTION}>Mọi lúc</option>
            </NativeSelect>
        </div>
    );
}
