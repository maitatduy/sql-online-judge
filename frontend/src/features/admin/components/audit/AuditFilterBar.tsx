import { X } from "lucide-react";

import { NativeSelect } from "@/components/shared/NativeSelect";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { AUDIT_ACTION_LABELS, AUDIT_ACTIONS } from "@/features/admin/audit/action-labels";
import {
    AUDIT_PERIOD_LABELS,
    AUDIT_PERIODS,
    hasActiveAuditFilter,
    NO_AUDIT_FILTERS,
    type AuditFilters,
    type AuditPeriod,
} from "@/features/admin/audit/search-params";
import type { AuditAction } from "@/features/admin/audit/types";
import type { AuditFilterChange } from "@/features/admin/hooks/useAuditFilters";
import { useDebouncedSearch } from "@/lib/useDebouncedSearch";

const ALL_TIME_OPTION = "all";

interface AuditFilterBarProps {
    filters: AuditFilters;
    onChange: (change: AuditFilterChange) => void;
    onClear: () => void;
}

export function AuditFilterBar({ filters, onChange, onClear }: AuditFilterBarProps) {
    const { searchText, changeSearchText, clearSearchText } = useDebouncedSearch(
        filters.actor ?? "",
        (actor) => onChange({ actor: actor || null }),
    );

    function handleClear() {
        clearSearchText();
        onClear();
    }

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
                <span className="inline-flex h-8 items-center gap-1 rounded-full bg-surface-2 pr-1 pl-3 text-[13px] font-semibold">
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
            <NativeSelect
                aria-label="Loại thao tác"
                className="w-44"
                value={filters.action ?? ""}
                onChange={(event) =>
                    onChange({ action: (event.target.value || null) as AuditAction | null })
                }
            >
                <option value="">Mọi thao tác</option>
                {AUDIT_ACTIONS.map((action) => (
                    <option key={action} value={action}>
                        {AUDIT_ACTION_LABELS[action]}
                    </option>
                ))}
            </NativeSelect>
            <Input
                type="search"
                aria-label="Người thực hiện"
                placeholder="Người thực hiện…"
                value={searchText}
                onChange={(event) => changeSearchText(event.target.value)}
                className="w-40 shadow-none"
            />
            <NativeSelect
                aria-label="Khoảng thời gian"
                className="w-36"
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
                <option value={ALL_TIME_OPTION}>Mọi lúc</option>
                {AUDIT_PERIODS.map((period) => (
                    <option key={period} value={period}>
                        {AUDIT_PERIOD_LABELS[period]}
                    </option>
                ))}
            </NativeSelect>
            {hasActiveAuditFilter(filters) ? (
                <Button type="button" variant="ghost" onClick={handleClear}>
                    Xoá lọc
                </Button>
            ) : null}
        </div>
    );
}
