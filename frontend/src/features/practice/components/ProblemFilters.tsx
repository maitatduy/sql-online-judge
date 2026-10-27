"use client";

import { Search } from "lucide-react";

import { NativeSelect } from "@/components/shared/NativeSelect";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { DIFFICULTIES, DIFFICULTY_LABELS, type Difficulty } from "@/lib/difficulty";
import { useDebouncedSearch } from "@/lib/useDebouncedSearch";

import type { ProblemFilterChange } from "../hooks/useProblemFilters";
import { useTags } from "../hooks/useTags";
import {
    hasActiveProblemFilter,
    PROBLEM_PROGRESSES,
    PROGRESS_LABELS,
    type ProblemFilters as Filters,
} from "../problem-filters";
import type { ProblemProgress } from "../types";

interface ProblemFiltersProps {
    filters: Filters;
    showProgress: boolean;
    onChange: (change: ProblemFilterChange) => void;
    onClear: () => void;
}

export function ProblemFilters({ filters, showProgress, onChange, onClear }: ProblemFiltersProps) {
    const { data: tags } = useTags();
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
                    aria-label="Tìm bài tập"
                    placeholder="Tìm theo tên bài hoặc số…"
                    value={searchText}
                    onChange={(event) => changeSearchText(event.target.value)}
                    className="pl-8.5 shadow-none"
                />
            </div>
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
            <NativeSelect
                aria-label="Lọc theo chủ đề"
                value={filters.tag ?? ""}
                onChange={(event) => onChange({ tag: event.target.value || null })}
            >
                <option value="">Mọi chủ đề</option>
                {tags?.items.map((tag) => (
                    <option key={tag.slug} value={tag.slug}>
                        {tag.name}
                    </option>
                ))}
            </NativeSelect>
            {showProgress ? (
                <NativeSelect
                    aria-label="Lọc theo tiến độ"
                    value={filters.progress ?? ""}
                    onChange={(event) =>
                        onChange({
                            progress: (event.target.value || null) as ProblemProgress | null,
                        })
                    }
                >
                    <option value="">Mọi tiến độ</option>
                    {PROBLEM_PROGRESSES.map((progress) => (
                        <option key={progress} value={progress}>
                            {PROGRESS_LABELS[progress]}
                        </option>
                    ))}
                </NativeSelect>
            ) : null}
            {hasActiveProblemFilter(filters) ? (
                <Button type="button" variant="ghost" onClick={handleClear}>
                    Xoá lọc
                </Button>
            ) : null}
        </div>
    );
}
