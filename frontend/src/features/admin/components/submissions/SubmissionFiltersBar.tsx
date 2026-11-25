import { Search } from "lucide-react";

import { NativeSelect } from "@/components/shared/NativeSelect";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { DIALECT_LABELS, type Dialect } from "@/lib/dialect";
import { useDebouncedSearch } from "@/lib/useDebouncedSearch";

import type { SubmissionFilterChange } from "../../hooks/useAdminSubmissions";
import {
    DIALECTS,
    hasActiveSubmissionFilter,
    PENDING_FILTER,
    type ResultFilter,
    SOURCES,
    type SubmissionFilters,
    VERDICT_FILTERS,
} from "../../submissions/search-params";
import type { SubmissionSource } from "../../submissions/types";

const SOURCE_LABELS: Record<SubmissionSource, string> = {
    PRACTICE: "Luyện tập",
    CONTEST: "Contest",
};

interface SubmissionFiltersBarProps {
    filters: SubmissionFilters;
    onChange: (change: SubmissionFilterChange) => void;
    onClear: () => void;
}

export function SubmissionFiltersBar({ filters, onChange, onClear }: SubmissionFiltersBarProps) {
    const username = useDebouncedSearch(filters.username, (value) => onChange({ username: value }));
    const problem = useDebouncedSearch(filters.problem, (value) => onChange({ problem: value }));

    function handleClear() {
        username.clearSearchText();
        problem.clearSearchText();
        onClear();
    }

    return (
        <div className="flex flex-wrap items-center gap-2 border-b px-3.5 py-3">
            <div className="relative min-w-45 flex-1">
                <Search
                    aria-hidden="true"
                    className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground"
                />
                <Input
                    type="search"
                    aria-label="Lọc theo username"
                    placeholder="Username…"
                    value={username.searchText}
                    onChange={(event) => username.changeSearchText(event.target.value)}
                    className="pl-8.5 shadow-none"
                />
            </div>
            <Input
                type="search"
                aria-label="Lọc theo slug bài"
                placeholder="Slug bài…"
                value={problem.searchText}
                onChange={(event) => problem.changeSearchText(event.target.value)}
                className="min-w-40 flex-1 shadow-none"
            />
            <NativeSelect
                aria-label="Lọc theo kết quả"
                value={filters.result ?? ""}
                onChange={(event) =>
                    onChange({ result: (event.target.value || null) as ResultFilter | null })
                }
            >
                <option value="">Mọi kết quả</option>
                {VERDICT_FILTERS.map((verdict) => (
                    <option key={verdict} value={verdict}>
                        {verdict}
                    </option>
                ))}
                <option value={PENDING_FILTER}>Đang chờ chấm</option>
            </NativeSelect>
            <NativeSelect
                aria-label="Lọc theo dialect"
                value={filters.dialect ?? ""}
                onChange={(event) =>
                    onChange({ dialect: (event.target.value || null) as Dialect | null })
                }
            >
                <option value="">Mọi dialect</option>
                {DIALECTS.map((dialect) => (
                    <option key={dialect} value={dialect}>
                        {DIALECT_LABELS[dialect]}
                    </option>
                ))}
            </NativeSelect>
            <NativeSelect
                aria-label="Lọc theo nguồn"
                value={filters.source ?? ""}
                onChange={(event) =>
                    onChange({ source: (event.target.value || null) as SubmissionSource | null })
                }
            >
                <option value="">Mọi nguồn</option>
                {SOURCES.map((source) => (
                    <option key={source} value={source}>
                        {SOURCE_LABELS[source]}
                    </option>
                ))}
            </NativeSelect>
            {hasActiveSubmissionFilter(filters) ? (
                <Button type="button" size="sm" variant="ghost" onClick={handleClear}>
                    Xoá lọc
                </Button>
            ) : null}
        </div>
    );
}
