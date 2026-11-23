"use client";

import { X } from "lucide-react";
import { useId, useState } from "react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useDebouncedValue } from "@/lib/useDebouncedValue";

import { useProblemOptions } from "../hooks/useCommunity";
import type { PickedProblem } from "../types";

const SEARCH_DELAY_MS = 300;

interface ProblemPickerProps {
    value: PickedProblem | null;
    onChange: (problem: PickedProblem | null) => void;
}

export function ProblemPicker({ value, onChange }: ProblemPickerProps) {
    const inputId = useId();
    const [query, setQuery] = useState("");
    const [isOpen, setIsOpen] = useState(false);
    const debouncedQuery = useDebouncedValue(query, SEARCH_DELAY_MS);
    const { data } = useProblemOptions(debouncedQuery);

    if (value) {
        return (
            <div className="flex items-center gap-2">
                <span className="rounded-md bg-brand-soft px-3 py-1.5 text-[15px] font-semibold text-brand">
                    {value.title}
                </span>
                <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    aria-label="Bỏ gắn bài tập"
                    onClick={() => onChange(null)}
                >
                    <X aria-hidden="true" />
                </Button>
            </div>
        );
    }

    return (
        <div className="relative">
            <label htmlFor={inputId} className="sr-only">
                Tìm bài tập
            </label>
            <Input
                id={inputId}
                role="combobox"
                aria-expanded={isOpen}
                aria-controls={`${inputId}-options`}
                placeholder="Tìm theo tên hoặc số bài"
                value={query}
                onFocus={() => setIsOpen(true)}
                onBlur={() => setIsOpen(false)}
                onChange={(event) => {
                    setQuery(event.target.value);
                    setIsOpen(true);
                }}
            />
            {isOpen && data && data.items.length > 0 ? (
                <ul
                    id={`${inputId}-options`}
                    role="listbox"
                    aria-label="Bài tập"
                    className="absolute z-10 mt-1 w-full overflow-hidden rounded-md border bg-card shadow-md"
                >
                    {data.items.map((problem) => (
                        <li
                            key={problem.id}
                            role="option"
                            aria-selected={false}
                            onMouseDown={(event) => {
                                event.preventDefault();
                                onChange(problem);
                                setQuery("");
                                setIsOpen(false);
                            }}
                            className="cursor-pointer px-3 py-2 text-[15px] hover:bg-surface-2"
                        >
                            <span className="text-muted-foreground">{`#${problem.number} `}</span>
                            {problem.title}
                        </li>
                    ))}
                </ul>
            ) : null}
        </div>
    );
}
