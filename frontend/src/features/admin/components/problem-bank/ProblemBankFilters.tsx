import { Search } from "lucide-react";

import { NativeSelect } from "@/components/shared/NativeSelect";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { DIFFICULTIES, DIFFICULTY_LABELS, type Difficulty } from "@/lib/difficulty";
import { useDebouncedSearch } from "@/lib/useDebouncedSearch";

import { useBankTags } from "../../hooks/useProblemBank";
import type { BankFilterChange } from "../../hooks/useProblemBankFilters";
import { VISIBILITIES, VISIBILITY_LABELS } from "../../problem-bank/labels";
import { hasActiveBankFilter, type ProblemBankFilters } from "../../problem-bank/search-params";
import type { ProblemVisibility } from "../../problem-reviews/types";

interface ProblemBankFiltersBarProps {
    filters: ProblemBankFilters;
    onChange: (change: BankFilterChange) => void;
    onClear: () => void;
}

export function ProblemBankFiltersBar({ filters, onChange, onClear }: ProblemBankFiltersBarProps) {
    const { data: tags } = useBankTags();
    const { searchText, changeSearchText, clearSearchText } = useDebouncedSearch(filters.q, (q) =>
        onChange({ q }),
    );

    function handleClear() {
        clearSearchText();
        onClear();
    }

    return (
        <div className="flex flex-wrap items-center gap-2 border-b px-3.5 py-3">
            <div className="relative min-w-55 flex-1">
                <Search
                    aria-hidden="true"
                    className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground"
                />
                <Input
                    type="search"
                    aria-label="Tìm bài"
                    placeholder="Số bài hoặc tên bài…"
                    value={searchText}
                    onChange={(event) => changeSearchText(event.target.value)}
                    className="pl-8.5 shadow-none"
                />
            </div>
            <NativeSelect
                aria-label="Lọc theo phạm vi hiển thị"
                value={filters.visibility ?? ""}
                onChange={(event) =>
                    onChange({
                        visibility: (event.target.value || null) as ProblemVisibility | null,
                    })
                }
            >
                <option value="">Mọi phạm vi</option>
                {VISIBILITIES.map((visibility) => (
                    <option key={visibility} value={visibility}>
                        {VISIBILITY_LABELS[visibility]}
                    </option>
                ))}
            </NativeSelect>
            <NativeSelect
                aria-label="Lọc theo chủ đề"
                value={filters.tag ?? ""}
                onChange={(event) => onChange({ tag: event.target.value || null })}
            >
                <option value="">Mọi chủ đề</option>
                {(tags?.items ?? []).map((tag) => (
                    <option key={tag.slug} value={tag.slug}>
                        {tag.name}
                    </option>
                ))}
            </NativeSelect>
            <NativeSelect
                aria-label="Lọc theo độ khó"
                value={filters.difficulty ?? ""}
                onChange={(event) =>
                    onChange({ difficulty: (event.target.value || null) as Difficulty | null })
                }
            >
                <option value="">Mọi độ khó</option>
                {DIFFICULTIES.map((difficulty) => (
                    <option key={difficulty} value={difficulty}>
                        {DIFFICULTY_LABELS[difficulty]}
                    </option>
                ))}
            </NativeSelect>
            {hasActiveBankFilter(filters) ? (
                <Button type="button" variant="ghost" onClick={handleClear}>
                    Xoá lọc
                </Button>
            ) : null}
        </div>
    );
}
